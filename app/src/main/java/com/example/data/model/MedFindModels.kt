package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

const val BOOTSTRAPPED_ADMIN_EMAIL = "jattanasatnam823@gmail.com"
const val DEFAULT_FRESHNESS_HOURS = 24L

enum class UserRole { CUSTOMER, PHARMACY, ADMIN }
enum class AccountStatus { ACTIVE, SUSPENDED }
enum class CatalogueStatus { VERIFIED, PENDING_REVIEW, ARCHIVED }
enum class PharmacyVerificationStatus { PENDING, APPROVED, REJECTED, SUSPENDED }

enum class AvailabilityStatus(val label: String) {
    REPORTED_AVAILABLE("Reported Available"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock"),
    PAUSED("Paused")
}

enum class ReportType(val label: String) {
    OUT_OF_STOCK_ON_VISIT("Out of stock on visit"),
    WRONG_QUANTITY("Wrong quantity listed"),
    WRONG_PRICE_OR_UNIT("Wrong unit or packaging"),
    CLOSED_OR_UNREACHABLE("Pharmacy closed / unreachable"),
    DUPLICATE_LISTING("Duplicate listing")
}

enum class ReportStatus(val label: String) {
    OPEN("Open"),
    UNDER_REVIEW("Under Review"),
    RESOLVED("Resolved"),
    DISMISSED("Dismissed")
}

enum class FreshnessTier(val label: String) {
    FRESH("Fresh"),
    AGING("Recent"),
    STALE("Stale (>24h)")
}

inline fun <reified T : Enum<T>> safeEnum(raw: String?, default: T): T {
    return enumValues<T>().firstOrNull { it.name == raw } ?: default
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val preferredLocality: String = "",
    val preferredPincode: String = "",
    val preferredLat: Double? = null,
    val preferredLon: Double? = null,
    val locationSharingEnabled: Boolean = false,
    val role: String = UserRole.CUSTOMER.name,
    val status: String = AccountStatus.ACTIVE.name,
    val recentSearches: List<String> = emptyList(),
    val savedPharmacyIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roleEnum(): UserRole = safeEnum(role, UserRole.CUSTOMER)
    fun accountStatusEnum(): AccountStatus = safeEnum(status, AccountStatus.ACTIVE)
}

@Keep
@IgnoreExtraProperties
data class Medicine(
    @DocumentId val id: String = "",
    val genericName: String = "",
    val brandName: String = "",
    val strength: String = "",
    val dosageForm: String = "",
    val category: String = "",
    val keywords: List<String> = emptyList(),
    val catalogueStatus: String = CatalogueStatus.VERIFIED.name,
    val submittedBy: String = "",
    val submittedPharmacyId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun displayTitle(): String = "$genericName $strength ($brandName)"
    fun statusEnum(): CatalogueStatus = safeEnum(catalogueStatus, CatalogueStatus.VERIFIED)
}

@Keep
@IgnoreExtraProperties
data class Pharmacy(
    @DocumentId val id: String = "",
    val ownerUserId: String = "",
    val name: String = "",
    val licenseNumber: String = "",
    val address: String = "",
    val locality: String = "",
    val pincode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phone: String = "",
    val openingHours: String = "",
    val verificationStatus: String = PharmacyVerificationStatus.PENDING.name,
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): PharmacyVerificationStatus =
        safeEnum(verificationStatus, PharmacyVerificationStatus.PENDING)
}

@Keep
@IgnoreExtraProperties
data class InventoryRecord(
    @DocumentId val id: String = "",
    val pharmacyId: String = "",
    val pharmacyName: String = "",
    val pharmacyAddress: String = "",
    val pharmacyLocality: String = "",
    val pharmacyPincode: String = "",
    val pharmacyPhone: String = "",
    val pharmacyHours: String = "",
    val pharmacyLat: Double = 0.0,
    val pharmacyLon: Double = 0.0,
    val ownerUserId: String = "",
    val pharmacyVerified: Boolean = false,
    val medicineId: String = "",
    val medicineName: String = "",
    val genericName: String = "",
    val brandName: String = "",
    val strength: String = "",
    val dosageForm: String = "",
    val availabilityStatus: String = AvailabilityStatus.REPORTED_AVAILABLE.name,
    val quantity: Long = 0L,
    val unit: String = "strips",
    val updatedBy: String = "",
    val updatedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun availabilityEnum(): AvailabilityStatus =
        safeEnum(availabilityStatus, AvailabilityStatus.REPORTED_AVAILABLE)

    fun effectiveTimestamp(): Timestamp = updatedAt ?: createdAt ?: Timestamp.now()

    fun freshnessTier(thresholdHours: Long = DEFAULT_FRESHNESS_HOURS): FreshnessTier {
        val updatedMs = effectiveTimestamp().toDate().time
        val ageHours = TimeUnit.MILLISECONDS.toHours((System.currentTimeMillis() - updatedMs).coerceAtLeast(0L))
        return when {
            ageHours <= 6L -> FreshnessTier.FRESH
            ageHours <= thresholdHours -> FreshnessTier.AGING
            else -> FreshnessTier.STALE
        }
    }

    fun isStale(thresholdHours: Long = DEFAULT_FRESHNESS_HOURS): Boolean =
        freshnessTier(thresholdHours) == FreshnessTier.STALE
}

@Keep
@IgnoreExtraProperties
data class InventoryHistoryRecord(
    @DocumentId val id: String = "",
    val inventoryId: String = "",
    val pharmacyId: String = "",
    val pharmacyName: String = "",
    val medicineId: String = "",
    val medicineName: String = "",
    val ownerUserId: String = "",
    val oldStatus: String = "",
    val newStatus: String = "",
    val oldQuantity: Long = 0L,
    val newQuantity: Long = 0L,
    val unit: String = "",
    val changedBy: String = "",
    val changedByName: String = "",
    @ServerTimestamp val changedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FeedbackReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val reporterName: String = "",
    val pharmacyId: String = "",
    val pharmacyName: String = "",
    val inventoryId: String = "",
    val medicineId: String = "",
    val medicineName: String = "",
    val pharmacyOwnerUserId: String = "",
    val reportType: String = ReportType.OUT_OF_STOCK_ON_VISIT.name,
    val message: String = "",
    val status: String = ReportStatus.OPEN.name,
    val resolutionNote: String = "",
    val reviewedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun typeEnum(): ReportType = safeEnum(reportType, ReportType.OUT_OF_STOCK_ON_VISIT)
    fun statusEnum(): ReportStatus = safeEnum(status, ReportStatus.OPEN)
}

@Keep
@IgnoreExtraProperties
data class AdminActionRecord(
    @DocumentId val id: String = "",
    val adminUserId: String = "",
    val adminEmail: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val action: String = "",
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

object GeoAndTimeUtils {
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double? {
        if ((lat1 == 0.0 && lon1 == 0.0) || (lat2 == 0.0 && lon2 == 0.0)) return null
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun formatRelativeAndLocalTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return "Just now"
        val diffMs = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
        val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)
        val rel = when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return "$rel (${sdf.format(date)})"
    }

    fun formatLocalDate(timestamp: Timestamp?): String {
        val date: Date = timestamp?.toDate() ?: return "Pending sync"
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
    }

    fun sanitizeFirestoreMap(raw: Map<String, Any?>): Map<String, Any> {
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun serverTime(): Any = FieldValue.serverTimestamp()
}
