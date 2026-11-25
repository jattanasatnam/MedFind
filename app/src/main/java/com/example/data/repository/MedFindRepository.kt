package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.FirestoreDatabaseHelper
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AccountStatus
import com.example.data.model.AdminActionRecord
import com.example.data.model.AvailabilityStatus
import com.example.data.model.BOOTSTRAPPED_ADMIN_EMAIL
import com.example.data.model.CatalogueStatus
import com.example.data.model.FeedbackReport
import com.example.data.model.InventoryHistoryRecord
import com.example.data.model.InventoryRecord
import com.example.data.model.Medicine
import com.example.data.model.Pharmacy
import com.example.data.model.PharmacyVerificationStatus
import com.example.data.model.ReportStatus
import com.example.data.model.ReportType
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class MedFindRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    val crudHelper: FirestoreDatabaseHelper = FirestoreDatabaseHelper(db, auth)

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> Flow<T>.logFirestoreErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // --- USER PROFILE & PREFERENCES ---

    suspend fun ensureUserProfile(
        uid: String,
        displayName: String?,
        email: String?
    ): Result<Unit> = crudHelper.createUserProfileIfMissing(uid, displayName, email)

    suspend fun getUserProfile(uid: String): Result<UserProfile?> =
        crudHelper.getUserProfile(uid)

    fun observeUserProfile(uid: String): Flow<UserProfile?> =
        crudHelper.observeUserProfile(uid)

    fun observeAllUsersForAdmin(uid: String): Flow<List<UserProfile>> =
        crudHelper.observeAllUsersForAdmin(uid)

    suspend fun checkIsAdminRecord(uid: String): Boolean {
        return try {
            db.collection("admins").document(uid).get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateUserLocationPreference(
        uid: String,
        locality: String,
        pincode: String,
        lat: Double?,
        lon: Double?,
        locationSharingEnabled: Boolean
    ): Result<Unit> = crudHelper.updateUserPreferences(
        uid = uid,
        locality = locality,
        pincode = pincode,
        lat = lat,
        lon = lon,
        locationSharingEnabled = locationSharingEnabled
    )

    suspend fun updateUserRoleAndStatus(
        targetUserId: String,
        newRole: UserRole,
        newStatus: AccountStatus,
        reason: String
    ): Result<Unit> = crudHelper.updateUserRoleAndStatusByAdmin(targetUserId, newRole, newStatus, reason)

    suspend fun deleteUserProfile(
        targetUserId: String,
        reason: String
    ): Result<Unit> = crudHelper.deleteUserProfileByAdmin(targetUserId, reason)

    suspend fun addRecentSearch(uid: String, query: String, currentRecent: List<String>): Result<Unit> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Authenticated user mismatch." }
            val cleaned = query.trim().take(80)
            if (cleaned.length < 2) return Result.success(Unit)
            val updated = (listOf(cleaned) + currentRecent.filterNot { it.equals(cleaned, ignoreCase = true) })
                .take(10)
            db.collection("users").document(uid).update(
                mapOf(
                    "recentSearches" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleSavedPharmacy(
        uid: String,
        pharmacyId: String,
        currentSaved: List<String>
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Authenticated user mismatch." }
            val updated = if (pharmacyId in currentSaved) {
                currentSaved - pharmacyId
            } else {
                (currentSaved + pharmacyId).distinct().take(50)
            }
            db.collection("users").document(uid).update(
                mapOf(
                    "savedPharmacyIds" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- MEDICINES CATALOGUE ---

    fun observeVerifiedMedicines(uid: String): Flow<List<Medicine>> =
        crudHelper.observeVerifiedMedicines(uid)

    fun observeMySubmittedMedicines(uid: String): Flow<List<Medicine>> =
        crudHelper.observeMySubmittedMedicines(uid)

    fun observeAllMedicinesForAdmin(uid: String): Flow<List<Medicine>> =
        crudHelper.observeAllMedicinesForAdmin(uid)

    suspend fun getMedicine(medicineId: String): Result<Medicine?> =
        crudHelper.getMedicine(medicineId)

    suspend fun submitMedicine(
        genericName: String,
        brandName: String,
        strength: String,
        dosageForm: String,
        category: String,
        keywordsRaw: String,
        submittedPharmacyId: String? = null,
        isAdmin: Boolean
    ): Result<Unit> = crudHelper.createMedicine(
        genericName = genericName,
        brandName = brandName,
        strength = strength,
        dosageForm = dosageForm,
        category = category,
        keywordsRaw = keywordsRaw,
        submittedPharmacyId = submittedPharmacyId
    ).map { }

    suspend fun moderateMedicine(
        medicineId: String,
        newStatus: CatalogueStatus,
        adminEmail: String,
        reason: String
    ): Result<Unit> = crudHelper.updateMedicineByAdmin(
        medicineId = medicineId,
        newStatus = newStatus,
        reason = reason
    )

    suspend fun updateMedicineDetails(
        medicineId: String,
        genericName: String,
        brandName: String,
        strength: String,
        dosageForm: String,
        category: String,
        keywordsRaw: String,
        newStatus: CatalogueStatus,
        reason: String
    ): Result<Unit> = crudHelper.updateMedicineByAdmin(
        medicineId = medicineId,
        newStatus = newStatus,
        reason = reason,
        genericName = genericName,
        brandName = brandName,
        strength = strength,
        dosageForm = dosageForm,
        category = category,
        keywordsRaw = keywordsRaw
    )

    suspend fun deleteMedicine(medicineId: String, reason: String): Result<Unit> =
        crudHelper.deleteMedicineByAdmin(medicineId, reason)

    // --- PHARMACIES ---

    fun observeApprovedPharmacies(uid: String): Flow<List<Pharmacy>> =
        crudHelper.observeApprovedPharmacies(uid)

    fun observeMyPharmacies(uid: String): Flow<List<Pharmacy>> =
        crudHelper.observeMyPharmacies(uid)

    fun observeAllPharmaciesForAdmin(uid: String): Flow<List<Pharmacy>> =
        crudHelper.observeAllPharmaciesForAdmin(uid)

    suspend fun getPharmacy(pharmacyId: String): Result<Pharmacy?> =
        crudHelper.getPharmacy(pharmacyId)

    suspend fun registerPharmacy(
        name: String,
        licenseNumber: String,
        address: String,
        locality: String,
        pincode: String,
        phone: String,
        openingHours: String,
        latitude: Double,
        longitude: Double,
        isAdmin: Boolean = false
    ): Result<Unit> = crudHelper.createPharmacy(
        name = name,
        licenseNumber = licenseNumber,
        address = address,
        locality = locality,
        pincode = pincode,
        phone = phone,
        openingHours = openingHours,
        latitude = latitude,
        longitude = longitude
    ).map { }

    suspend fun updatePharmacyProfile(
        pharmacyId: String,
        name: String,
        address: String,
        locality: String,
        pincode: String,
        phone: String,
        openingHours: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<Unit> = crudHelper.updatePharmacyProfile(
        pharmacyId = pharmacyId,
        name = name,
        address = address,
        locality = locality,
        pincode = pincode,
        phone = phone,
        openingHours = openingHours,
        latitude = latitude,
        longitude = longitude
    )

    suspend fun moderatePharmacy(
        pharmacyId: String,
        newStatus: PharmacyVerificationStatus,
        adminNote: String,
        adminEmail: String
    ): Result<Unit> = crudHelper.moderatePharmacyByAdmin(
        pharmacyId = pharmacyId,
        newStatus = newStatus,
        adminNote = adminNote
    )

    suspend fun deletePharmacy(pharmacyId: String, reason: String): Result<Unit> =
        crudHelper.deletePharmacyByAdmin(pharmacyId, reason)

    // --- INVENTORY & ATOMIC HISTORY ---

    fun observeInventoryForMedicine(uid: String, medicineId: String): Flow<List<InventoryRecord>> =
        crudHelper.observeInventoryForMedicine(uid, medicineId)

    fun observeInventoryForPharmacy(uid: String, pharmacyId: String): Flow<List<InventoryRecord>> =
        crudHelper.observeInventoryForPharmacy(uid, pharmacyId)

    fun observePharmacyInventory(uid: String): Flow<List<InventoryRecord>> =
        crudHelper.observePharmacyInventory(uid)

    fun observeAllInventoryForAdmin(uid: String): Flow<List<InventoryRecord>> =
        crudHelper.observeAllInventoryForAdmin(uid)

    suspend fun getInventoryItem(pharmacyId: String, medicineId: String): Result<InventoryRecord?> =
        crudHelper.getInventoryItem(pharmacyId, medicineId)

    suspend fun upsertInventoryWithHistory(
        pharmacy: Pharmacy,
        medicine: Medicine,
        availabilityStatus: AvailabilityStatus,
        quantity: Long,
        unit: String,
        staffDisplayName: String,
        isAdmin: Boolean = false
    ): Result<Unit> = crudHelper.upsertInventoryWithHistory(
        pharmacy = pharmacy,
        medicine = medicine,
        availabilityStatus = availabilityStatus,
        quantity = quantity,
        unit = unit,
        staffDisplayName = staffDisplayName
    )

    suspend fun deleteInventoryItem(inventoryId: String, reason: String): Result<Unit> =
        crudHelper.deleteInventoryByAdmin(inventoryId, reason)

    fun observePharmacyHistory(uid: String): Flow<List<InventoryHistoryRecord>> =
        db.collection("inventory_history")
            .whereEqualTo("ownerUserId", uid)
            .snapshots()
            .map { it.toObjects(InventoryHistoryRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "inventory_history", uid)

    fun observeAllHistoryForAdmin(uid: String): Flow<List<InventoryHistoryRecord>> =
        db.collection("inventory_history")
            .snapshots()
            .map { it.toObjects(InventoryHistoryRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "inventory_history", uid)

    // --- FEEDBACK REPORTS ---

    fun observeMyReports(uid: String): Flow<List<FeedbackReport>> =
        db.collection("feedback_reports")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { it.toObjects(FeedbackReport::class.java, ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "feedback_reports", uid)

    fun observePharmacyReports(uid: String): Flow<List<FeedbackReport>> =
        db.collection("feedback_reports")
            .whereEqualTo("pharmacyOwnerUserId", uid)
            .snapshots()
            .map { it.toObjects(FeedbackReport::class.java, ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "feedback_reports", uid)

    fun observeAllReportsForAdmin(uid: String): Flow<List<FeedbackReport>> =
        db.collection("feedback_reports")
            .snapshots()
            .map { it.toObjects(FeedbackReport::class.java, ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "feedback_reports", uid)

    suspend fun submitFeedbackReport(
        inventory: InventoryRecord,
        reportType: ReportType,
        message: String,
        reporterName: String
    ): Result<Unit> {
        var path = "feedback_reports"
        return try {
            val uid = requireUserId()
            require(inventory.id.isNotBlank()) { "Inventory listing is required." }
            val cleanMessage = message.trim().take(500)
            val cleanReporter = reporterName.trim().take(80).ifBlank { "MedFind User" }
            val docRef = db.collection("feedback_reports").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "reporterName" to cleanReporter,
                    "pharmacyId" to inventory.pharmacyId,
                    "pharmacyName" to inventory.pharmacyName,
                    "inventoryId" to inventory.id,
                    "medicineId" to inventory.medicineId,
                    "medicineName" to inventory.medicineName,
                    "pharmacyOwnerUserId" to inventory.ownerUserId,
                    "reportType" to reportType.name,
                    "message" to cleanMessage,
                    "status" to ReportStatus.OPEN.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveFeedbackReport(
        reportId: String,
        newStatus: ReportStatus,
        resolutionNote: String,
        adminEmail: String
    ): Result<Unit> {
        val path = "feedback_reports/$reportId"
        return try {
            val uid = requireUserId()
            require(reportId.isNotBlank()) { "Report ID required." }
            val cleanNote = resolutionNote.trim().take(300)
            val batch = db.batch()
            val repRef = db.collection("feedback_reports").document(reportId)
            batch.update(
                repRef,
                mapOf(
                    "status" to newStatus.name,
                    "resolutionNote" to cleanNote,
                    "reviewedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val actionRef = db.collection("admin_actions").document()
            batch.set(
                actionRef,
                mapOf(
                    "adminUserId" to uid,
                    "adminEmail" to adminEmail.ifBlank { BOOTSTRAPPED_ADMIN_EMAIL }.take(120),
                    "targetType" to "REPORT",
                    "targetId" to reportId,
                    "action" to "REPORT_${newStatus.name}",
                    "reason" to cleanNote,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeAdminActions(uid: String): Flow<List<AdminActionRecord>> =
        db.collection("admin_actions")
            .snapshots()
            .map { it.toObjects(AdminActionRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "admin_actions", uid)

    // --- ADMIN PILOT SEEDING (Real Persistent Firestore Records) ---

    suspend fun seedPilotData(adminEmail: String, adminName: String): Result<Unit> {
        val path = "medicines"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            val now = FieldValue.serverTimestamp()
            val staleTime = Timestamp(Date(System.currentTimeMillis() - 36L * 3600L * 1000L))

            data class SeedMed(
                val id: String,
                val generic: String,
                val brand: String,
                val strength: String,
                val form: String,
                val category: String,
                val keywords: List<String>
            )

            val meds = listOf(
                SeedMed("med_paracetamol_650", "Paracetamol", "Dolo 650", "650 mg", "Tablet", "Fever & Pain", listOf("paracetamol", "acetaminophen", "dolo", "fever")),
                SeedMed("med_amoxicillin_500", "Amoxicillin", "Mox 500", "500 mg", "Capsule", "Antibiotic", listOf("amoxicillin", "mox", "antibiotic")),
                SeedMed("med_salbutamol_100", "Salbutamol", "Asthalin", "100 mcg", "Inhaler", "Respiratory", listOf("salbutamol", "asthalin", "inhaler", "asthma")),
                SeedMed("med_metformin_500", "Metformin", "Glycomet", "500 mg", "Tablet", "Diabetes Care", listOf("metformin", "glycomet", "diabetes")),
                SeedMed("med_omeprazole_20", "Omeprazole", "Omez", "20 mg", "Capsule", "Digestive", listOf("omeprazole", "omez", "acidity")),
                SeedMed("med_cetirizine_10", "Cetirizine", "Cetzine", "10 mg", "Tablet", "Allergy", listOf("cetirizine", "cetzine", "allergy")),
                SeedMed("med_azithromycin_500", "Azithromycin", "Azithral", "500 mg", "Tablet", "Antibiotic", listOf("azithromycin", "azithral", "antibiotic")),
                SeedMed("med_atorvastatin_10", "Atorvastatin", "Atorva", "10 mg", "Tablet", "Cardiac Care", listOf("atorvastatin", "atorva", "cholesterol"))
            )

            meds.forEach { m ->
                batch.set(
                    db.collection("medicines").document(m.id),
                    mapOf(
                        "genericName" to m.generic,
                        "brandName" to m.brand,
                        "strength" to m.strength,
                        "dosageForm" to m.form,
                        "category" to m.category,
                        "keywords" to m.keywords,
                        "catalogueStatus" to CatalogueStatus.VERIFIED.name,
                        "submittedBy" to uid,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
            }

            data class SeedPharm(
                val id: String,
                val name: String,
                val lic: String,
                val addr: String,
                val loc: String,
                val pin: String,
                val lat: Double,
                val lon: Double,
                val phone: String,
                val hours: String,
                val status: PharmacyVerificationStatus
            )

            val pharmacies = listOf(
                SeedPharm("pharm_careplus_ind", "CarePlus Community Pharmacy", "KA-BLR-1042", "100 Feet Road, HAL 2nd Stage, Indiranagar", "Indiranagar", "560038", 12.9784, 77.6408, "+918041122334", "07:30 AM - 11:00 PM", PharmacyVerificationStatus.APPROVED),
                SeedPharm("pharm_medlife_kora", "GreenCross 24x7 Pharmacy", "KA-BLR-2188", "80 Feet Road, 4th Block, Koramangala", "Koramangala", "560034", 12.9352, 77.6245, "+918042233445", "Open 24 Hours", PharmacyVerificationStatus.APPROVED),
                SeedPharm("pharm_wellness_jay", "Wellness Square Chemist", "KA-BLR-3095", "11th Main Road, 4th Block, Jayanagar", "Jayanagar", "560011", 12.9250, 77.5938, "+918043344556", "08:00 AM - 10:30 PM", PharmacyVerificationStatus.APPROVED),
                SeedPharm("pharm_sunrise_hsr", "Sunrise Apothecary", "KA-BLR-4410", "27th Main Road, Sector 1, HSR Layout", "HSR Layout", "560102", 12.9121, 77.6446, "+918045566778", "08:30 AM - 10:00 PM", PharmacyVerificationStatus.PENDING)
            )

            pharmacies.forEach { p ->
                batch.set(
                    db.collection("pharmacies").document(p.id),
                    mapOf(
                        "ownerUserId" to uid,
                        "name" to p.name,
                        "licenseNumber" to p.lic,
                        "address" to p.addr,
                        "locality" to p.loc,
                        "pincode" to p.pin,
                        "latitude" to p.lat,
                        "longitude" to p.lon,
                        "phone" to p.phone,
                        "openingHours" to p.hours,
                        "verificationStatus" to p.status.name,
                        "adminNote" to if (p.status == PharmacyVerificationStatus.APPROVED) "Pilot verified" else "Awaiting inspection",
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
            }

            fun addInv(
                pharm: SeedPharm,
                med: SeedMed,
                status: AvailabilityStatus,
                qty: Long,
                unit: String,
                isStaleDemo: Boolean = false
            ) {
                val invId = "${pharm.id}_${med.id}"
                val ts = if (isStaleDemo) staleTime else now
                batch.set(
                    db.collection("inventory").document(invId),
                    mapOf(
                        "pharmacyId" to pharm.id,
                        "pharmacyName" to pharm.name,
                        "pharmacyAddress" to pharm.addr,
                        "pharmacyLocality" to pharm.loc,
                        "pharmacyPincode" to pharm.pin,
                        "pharmacyPhone" to pharm.phone,
                        "pharmacyHours" to pharm.hours,
                        "pharmacyLat" to pharm.lat,
                        "pharmacyLon" to pharm.lon,
                        "ownerUserId" to uid,
                        "pharmacyVerified" to true,
                        "medicineId" to med.id,
                        "medicineName" to "${med.generic} ${med.strength} (${med.brand})",
                        "genericName" to med.generic,
                        "brandName" to med.brand,
                        "strength" to med.strength,
                        "dosageForm" to med.form,
                        "availabilityStatus" to status.name,
                        "quantity" to qty,
                        "unit" to unit,
                        "updatedBy" to uid,
                        "updatedByName" to adminName.ifBlank { "Pilot Admin" }.take(80),
                        "createdAt" to ts,
                        "updatedAt" to ts
                    )
                )
            }

            // Rich realistic inventory across approved pharmacies
            addInv(pharmacies[0], meds[0], AvailabilityStatus.REPORTED_AVAILABLE, 64, "strips")
            addInv(pharmacies[1], meds[0], AvailabilityStatus.LOW_STOCK, 6, "strips")
            addInv(pharmacies[2], meds[0], AvailabilityStatus.REPORTED_AVAILABLE, 28, "strips", isStaleDemo = true)

            addInv(pharmacies[0], meds[1], AvailabilityStatus.LOW_STOCK, 4, "strips")
            addInv(pharmacies[1], meds[1], AvailabilityStatus.REPORTED_AVAILABLE, 32, "strips")

            addInv(pharmacies[0], meds[2], AvailabilityStatus.REPORTED_AVAILABLE, 11, "inhalers")
            addInv(pharmacies[2], meds[2], AvailabilityStatus.OUT_OF_STOCK, 0, "inhalers")

            addInv(pharmacies[0], meds[3], AvailabilityStatus.REPORTED_AVAILABLE, 40, "strips")
            addInv(pharmacies[1], meds[4], AvailabilityStatus.REPORTED_AVAILABLE, 22, "strips")
            addInv(pharmacies[2], meds[5], AvailabilityStatus.REPORTED_AVAILABLE, 55, "strips")
            addInv(pharmacies[1], meds[6], AvailabilityStatus.LOW_STOCK, 5, "strips")
            addInv(pharmacies[0], meds[7], AvailabilityStatus.REPORTED_AVAILABLE, 19, "strips")

            batch.set(
                db.collection("admin_actions").document(),
                mapOf(
                    "adminUserId" to uid,
                    "adminEmail" to adminEmail.ifBlank { BOOTSTRAPPED_ADMIN_EMAIL }.take(120),
                    "targetType" to "SEED",
                    "targetId" to "pilot_catalogue_v1",
                    "action" to "SEED_PILOT_DATA",
                    "reason" to "Initialized verified pilot medicines, pharmacies, and stock timestamps.",
                    "createdAt" to now
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
