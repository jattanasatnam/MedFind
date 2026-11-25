package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AccountStatus
import com.example.data.model.AvailabilityStatus
import com.example.data.model.BOOTSTRAPPED_ADMIN_EMAIL
import com.example.data.model.CatalogueStatus
import com.example.data.model.GeoAndTimeUtils.sanitizeFirestoreMap
import com.example.data.model.InventoryRecord
import com.example.data.model.Medicine
import com.example.data.model.Pharmacy
import com.example.data.model.PharmacyVerificationStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.safeEnum
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

/**
 * Role-enforced Firestore CRUD helper for MedFind's core collections:
 * `users`, `pharmacies`, `medicines`, and `inventory`.
 *
 * Security model (client preflight + server-side `firestore.rules` parity):
 * 1. Requires a signed-in Firebase user (never anonymous or unauthenticated).
 * 2. Resolves caller role (`CUSTOMER`, `PHARMACY`, `ADMIN`) and account status (`ACTIVE`, `SUSPENDED`)
 *    from verified auth claims, `/admins/{uid}`, `/users/{uid}`, and approved `/pharmacies`.
 * 3. Blocks self-escalation (customers cannot assign themselves `PHARMACY`/`ADMIN` or `APPROVED`),
 *    blocks cross-tenant writes, validates all field bounds, uses `FieldValue.serverTimestamp()`,
 *    and records immutable audit trails in `inventory_history` and `admin_actions`.
 */
class FirestoreDatabaseHelper(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireSignedInUser(): FirebaseUser =
        auth.currentUser
            ?: throw SecurityException("Authentication required: please sign in with Google first.")

    private fun requireSignedInUid(): String = requireSignedInUser().uid

    private fun <T> Flow<T>.wrapFirestoreFlow(
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

    /**
     * Resolves verified role & account state for the active user before privileged CRUD operations.
     */
    suspend fun resolveCallerContext(): CallerRoleContext {
        val user = requireSignedInUser()
        val uid = user.uid
        val email = user.email.orEmpty()
        val isVerifiedAdminEmail =
            email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified

        val hasAdminDoc = if (isVerifiedAdminEmail) {
            true
        } else {
            runCatching {
                db.collection(FirestoreCollections.ADMINS).document(uid).get().await().exists()
            }.getOrDefault(false)
        }

        val userSnap = runCatching {
            db.collection(FirestoreCollections.USERS).document(uid).get().await()
        }.getOrNull()

        val accountStatus = if (userSnap != null && userSnap.exists()) {
            safeEnum(userSnap.getString(UserFields.STATUS), AccountStatus.ACTIVE)
        } else {
            AccountStatus.ACTIVE
        }

        val hasApprovedPharmacy = runCatching {
            db.collection(FirestoreCollections.PHARMACIES)
                .whereEqualTo(PharmacyFields.OWNER_USER_ID, uid)
                .whereEqualTo(PharmacyFields.VERIFICATION_STATUS, PharmacyVerificationStatus.APPROVED.name)
                .limit(1)
                .get()
                .await()
                .isEmpty
                .not()
        }.getOrDefault(false)

        val effectiveRole = when {
            hasAdminDoc -> UserRole.ADMIN
            hasApprovedPharmacy -> UserRole.PHARMACY
            else -> UserRole.CUSTOMER
        }

        return CallerRoleContext(
            uid = uid,
            email = email,
            isEmailVerified = user.isEmailVerified,
            effectiveRole = effectiveRole,
            accountStatus = accountStatus,
            hasApprovedPharmacy = hasApprovedPharmacy
        )
    }

    private fun buildAdminAuditMap(
        caller: CallerRoleContext,
        targetType: String,
        targetId: String,
        action: String,
        reason: String
    ): Map<String, Any> = mapOf(
        "adminUserId" to caller.uid,
        "adminEmail" to caller.email.ifBlank { BOOTSTRAPPED_ADMIN_EMAIL }.take(120),
        "targetType" to targetType,
        "targetId" to targetId.take(128),
        "action" to action.take(80),
        "reason" to reason.trim().take(300),
        "createdAt" to FieldValue.serverTimestamp()
    )

    // =========================================================================
    // 1. USERS COLLECTION CRUD (`/users/{userId}`)
    // =========================================================================

    /**
     * CREATE: Initializes the signed-in user's profile with safe default CUSTOMER role & ACTIVE status.
     */
    suspend fun createUserProfileIfMissing(
        uid: String,
        displayName: String?,
        email: String?,
        defaultLocality: String = "Indiranagar",
        defaultPincode: String = "560038",
        defaultLat: Double = 12.9784,
        defaultLon: Double = 77.6408
    ): Result<Unit> {
        val cleanUid = uid.trim()
        val path = "${FirestoreCollections.USERS}/$cleanUid"
        return try {
            val activeUid = requireSignedInUid()
            FirestoreSchemaValidator.requireValidDocId(cleanUid, "User ID")
            if (activeUid != cleanUid) {
                throw SecurityException("Users may only initialize their own profile document.")
            }
            val docRef = db.collection(FirestoreCollections.USERS).document(cleanUid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = displayName?.trim()?.take(80)?.ifBlank { null } ?: "MedFind User"
                val safeEmail = email?.trim()?.take(120)?.ifBlank { null }
                val payload = sanitizeFirestoreMap(
                    mapOf(
                        UserFields.USER_ID to cleanUid,
                        UserFields.DISPLAY_NAME to safeName,
                        UserFields.EMAIL to safeEmail,
                        UserFields.PREFERRED_LOCALITY to defaultLocality.trim().take(100),
                        UserFields.PREFERRED_PINCODE to defaultPincode.trim().take(12),
                        UserFields.PREFERRED_LAT to defaultLat,
                        UserFields.PREFERRED_LON to defaultLon,
                        UserFields.LOCATION_SHARING_ENABLED to false,
                        UserFields.ROLE to UserRole.CUSTOMER.name,
                        UserFields.STATUS to AccountStatus.ACTIVE.name,
                        UserFields.RECENT_SEARCHES to emptyList<String>(),
                        UserFields.SAVED_PHARMACY_IDS to emptyList<String>(),
                        UserFields.CREATED_AT to FieldValue.serverTimestamp(),
                        UserFields.UPDATED_AT to FieldValue.serverTimestamp()
                    )
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /** READ (single): Enforces owner-only or Admin access. */
    suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        val cleanUid = uid.trim()
        val path = "${FirestoreCollections.USERS}/$cleanUid"
        return try {
            val caller = resolveCallerContext()
            FirestoreSchemaValidator.requireValidDocId(cleanUid, "User ID")
            caller.requireOwnerOrAdmin(cleanUid, "user profile")
            val snap = db.collection(FirestoreCollections.USERS).document(cleanUid).get().await()
            Result.success(snap.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /** READ (realtime): Observes a single user profile. */
    fun observeUserProfile(uid: String): Flow<UserProfile?> =
        db.collection(FirestoreCollections.USERS).document(uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
            }
            .wrapFirestoreFlow(OperationType.GET, "${FirestoreCollections.USERS}/$uid", uid)

    /** READ (admin list): Streams all user profiles; protected by `allow list: if isAdmin()`. */
    fun observeAllUsersForAdmin(adminUid: String): Flow<List<UserProfile>> =
        db.collection(FirestoreCollections.USERS)
            .snapshots()
            .map { it.toObjects(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
            .wrapFirestoreFlow(OperationType.LIST, FirestoreCollections.USERS, adminUid)

    /**
     * UPDATE (owner preferences): Updates allowed profile fields (`displayName`, `phone`, location).
     * Never mutates `role`, `status`, `email`, or `createdAt`.
     */
    suspend fun updateUserPreferences(
        uid: String,
        displayName: String? = null,
        phone: String? = null,
        locality: String? = null,
        pincode: String? = null,
        lat: Double? = null,
        lon: Double? = null,
        locationSharingEnabled: Boolean? = null
    ): Result<Unit> {
        val cleanUid = uid.trim()
        val path = "${FirestoreCollections.USERS}/$cleanUid"
        return try {
            val caller = resolveCallerContext()
            FirestoreSchemaValidator.requireValidDocId(cleanUid, "User ID")
            if (caller.uid != cleanUid) {
                throw SecurityException("Users may only update their own preferences.")
            }
            caller.requireActiveAccount()

            val updates = mutableMapOf<String, Any>(
                UserFields.UPDATED_AT to FieldValue.serverTimestamp()
            )
            displayName?.trim()?.take(80)?.let {
                require(it.isNotEmpty()) { "Display name cannot be blank." }
                updates[UserFields.DISPLAY_NAME] = it
            }
            phone?.trim()?.take(20)?.let { updates[UserFields.PHONE] = it }
            locality?.trim()?.take(100)?.let { updates[UserFields.PREFERRED_LOCALITY] = it }
            pincode?.trim()?.take(12)?.let { updates[UserFields.PREFERRED_PINCODE] = it }
            if (lat != null && lon != null) {
                FirestoreSchemaValidator.requireValidCoordinates(lat, lon)
                updates[UserFields.PREFERRED_LAT] = lat
                updates[UserFields.PREFERRED_LON] = lon
            }
            if (locationSharingEnabled != null) {
                updates[UserFields.LOCATION_SHARING_ENABLED] = locationSharingEnabled
            }

            db.collection(FirestoreCollections.USERS).document(cleanUid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /** UPDATE (admin governance): Updates another user's `role` or `status` with audit trail. */
    suspend fun updateUserRoleAndStatusByAdmin(
        targetUserId: String,
        newRole: UserRole,
        newStatus: AccountStatus,
        reason: String
    ): Result<Unit> {
        val cleanTarget = targetUserId.trim()
        val path = "${FirestoreCollections.USERS}/$cleanTarget"
        return try {
            val caller = resolveCallerContext()
            caller.requireAdmin("update user roles or account status")
            FirestoreSchemaValidator.requireValidDocId(cleanTarget, "Target User ID")
            require(cleanTarget != caller.uid || newStatus == AccountStatus.ACTIVE) {
                "Administrators cannot suspend their own active session."
            }

            val userRef = db.collection(FirestoreCollections.USERS).document(cleanTarget)
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    UserFields.ROLE to newRole.name,
                    UserFields.STATUS to newStatus.name,
                    UserFields.UPDATED_AT to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection(FirestoreCollections.ADMIN_ACTIONS).document(),
                buildAdminAuditMap(
                    caller = caller,
                    targetType = "USER",
                    targetId = cleanTarget,
                    action = "SET_USER_${newRole.name}_${newStatus.name}",
                    reason = reason.ifBlank { "Updated user role/status via Admin Console" }
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

    /** DELETE (admin-only): Removes a user document and logs an audit entry. */
    suspend fun deleteUserProfileByAdmin(
        targetUserId: String,
        reason: String
    ): Result<Unit> {
        val cleanTarget = targetUserId.trim()
        val path = "${FirestoreCollections.USERS}/$cleanTarget"
        return try {
            val caller = resolveCallerContext()
            caller.requireAdmin("delete user profiles")
            FirestoreSchemaValidator.requireValidDocId(cleanTarget, "Target User ID")
            require(cleanTarget != caller.uid) {
                "Administrators cannot delete their own account record."
            }

            val batch = db.batch()
            batch.delete(db.collection(FirestoreCollections.USERS).document(cleanTarget))
            batch.set(
                db.collection(FirestoreCollections.ADMIN_ACTIONS).document(),
                buildAdminAuditMap(
                    caller = caller,
                    targetType = "USER",
                    targetId = cleanTarget,
                    action = "DELETE_USER",
                    reason = reason.ifBlank { "Removed user record" }
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 2. PHARMACIES COLLECTION CRUD (`/pharmacies/{pharmacyId}`)
    // =========================================================================

    /**
     * CREATE: Active users register pharmacies as `PENDING`; Admins may create directly as `APPROVED`.
     */
    suspend fun createPharmacy(
        name: String,
        licenseNumber: String,
        address: String,
        locality: String,
        pincode: String,
        phone: String,
        openingHours: String,
        latitude: Double,
        longitude: Double
    ): Result<String> {
        var path = FirestoreCollections.PHARMACIES
        return try {
            val caller = resolveCallerContext()
            caller.requireActiveAccount()

            val cleanName = name.trim()
            val cleanLic = licenseNumber.trim()
            val cleanAddr = address.trim()
            val cleanLoc = locality.trim()
            val cleanPin = pincode.trim()
            val cleanPhone = phone.trim()
            val cleanHours = openingHours.trim()

            require(cleanName.length in 2..100) { "Pharmacy name must be 2-100 characters." }
            require(cleanLic.length in 2..60) { "License number must be 2-60 characters." }
            require(cleanAddr.length in 5..200) { "Address must be 5-200 characters." }
            require(cleanLoc.length in 2..80) { "Locality must be 2-80 characters." }
            require(cleanPin.length in 3..12) { "Pincode must be 3-12 characters." }
            require(cleanPhone.length in 5..20) { "Phone must be 5-20 characters." }
            require(cleanHours.length in 2..80) { "Opening hours must be 2-80 characters." }
            FirestoreSchemaValidator.requireValidCoordinates(latitude, longitude)

            val docRef = db.collection(FirestoreCollections.PHARMACIES).document()
            path = docRef.path

            val payload = mutableMapOf<String, Any>(
                PharmacyFields.OWNER_USER_ID to caller.uid,
                PharmacyFields.NAME to cleanName,
                PharmacyFields.LICENSE_NUMBER to cleanLic,
                PharmacyFields.ADDRESS to cleanAddr,
                PharmacyFields.LOCALITY to cleanLoc,
                PharmacyFields.PINCODE to cleanPin,
                PharmacyFields.LATITUDE to latitude,
                PharmacyFields.LONGITUDE to longitude,
                PharmacyFields.PHONE to cleanPhone,
                PharmacyFields.OPENING_HOURS to cleanHours,
                PharmacyFields.VERIFICATION_STATUS to if (caller.isAdmin) {
                    PharmacyVerificationStatus.APPROVED.name
                } else {
                    PharmacyVerificationStatus.PENDING.name
                },
                PharmacyFields.CREATED_AT to FieldValue.serverTimestamp(),
                PharmacyFields.UPDATED_AT to FieldValue.serverTimestamp()
            )
            if (caller.isAdmin) {
                payload[PharmacyFields.ADMIN_NOTE] = "Verified admin pharmacy"
            }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /** READ (single): Returns pharmacy if APPROVED, owned by caller, or caller is Admin. */
    suspend fun getPharmacy(pharmacyId: String): Result<Pharmacy?> {
        val cleanId = pharmacyId.trim()
        val path = "${FirestoreCollections.PHARMACIES}/$cleanId"
        return try {
            val caller = resolveCallerContext()
            FirestoreSchemaValidator.requireValidDocId(cleanId, "Pharmacy ID")
            val snap = db.collection(FirestoreCollections.PHARMACIES).document(cleanId).get().await()
            val pharmacy = snap.toObject(Pharmacy::class.java, ServerTimestampBehavior.ESTIMATE)
            if (pharmacy != null &&
                pharmacy.statusEnum() != PharmacyVerificationStatus.APPROVED &&
                pharmacy.ownerUserId != caller.uid &&
                !caller.isAdmin
            ) {
                throw SecurityException("Only approved pharmacies or the store owner may view this record.")
            }
            Result.success(pharmacy)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /** READ (streams) aligned with `firestore.rules` list predicates. */
    fun observeApprovedPharmacies(uid: String): Flow<List<Pharmacy>> =
        db.collection(FirestoreCollections.PHARMACIES)
            .whereEqualTo(PharmacyFields.VERIFICATION_STATUS, PharmacyVerificationStatus.APPROVED.name)
            .snapshots()
            .map { it.toObjects(Pharmacy::class.java, ServerTimestampBehavior.ESTIMATE) }
            .wrapFirestoreFlow(OperationType.LIST, FirestoreCollections.PHARMACIES, uid)

    fun observeMyPharmacies(ownerUid: String): Flow<List<Pharmacy>> =
        db.collection(FirestoreCollections.PHARMACIES)
            .whereEqualTo(PharmacyFields.OWNER_USER_ID, ownerUid)
            .snapshots()
            .map { it.toObjects(Pharmacy::class.java, ServerTimestampBehavior.ESTIMATE) }
            .wrapFirestoreFlow(OperationType.LIST, FirestoreCollections.PHARMACIES, ownerUid)

    fun observeAllPharmaciesForAdmin(adminUid: String): Flow<List<Pharmacy>> =
        db.collection(FirestoreCollections.PHARMACIES)
            .snapshots()
            .map { it.toObjects(Pharmacy::class.java, ServerTimestampBehavior.ESTIMATE) }
            .wrapFirestoreFlow(OperationType.LIST, FirestoreCollections.PHARMACIES, adminUid)

    /**
     * UPDATE (owner profile): Owner or Admin updates storefront details without touching `verificationStatus`
     * or `licenseNumber`. Rejects modifications to REJECTED or SUSPENDED stores unless Admin.
     */
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
    ): Result<Unit> {
        val cleanId = pharmacyId.trim()
        val path = "${FirestoreCollections.PHARMACIES}/$cleanId"
        return try {
            val caller = resolveCallerContext()
            caller.requireActiveAccount()
            FirestoreSchemaValidator.requireValidDocId(cleanId, "Pharmacy ID")

            val pharmRef = db.collection(FirestoreCollections.PHARMACIES).document(cleanId)
            val snap = pharmRef.get().await()
            val existing = snap.toObject(Pharmacy::class.java, ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalArgumentException("Pharmacy not found.")
            caller.requireOwnerOrAdmin(existing.ownerUserId, "pharmacy")
            if (!caller.isAdmin &&
                existing.statusEnum() !in setOf(
                    PharmacyVerificationStatus.PENDING,
                    PharmacyVerificationStatus.APPROVED
                )
            ) {
                throw SecurityException("Suspended or rejected pharmacies cannot be edited by the owner.")
            }

            val cleanName = name.trim()
            val cleanAddr = address.trim()
            val cleanLoc = locality.trim()
            val cleanPin = pincode.trim()
            val cleanPhone = phone.trim()
            val cleanHours = openingHours.trim()
            require(cleanName.length in 2..100) { "Pharmacy name must be 2-100 characters." }
            require(cleanAddr.length in 5..200) { "Address must be 5-200 characters." }
            require(cleanLoc.length in 2..80) { "Locality must be 2-80 characters." }
            require(cleanPin.length in 3..12) { "Pincode must be 3-12 characters." }
            require(cleanPhone.length in 5..20) { "Phone must be 5-20 characters." }
            require(cleanHours.length in 2..80) { "Opening hours must be 2-80 characters." }

            val updates = mutableMapOf<String, Any>(
                PharmacyFields.NAME to cleanName,
                PharmacyFields.ADDRESS to cleanAddr,
                PharmacyFields.LOCALITY to cleanLoc,
                PharmacyFields.PINCODE to cleanPin,
                PharmacyFields.PHONE to cleanPhone,
                PharmacyFields.OPENING_HOURS to cleanHours,
                PharmacyFields.UPDATED_AT to FieldValue.serverTimestamp()
            )
            if (latitude != null && longitude != null) {
                FirestoreSchemaValidator.requireValidCoordinates(latitude, longitude)
                updates[PharmacyFields.LATITUDE] = latitude
                updates[PharmacyFields.LONGITUDE] = longitude
            }

            pharmRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * UPDATE (admin verification): Moderates pharmacy approval/suspension, syncs `pharmacyVerified`
     * on linked inventory, elevates/demotes owner role, and appends an `admin_actions` audit record.
     */
    suspend fun moderatePharmacyByAdmin(
        pharmacyId: String,
        newStatus: PharmacyVerificationStatus,
        adminNote: String
    ): Result<Unit> {
        val cleanId = pharmacyId.trim()
        val path = "${FirestoreCollections.PHARMACIES}/$cleanId"
        return try {
            val caller = resolveCallerContext()
            caller.requireAdmin("verify or suspend pharmacies")
            FirestoreSchemaValidator.requireValidDocId(cleanId, "Pharmacy ID")
            val isVerified = newStatus == PharmacyVerificationStatus.APPROVED

            val pharmRef = db.collection(FirestoreCollections.PHARMACIES).document(cleanId)
            val pharmSnap = pharmRef.get().await()
            val ownerUserId = pharmSnap.getString(PharmacyFields.OWNER_USER_ID).orEmpty()

            val ownerPharmacies = if (ownerUserId.isNotBlank()) {
                db.collection(FirestoreCollections.PHARMACIES)
                    .whereEqualTo(PharmacyFields.OWNER_USER_ID, ownerUserId)
                    .get()
                    .await()
                    .documents
            } else emptyList()

            val ownerUserRef = ownerUserId.takeIf { it.isNotBlank() }?.let {
                db.collection(FirestoreCollections.USERS).document(it)
            }
            val ownerUserSnap = ownerUserRef?.get()?.await()

            val invDocs = db.collection(FirestoreCollections.INVENTORY)
                .whereEqualTo(InventoryFields.PHARMACY_ID, cleanId)
                .get()
                .await()

            val batch = db.batch()
            batch.update(
                pharmRef,
                mapOf(
                    PharmacyFields.VERIFICATION_STATUS to newStatus.name,
                    PharmacyFields.ADMIN_NOTE to adminNote.trim().take(250),
                    PharmacyFields.UPDATED_AT to FieldValue.serverTimestamp()
                )
            )
            invDocs.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf(
                        InventoryFields.PHARMACY_VERIFIED to isVerified,
                        InventoryFields.UPDATED_AT to FieldValue.serverTimestamp()
                    )
                )
            }
            if (ownerUserRef != null && ownerUserSnap != null && ownerUserSnap.exists()) {
                val existingRole = ownerUserSnap.getString(UserFields.ROLE)
                if (existingRole != UserRole.ADMIN.name && ownerUserId != caller.uid) {
                    val willHaveApproved = isVerified || ownerPharmacies.any { doc ->
                        doc.id != cleanId &&
                            doc.getString(PharmacyFields.VERIFICATION_STATUS) == PharmacyVerificationStatus.APPROVED.name
                    }
                    val targetRole = if (willHaveApproved) UserRole.PHARMACY.name else UserRole.CUSTOMER.name
                    if (existingRole != targetRole) {
                        batch.update(
                            ownerUserRef,
                            mapOf(
                                UserFields.ROLE to targetRole,
                                UserFields.UPDATED_AT to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }
            batch.set(
                db.collection(FirestoreCollections.ADMIN_ACTIONS).document(),
                buildAdminAuditMap(
                    caller = caller,
                    targetType = "PHARMACY",
                    targetId = cleanId,
                    action = "PHARMACY_${newStatus.name}",
                    reason = adminNote
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

    /** DELETE (admin-only): Deletes a pharmacy, removes its inventory records, and records audit log. */
    suspend fun deletePharmacyByAdmin(
        pharmacyId: String,
        reason: String
    ): Result<Unit> {
        val cleanId = pharmacyId.trim()
        val path = "${FirestoreCollections.PHARMACIES}/$cleanId"
        return try {
            val caller = resolveCallerContext()
            caller.requireAdmin("delete pharmacies")
            FirestoreSchemaValidator.requireValidDocId(cleanId, "Pharmacy ID")

            val invDocs = db.collection(FirestoreCollections.INVENTORY)
                .whereEqualTo(InventoryFields.PHARMACY_ID, cleanId)
                .get()
                .await()

            val batch = db.batch()
            invDocs.documents.forEach { batch.delete(it.reference) }
            batch.delete(db.collection(FirestoreCollections.PHARMACIES).document(cleanId))
            batch.set(
                db.collection(FirestoreCollections.ADMIN_ACTIONS).document(),
                buildAdminAuditMap(
                    caller = caller,
                    targetType = "PHARMACY",
                    targetId = cleanId,
                    action = "DELETE_PHARMACY",
                    reason = reason.ifBlank { "Pharmacy removed by Admin" }
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 3. MEDICINES COLLECTION CRUD (`/medicines/{medicineId}`)
    // =========================================================================

    /**
     * CREATE: Only Admins (VERIFIED) or owners of an APPROVED pharmacy (PENDING_REVIEW) may add medicines.
     * Regular customers are explicitly blocked before and at Firestore rules.
     */
    suspend fun createMedicine(
        genericName: String,
        brandName: String,
        strength: String,
        dosageForm: String,
        category: String,
        keywordsRaw: String,
        submittedPharmacyId: String? = null
    ): Result<String> {
        var path = FirestoreCollections.MEDICINES
        return try {
            val caller = resolveCallerContext()
            caller.requireActiveAccount()

            val cleanGeneric = genericName.trim()
            val cleanBrand = brandName.trim()
            val cleanStrength = strength.trim()
            val cleanForm = dosageForm.trim()
            val cleanCategory = category.trim()
            val cleanPharmacyId = submittedPharmacyId?.trim()?.takeIf { it.isNotBlank() }

            if (!caller.isAdmin) {
                val pharmId = cleanPharmacyId
                    ?: throw SecurityException("Only verified pharmacies or administrators can submit catalogue medicines.")
                FirestoreSchemaValidator.requireValidDocId(pharmId, "Submitted Pharmacy ID")
                val pharmSnap = db.collection(FirestoreCollections.PHARMACIES).document(pharmId).get().await()
                val pharm = pharmSnap.toObject(Pharmacy::class.java, ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalArgumentException("Submitted pharmacy record not found.")
                if (pharm.ownerUserId != caller.uid || pharm.statusEnum() != PharmacyVerificationStatus.APPROVED) {
                    throw SecurityException("Your pharmacy must be APPROVED before submitting new medicines.")
                }
            }

            require(cleanGeneric.length in 2..120) { "Generic name must be 2-120 characters." }
            require(cleanBrand.length in 1..120) { "Brand name must be 1-120 characters." }
            require(cleanStrength.length in 1..40) { "Strength must be 1-40 characters." }
            require(cleanForm.length in 1..40) { "Dosage form must be 1-40 characters." }
            require(cleanCategory.length in 1..60) { "Category must be 1-60 characters." }

            val keywords = (listOf(cleanGeneric, cleanBrand, cleanStrength, cleanCategory) +
                keywordsRaw.split(",").map { it.trim() })
                .map { it.lowercase().take(60) }
                .filter { it.isNotBlank() }
                .distinct()
                .take(15)

            val docRef = db.collection(FirestoreCollections.MEDICINES).document()
            path = docRef.path
            val status = if (caller.isAdmin) CatalogueStatus.VERIFIED.name else CatalogueStatus.PENDING_REVIEW.name

            val payload = sanitizeFirestoreMap(
                mapOf(
                    MedicineFields.GENERIC_NAME to cleanGeneric,
                    MedicineFields.BRAND_NAME to cleanBrand,
                    MedicineFields.STRENGTH to cleanStrength,
                    MedicineFields.DOSAGE_FORM to cleanForm,
                    MedicineFields.CATEGORY to cleanCategory,
                    MedicineFields.KEYWORDS to keywords,
                    MedicineFields.CATALOGUE_STATUS to status,
                    MedicineFields.SUBMITTED_BY to caller.uid,
                    MedicineFields.SUBMITTED_PHARMACY_ID to cleanPharmacyId,
                    MedicineFields.CREATED_AT to FieldValue.serverTimestamp(),
                    MedicineFields.UPDATED_AT to FieldValue.serverTimestamp()
                )
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /** READ (single): Returns medicine if VERIFIED, submitted by caller, or caller is Admin. */
    suspend fun getMedicine(medicineId: String): Result<Medicine?> {
        val cleanId = medicineId.trim()
        val path = "${FirestoreCollections.MEDICINES}/$cleanId"
        return try {
            val caller = resolveCallerContext()
            FirestoreSchemaValidator.requireValidDocId(cleanId, "Medicine ID")
            val snap = db.collection(FirestoreCollections.MEDICINES).document(cleanId).get().await()
            val med = snap.toObject(Medicine::class.java, ServerTimestampBehavior.ESTIMATE)
            if (med != null &&
                med.statusEnum() != CatalogueStatus.VERIFIED &&
                med.submittedBy != caller.uid &&
                !caller.isAdmin
            ) {
                throw SecurityException("Unverified medicines are restricted to the submitter and administrators.")
            }
            Result.success(med)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeVerifiedMedicines(uid: String): Flow<List<Medicine>> =
        db.collection(FirestoreCollections.MEDICINES)
            .whereEqualTo(MedicineFields.CATALOGUE_STATUS, CatalogueStatus.VERIFIED.name)
            .snapshots()
            .map { it.toObjects(Medicine::class.java, ServerTimestampBehavior.ESTIMATE) }
            .wrapFirestoreFlow(OperationType.LIST, FirestoreCollections.MEDICINES, uid)

    fun observeMySubmittedMedicines(uid: String): Flow<List<Medicine>> =
        db.collection(FirestoreCollections.MEDICINES)
            .whereEqualTo(MedicineFields.SUBMITTED_BY, uid)
            .snapshots()
            .map { it.toObjects(Medicine::class.java, ServerTimestampBehavior.ESTIMATE) }
            .wrapFirestoreFlow(OperationType.LIST, FirestoreCollections.MEDICINES, uid)

    fun observeAllMedicinesForAdmin(adminUid: String): Flow<List<Medicine>> =
        db.collection(FirestoreCollections.MEDICINES)
            .snapshots()
            .map { it.toObjects(Medicine::class.java, ServerTimestampBehavior.ESTIMATE) }
            .wrapFirestoreFlow(OperationType.LIST, FirestoreCollections.MEDICINES, adminUid)

    /** UPDATE (admin-only): Updates catalogue metadata or approval status and logs an audit action. */
    suspend fun updateMedicineByAdmin(
        medicineId: String,
        newStatus: CatalogueStatus,
        reason: String,
        genericName: String? = null,
        brandName: String? = null,
        strength: String? = null,
        dosageForm: String? = null,
        category: String? = null,
        keywordsRaw: String? = null
    ): Result<Unit> {
        val cleanId = medicineId.trim()
        val path = "${FirestoreCollections.MEDICINES}/$cleanId"
        return try {
            val caller = resolveCallerContext()
            caller.requireAdmin("update or verify catalogue medicines")
            FirestoreSchemaValidator.requireValidDocId(cleanId, "Medicine ID")

            val updates = mutableMapOf<String, Any>(
                MedicineFields.CATALOGUE_STATUS to newStatus.name,
                MedicineFields.UPDATED_AT to FieldValue.serverTimestamp()
            )
            genericName?.trim()?.let {
                require(it.length in 2..120) { "Generic name must be 2-120 characters." }
                updates[MedicineFields.GENERIC_NAME] = it
            }
            brandName?.trim()?.let {
                require(it.length in 1..120) { "Brand name must be 1-120 characters." }
                updates[MedicineFields.BRAND_NAME] = it
            }
            strength?.trim()?.let {
                require(it.length in 1..40) { "Strength must be 1-40 characters." }
                updates[MedicineFields.STRENGTH] = it
            }
            dosageForm?.trim()?.let {
                require(it.length in 1..40) { "Dosage form must be 1-40 characters." }
                updates[MedicineFields.DOSAGE_FORM] = it
            }
            category?.trim()?.let {
                require(it.length in 1..60) { "Category must be 1-60 characters." }
                updates[MedicineFields.CATEGORY] = it
            }
            keywordsRaw?.let { raw ->
                val keywords = raw.split(",").map { it.trim().lowercase().take(60) }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(15)
                updates[MedicineFields.KEYWORDS] = keywords
            }

            val batch = db.batch()
            batch.update(db.collection(FirestoreCollections.MEDICINES).document(cleanId), updates)
            batch.set(
                db.collection(FirestoreCollections.ADMIN_ACTIONS).document(),
                buildAdminAuditMap(
                    caller = caller,
                    targetType = "MEDICINE",
                    targetId = cleanId,
                    action = "SET_STATUS_${newStatus.name}",
                    reason = reason
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

    /** DELETE (admin-only): Deletes a catalogue medicine, removes linked inventory rows, and logs audit. */
    suspend fun deleteMedicineByAdmin(
        medicineId: String,
        reason: String
    ): Result<Unit> {
        val cleanId = medicineId.trim()
        val path = "${FirestoreCollections.MEDICINES}/$cleanId"
        return try {
            val caller = resolveCallerContext()
            caller.requireAdmin("delete catalogue medicines")
            FirestoreSchemaValidator.requireValidDocId(cleanId, "Medicine ID")

            val invDocs = db.collection(FirestoreCollections.INVENTORY)
                .whereEqualTo(InventoryFields.MEDICINE_ID, cleanId)
                .get()
                .await()

            val batch = db.batch()
            invDocs.documents.forEach { batch.delete(it.reference) }
            batch.delete(db.collection(FirestoreCollections.MEDICINES).document(cleanId))
            batch.set(
                db.collection(FirestoreCollections.ADMIN_ACTIONS).document(),
                buildAdminAuditMap(
                    caller = caller,
                    targetType = "MEDICINE",
                    targetId = cleanId,
                    action = "DELETE_MEDICINE",
                    reason = reason.ifBlank { "Removed medicine from catalogue" }
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 4. INVENTORY COLLECTION CRUD (`/inventory/{pharmacyId}_{medicineId}`)
    // =========================================================================

    /**
     * CREATE / UPDATE: Upserts deterministic `pharmacyId_medicineId` inventory record along with an
     * atomic `inventory_history` snapshot. Enforces approved pharmacy ownership & verified medicine.
     */
    suspend fun upsertInventoryWithHistory(
        pharmacy: Pharmacy,
        medicine: Medicine,
        availabilityStatus: AvailabilityStatus,
        quantity: Long,
        unit: String,
        staffDisplayName: String
    ): Result<Unit> {
        val invId = "${pharmacy.id}_${medicine.id}"
        val path = "${FirestoreCollections.INVENTORY}/$invId"
        return try {
            val caller = resolveCallerContext()
            caller.requireActiveAccount()

            val deterministicId = FirestoreSchemaValidator.inventoryDocumentId(pharmacy.id, medicine.id)
            caller.requireOwnerOrAdmin(pharmacy.ownerUserId, "pharmacy inventory")

            require(pharmacy.statusEnum() == PharmacyVerificationStatus.APPROVED) {
                "Pharmacy must be APPROVED before publishing inventory."
            }
            require(caller.isAdmin || medicine.statusEnum() == CatalogueStatus.VERIFIED) {
                "Only VERIFIED catalogue medicines can be listed in public pharmacy inventory."
            }

            val effectiveQuantity = if (availabilityStatus == AvailabilityStatus.OUT_OF_STOCK) 0L else quantity
            val cleanUnit = unit.trim()
            require(cleanUnit.length in 1..30) { "Unit must be 1-30 characters." }
            require(effectiveQuantity in 0L..1_000_000L) { "Quantity must be between 0 and 1,000,000." }

            val cleanStaffName = staffDisplayName.trim().take(80).ifBlank { "Pharmacy Staff" }
            val invRef = db.collection(FirestoreCollections.INVENTORY).document(deterministicId)
            val existingSnap = invRef.get().await()
            val histRef = db.collection(FirestoreCollections.INVENTORY_HISTORY).document()
            val batch = db.batch()

            val oldStatus: String
            val oldQty: Long

            if (existingSnap.exists()) {
                oldStatus = existingSnap.getString(InventoryFields.AVAILABILITY_STATUS) ?: "NONE"
                oldQty = existingSnap.getLong(InventoryFields.QUANTITY) ?: 0L
                batch.update(
                    invRef,
                    mapOf(
                        InventoryFields.PHARMACY_NAME to pharmacy.name,
                        InventoryFields.PHARMACY_ADDRESS to pharmacy.address,
                        InventoryFields.PHARMACY_LOCALITY to pharmacy.locality,
                        InventoryFields.PHARMACY_PINCODE to pharmacy.pincode,
                        InventoryFields.PHARMACY_PHONE to pharmacy.phone,
                        InventoryFields.PHARMACY_HOURS to pharmacy.openingHours,
                        InventoryFields.PHARMACY_LAT to pharmacy.latitude,
                        InventoryFields.PHARMACY_LON to pharmacy.longitude,
                        InventoryFields.AVAILABILITY_STATUS to availabilityStatus.name,
                        InventoryFields.QUANTITY to effectiveQuantity,
                        InventoryFields.UNIT to cleanUnit,
                        InventoryFields.UPDATED_BY to caller.uid,
                        InventoryFields.UPDATED_BY_NAME to cleanStaffName,
                        InventoryFields.UPDATED_AT to FieldValue.serverTimestamp()
                    )
                )
            } else {
                oldStatus = "NONE"
                oldQty = 0L
                batch.set(
                    invRef,
                    mapOf(
                        InventoryFields.PHARMACY_ID to pharmacy.id,
                        InventoryFields.PHARMACY_NAME to pharmacy.name,
                        InventoryFields.PHARMACY_ADDRESS to pharmacy.address,
                        InventoryFields.PHARMACY_LOCALITY to pharmacy.locality,
                        InventoryFields.PHARMACY_PINCODE to pharmacy.pincode,
                        InventoryFields.PHARMACY_PHONE to pharmacy.phone,
                        InventoryFields.PHARMACY_HOURS to pharmacy.openingHours,
                        InventoryFields.PHARMACY_LAT to pharmacy.latitude,
                        InventoryFields.PHARMACY_LON to pharmacy.longitude,
                        InventoryFields.OWNER_USER_ID to pharmacy.ownerUserId,
                        InventoryFields.PHARMACY_VERIFIED to true,
                        InventoryFields.MEDICINE_ID to medicine.id,
                        InventoryFields.MEDICINE_NAME to medicine.displayTitle(),
                        InventoryFields.GENERIC_NAME to medicine.genericName,
                        InventoryFields.BRAND_NAME to medicine.brandName,
                        InventoryFields.STRENGTH to medicine.strength,
                        InventoryFields.DOSAGE_FORM to medicine.dosageForm,
                        InventoryFields.AVAILABILITY_STATUS to availabilityStatus.name,
                        InventoryFields.QUANTITY to effectiveQuantity,
                        InventoryFields.UNIT to cleanUnit,
                        InventoryFields.UPDATED_BY to caller.uid,
                        InventoryFields.UPDATED_BY_NAME to cleanStaffName,
                        InventoryFields.CREATED_AT to FieldValue.serverTimestamp(),
                        InventoryFields.UPDATED_AT to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.set(
                histRef,
                mapOf(
                    "inventoryId" to deterministicId,
                    "pharmacyId" to pharmacy.id,
                    "pharmacyName" to pharmacy.name,
                    "medicineId" to medicine.id,
                    "medicineName" to medicine.displayTitle(),
                    "ownerUserId" to pharmacy.ownerUserId,
                    "oldStatus" to oldStatus,
                    "newStatus" to availabilityStatus.name,
                    "oldQuantity" to oldQty,
                    "newQuantity" to effectiveQuantity,
                    "unit" to cleanUnit,
                    "changedBy" to caller.uid,
                    "changedByName" to cleanStaffName,
                    "changedAt" to FieldValue.serverTimestamp()
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

    /** READ (single): Fetches deterministic inventory item with verification/ownership check. */
    suspend fun getInventoryItem(pharmacyId: String, medicineId: String): Result<InventoryRecord?> {
        val invId = FirestoreSchemaValidator.inventoryDocumentId(pharmacyId, medicineId)
        val path = "${FirestoreCollections.INVENTORY}/$invId"
        return try {
            val caller = resolveCallerContext()
            val snap = db.collection(FirestoreCollections.INVENTORY).document(invId).get().await()
            val record = snap.toObject(InventoryRecord::class.java, ServerTimestampBehavior.ESTIMATE)
            if (record != null &&
                !record.pharmacyVerified &&
                record.ownerUserId != caller.uid &&
                !caller.isAdmin
            ) {
                throw SecurityException("Unverified inventory listings are restricted to the owner and admins.")
            }
            Result.success(record)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeInventoryForMedicine(uid: String, medicineId: String): Flow<List<InventoryRecord>> =
        db.collection(FirestoreCollections.INVENTORY)
            .whereEqualTo(InventoryFields.MEDICINE_ID, medicineId)
            .whereEqualTo(InventoryFields.PHARMACY_VERIFIED, true)
            .snapshots()
            .map { it.toObjects(InventoryRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
            .wrapFirestoreFlow(OperationType.LIST, FirestoreCollections.INVENTORY, uid)

    fun observeInventoryForPharmacy(uid: String, pharmacyId: String): Flow<List<InventoryRecord>> =
        db.collection(FirestoreCollections.INVENTORY)
            .whereEqualTo(InventoryFields.PHARMACY_ID, pharmacyId)
            .whereEqualTo(InventoryFields.PHARMACY_VERIFIED, true)
            .snapshots()
            .map { it.toObjects(InventoryRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
            .wrapFirestoreFlow(OperationType.LIST, FirestoreCollections.INVENTORY, uid)

    fun observePharmacyInventory(ownerUid: String): Flow<List<InventoryRecord>> =
        db.collection(FirestoreCollections.INVENTORY)
            .whereEqualTo(InventoryFields.OWNER_USER_ID, ownerUid)
            .snapshots()
            .map { it.toObjects(InventoryRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
            .wrapFirestoreFlow(OperationType.LIST, FirestoreCollections.INVENTORY, ownerUid)

    fun observeAllInventoryForAdmin(adminUid: String): Flow<List<InventoryRecord>> =
        db.collection(FirestoreCollections.INVENTORY)
            .snapshots()
            .map { it.toObjects(InventoryRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
            .wrapFirestoreFlow(OperationType.LIST, FirestoreCollections.INVENTORY, adminUid)

    /** DELETE (admin-only): Deletes an inventory listing and records an audit trail entry. */
    suspend fun deleteInventoryByAdmin(
        inventoryId: String,
        reason: String
    ): Result<Unit> {
        val cleanId = inventoryId.trim()
        val path = "${FirestoreCollections.INVENTORY}/$cleanId"
        return try {
            val caller = resolveCallerContext()
            caller.requireAdmin("delete inventory records")
            FirestoreSchemaValidator.requireValidDocId(cleanId, "Inventory ID")

            val batch = db.batch()
            batch.delete(db.collection(FirestoreCollections.INVENTORY).document(cleanId))
            batch.set(
                db.collection(FirestoreCollections.ADMIN_ACTIONS).document(),
                buildAdminAuditMap(
                    caller = caller,
                    targetType = "INVENTORY",
                    targetId = cleanId,
                    action = "DELETE_INVENTORY",
                    reason = reason.ifBlank { "Removed inventory listing" }
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
