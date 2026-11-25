package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.UiState
import com.example.data.firebase.toFriendlyFirestoreMessage
import com.example.data.model.AdminActionRecord
import com.example.data.model.AvailabilityStatus
import com.example.data.model.BOOTSTRAPPED_ADMIN_EMAIL
import com.example.data.model.CatalogueStatus
import com.example.data.model.FeedbackReport
import com.example.data.model.FreshnessTier
import com.example.data.model.GeoAndTimeUtils
import com.example.data.model.InventoryHistoryRecord
import com.example.data.model.InventoryRecord
import com.example.data.model.Medicine
import com.example.data.model.Pharmacy
import com.example.data.model.PharmacyVerificationStatus
import com.example.data.model.ReportStatus
import com.example.data.model.ReportType
import com.example.data.model.UserProfile
import com.example.data.repository.MedFindRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PharmacySortOrder(val label: String) {
    DISTANCE("Nearest"),
    FRESHNESS("Most Recent"),
    STOCK_STATUS("Availability")
}

data class PharmacyInventoryItem(
    val record: InventoryRecord,
    val distanceKm: Double?,
    val freshnessTier: FreshnessTier
)

@OptIn(ExperimentalCoroutinesApi::class)
class MedFindViewModel(
    private val repository: MedFindRepository,
    val currentUserId: String,
    val userEmail: String,
    val userDisplayName: String,
    private val isEmailVerified: Boolean
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(
        userEmail.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) && isEmailVerified
    )
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedMedicine = MutableStateFlow<Medicine?>(null)
    val selectedMedicine: StateFlow<Medicine?> = _selectedMedicine.asStateFlow()

    private val _selectedPharmacy = MutableStateFlow<Pharmacy?>(null)
    val selectedPharmacy: StateFlow<Pharmacy?> = _selectedPharmacy.asStateFlow()

    private val _sortOrder = MutableStateFlow(PharmacySortOrder.DISTANCE)
    val sortOrder: StateFlow<PharmacySortOrder> = _sortOrder.asStateFlow()

    private val _hideStaleReports = MutableStateFlow(false)
    val hideStaleReports: StateFlow<Boolean> = _hideStaleReports.asStateFlow()

    private val _availableOnly = MutableStateFlow(false)
    val availableOnly: StateFlow<Boolean> = _availableOnly.asStateFlow()

    private val _deviceLat = MutableStateFlow<Double?>(null)
    private val _deviceLon = MutableStateFlow<Double?>(null)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, userDisplayName, userEmail)
                .onFailure { e ->
                    Log.w(TAG, "Profile bootstrap failed", e)
                }
            if (!_isAdmin.value) {
                _isAdmin.value = repository.checkIsAdminRecord(currentUserId)
            }
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.toFriendlyFirestoreMessage("Failed to load $label")))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiState("user profile")

    val verifiedMedicinesState: StateFlow<UiState<List<Medicine>>> =
        repository.observeVerifiedMedicines(currentUserId)
            .map { list -> list.sortedBy { it.genericName.lowercase() } }
            .toUiState("medicines")

    val mySubmittedMedicinesState: StateFlow<UiState<List<Medicine>>> =
        repository.observeMySubmittedMedicines(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
            .toUiState("submitted medicines")

    val approvedPharmaciesState: StateFlow<UiState<List<Pharmacy>>> =
        repository.observeApprovedPharmacies(currentUserId)
            .map { list -> list.sortedBy { it.name.lowercase() } }
            .toUiState("pharmacies")

    val myPharmaciesState: StateFlow<UiState<List<Pharmacy>>> =
        repository.observeMyPharmacies(currentUserId)
            .map { list -> list.sortedByDescending { it.updatedAt?.seconds ?: 0L } }
            .toUiState("my pharmacies")

    val myPharmacyInventoryState: StateFlow<UiState<List<InventoryRecord>>> =
        repository.observePharmacyInventory(currentUserId)
            .map { list -> list.sortedByDescending { it.updatedAt?.seconds ?: 0L } }
            .toUiState("pharmacy inventory")

    val myPharmacyHistoryState: StateFlow<UiState<List<InventoryHistoryRecord>>> =
        repository.observePharmacyHistory(currentUserId)
            .map { list -> list.sortedByDescending { it.changedAt?.seconds ?: 0L } }
            .toUiState("inventory history")

    val myReportsState: StateFlow<UiState<List<FeedbackReport>>> =
        repository.observeMyReports(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
            .toUiState("my reports")

    val pharmacyReportsState: StateFlow<UiState<List<FeedbackReport>>> =
        repository.observePharmacyReports(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
            .toUiState("pharmacy reports")

    // Admin-gated streams (only attach queries when verified admin)
    val adminMedicinesState: StateFlow<UiState<List<Medicine>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllMedicinesForAdmin(currentUserId)
            else flowOf(emptyList())
        }.toUiState("admin medicines")

    val adminPharmaciesState: StateFlow<UiState<List<Pharmacy>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllPharmaciesForAdmin(currentUserId)
            else flowOf(emptyList())
        }.toUiState("admin pharmacies")

    val adminReportsState: StateFlow<UiState<List<FeedbackReport>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllReportsForAdmin(currentUserId)
            else flowOf(emptyList())
        }.toUiState("admin reports")

    val adminHistoryState: StateFlow<UiState<List<InventoryHistoryRecord>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllHistoryForAdmin(currentUserId)
            else flowOf(emptyList())
        }.toUiState("admin inventory history")

    val adminInventoryState: StateFlow<UiState<List<InventoryRecord>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllInventoryForAdmin(currentUserId)
            else flowOf(emptyList())
        }.toUiState("admin inventory")

    val adminActionsState: StateFlow<UiState<List<AdminActionRecord>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAdminActions(currentUserId)
            else flowOf(emptyList())
        }.toUiState("admin actions")

    // Selected medicine inventory discovery results (sorted & filtered with freshness)
    val selectedMedicineResultsState: StateFlow<UiState<List<PharmacyInventoryItem>>> =
        _selectedMedicine.flatMapLatest { med ->
            if (med == null) {
                flowOf(emptyList())
            } else {
                combine(
                    repository.observeInventoryForMedicine(currentUserId, med.id),
                    userProfileState,
                    _deviceLat,
                    _deviceLon,
                    _sortOrder,
                    _hideStaleReports,
                    _availableOnly
                ) { values ->
                    @Suppress("UNCHECKED_CAST")
                    val records = values[0] as List<InventoryRecord>
                    @Suppress("UNCHECKED_CAST")
                    val profileState = values[1] as UiState<UserProfile?>
                    val gpsLat = values[2] as Double?
                    val gpsLon = values[3] as Double?
                    val sort = values[4] as PharmacySortOrder
                    val hideStale = values[5] as Boolean
                    val availOnly = values[6] as Boolean

                    val profile = (profileState as? UiState.Success)?.data
                    val refLat = gpsLat ?: profile?.preferredLat
                    val refLon = gpsLon ?: profile?.preferredLon

                    records
                        .map { rec ->
                            val dist = if (refLat != null && refLon != null) {
                                GeoAndTimeUtils.distanceKm(refLat, refLon, rec.pharmacyLat, rec.pharmacyLon)
                            } else null
                            PharmacyInventoryItem(
                                record = rec,
                                distanceKm = dist,
                                freshnessTier = rec.freshnessTier()
                            )
                        }
                        .filter { item ->
                            val status = item.record.availabilityEnum()
                            val passAvail = !availOnly ||
                                (status == AvailabilityStatus.REPORTED_AVAILABLE || status == AvailabilityStatus.LOW_STOCK)
                            val passFresh = !hideStale || item.freshnessTier != FreshnessTier.STALE
                            passAvail && passFresh
                        }
                        .sortedWith(
                            compareBy<PharmacyInventoryItem>(
                                // Stale reports always rank below fresh results unless explicitly sorted otherwise
                                { if (it.freshnessTier == FreshnessTier.STALE) 1 else 0 },
                                {
                                    when (sort) {
                                        PharmacySortOrder.DISTANCE -> it.distanceKm ?: Double.MAX_VALUE
                                        PharmacySortOrder.FRESHNESS -> -(it.record.effectiveTimestamp().seconds.toDouble())
                                        PharmacySortOrder.STOCK_STATUS -> when (it.record.availabilityEnum()) {
                                            AvailabilityStatus.REPORTED_AVAILABLE -> 0.0
                                            AvailabilityStatus.LOW_STOCK -> 1.0
                                            AvailabilityStatus.OUT_OF_STOCK -> 2.0
                                            AvailabilityStatus.PAUSED -> 3.0
                                        }
                                    }
                                },
                                { -(it.record.effectiveTimestamp().seconds) }
                            )
                        )
                }
            }
        }.toUiState("pharmacy results")

    val selectedPharmacyInventoryState: StateFlow<UiState<List<InventoryRecord>>> =
        _selectedPharmacy.flatMapLatest { pharmacy ->
            if (pharmacy == null) flowOf(emptyList())
            else repository.observeInventoryForPharmacy(currentUserId, pharmacy.id)
        }.toUiState("pharmacy stock")

    // --- UI ACTIONS ---

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectMedicine(medicine: Medicine?) {
        _selectedMedicine.value = medicine
        if (medicine != null) {
            val currentRecent = (userProfileState.value as? UiState.Success)?.data?.recentSearches.orEmpty()
            viewModelScope.launch {
                repository.addRecentSearch(currentUserId, medicine.genericName, currentRecent)
            }
        }
    }

    fun selectPharmacy(pharmacy: Pharmacy?) {
        _selectedPharmacy.value = pharmacy
    }

    fun setSortOrder(order: PharmacySortOrder) {
        _sortOrder.value = order
    }

    fun setHideStaleReports(hide: Boolean) {
        _hideStaleReports.value = hide
    }

    fun setAvailableOnly(availOnly: Boolean) {
        _availableOnly.value = availOnly
    }

    fun updateDeviceLocation(lat: Double, lon: Double, localityLabel: String? = null) {
        _deviceLat.value = lat
        _deviceLon.value = lon
        val profile = (userProfileState.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.updateUserLocationPreference(
                uid = currentUserId,
                locality = localityLabel ?: profile?.preferredLocality?.ifBlank { "Current GPS Area" } ?: "Current GPS Area",
                pincode = profile?.preferredPincode ?: "",
                lat = lat,
                lon = lon,
                locationSharingEnabled = true
            )
            _statusMessage.value = "Updated nearby search coordinates."
        }
    }

    fun saveManualLocation(locality: String, pincode: String) {
        val presetCoords = KNOWN_LOCALITY_COORDS.entries.firstOrNull { (key, _) ->
            locality.contains(key, ignoreCase = true) || pincode.trim() == key
        }?.value
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateUserLocationPreference(
                uid = currentUserId,
                locality = locality,
                pincode = pincode,
                lat = presetCoords?.first,
                lon = presetCoords?.second,
                locationSharingEnabled = false
            ).onSuccess {
                _deviceLat.value = presetCoords?.first
                _deviceLon.value = presetCoords?.second
                _statusMessage.value = "Location updated to ${locality.ifBlank { pincode }}."
            }.onFailure {
                _statusMessage.value = it.toFriendlyFirestoreMessage("Failed to save location.")
            }
            _isBusy.value = false
        }
    }

    fun toggleSavedPharmacy(pharmacyId: String) {
        val currentSaved = (userProfileState.value as? UiState.Success)?.data?.savedPharmacyIds.orEmpty()
        viewModelScope.launch {
            repository.toggleSavedPharmacy(currentUserId, pharmacyId, currentSaved)
                .onSuccess {
                    val nowSaved = pharmacyId !in currentSaved
                    _statusMessage.value = if (nowSaved) "Pharmacy saved." else "Removed from saved."
                }
                .onFailure {
                    _statusMessage.value = it.toFriendlyFirestoreMessage("Unable to update saved pharmacies.")
                }
        }
    }

    fun submitMedicine(
        genericName: String,
        brandName: String,
        strength: String,
        dosageForm: String,
        category: String,
        keywordsRaw: String,
        submittedPharmacyId: String? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.submitMedicine(
                genericName = genericName,
                brandName = brandName,
                strength = strength,
                dosageForm = dosageForm,
                category = category,
                keywordsRaw = keywordsRaw,
                submittedPharmacyId = submittedPharmacyId,
                isAdmin = _isAdmin.value
            ).onSuccess {
                _statusMessage.value = if (_isAdmin.value) {
                    "Medicine added to verified catalogue."
                } else {
                    "Medicine submitted for admin catalogue review."
                }
                onComplete()
            }.onFailure {
                _statusMessage.value = it.toFriendlyFirestoreMessage("Could not submit medicine.")
            }
            _isBusy.value = false
        }
    }

    fun registerPharmacy(
        name: String,
        licenseNumber: String,
        address: String,
        locality: String,
        pincode: String,
        phone: String,
        openingHours: String,
        onComplete: () -> Unit = {}
    ) {
        val preset = KNOWN_LOCALITY_COORDS.entries.firstOrNull { (key, _) ->
            locality.contains(key, ignoreCase = true) || pincode.trim() == key
        }?.value
        val lat = _deviceLat.value ?: preset?.first ?: 12.9716
        val lon = _deviceLon.value ?: preset?.second ?: 77.5946

        viewModelScope.launch {
            _isBusy.value = true
            repository.registerPharmacy(
                name = name,
                licenseNumber = licenseNumber,
                address = address,
                locality = locality,
                pincode = pincode,
                phone = phone,
                openingHours = openingHours,
                latitude = lat,
                longitude = lon,
                isAdmin = _isAdmin.value
            ).onSuccess {
                _statusMessage.value = if (_isAdmin.value) {
                    "Pharmacy registered and verified! You can insert stock immediately."
                } else {
                    "Pharmacy registered and awaiting administrator verification."
                }
                onComplete()
            }.onFailure {
                _statusMessage.value = it.toFriendlyFirestoreMessage("Pharmacy registration failed.")
            }
            _isBusy.value = false
        }
    }

    fun updatePharmacyProfile(
        pharmacyId: String,
        name: String,
        address: String,
        locality: String,
        pincode: String,
        phone: String,
        openingHours: String,
        onComplete: () -> Unit = {}
    ) {
        val preset = KNOWN_LOCALITY_COORDS.entries.firstOrNull { (key, _) ->
            locality.contains(key, ignoreCase = true) || pincode.trim() == key
        }?.value
        viewModelScope.launch {
            _isBusy.value = true
            repository.updatePharmacyProfile(
                pharmacyId = pharmacyId,
                name = name,
                address = address,
                locality = locality,
                pincode = pincode,
                phone = phone,
                openingHours = openingHours,
                latitude = preset?.first,
                longitude = preset?.second
            ).onSuccess {
                _statusMessage.value = "Pharmacy profile updated."
                onComplete()
            }.onFailure {
                _statusMessage.value = it.toFriendlyFirestoreMessage("Failed to update pharmacy profile.")
            }
            _isBusy.value = false
        }
    }

    fun upsertInventory(
        pharmacy: Pharmacy,
        medicine: Medicine,
        availabilityStatus: AvailabilityStatus,
        quantity: Long,
        unit: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.upsertInventoryWithHistory(
                pharmacy = pharmacy,
                medicine = medicine,
                availabilityStatus = availabilityStatus,
                quantity = quantity,
                unit = unit,
                staffDisplayName = userDisplayName,
                isAdmin = _isAdmin.value
            ).onSuccess {
                _statusMessage.value = "Inventory updated with live timestamp."
                onComplete()
            }.onFailure {
                _statusMessage.value = it.toFriendlyFirestoreMessage("Inventory update failed.")
            }
            _isBusy.value = false
        }
    }

    fun submitFeedbackReport(
        inventory: InventoryRecord,
        reportType: ReportType,
        message: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.submitFeedbackReport(
                inventory = inventory,
                reportType = reportType,
                message = message,
                reporterName = userDisplayName
            ).onSuccess {
                _statusMessage.value = "Stock report sent for administrator review."
                onComplete()
            }.onFailure {
                _statusMessage.value = it.toFriendlyFirestoreMessage("Failed to submit report.")
            }
            _isBusy.value = false
        }
    }

    fun moderatePharmacy(
        pharmacyId: String,
        newStatus: PharmacyVerificationStatus,
        note: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.moderatePharmacy(pharmacyId, newStatus, note, userEmail)
                .onSuccess { _statusMessage.value = "Pharmacy status set to ${newStatus.name}." }
                .onFailure { _statusMessage.value = it.toFriendlyFirestoreMessage("Moderation failed.") }
            _isBusy.value = false
        }
    }

    fun moderateMedicine(medicineId: String, newStatus: CatalogueStatus, reason: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.moderateMedicine(medicineId, newStatus, userEmail, reason)
                .onSuccess { _statusMessage.value = "Medicine catalogue updated." }
                .onFailure { _statusMessage.value = it.toFriendlyFirestoreMessage("Catalogue action failed.") }
            _isBusy.value = false
        }
    }

    fun resolveReport(reportId: String, newStatus: ReportStatus, note: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.resolveFeedbackReport(reportId, newStatus, note, userEmail)
                .onSuccess { _statusMessage.value = "Report marked ${newStatus.label}." }
                .onFailure { _statusMessage.value = it.toFriendlyFirestoreMessage("Could not update report.") }
            _isBusy.value = false
        }
    }

    fun seedPilotData() {
        viewModelScope.launch {
            _isBusy.value = true
            repository.seedPilotData(userEmail, userDisplayName)
                .onSuccess { _statusMessage.value = "Pilot medicines, pharmacies, and stock records synced to Firestore." }
                .onFailure { _statusMessage.value = it.toFriendlyFirestoreMessage("Pilot seed failed.") }
            _isBusy.value = false
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private companion object {
        const val TAG = "MedFindVM"
        const val STOP_TIMEOUT_MILLIS = 5000L

        val KNOWN_LOCALITY_COORDS = mapOf(
            "Indiranagar" to (12.9784 to 77.6408),
            "560038" to (12.9784 to 77.6408),
            "Koramangala" to (12.9352 to 77.6245),
            "560034" to (12.9352 to 77.6245),
            "Jayanagar" to (12.9250 to 77.5938),
            "560011" to (12.9250 to 77.5938),
            "HSR Layout" to (12.9121 to 77.6446),
            "560102" to (12.9121 to 77.6446),
            "Whitefield" to (12.9698 to 77.7500),
            "560066" to (12.9698 to 77.7500)
        )
    }
}
