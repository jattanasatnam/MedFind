package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.UiState
import com.example.data.model.Medicine
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.viewmodel.AgentChatMessage
import com.example.ui.viewmodel.AgentTurnState
import com.example.ui.viewmodel.HealthAgentViewModel
import com.example.ui.viewmodel.MedFindViewModel

private val starterPrompts = listOf(
    "Where can I find Paracetamol nearby?",
    "How do fresh vs stale stock badges work?",
    "Questions to ask my pharmacist about antibiotics",
    "How can a medical shopkeeper add medicine stock?"
)

@Composable
fun HealthAgentScreen(
    agentViewModel: HealthAgentViewModel,
    medFindViewModel: MedFindViewModel,
    onFindMedicineInStores: (Medicine) -> Unit,
    onOpenSearch: () -> Unit
) {
    val messages by agentViewModel.messages.collectAsStateWithLifecycle()
    val turnState by agentViewModel.turnState.collectAsStateWithLifecycle()
    val medicinesState by medFindViewModel.verifiedMedicinesState.collectAsStateWithLifecycle()
    val pharmaciesState by medFindViewModel.approvedPharmaciesState.collectAsStateWithLifecycle()
    val profileState by medFindViewModel.userProfileState.collectAsStateWithLifecycle()

    val verifiedMedicines = (medicinesState as? UiState.Success)?.data.orEmpty()
    val approvedPharmacies = (pharmaciesState as? UiState.Success)?.data.orEmpty()
    val locality = (profileState as? UiState.Success)?.data?.preferredLocality.orEmpty()

    var draft by rememberSaveable { mutableStateOf("") }
    var lastSentPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val isGenerating = turnState is AgentTurnState.Loading || turnState is AgentTurnState.Streaming

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun submitPrompt(prompt: String) {
        val cleaned = prompt.trim()
        if (cleaned.isBlank() || isGenerating) return
        lastSentPrompt = cleaned
        draft = ""
        agentViewModel.sendMessage(
            userText = cleaned,
            verifiedMedicines = verifiedMedicines,
            approvedPharmacies = approvedPharmacies,
            userLocality = locality
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.HealthAndSafety,
                                contentDescription = "AI Health Agent",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "AI Health & Pharmacy Guide",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${verifiedMedicines.size} verified medicines • ${approvedPharmacies.size} stores connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (messages.isNotEmpty()) {
                    IconButton(
                        onClick = { agentViewModel.clearChat() },
                        modifier = Modifier.testTag("ai_agent_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Clear conversation"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "agent_disclaimer") {
                    MedicalDisclaimerBanner()
                }
                if (messages.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Ask about medicine discovery & pharmacy stock",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = "I can match medicines in your verified MedFind database, help check nearby verified stores, explain stock freshness labels, or guide shopkeepers through inventory updates. For diagnosis or prescriptions, always consult a licensed doctor.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Try asking:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                starterPrompts.forEachIndexed { idx, prompt ->
                                    OutlinedButton(
                                        onClick = { submitPrompt(prompt) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ai_starter_prompt_$idx"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = prompt,
                                            modifier = Modifier.fillMaxWidth(),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        AgentMessageBubble(
                            message = msg,
                            onSelectMedicine = onFindMedicineInStores
                        )
                    }
                }

                if (turnState is AgentTurnState.Error) {
                    val errorMsg = (turnState as AgentTurnState.Error).message
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_agent_error_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "AI Health Agent Error",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = errorMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (lastSentPrompt.isNotBlank()) {
                                        Button(
                                            onClick = {
                                                agentViewModel.dismissError()
                                                submitPrompt(lastSentPrompt)
                                            }
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                    TextButton(onClick = { agentViewModel.dismissError() }) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onOpenSearch,
                    leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(16.dp)) },
                    label = { Text("Browse All Medicines") }
                )
                if (messages.isNotEmpty()) {
                    starterPrompts.take(2).forEach { quick ->
                        AssistChip(
                            onClick = { submitPrompt(quick) },
                            enabled = !isGenerating,
                            label = { Text(quick) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Ask about medicines, stores, or stock freshness...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submitPrompt(draft) }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_agent_input")
                )
                FilledIconButton(
                    onClick = { submitPrompt(draft) },
                    enabled = draft.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("ai_agent_send_button")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send question to AI Health Agent"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentMessageBubble(
    message: AgentChatMessage,
    onSelectMedicine: (Medicine) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            border = if (message.isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (message.isUser) "You" else "MedFind AI Guide",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (message.isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    if (message.isStreaming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.8.dp
                        )
                    }
                }

                Text(
                    text = message.text.ifBlank { "Thinking..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (!message.isUser && !message.isStreaming && message.matchedMedicines.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Check live stores & Google Maps:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        message.matchedMedicines.forEach { med ->
                            AssistChip(
                                onClick = { onSelectMedicine(med) },
                                leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(15.dp)) },
                                label = { Text("${med.genericName} ${med.strength}") }
                            )
                        }
                    }
                }
            }
        }
    }
}
