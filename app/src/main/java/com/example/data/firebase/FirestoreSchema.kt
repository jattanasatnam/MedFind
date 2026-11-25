package com.example.data.firebase

import com.example.data.model.AccountStatus
import com.example.data.model.UserRole

/**
 * Canonical Firestore data model design for MedFind.
 * Mirrors /firebase-blueprint.json and /firestore.rules 1:1.
 *
 * Collections:
 * - `users/{userId}`: Authenticated user profile, preferences, saved pharmacies, server-managed role/status.
 * - `pharmacies/{pharmacyId}`: Participating medical stores with drug license, geolocation, and admin verification.
 * - `medicines/{medicineId}`: Searchable medicine catalogue with VERIFIED / PENDING_REVIEW / ARCHIVED status.
 * - `inventory/{pharmacyId}_{medicineId}`: Deterministic pharmacy-medicine availability & quantity snapshot.
 * - `inventory_history/{historyId}`: Immutable audit trail written atomically with inventory changes.
 * - `feedback_reports/{reportId}`: User stock correction disputes reviewed by admins.
 * - `admins/{userId}`: Trusted admin grant records.
 * - `admin_actions/{actionId}`: Immutable admin moderation & CRUD audit log.
 */
object FirestoreCollections {
    const val USERS = "users"
    const val PHARMACIES = "pharmacies"
    const val MEDICINES = "medicines"
    const val INVENTORY = "inventory"
    const val INVENTORY_HISTORY = "inventory_history"
    const val FEEDBACK_REPORTS = "feedback_reports"
    const val ADMINS = "admins"
    const val ADMIN_ACTIONS = "admin_actions"
}

object UserFields {
    const val USER_ID = "userId"
    const val DISPLAY_NAME = "displayName"
    const val EMAIL = "email"
    const val PHONE = "phone"
    const val PREFERRED_LOCALITY = "preferredLocality"
    const val PREFERRED_PINCODE = "preferredPincode"
    const val PREFERRED_LAT = "preferredLat"
    const val PREFERRED_LON = "preferredLon"
    const val LOCATION_SHARING_ENABLED = "locationSharingEnabled"
    const val ROLE = "role"
    const val STATUS = "status"
    const val RECENT_SEARCHES = "recentSearches"
    const val SAVED_PHARMACY_IDS = "savedPharmacyIds"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
}

object PharmacyFields {
    const val OWNER_USER_ID = "ownerUserId"
    const val NAME = "name"
    const val LICENSE_NUMBER = "licenseNumber"
    const val ADDRESS = "address"
    const val LOCALITY = "locality"
    const val PINCODE = "pincode"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val PHONE = "phone"
    const val OPENING_HOURS = "openingHours"
    const val VERIFICATION_STATUS = "verificationStatus"
    const val ADMIN_NOTE = "adminNote"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
}

object MedicineFields {
    const val GENERIC_NAME = "genericName"
    const val BRAND_NAME = "brandName"
    const val STRENGTH = "strength"
    const val DOSAGE_FORM = "dosageForm"
    const val CATEGORY = "category"
    const val KEYWORDS = "keywords"
    const val CATALOGUE_STATUS = "catalogueStatus"
    const val SUBMITTED_BY = "submittedBy"
    const val SUBMITTED_PHARMACY_ID = "submittedPharmacyId"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
}

object InventoryFields {
    const val PHARMACY_ID = "pharmacyId"
    const val PHARMACY_NAME = "pharmacyName"
    const val PHARMACY_ADDRESS = "pharmacyAddress"
    const val PHARMACY_LOCALITY = "pharmacyLocality"
    const val PHARMACY_PINCODE = "pharmacyPincode"
    const val PHARMACY_PHONE = "pharmacyPhone"
    const val PHARMACY_HOURS = "pharmacyHours"
    const val PHARMACY_LAT = "pharmacyLat"
    const val PHARMACY_LON = "pharmacyLon"
    const val OWNER_USER_ID = "ownerUserId"
    const val PHARMACY_VERIFIED = "pharmacyVerified"
    const val MEDICINE_ID = "medicineId"
    const val MEDICINE_NAME = "medicineName"
    const val GENERIC_NAME = "genericName"
    const val BRAND_NAME = "brandName"
    const val STRENGTH = "strength"
    const val DOSAGE_FORM = "dosageForm"
    const val AVAILABILITY_STATUS = "availabilityStatus"
    const val QUANTITY = "quantity"
    const val UNIT = "unit"
    const val UPDATED_BY = "updatedBy"
    const val UPDATED_BY_NAME = "updatedByName"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
}

object FirestoreSchemaValidator {
    private val docIdRegex = Regex("^[a-zA-Z0-9_\\-]+$")

    fun isValidDocId(id: String): Boolean =
        id.length in 1..128 && docIdRegex.matches(id)

    fun requireValidDocId(id: String, label: String): String {
        val trimmed = id.trim()
        require(isValidDocId(trimmed)) {
            "$label must be 1-128 alphanumeric, underscore, or hyphen characters."
        }
        return trimmed
    }

    fun inventoryDocumentId(pharmacyId: String, medicineId: String): String {
        val cleanPharmacyId = requireValidDocId(pharmacyId, "Pharmacy ID")
        val cleanMedicineId = requireValidDocId(medicineId, "Medicine ID")
        return "${cleanPharmacyId}_${cleanMedicineId}"
    }

    fun requireValidCoordinates(lat: Double, lon: Double) {
        require(lat in -90.0..90.0 && lon in -180.0..180.0) {
            "Coordinates must fall within valid latitude [-90, 90] and longitude [-180, 180]."
        }
    }
}

/**
 * Resolved runtime caller authorization snapshot verified against Firebase Auth + Firestore.
 * Mirrors `firestore.rules` role gates so unauthorized operations fail fast with clear reasons
 * before hitting Firestore rules, while Firestore rules remain the authoritative enforcement barrier.
 */
data class CallerRoleContext(
    val uid: String,
    val email: String,
    val isEmailVerified: Boolean,
    val effectiveRole: UserRole,
    val accountStatus: AccountStatus,
    val hasApprovedPharmacy: Boolean
) {
    val isAdmin: Boolean get() = effectiveRole == UserRole.ADMIN
    val isActive: Boolean get() = accountStatus == AccountStatus.ACTIVE
    val isPharmacyOrAdmin: Boolean get() = isAdmin || (effectiveRole == UserRole.PHARMACY && hasApprovedPharmacy)

    fun requireActiveAccount() {
        if (!isActive) {
            throw SecurityException("Your account is suspended. Contact an administrator for assistance.")
        }
    }

    fun requireAdmin(actionLabel: String) {
        if (!isAdmin) {
            throw SecurityException("Administrator role required to $actionLabel.")
        }
    }

    fun requireOwnerOrAdmin(ownerUserId: String, resourceLabel: String) {
        if (!isAdmin && uid != ownerUserId) {
            throw SecurityException("Access denied: you do not own this $resourceLabel.")
        }
    }
}
