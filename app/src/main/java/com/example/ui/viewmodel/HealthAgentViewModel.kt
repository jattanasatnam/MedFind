package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Medicine
import com.example.data.model.Pharmacy
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

sealed interface AgentTurnState {
    data object Idle : AgentTurnState
    data object Loading : AgentTurnState
    data class Streaming(val text: String) : AgentTurnState
    data class Error(val message: String) : AgentTurnState
}

data class AgentChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val matchedMedicines: List<Medicine> = emptyList()
)

class HealthAgentViewModel : ViewModel() {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are the MedFind AI Health & Pharmacy Guide inside an Android medicine availability and pharmacy discovery app.
                    Your role:
                    1. Help customers identify which medicine entries in the verified MedFind catalogue match their search (by generic name, brand name, dosage form, or category) and explain how to check nearby pharmacies and open Google Maps directions.
                    2. Provide clear, plain-language, general educational information about medicine forms (e.g., tablet vs capsule vs inhaler), proper storage, and helpful questions to ask a pharmacist or doctor.
                    3. Explain MedFind's stock freshness policy (Fresh <= 6h, Recent <= 24h, Stale > 24h) and remind users to call the pharmacy to confirm stock before travelling.
                    4. Explain how medical shopkeepers can register their pharmacy, wait for admin verification, add medicines to the catalogue, and publish live inventory updates with timestamps.
                    
                    Strict safety rules:
                    - NEVER diagnose illnesses, prescribe treatments, recommend personal dosages, or tell a user to stop/start a medication.
                    - NEVER guarantee that a medicine is physically in stock at a pharmacy; always present reported availability with a reminder to confirm by phone.
                    - For personal symptoms, drug interactions, allergies, pregnancy/pediatric questions, or urgent symptoms, clearly advise consulting a licensed physician, pharmacist, or local emergency services.
                    - Keep answers concise, structured, and easy to read on a mobile screen.
                    """.trimIndent()
                )
            }
        )
    }

    private var chatSession = model.startChat()

    private val _messages = MutableStateFlow<List<AgentChatMessage>>(emptyList())
    val messages: StateFlow<List<AgentChatMessage>> = _messages.asStateFlow()

    private val _turnState = MutableStateFlow<AgentTurnState>(AgentTurnState.Idle)
    val turnState: StateFlow<AgentTurnState> = _turnState.asStateFlow()

    fun sendMessage(
        userText: String,
        verifiedMedicines: List<Medicine>,
        approvedPharmacies: List<Pharmacy>,
        userLocality: String
    ) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return
        if (_turnState.value is AgentTurnState.Loading || _turnState.value is AgentTurnState.Streaming) return

        val userMsg = AgentChatMessage(isUser = true, text = trimmed)
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderAssistant = AgentChatMessage(
            id = assistantMsgId,
            isUser = false,
            text = "",
            isStreaming = true
        )
        _messages.value = _messages.value + userMsg + placeholderAssistant
        _turnState.value = AgentTurnState.Loading

        val catalogSnapshot = buildString {
            appendLine("User's current discovery area: ${userLocality.ifBlank { "Not specified" }}")
            if (verifiedMedicines.isNotEmpty()) {
                appendLine("Verified medicines in MedFind database (${verifiedMedicines.size}):")
                verifiedMedicines.take(20).forEach { m ->
                    appendLine("- ${m.genericName} (${m.brandName}) ${m.strength}, ${m.dosageForm}, Category: ${m.category}")
                }
            } else {
                appendLine("Verified medicines in MedFind database: currently 0 (Admin can populate pilot data or shopkeepers can submit medicines).")
            }
            if (approvedPharmacies.isNotEmpty()) {
                appendLine("Verified participating pharmacies (${approvedPharmacies.size}):")
                approvedPharmacies.take(12).forEach { p ->
                    appendLine("- ${p.name} in ${p.locality} (${p.pincode}), Hours: ${p.openingHours}, Address: ${p.address}")
                }
            }
        }

        val groundedPrompt = """
            [Live MedFind Context]
            $catalogSnapshot
            
            [User Message]
            $trimmed
        """.trimIndent()

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    chatSession.sendMessageStream(groundedPrompt).collect { chunk ->
                        val part = chunk.text.orEmpty()
                        if (part.isNotEmpty()) {
                            accumulated.append(part)
                            val partial = accumulated.toString()
                            _turnState.value = AgentTurnState.Streaming(partial)
                            updateAssistantMessage(
                                id = assistantMsgId,
                                text = partial,
                                isStreaming = true,
                                matchedMedicines = emptyList()
                            )
                        }
                    }
                }

                val finalText = accumulated.toString().trim()
                if (finalText.isNotEmpty()) {
                    val matched = findMentionedMedicines(
                        query = trimmed,
                        response = finalText,
                        catalogue = verifiedMedicines
                    )
                    updateAssistantMessage(
                        id = assistantMsgId,
                        text = finalText,
                        isStreaming = false,
                        matchedMedicines = matched
                    )
                    _turnState.value = AgentTurnState.Idle
                } else {
                    removeMessage(assistantMsgId)
                    _turnState.value = AgentTurnState.Error("The AI Health Agent returned an empty response. Please try again.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Health Agent request timed out", e)
                if (accumulated.isBlank()) removeMessage(assistantMsgId)
                else finalizeStreamingMessage(assistantMsgId)
                _turnState.value = AgentTurnState.Error("Request timed out after 60 seconds. Please check your connection and try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Health Agent inference failed", e)
                if (accumulated.isBlank()) removeMessage(assistantMsgId)
                else finalizeStreamingMessage(assistantMsgId)
                _turnState.value = AgentTurnState.Error(
                    e.localizedMessage ?: "Unable to reach AI Health Agent right now."
                )
            }
        }
    }

    fun clearChat() {
        chatSession = model.startChat()
        _messages.value = emptyList()
        _turnState.value = AgentTurnState.Idle
    }

    fun dismissError() {
        if (_turnState.value is AgentTurnState.Error) {
            _turnState.value = AgentTurnState.Idle
        }
    }

    private fun updateAssistantMessage(
        id: String,
        text: String,
        isStreaming: Boolean,
        matchedMedicines: List<Medicine>
    ) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == id) {
                msg.copy(
                    text = text,
                    isStreaming = isStreaming,
                    matchedMedicines = matchedMedicines
                )
            } else {
                msg
            }
        }
    }

    private fun finalizeStreamingMessage(id: String) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == id) msg.copy(isStreaming = false) else msg
        }
    }

    private fun removeMessage(id: String) {
        _messages.value = _messages.value.filterNot { it.id == id }
    }

    private fun findMentionedMedicines(
        query: String,
        response: String,
        catalogue: List<Medicine>
    ): List<Medicine> {
        val combined = "$query $response".lowercase()
        return catalogue.filter { med ->
            med.genericName.lowercase() in combined || med.brandName.lowercase() in combined
        }.take(3)
    }

    private companion object {
        const val TAG = "HealthAgentVM"
    }
}
