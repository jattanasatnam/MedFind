package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.google.firebase.FirebaseApp
import com.example.data.firebase.UiState
import com.example.data.model.AvailabilityStatus
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
import com.example.ui.components.AvailabilityBadge
import com.example.ui.components.ColoredAccentKpiCard
import com.example.ui.components.ConsoleFieldRow
import com.example.ui.components.ConsoleProjectHeaderCard
import com.example.ui.components.FreshnessTrendChartCard
import com.example.ui.components.QuickActionPillButton
import com.example.ui.components.StatusPill
import com.example.ui.components.StockFreshnessCanvasCard
import com.example.ui.components.SubmitMedicineDialog
import com.example.ui.components.VerificationBadge
import com.example.ui.components.openPharmacyDirections
import com.example.ui.theme.AccentAmberBg
import com.example.ui.theme.AccentAmberFg
import com.example.ui.theme.AccentBlueBg
import com.example.ui.theme.AccentBlueFg
import com.example.ui.theme.AccentMintBg
import com.example.ui.theme.AccentMintFg
import com.example.ui.theme.AccentRoseBg
import com.example.ui.theme.AccentRoseFg
import com.example.ui.theme.AccentSkyBg
import com.example.ui.theme.AccentSkyFg
import com.example.ui.theme.AccentTealBg
import com.example.ui.theme.AccentTealFg
import com.example.ui.theme.AccentVioletBg
import com.example.ui.theme.AccentVioletFg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusAvailableBg
import com.example.ui.theme.StatusAvailableFg
import com.example.ui.theme.StatusLowStockBg
import com.example.ui.theme.StatusLowStockFg
import com.example.ui.viewmodel.MedFindViewModel

enum class FirestoreCollectionTab(val path: String, val label: String) {
    INVENTORY("inventory", "Inventory"),
    PHARMACIES("pharmacies", "Pharmacies"),
    MEDICINES("medicines", "Medicines"),
    HISTORY("inventory_history", "History"),
    REPORTS("feedback_reports", "Reports"),
    ADMIN_ACTIONS("admin_actions", "Admin Actions")
}

@Composable
fun ConsoleOverviewPane(
    viewModel: MedFindViewModel,
    onOpenCollection: (FirestoreCollectionTab) -> Unit,
    onOpenPharmacyPortal: () -> Unit,
    onOpenAdminGovernance: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToAgent: () -> Unit
) {
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val verifiedMedsState by viewModel.verifiedMedicinesState.collectAsStateWithLifecycle()
    val adminMedsState by viewModel.adminMedicinesState.collectAsStateWithLifecycle()
    val myPharmaciesState by viewModel.myPharmaciesState.collectAsStateWithLifecycle()
    val adminPharmaciesState by viewModel.adminPharmaciesState.collectAsStateWithLifecycle()
    val myInventoryState by viewModel.myPharmacyInventoryState.collectAsStateWithLifecycle()
    val adminInventoryState by viewModel.adminInventoryState.collectAsStateWithLifecycle()
    val pharmacyReportsState by viewModel.pharmacyReportsState.collectAsStateWithLifecycle()
    val adminReportsState by viewModel.adminReportsState.collectAsStateWithLifecycle()
    val myHistoryState by viewModel.myPharmacyHistoryState.collectAsStateWithLifecycle()
    val adminHistoryState by viewModel.adminHistoryState.collectAsStateWithLifecycle()

    val medicines = if (isAdmin) {
        (adminMedsState as? UiState.Success)?.data.orEmpty()
    } else {
        (verifiedMedsState as? UiState.Success)?.data.orEmpty()
    }
    val pharmacies = if (isAdmin) {
        (adminPharmaciesState as? UiState.Success)?.data.orEmpty()
    } else {
        (myPharmaciesState as? UiState.Success)?.data.orEmpty()
    }
    val inventory = if (isAdmin) {
        (adminInventoryState as? UiState.Success)?.data.orEmpty()
    } else {
        (myInventoryState as? UiState.Success)?.data.orEmpty()
    }
    val reports = if (isAdmin) {
        (adminReportsState as? UiState.Success)?.data.orEmpty()
    } else {
        (pharmacyReportsState as? UiState.Success)?.data.orEmpty()
    }
    val history = if (isAdmin) {
        (adminHistoryState as? UiState.Success)?.data.orEmpty()
    } else {
        (myHistoryState as? UiState.Success)?.data.orEmpty()
    }

    val approvedCount = pharmacies.count { it.statusEnum() == PharmacyVerificationStatus.APPROVED }
    val pendingPharmCount = pharmacies.count { it.statusEnum() == PharmacyVerificationStatus.PENDING }
    val verifiedMedCount = medicines.count { it.statusEnum() == CatalogueStatus.VERIFIED }
    val openReportCount = reports.count {
        it.statusEnum() == ReportStatus.OPEN || it.statusEnum() == ReportStatus.UNDER_REVIEW
    }

    val freshCount = inventory.count {
        (it.availabilityEnum() == AvailabilityStatus.REPORTED_AVAILABLE || it.availabilityEnum() == AvailabilityStatus.LOW_STOCK) &&
            it.freshnessTier() == FreshnessTier.FRESH
    }
    val agingCount = inventory.count {
        (it.availabilityEnum() == AvailabilityStatus.REPORTED_AVAILABLE || it.availabilityEnum() == AvailabilityStatus.LOW_STOCK) &&
            it.freshnessTier() == FreshnessTier.AGING
    }
    val staleCount = inventory.count {
        (it.availabilityEnum() == AvailabilityStatus.REPORTED_AVAILABLE || it.availabilityEnum() == AvailabilityStatus.LOW_STOCK) &&
            it.freshnessTier() == FreshnessTier.STALE
    }
    val outCount = inventory.count {
        it.availabilityEnum() == AvailabilityStatus.OUT_OF_STOCK || it.availabilityEnum() == AvailabilityStatus.PAUSED
    }

    var showQuickMedDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val liveRecordsCount = pharmacies.size + medicines.size + inventory.size

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "console_project_header") {
                val projectId = remember {
                    runCatching { FirebaseApp.getInstance().options.projectId.orEmpty() }
                        .getOrDefault("")
                        .ifBlank { "medfind-firebase" }
                }
                ConsoleProjectHeaderCard(
                    title = if (isAdmin) "MedFind Admin Console" else "MedFind Store Console",
                    projectId = projectId,
                    databaseId = context.getString(R.string.firestore_database_id),
                    roleLabel = if (isAdmin) "ADMIN" else "PHARMACY",
                    liveRecordsCount = liveRecordsCount
                )
            }

            item(key = "console_kpi_grid") {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWide = maxWidth >= 700.dp
                    val freshTotal = freshCount + agingCount
                    val alertTotal = openReportCount + pendingPharmCount

                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ColoredAccentKpiCard(
                                title = "Fresh Listings",
                                value = "$freshTotal/${inventory.size}",
                                footerLabel = "View Inventory",
                                icon = Icons.Outlined.Inventory2,
                                accentFg = AccentMintFg,
                                accentBg = AccentMintBg,
                                onClick = { onOpenCollection(FirestoreCollectionTab.INVENTORY) },
                                testTag = "console_metric_inventory",
                                modifier = Modifier.weight(1f)
                            )
                            ColoredAccentKpiCard(
                                title = if (isAdmin) "Pharmacies" else "My Stores",
                                value = pharmacies.size.toString(),
                                footerLabel = "$approvedCount Verified",
                                icon = Icons.Outlined.LocalPharmacy,
                                accentFg = AccentAmberFg,
                                accentBg = AccentAmberBg,
                                onClick = { onOpenCollection(FirestoreCollectionTab.PHARMACIES) },
                                testTag = "console_metric_pharmacies",
                                modifier = Modifier.weight(1f)
                            )
                            ColoredAccentKpiCard(
                                title = "Medicines",
                                value = medicines.size.toString(),
                                footerLabel = "$verifiedMedCount Verified",
                                icon = Icons.Outlined.Medication,
                                accentFg = AccentSkyFg,
                                accentBg = AccentSkyBg,
                                onClick = { onOpenCollection(FirestoreCollectionTab.MEDICINES) },
                                testTag = "console_metric_medicines",
                                modifier = Modifier.weight(1f)
                            )
                            ColoredAccentKpiCard(
                                title = "Open Alerts",
                                value = alertTotal.toString(),
                                footerLabel = if (openReportCount > 0) "Resolve Reports" else "View Reports",
                                icon = Icons.Outlined.Flag,
                                accentFg = if (alertTotal > 0) AccentRoseFg else AccentMintFg,
                                accentBg = if (alertTotal > 0) AccentRoseBg else AccentMintBg,
                                onClick = { onOpenCollection(FirestoreCollectionTab.REPORTS) },
                                testTag = "console_metric_reports",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ColoredAccentKpiCard(
                                    title = "Fresh Listings",
                                    value = "$freshTotal/${inventory.size}",
                                    footerLabel = "View Inventory",
                                    icon = Icons.Outlined.Inventory2,
                                    accentFg = AccentMintFg,
                                    accentBg = AccentMintBg,
                                    onClick = { onOpenCollection(FirestoreCollectionTab.INVENTORY) },
                                    testTag = "console_metric_inventory",
                                    modifier = Modifier.weight(1f)
                                )
                                ColoredAccentKpiCard(
                                    title = if (isAdmin) "Pharmacies" else "My Stores",
                                    value = pharmacies.size.toString(),
                                    footerLabel = "$approvedCount Verified",
                                    icon = Icons.Outlined.LocalPharmacy,
                                    accentFg = AccentAmberFg,
                                    accentBg = AccentAmberBg,
                                    onClick = { onOpenCollection(FirestoreCollectionTab.PHARMACIES) },
                                    testTag = "console_metric_pharmacies",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ColoredAccentKpiCard(
                                    title = "Medicines",
                                    value = medicines.size.toString(),
                                    footerLabel = "$verifiedMedCount Verified",
                                    icon = Icons.Outlined.Medication,
                                    accentFg = AccentSkyFg,
                                    accentBg = AccentSkyBg,
                                    onClick = { onOpenCollection(FirestoreCollectionTab.MEDICINES) },
                                    testTag = "console_metric_medicines",
                                    modifier = Modifier.weight(1f)
                                )
                                ColoredAccentKpiCard(
                                    title = "Open Alerts",
                                    value = alertTotal.toString(),
                                    footerLabel = if (openReportCount > 0) "Resolve Reports" else "View Reports",
                                    icon = Icons.Outlined.Flag,
                                    accentFg = if (alertTotal > 0) AccentRoseFg else AccentMintFg,
                                    accentBg = if (alertTotal > 0) AccentRoseBg else AccentMintBg,
                                    onClick = { onOpenCollection(FirestoreCollectionTab.REPORTS) },
                                    testTag = "console_metric_reports",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "console_quick_actions") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isWide = maxWidth >= 640.dp
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                QuickActionPillButton(
                                    label = "Insert Stock",
                                    icon = Icons.Outlined.Inventory2,
                                    iconBg = AccentMintBg,
                                    iconFg = AccentMintFg,
                                    onClick = onOpenPharmacyPortal,
                                    testTag = "tile_insert_stock",
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionPillButton(
                                    label = "Add Medicine",
                                    icon = Icons.Outlined.Medication,
                                    iconBg = AccentVioletBg,
                                    iconFg = AccentVioletFg,
                                    onClick = { showQuickMedDialog = true },
                                    testTag = "tile_add_medicine",
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionPillButton(
                                    label = "Search & Maps",
                                    icon = Icons.Outlined.Search,
                                    iconBg = AccentBlueBg,
                                    iconFg = AccentBlueFg,
                                    onClick = onNavigateToSearch,
                                    testTag = "tile_search_maps",
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionPillButton(
                                    label = "AI Health Guide",
                                    icon = Icons.Outlined.AutoAwesome,
                                    iconBg = AccentTealBg,
                                    iconFg = AccentTealFg,
                                    onClick = onNavigateToAgent,
                                    testTag = "tile_ai_guide",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    QuickActionPillButton(
                                        label = "Insert Stock",
                                        icon = Icons.Outlined.Inventory2,
                                        iconBg = AccentMintBg,
                                        iconFg = AccentMintFg,
                                        onClick = onOpenPharmacyPortal,
                                        testTag = "tile_insert_stock",
                                        modifier = Modifier.weight(1f)
                                    )
                                    QuickActionPillButton(
                                        label = "Add Medicine",
                                        icon = Icons.Outlined.Medication,
                                        iconBg = AccentVioletBg,
                                        iconFg = AccentVioletFg,
                                        onClick = { showQuickMedDialog = true },
                                        testTag = "tile_add_medicine",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    QuickActionPillButton(
                                        label = "Search & Maps",
                                        icon = Icons.Outlined.Search,
                                        iconBg = AccentBlueBg,
                                        iconFg = AccentBlueFg,
                                        onClick = onNavigateToSearch,
                                        testTag = "tile_search_maps",
                                        modifier = Modifier.weight(1f)
                                    )
                                    QuickActionPillButton(
                                        label = "AI Health Guide",
                                        icon = Icons.Outlined.AutoAwesome,
                                        iconBg = AccentTealBg,
                                        iconFg = AccentTealFg,
                                        onClick = onNavigateToAgent,
                                        testTag = "tile_ai_guide",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "console_freshness_curve") {
                FreshnessTrendChartCard(
                    title = "Stock Freshness Distribution",
                    subtitle = "Live inventory listings grouped by last-updated freshness window",
                    buckets = listOf(
                        "Fresh <=6h" to freshCount,
                        "Recent <=24h" to agingCount,
                        "Stale >24h" to staleCount,
                        "Out/Paused" to outCount
                    )
                )
            }

            item(key = "console_freshness_bar") {
                StockFreshnessCanvasCard(
                    freshCount = freshCount,
                    agingCount = agingCount,
                    staleCount = staleCount,
                    outOfStockCount = outCount
                )
            }

            item(key = "console_operational_summary") {
                val categoryCount = remember(medicines) {
                    medicines.mapNotNull { it.category.takeIf { c -> c.isNotBlank() } }.distinct().size
                }
                val lowStockDocs = inventory.count { it.availabilityEnum() == AvailabilityStatus.LOW_STOCK }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OperationalSummaryCard(
                            title = "Inventory Health",
                            linkLabel = "Open Collection >>",
                            leftStatValue = inventory.size.toString(),
                            leftStatLabel = "Stock Listings",
                            rightStatValue = lowStockDocs.toString(),
                            rightStatLabel = "Low Stock",
                            onLinkClick = { onOpenCollection(FirestoreCollectionTab.INVENTORY) },
                            modifier = Modifier.weight(1f)
                        )
                        OperationalSummaryCard(
                            title = "Medicine Catalogue",
                            linkLabel = "Inspect >>",
                            leftStatValue = verifiedMedCount.toString(),
                            leftStatLabel = "Verified Meds",
                            rightStatValue = categoryCount.toString(),
                            rightStatLabel = "Categories",
                            onLinkClick = { onOpenCollection(FirestoreCollectionTab.MEDICINES) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OperationalSummaryCard(
                            title = "Pharmacy Network",
                            linkLabel = "Manage Stores >>",
                            leftStatValue = approvedCount.toString(),
                            leftStatLabel = "Approved Stores",
                            rightStatValue = pendingPharmCount.toString(),
                            rightStatLabel = "Pending Queue",
                            onLinkClick = { onOpenCollection(FirestoreCollectionTab.PHARMACIES) },
                            modifier = Modifier.weight(1f)
                        )
                        OperationalSummaryCard(
                            title = "Reports & Audit",
                            linkLabel = "View Reports >>",
                            leftStatValue = openReportCount.toString(),
                            leftStatLabel = "Open Reports",
                            rightStatValue = history.size.toString(),
                            rightStatLabel = "Audit Events",
                            onLinkClick = { onOpenCollection(FirestoreCollectionTab.REPORTS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (isAdmin) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Admin Database Tools",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "$pendingPharmCount pharmacies pending • Seed or moderate records",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.seedPilotData() },
                                    enabled = !isBusy
                                ) {
                                    Icon(Icons.Outlined.Dataset, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Seed")
                                }
                                Button(onClick = onOpenAdminGovernance) {
                                    Icon(Icons.Outlined.AdminPanelSettings, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Review")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Realtime Activity",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onOpenCollection(FirestoreCollectionTab.HISTORY) }
                    )
                }
            }

            val recentHistory = history.sortedByDescending { it.changedAt?.seconds ?: 0L }.take(5)
            if (recentHistory.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "No stock updates recorded yet. Insert or adjust medicine stock to see live audit events stream here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(recentHistory, key = { it.id }) { h ->
                    HistoryAuditRow(h)
                }
            }
        }
    }

    if (showQuickMedDialog) {
        SubmitMedicineDialog(
            isAdmin = isAdmin,
            onDismiss = { showQuickMedDialog = false },
            onSubmit = { gen, brand, str, form, cat, kw ->
                viewModel.submitMedicine(gen, brand, str, form, cat, kw) {
                    showQuickMedDialog = false
                }
            }
        )
    }
}

@Composable
fun FirestoreDataExplorerPane(
    viewModel: MedFindViewModel,
    selectedCollection: FirestoreCollectionTab,
    onSelectCollection: (FirestoreCollectionTab) -> Unit,
    onJumpToPharmacyPortal: () -> Unit
) {
    val context = LocalContext.current
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val verifiedMedsState by viewModel.verifiedMedicinesState.collectAsStateWithLifecycle()
    val adminMedsState by viewModel.adminMedicinesState.collectAsStateWithLifecycle()
    val myPharmaciesState by viewModel.myPharmaciesState.collectAsStateWithLifecycle()
    val adminPharmaciesState by viewModel.adminPharmaciesState.collectAsStateWithLifecycle()
    val myInventoryState by viewModel.myPharmacyInventoryState.collectAsStateWithLifecycle()
    val adminInventoryState by viewModel.adminInventoryState.collectAsStateWithLifecycle()
    val myHistoryState by viewModel.myPharmacyHistoryState.collectAsStateWithLifecycle()
    val adminHistoryState by viewModel.adminHistoryState.collectAsStateWithLifecycle()
    val pharmacyReportsState by viewModel.pharmacyReportsState.collectAsStateWithLifecycle()
    val adminReportsState by viewModel.adminReportsState.collectAsStateWithLifecycle()
    val adminActionsState by viewModel.adminActionsState.collectAsStateWithLifecycle()

    val inventoryDocs = if (isAdmin) (adminInventoryState as? UiState.Success)?.data.orEmpty()
    else (myInventoryState as? UiState.Success)?.data.orEmpty()
    val pharmacyDocs = if (isAdmin) (adminPharmaciesState as? UiState.Success)?.data.orEmpty()
    else (myPharmaciesState as? UiState.Success)?.data.orEmpty()
    val medicineDocs = if (isAdmin) (adminMedsState as? UiState.Success)?.data.orEmpty()
    else (verifiedMedsState as? UiState.Success)?.data.orEmpty()
    val historyDocs = if (isAdmin) (adminHistoryState as? UiState.Success)?.data.orEmpty()
    else (myHistoryState as? UiState.Success)?.data.orEmpty()
    val reportDocs = if (isAdmin) (adminReportsState as? UiState.Success)?.data.orEmpty()
    else (pharmacyReportsState as? UiState.Success)?.data.orEmpty()
    val adminActionDocs = (adminActionsState as? UiState.Success)?.data.orEmpty()

    var docFilter by remember { mutableStateOf("") }
    var selectedDocId by remember(selectedCollection) { mutableStateOf<String?>(null) }
    var showAddMedDialog by remember { mutableStateOf(false) }

    val availableTabs = remember(isAdmin) {
        if (isAdmin) FirestoreCollectionTab.entries
        else FirestoreCollectionTab.entries.filter { it != FirestoreCollectionTab.ADMIN_ACTIONS }
    }

    fun countForTab(tab: FirestoreCollectionTab): Int = when (tab) {
        FirestoreCollectionTab.INVENTORY -> inventoryDocs.size
        FirestoreCollectionTab.PHARMACIES -> pharmacyDocs.size
        FirestoreCollectionTab.MEDICINES -> medicineDocs.size
        FirestoreCollectionTab.HISTORY -> historyDocs.size
        FirestoreCollectionTab.REPORTS -> reportDocs.size
        FirestoreCollectionTab.ADMIN_ACTIONS -> adminActionDocs.size
    }

    val q = docFilter.trim().lowercase()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp

        @Composable
        fun ExplorerToolbarContent() {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cloud Firestore Explorer",
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableTabs.forEach { tab ->
                        FilterChip(
                            selected = selectedCollection == tab,
                            onClick = { onSelectCollection(tab) },
                            leadingIcon = { Icon(Icons.Outlined.FolderOpen, null, Modifier.size(16.dp)) },
                            label = { Text("${tab.path} (${countForTab(tab)})") },
                            modifier = Modifier.testTag("collection_chip_${tab.path}")
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = docFilter,
                        onValueChange = { docFilter = it },
                        placeholder = { Text("Filter /${selectedCollection.path} documents...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            when (selectedCollection) {
                                FirestoreCollectionTab.MEDICINES -> showAddMedDialog = true
                                else -> onJumpToPharmacyPortal()
                            }
                        },
                        modifier = Modifier.testTag("explorer_add_doc_button")
                    ) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when (selectedCollection) {
                                FirestoreCollectionTab.MEDICINES -> "Add Med"
                                FirestoreCollectionTab.INVENTORY -> "Add Stock"
                                else -> "Manage"
                            }
                        )
                    }
                }
            }
        }

            @Composable
            fun DocumentInspectorContent(docId: String?) {
                if (docId == null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Select a document to inspect fields",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Tap any document ID in /${selectedCollection.path} to view live typed Firestore fields and trigger inline actions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    return
                }

                when (selectedCollection) {
                    FirestoreCollectionTab.INVENTORY -> {
                        val rec = inventoryDocs.firstOrNull { it.id == docId } ?: return
                        val ownerPharmacy = pharmacyDocs.firstOrNull { it.id == rec.pharmacyId }
                        val canEditInline = ownerPharmacy != null &&
                            ownerPharmacy.statusEnum() == PharmacyVerificationStatus.APPROVED &&
                            ownerPharmacy.ownerUserId == viewModel.currentUserId
                        val linkedMed = medicineDocs.firstOrNull { it.id == rec.medicineId }
                            ?: Medicine(
                                id = rec.medicineId,
                                genericName = rec.genericName,
                                brandName = rec.brandName,
                                strength = rec.strength,
                                dosageForm = rec.dosageForm
                            )

                        InspectorCard(path = "/inventory/${rec.id}") {
                            ConsoleFieldRow("medicineName", "string", rec.medicineName)
                            ConsoleFieldRow("pharmacyName", "string", rec.pharmacyName)
                            ConsoleFieldRow("availabilityStatus", "string", rec.availabilityStatus)
                            ConsoleFieldRow("quantity", "number", "${rec.quantity} ${rec.unit}")
                            ConsoleFieldRow("pharmacyVerified", "boolean", rec.pharmacyVerified.toString())
                            ConsoleFieldRow("coordinates", "geo", "${rec.pharmacyLat}, ${rec.pharmacyLon}")
                            ConsoleFieldRow("updatedByName", "string", rec.updatedByName)
                            ConsoleFieldRow("updatedAt", "timestamp", GeoAndTimeUtils.formatRelativeAndLocalTime(rec.effectiveTimestamp()))

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        openPharmacyDirections(
                                            context,
                                            rec.pharmacyLat,
                                            rec.pharmacyLon,
                                            rec.pharmacyName,
                                            rec.pharmacyAddress
                                        )
                                    }
                                ) {
                                    Icon(Icons.Outlined.Directions, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Google Maps")
                                }
                                if (canEditInline) {
                                    IconButton(
                                        onClick = {
                                            val nextQty = (rec.quantity - 1L).coerceAtLeast(0L)
                                            val nextSt = if (nextQty == 0L) AvailabilityStatus.OUT_OF_STOCK
                                            else if (nextQty <= 5L) AvailabilityStatus.LOW_STOCK
                                            else AvailabilityStatus.REPORTED_AVAILABLE
                                            viewModel.upsertInventory(ownerPharmacy, linkedMed, nextSt, nextQty, rec.unit)
                                        },
                                        enabled = !isBusy && rec.quantity > 0L
                                    ) {
                                        Icon(Icons.Outlined.Remove, "Decrease stock")
                                    }
                                    IconButton(
                                        onClick = {
                                            val nextQty = rec.quantity + 1L
                                            val nextSt = if (nextQty <= 5L) AvailabilityStatus.LOW_STOCK
                                            else AvailabilityStatus.REPORTED_AVAILABLE
                                            viewModel.upsertInventory(ownerPharmacy, linkedMed, nextSt, nextQty, rec.unit)
                                        },
                                        enabled = !isBusy
                                    ) {
                                        Icon(Icons.Outlined.Add, "Increase stock")
                                    }
                                }
                            }
                        }
                    }

                    FirestoreCollectionTab.PHARMACIES -> {
                        val p = pharmacyDocs.firstOrNull { it.id == docId } ?: return
                        InspectorCard(path = "/pharmacies/${p.id}") {
                            ConsoleFieldRow("name", "string", p.name)
                            ConsoleFieldRow("verificationStatus", "string", p.verificationStatus)
                            ConsoleFieldRow("licenseNumber", "string", p.licenseNumber)
                            ConsoleFieldRow("locality", "string", "${p.locality} (${p.pincode})")
                            ConsoleFieldRow("phone", "string", p.phone)
                            ConsoleFieldRow("openingHours", "string", p.openingHours)
                            ConsoleFieldRow("coordinates", "geo", "${p.latitude}, ${p.longitude}")
                            ConsoleFieldRow("updatedAt", "timestamp", GeoAndTimeUtils.formatLocalDate(p.updatedAt))

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        openPharmacyDirections(context, p.latitude, p.longitude, p.name, p.address)
                                    }
                                ) {
                                    Icon(Icons.Outlined.Directions, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Google Maps")
                                }
                                if (isAdmin && p.statusEnum() != PharmacyVerificationStatus.APPROVED) {
                                    Button(
                                        onClick = {
                                            viewModel.moderatePharmacy(p.id, PharmacyVerificationStatus.APPROVED, "Approved from Console Explorer")
                                        },
                                        enabled = !isBusy
                                    ) {
                                        Text("Approve")
                                    }
                                }
                                if (isAdmin && p.statusEnum() != PharmacyVerificationStatus.SUSPENDED) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.moderatePharmacy(p.id, PharmacyVerificationStatus.SUSPENDED, "Suspended from Console Explorer")
                                        },
                                        enabled = !isBusy
                                    ) {
                                        Text("Suspend")
                                    }
                                }
                            }
                        }
                    }

                    FirestoreCollectionTab.MEDICINES -> {
                        val m = medicineDocs.firstOrNull { it.id == docId } ?: return
                        InspectorCard(path = "/medicines/${m.id}") {
                            ConsoleFieldRow("genericName", "string", m.genericName)
                            ConsoleFieldRow("brandName", "string", m.brandName)
                            ConsoleFieldRow("strength", "string", m.strength)
                            ConsoleFieldRow("dosageForm", "string", m.dosageForm)
                            ConsoleFieldRow("category", "string", m.category)
                            ConsoleFieldRow("catalogueStatus", "string", m.catalogueStatus)
                            ConsoleFieldRow("keywords", "array", m.keywords.joinToString(", "))

                            if (isAdmin) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (m.statusEnum() != CatalogueStatus.VERIFIED) {
                                        Button(
                                            onClick = { viewModel.moderateMedicine(m.id, CatalogueStatus.VERIFIED, "Verified in Console Explorer") },
                                            enabled = !isBusy
                                        ) {
                                            Text("Verify")
                                        }
                                    }
                                    if (m.statusEnum() != CatalogueStatus.ARCHIVED) {
                                        OutlinedButton(
                                            onClick = { viewModel.moderateMedicine(m.id, CatalogueStatus.ARCHIVED, "Archived in Console Explorer") },
                                            enabled = !isBusy
                                        ) {
                                            Text("Archive")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FirestoreCollectionTab.HISTORY -> {
                        val h = historyDocs.firstOrNull { it.id == docId } ?: return
                        InspectorCard(path = "/inventory_history/${h.id}") {
                            ConsoleFieldRow("pharmacyName", "string", h.pharmacyName)
                            ConsoleFieldRow("medicineName", "string", h.medicineName)
                            ConsoleFieldRow("transition", "string", "${h.oldStatus} → ${h.newStatus}")
                            ConsoleFieldRow("quantityDelta", "number", "${h.oldQuantity} → ${h.newQuantity} ${h.unit}")
                            ConsoleFieldRow("changedByName", "string", h.changedByName)
                            ConsoleFieldRow("changedAt", "timestamp", GeoAndTimeUtils.formatLocalDate(h.changedAt))
                        }
                    }

                    FirestoreCollectionTab.REPORTS -> {
                        val r = reportDocs.firstOrNull { it.id == docId } ?: return
                        InspectorCard(path = "/feedback_reports/${r.id}") {
                            ConsoleFieldRow("pharmacyName", "string", r.pharmacyName)
                            ConsoleFieldRow("medicineName", "string", r.medicineName)
                            ConsoleFieldRow("reportType", "string", r.reportType)
                            ConsoleFieldRow("status", "string", r.status)
                            ConsoleFieldRow("reporterName", "string", r.reporterName)
                            ConsoleFieldRow("message", "string", r.message.ifBlank { "—" })
                            ConsoleFieldRow("createdAt", "timestamp", GeoAndTimeUtils.formatLocalDate(r.createdAt))

                            if (isAdmin && (r.statusEnum() == ReportStatus.OPEN || r.statusEnum() == ReportStatus.UNDER_REVIEW)) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.resolveReport(r.id, ReportStatus.RESOLVED, "Resolved via Console Explorer") },
                                        enabled = !isBusy
                                    ) {
                                        Text("Mark Resolved")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.resolveReport(r.id, ReportStatus.DISMISSED, "Dismissed via Console Explorer") },
                                        enabled = !isBusy
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }

                    FirestoreCollectionTab.ADMIN_ACTIONS -> {
                        val a = adminActionDocs.firstOrNull { it.id == docId } ?: return
                        InspectorCard(path = "/admin_actions/${a.id}") {
                            ConsoleFieldRow("targetType", "string", a.targetType)
                            ConsoleFieldRow("targetId", "string", a.targetId)
                            ConsoleFieldRow("action", "string", a.action)
                            ConsoleFieldRow("adminEmail", "string", a.adminEmail)
                            ConsoleFieldRow("reason", "string", a.reason.ifBlank { "—" })
                            ConsoleFieldRow("createdAt", "timestamp", GeoAndTimeUtils.formatLocalDate(a.createdAt))
                        }
                    }
                }
            }

            @Composable
            fun DocumentListColumn(includeToolbar: Boolean, modifier: Modifier = Modifier) {
                LazyColumn(
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (includeToolbar) {
                        item(key = "explorer_toolbar") {
                            ExplorerToolbarContent()
                        }
                    }
                    when (selectedCollection) {
                        FirestoreCollectionTab.INVENTORY -> {
                            val items = inventoryDocs.filter {
                                q.isBlank() || it.medicineName.lowercase().contains(q) || it.pharmacyName.lowercase().contains(q)
                            }
                            if (items.isEmpty()) item { EmptyCollectionNotice("/inventory") }
                            else items(items, key = { it.id }) { rec ->
                                ConsoleDocumentRow(
                                    docId = rec.id,
                                    title = rec.medicineName,
                                    subtitle = "${rec.pharmacyName} • Qty: ${rec.quantity} ${rec.unit}",
                                    isSelected = selectedDocId == rec.id,
                                    badge = { AvailabilityBadge(rec.availabilityEnum()) },
                                    onClick = { selectedDocId = if (selectedDocId == rec.id && !isWide) null else rec.id },
                                    expandedContent = if (!isWide && selectedDocId == rec.id) {
                                        { DocumentInspectorContent(rec.id) }
                                    } else null
                                )
                            }
                        }

                        FirestoreCollectionTab.PHARMACIES -> {
                            val items = pharmacyDocs.filter {
                                q.isBlank() || it.name.lowercase().contains(q) || it.locality.lowercase().contains(q)
                            }
                            if (items.isEmpty()) item { EmptyCollectionNotice("/pharmacies") }
                            else items(items, key = { it.id }) { p ->
                                ConsoleDocumentRow(
                                    docId = p.id,
                                    title = p.name,
                                    subtitle = "${p.locality} (${p.pincode}) • ${p.licenseNumber}",
                                    isSelected = selectedDocId == p.id,
                                    badge = { VerificationBadge(p.statusEnum()) },
                                    onClick = { selectedDocId = if (selectedDocId == p.id && !isWide) null else p.id },
                                    expandedContent = if (!isWide && selectedDocId == p.id) {
                                        { DocumentInspectorContent(p.id) }
                                    } else null
                                )
                            }
                        }

                        FirestoreCollectionTab.MEDICINES -> {
                            val items = medicineDocs.filter {
                                q.isBlank() || it.genericName.lowercase().contains(q) || it.brandName.lowercase().contains(q)
                            }
                            if (items.isEmpty()) item { EmptyCollectionNotice("/medicines") }
                            else items(items, key = { it.id }) { m ->
                                ConsoleDocumentRow(
                                    docId = m.id,
                                    title = m.displayTitle(),
                                    subtitle = "${m.dosageForm} • ${m.category}",
                                    isSelected = selectedDocId == m.id,
                                    badge = {
                                        StatusPill(
                                            text = m.statusEnum().name,
                                            bgColor = if (m.statusEnum() == CatalogueStatus.VERIFIED) StatusAvailableBg else StatusLowStockBg,
                                            fgColor = if (m.statusEnum() == CatalogueStatus.VERIFIED) StatusAvailableFg else StatusLowStockFg
                                        )
                                    },
                                    onClick = { selectedDocId = if (selectedDocId == m.id && !isWide) null else m.id },
                                    expandedContent = if (!isWide && selectedDocId == m.id) {
                                        { DocumentInspectorContent(m.id) }
                                    } else null
                                )
                            }
                        }

                        FirestoreCollectionTab.HISTORY -> {
                            val items = historyDocs.sortedByDescending { it.changedAt?.seconds ?: 0L }.filter {
                                q.isBlank() || it.medicineName.lowercase().contains(q) || it.pharmacyName.lowercase().contains(q)
                            }
                            if (items.isEmpty()) item { EmptyCollectionNotice("/inventory_history") }
                            else items(items, key = { it.id }) { h ->
                                ConsoleDocumentRow(
                                    docId = h.id,
                                    title = "${h.pharmacyName} • ${h.medicineName}",
                                    subtitle = "${h.oldStatus} → ${h.newStatus} (${h.newQuantity} ${h.unit})",
                                    isSelected = selectedDocId == h.id,
                                    badge = { Icon(Icons.Outlined.History, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { selectedDocId = if (selectedDocId == h.id && !isWide) null else h.id },
                                    expandedContent = if (!isWide && selectedDocId == h.id) {
                                        { DocumentInspectorContent(h.id) }
                                    } else null
                                )
                            }
                        }

                        FirestoreCollectionTab.REPORTS -> {
                            val items = reportDocs.sortedByDescending { it.createdAt?.seconds ?: 0L }.filter {
                                q.isBlank() || it.pharmacyName.lowercase().contains(q) || it.medicineName.lowercase().contains(q)
                            }
                            if (items.isEmpty()) item { EmptyCollectionNotice("/feedback_reports") }
                            else items(items, key = { it.id }) { r ->
                                ConsoleDocumentRow(
                                    docId = r.id,
                                    title = "${r.pharmacyName} • ${r.medicineName}",
                                    subtitle = r.typeEnum().label,
                                    isSelected = selectedDocId == r.id,
                                    badge = {
                                        StatusPill(
                                            text = r.statusEnum().label,
                                            bgColor = if (r.statusEnum() == ReportStatus.RESOLVED) StatusAvailableBg else StatusLowStockBg,
                                            fgColor = if (r.statusEnum() == ReportStatus.RESOLVED) StatusAvailableFg else StatusLowStockFg
                                        )
                                    },
                                    onClick = { selectedDocId = if (selectedDocId == r.id && !isWide) null else r.id },
                                    expandedContent = if (!isWide && selectedDocId == r.id) {
                                        { DocumentInspectorContent(r.id) }
                                    } else null
                                )
                            }
                        }

                        FirestoreCollectionTab.ADMIN_ACTIONS -> {
                            val items = adminActionDocs.sortedByDescending { it.createdAt?.seconds ?: 0L }.filter {
                                q.isBlank() || it.action.lowercase().contains(q) || it.targetType.lowercase().contains(q)
                            }
                            if (items.isEmpty()) item { EmptyCollectionNotice("/admin_actions") }
                            else items(items, key = { it.id }) { a ->
                                ConsoleDocumentRow(
                                    docId = a.id,
                                    title = "${a.targetType} • ${a.action}",
                                    subtitle = a.adminEmail,
                                    isSelected = selectedDocId == a.id,
                                    badge = { Icon(Icons.Outlined.AdminPanelSettings, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { selectedDocId = if (selectedDocId == a.id && !isWide) null else a.id },
                                    expandedContent = if (!isWide && selectedDocId == a.id) {
                                        { DocumentInspectorContent(a.id) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            if (isWide) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExplorerToolbarContent()
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DocumentListColumn(
                            includeToolbar = false,
                            modifier = Modifier
                                .weight(0.48f)
                                .fillMaxHeight()
                        )
                        Box(
                            modifier = Modifier
                                .weight(0.52f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            DocumentInspectorContent(selectedDocId)
                        }
                    }
                }
            } else {
                DocumentListColumn(
                    includeToolbar = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

    if (showAddMedDialog) {
        SubmitMedicineDialog(
            isAdmin = isAdmin,
            onDismiss = { showAddMedDialog = false },
            onSubmit = { gen, brand, str, form, cat, kw ->
                viewModel.submitMedicine(gen, brand, str, form, cat, kw) {
                    showAddMedDialog = false
                }
            }
        )
    }
}

@Composable
private fun ConsoleDocumentRow(
    docId: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    badge: @Composable () -> Unit,
    onClick: () -> Unit,
    expandedContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("console_doc_$docId"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = docId,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                badge()
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (expandedContent != null) {
                AnimatedVisibility(visible = isSelected) {
                    Column {
                        Spacer(Modifier.height(6.dp))
                        expandedContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorCard(
    path: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = path,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.secondary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            content()
        }
    }
}

@Composable
private fun EmptyCollectionNotice(collectionPath: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "No documents currently matched in $collectionPath.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun OperationalSummaryCard(
    title: String,
    linkLabel: String,
    leftStatValue: String,
    leftStatLabel: String,
    rightStatValue: String,
    rightStatLabel: String,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = linkLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onLinkClick)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = leftStatValue,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = leftStatLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rightStatValue,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = rightStatLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

