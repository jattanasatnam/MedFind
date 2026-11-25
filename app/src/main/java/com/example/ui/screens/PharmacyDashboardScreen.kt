package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingStateBox
import com.example.ui.components.QuickActionPillButton
import com.example.ui.components.SmartStoreInsightsCard
import com.example.ui.components.StatusPill
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
import com.example.ui.theme.AccentTealBg
import com.example.ui.theme.AccentTealFg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PharmacyHeroEnd
import com.example.ui.theme.PharmacyHeroStart
import com.example.ui.theme.StatusAvailableBg
import com.example.ui.theme.StatusLowStockBg
import com.example.ui.theme.StatusLowStockFg
import com.example.ui.viewmodel.MedFindViewModel

@Composable
fun PharmacyDashboardScreen(
    viewModel: MedFindViewModel
) {
    val context = LocalContext.current
    val myPharmaciesState by viewModel.myPharmaciesState.collectAsStateWithLifecycle()
    val myInventoryState by viewModel.myPharmacyInventoryState.collectAsStateWithLifecycle()
    val myHistoryState by viewModel.myPharmacyHistoryState.collectAsStateWithLifecycle()
    val verifiedMedicinesState by viewModel.verifiedMedicinesState.collectAsStateWithLifecycle()
    val mySubmittedMedicinesState by viewModel.mySubmittedMedicinesState.collectAsStateWithLifecycle()
    val pharmacyReportsState by viewModel.pharmacyReportsState.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedStoreId by rememberSaveable { mutableStateOf<String?>(null) }
    var inventorySearch by rememberSaveable { mutableStateOf("") }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var editingPharmacy by remember { mutableStateOf<Pharmacy?>(null) }
    var showStockDialog by remember { mutableStateOf(false) }
    var showAddMedicineDialog by remember { mutableStateOf(false) }
    var preselectedMedForStock by remember { mutableStateOf<Medicine?>(null) }
    var preselectedInvForStock by remember { mutableStateOf<InventoryRecord?>(null) }

    val pharmacies = (myPharmaciesState as? UiState.Success)?.data.orEmpty()
    val primaryPharmacy = remember(pharmacies, selectedStoreId) {
        pharmacies.firstOrNull { it.id == selectedStoreId }
            ?: pharmacies.firstOrNull { it.statusEnum() == PharmacyVerificationStatus.APPROVED }
            ?: pharmacies.firstOrNull()
    }
    val hasApprovedStore = isAdmin || pharmacies.any {
        it.statusEnum() == PharmacyVerificationStatus.APPROVED
    }
    val allMyInventory = (myInventoryState as? UiState.Success)?.data.orEmpty()
    val allMyReports = (pharmacyReportsState as? UiState.Success)?.data.orEmpty()
    val verifiedMeds = (verifiedMedicinesState as? UiState.Success)?.data.orEmpty()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 920.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "pharmacy_hero_header") {
                val storeInventory = if (primaryPharmacy != null) {
                    allMyInventory.filter { it.pharmacyId == primaryPharmacy.id }
                } else emptyList()
                val freshCount = storeInventory.count {
                    (it.availabilityEnum() == AvailabilityStatus.REPORTED_AVAILABLE ||
                        it.availabilityEnum() == AvailabilityStatus.LOW_STOCK) &&
                        it.freshnessTier() == FreshnessTier.FRESH
                }
                val isApproved = primaryPharmacy?.statusEnum() == PharmacyVerificationStatus.APPROVED

                PharmacyHeroBanner(
                    store = primaryPharmacy,
                    totalListings = storeInventory.size,
                    freshListings = freshCount,
                    hasApprovedStore = hasApprovedStore,
                    isApprovedPrimary = isApproved,
                    hasAnyStore = pharmacies.isNotEmpty(),
                    onAddMedicine = { showAddMedicineDialog = true },
                    onInsertStock = {
                        preselectedInvForStock = null
                        preselectedMedForStock = null
                        showStockDialog = true
                    },
                    onRegisterStore = { showRegisterDialog = true }
                )
            }

            when (val pState = myPharmaciesState) {
                is UiState.Loading -> item { LoadingStateBox("Loading your pharmacy store...") }
                is UiState.Error -> item { ErrorStateCard(pState.message) }
                is UiState.Success -> {
                    if (pState.data.isEmpty() || primaryPharmacy == null) {
                        item(key = "register_store_onboarding") {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = AccentTealBg,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.LocalPharmacy,
                                                contentDescription = null,
                                                tint = AccentTealFg
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Register Your Medical Store to Update Stock",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = if (isAdmin) {
                                            "Signed in with the Admin account: registering your store approves it immediately so you can insert medicines and live stock quantities right away (or seed pilot stores from Admin Console)."
                                        } else {
                                            "1. Submit your medical store name, drug license, address & phone.\n2. Once verified by an Admin, your store status turns Verified.\n3. Add medicines & update stock quantities so nearby customers can call or navigate via Google Maps."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { showRegisterDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("cta_register_pharmacy")
                                    ) {
                                        Icon(Icons.Outlined.AddBusiness, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Register Medical Store")
                                    }
                                }
                            }
                        }
                    } else {
                        val isApproved = primaryPharmacy.statusEnum() == PharmacyVerificationStatus.APPROVED
                        val storeInventory = allMyInventory.filter { it.pharmacyId == primaryPharmacy.id }
                        val storeReports = allMyReports.filter { it.pharmacyId == primaryPharmacy.id }

                        val lowStockItems = storeInventory.filter {
                            it.availabilityEnum() == AvailabilityStatus.LOW_STOCK ||
                                it.availabilityEnum() == AvailabilityStatus.OUT_OF_STOCK
                        }
                        val staleItems = storeInventory.filter {
                            (it.availabilityEnum() == AvailabilityStatus.REPORTED_AVAILABLE ||
                                it.availabilityEnum() == AvailabilityStatus.LOW_STOCK) &&
                                it.isStale()
                        }
                        val freshCount = storeInventory.count {
                            (it.availabilityEnum() == AvailabilityStatus.REPORTED_AVAILABLE ||
                                it.availabilityEnum() == AvailabilityStatus.LOW_STOCK) &&
                                it.freshnessTier() == FreshnessTier.FRESH
                        }
                        val openReports = storeReports.filter {
                            it.statusEnum() == ReportStatus.OPEN || it.statusEnum() == ReportStatus.UNDER_REVIEW
                        }

                        if (pState.data.size > 1) {
                            item(key = "store_selector") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Active Store:",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    pState.data.forEach { store ->
                                        FilterChip(
                                            selected = store.id == primaryPharmacy.id,
                                            onClick = { selectedStoreId = store.id },
                                            label = { Text(store.name) }
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "store_profile_card") {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = primaryPharmacy.name,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "${primaryPharmacy.locality} • ${primaryPharmacy.pincode} • Lic: ${primaryPharmacy.licenseNumber}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${primaryPharmacy.openingHours} • ${primaryPharmacy.phone}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        VerificationBadge(primaryPharmacy.statusEnum())
                                    }
                                    if (primaryPharmacy.adminNote.isNotBlank()) {
                                        Text(
                                            text = "Admin note: ${primaryPharmacy.adminNote}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    if (!isApproved) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = StatusLowStockBg,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (isAdmin) {
                                                    "Store is awaiting approval. Switch to Admin Console via Profile -> Approvals to approve it immediately."
                                                } else {
                                                    "Store verification is PENDING. Once approved by an administrator, you can insert medicines and update live customer stock."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StatusLowStockFg,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "pharmacy_quick_actions") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Quick Store Actions",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (isApproved) {
                                        QuickActionPillButton(
                                            label = "Update Stock",
                                            icon = Icons.Outlined.Inventory2,
                                            iconBg = AccentMintBg,
                                            iconFg = AccentMintFg,
                                            onClick = {
                                                preselectedInvForStock = null
                                                preselectedMedForStock = null
                                                showStockDialog = true
                                            },
                                            testTag = "quick_update_stock",
                                            modifier = Modifier.width(116.dp)
                                        )
                                        QuickActionPillButton(
                                            label = "Add Medicine",
                                            icon = Icons.Outlined.Medication,
                                            iconBg = AccentBlueBg,
                                            iconFg = AccentBlueFg,
                                            onClick = { showAddMedicineDialog = true },
                                            testTag = "quick_add_medicine",
                                            modifier = Modifier.width(116.dp)
                                        )
                                    }
                                    QuickActionPillButton(
                                        label = "Edit Store",
                                        icon = Icons.Outlined.Edit,
                                        iconBg = AccentAmberBg,
                                        iconFg = AccentAmberFg,
                                        onClick = { editingPharmacy = primaryPharmacy },
                                        testTag = "quick_edit_store",
                                        modifier = Modifier.width(116.dp)
                                    )
                                    QuickActionPillButton(
                                        label = "Google Maps",
                                        icon = Icons.Outlined.Directions,
                                        iconBg = AccentTealBg,
                                        iconFg = AccentTealFg,
                                        onClick = {
                                            openPharmacyDirections(
                                                context = context,
                                                lat = primaryPharmacy.latitude,
                                                lon = primaryPharmacy.longitude,
                                                label = primaryPharmacy.name,
                                                address = primaryPharmacy.address
                                            )
                                        },
                                        testTag = "quick_store_maps",
                                        modifier = Modifier.width(116.dp)
                                    )
                                }
                            }
                        }

                        item(key = "pharmacy_insights_and_alerts") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SmartStoreInsightsCard(
                                    freshCount = freshCount,
                                    lowStockCount = lowStockItems.size,
                                    staleCount = staleItems.size,
                                    openReportsCount = openReports.size
                                )

                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val isWide = maxWidth >= 700.dp
                                    if (isWide) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            LowStockAlertCard(
                                                lowStockItems = lowStockItems,
                                                canRestock = isApproved,
                                                onRestock = { firstRec ->
                                                    preselectedInvForStock = firstRec
                                                    preselectedMedForStock = firstRec?.let { rec ->
                                                        verifiedMeds.firstOrNull { it.id == rec.medicineId }
                                                            ?: Medicine(
                                                                id = rec.medicineId,
                                                                genericName = rec.genericName,
                                                                brandName = rec.brandName,
                                                                strength = rec.strength,
                                                                dosageForm = rec.dosageForm
                                                            )
                                                    }
                                                    showStockDialog = true
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            StaleFreshnessAlertCard(
                                                staleItems = staleItems,
                                                totalActive = storeInventory.size,
                                                canRefresh = isApproved,
                                                onRefresh = { firstStale ->
                                                    preselectedInvForStock = firstStale
                                                    preselectedMedForStock = firstStale?.let { rec ->
                                                        verifiedMeds.firstOrNull { it.id == rec.medicineId }
                                                            ?: Medicine(
                                                                id = rec.medicineId,
                                                                genericName = rec.genericName,
                                                                brandName = rec.brandName,
                                                                strength = rec.strength,
                                                                dosageForm = rec.dosageForm
                                                            )
                                                    }
                                                    if (firstStale != null && isApproved) {
                                                        showStockDialog = true
                                                    } else {
                                                        selectedTab = 0
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            StoreReportsSummaryCard(
                                                reports = storeReports,
                                                onViewReports = { selectedTab = 2 },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                LowStockAlertCard(
                                                    lowStockItems = lowStockItems,
                                                    canRestock = isApproved,
                                                    onRestock = { firstRec ->
                                                        preselectedInvForStock = firstRec
                                                        preselectedMedForStock = firstRec?.let { rec ->
                                                            verifiedMeds.firstOrNull { it.id == rec.medicineId }
                                                                ?: Medicine(
                                                                    id = rec.medicineId,
                                                                    genericName = rec.genericName,
                                                                    brandName = rec.brandName,
                                                                    strength = rec.strength,
                                                                    dosageForm = rec.dosageForm
                                                                )
                                                        }
                                                        showStockDialog = true
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                StaleFreshnessAlertCard(
                                                    staleItems = staleItems,
                                                    totalActive = storeInventory.size,
                                                    canRefresh = isApproved,
                                                    onRefresh = { firstStale ->
                                                        preselectedInvForStock = firstStale
                                                        preselectedMedForStock = firstStale?.let { rec ->
                                                            verifiedMeds.firstOrNull { it.id == rec.medicineId }
                                                                ?: Medicine(
                                                                    id = rec.medicineId,
                                                                    genericName = rec.genericName,
                                                                    brandName = rec.brandName,
                                                                    strength = rec.strength,
                                                                    dosageForm = rec.dosageForm
                                                                )
                                                        }
                                                        if (firstStale != null && isApproved) {
                                                            showStockDialog = true
                                                        } else {
                                                            selectedTab = 0
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            StoreReportsSummaryCard(
                                                reports = storeReports,
                                                onViewReports = { selectedTab = 2 },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "pharmacy_tabs") {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                TabRow(
                                    selectedTabIndex = selectedTab,
                                    containerColor = Color.Transparent
                                ) {
                                    Tab(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        text = { Text("Inventory (${storeInventory.size})") }
                                    )
                                    Tab(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        text = { Text("History") }
                                    )
                                    Tab(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2 },
                                        text = { Text("Reports (${storeReports.size})") }
                                    )
                                }
                            }
                        }

                        when (selectedTab) {
                            0 -> {
                                val submittedMeds = (mySubmittedMedicinesState as? UiState.Success)?.data
                                    ?.filter { it.statusEnum() != CatalogueStatus.VERIFIED }
                                    .orEmpty()

                                if (submittedMeds.isNotEmpty()) {
                                    item(key = "pending_meds_card") {
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Submitted Medicines Pending Catalogue Review",
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                submittedMeds.forEach { sm ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = sm.displayTitle(),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        StatusPill(
                                                            text = sm.statusEnum().name,
                                                            bgColor = StatusLowStockBg,
                                                            fgColor = StatusLowStockFg
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                when (val iState = myInventoryState) {
                                    is UiState.Loading -> item { LoadingStateBox() }
                                    is UiState.Error -> item { ErrorStateCard(iState.message) }
                                    is UiState.Success -> {
                                        if (storeInventory.isEmpty()) {
                                            item(key = "empty_inventory") {
                                                Card(
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surface
                                                    ),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(16.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isApproved) {
                                                                "No stock listings published for ${primaryPharmacy.name} yet. Tap 'Insert / Update Stock' above to add available medicines."
                                                            } else {
                                                                "Inventory publishing unlocks as soon as your pharmacy is approved by an administrator."
                                                            },
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            item(key = "inventory_search_bar") {
                                                OutlinedTextField(
                                                    value = inventorySearch,
                                                    onValueChange = { inventorySearch = it },
                                                    placeholder = { Text("Search medicine in store inventory...") },
                                                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            val invQ = inventorySearch.trim().lowercase()
                                            val displayedInventory = if (invQ.isBlank()) storeInventory else storeInventory.filter {
                                                it.medicineName.lowercase().contains(invQ) ||
                                                    it.genericName.lowercase().contains(invQ) ||
                                                    it.brandName.lowercase().contains(invQ)
                                            }
                                            items(displayedInventory, key = { it.id }) { rec ->
                                                val linkedMed = verifiedMeds
                                                    .firstOrNull { it.id == rec.medicineId }
                                                    ?: Medicine(
                                                        id = rec.medicineId,
                                                        genericName = rec.genericName,
                                                        brandName = rec.brandName,
                                                        strength = rec.strength,
                                                        dosageForm = rec.dosageForm
                                                    )
                                                val formBadgeLetter = rec.dosageForm.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
                                                Card(
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(14.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.weight(1f),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                            ) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    color = AccentTealBg,
                                                                    modifier = Modifier.size(34.dp)
                                                                ) {
                                                                    Box(contentAlignment = Alignment.Center) {
                                                                        Text(
                                                                            text = formBadgeLetter,
                                                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                                            color = AccentTealFg
                                                                        )
                                                                    }
                                                                }
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = rec.medicineName,
                                                                        style = MaterialTheme.typography.titleSmall,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    Text(
                                                                        text = "${rec.dosageForm.ifBlank { "Medicine" }} • Qty: ${rec.quantity} ${rec.unit}",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }
                                                            Spacer(Modifier.width(6.dp))
                                                            AvailabilityBadge(rec.availabilityEnum())
                                                        }
                                                        Text(
                                                            text = "Updated by ${rec.updatedByName} • ${GeoAndTimeUtils.formatRelativeAndLocalTime(rec.effectiveTimestamp())}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        if (isApproved) {
                                                            Row(
                                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                IconButton(
                                                                    onClick = {
                                                                        val nextQty = (rec.quantity - 1L).coerceAtLeast(0L)
                                                                        val nextStatus = when {
                                                                            nextQty == 0L -> AvailabilityStatus.OUT_OF_STOCK
                                                                            nextQty <= 5L -> AvailabilityStatus.LOW_STOCK
                                                                            else -> AvailabilityStatus.REPORTED_AVAILABLE
                                                                        }
                                                                        viewModel.upsertInventory(
                                                                            pharmacy = primaryPharmacy,
                                                                            medicine = linkedMed,
                                                                            availabilityStatus = nextStatus,
                                                                            quantity = nextQty,
                                                                            unit = rec.unit
                                                                        )
                                                                    },
                                                                    enabled = !isBusy && rec.quantity > 0L
                                                                ) {
                                                                    Icon(Icons.Outlined.Remove, contentDescription = "Decrease quantity")
                                                                }
                                                                IconButton(
                                                                    onClick = {
                                                                        val nextQty = rec.quantity + 1L
                                                                        val nextStatus = if (nextQty <= 5L) {
                                                                            AvailabilityStatus.LOW_STOCK
                                                                        } else {
                                                                            AvailabilityStatus.REPORTED_AVAILABLE
                                                                        }
                                                                        viewModel.upsertInventory(
                                                                            pharmacy = primaryPharmacy,
                                                                            medicine = linkedMed,
                                                                            availabilityStatus = nextStatus,
                                                                            quantity = nextQty,
                                                                            unit = rec.unit
                                                                        )
                                                                    },
                                                                    enabled = !isBusy
                                                                ) {
                                                                    Icon(Icons.Outlined.Add, contentDescription = "Increase quantity")
                                                                }
                                                                OutlinedButton(
                                                                    onClick = {
                                                                        preselectedInvForStock = rec
                                                                        preselectedMedForStock = linkedMed
                                                                        showStockDialog = true
                                                                    }
                                                                ) {
                                                                    Text("Edit Stock")
                                                                }
                                                                if (rec.availabilityEnum() != AvailabilityStatus.REPORTED_AVAILABLE) {
                                                                    OutlinedButton(
                                                                        onClick = {
                                                                            viewModel.upsertInventory(
                                                                                pharmacy = primaryPharmacy,
                                                                                medicine = linkedMed,
                                                                                availabilityStatus = AvailabilityStatus.REPORTED_AVAILABLE,
                                                                                quantity = rec.quantity.coerceAtLeast(10L),
                                                                                unit = rec.unit
                                                                            )
                                                                        },
                                                                        enabled = !isBusy
                                                                    ) {
                                                                        Text("Mark Available")
                                                                    }
                                                                }
                                                                if (rec.availabilityEnum() != AvailabilityStatus.PAUSED) {
                                                                    OutlinedButton(
                                                                        onClick = {
                                                                            viewModel.upsertInventory(
                                                                                pharmacy = primaryPharmacy,
                                                                                medicine = linkedMed,
                                                                                availabilityStatus = AvailabilityStatus.PAUSED,
                                                                                quantity = rec.quantity,
                                                                                unit = rec.unit
                                                                            )
                                                                        },
                                                                        enabled = !isBusy
                                                                    ) {
                                                                        Icon(Icons.Outlined.PauseCircle, null, Modifier.size(16.dp))
                                                                        Spacer(Modifier.width(4.dp))
                                                                        Text("Pause")
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            1 -> {
                                when (val hState = myHistoryState) {
                                    is UiState.Loading -> item { LoadingStateBox() }
                                    is UiState.Error -> item { ErrorStateCard(hState.message) }
                                    is UiState.Success -> {
                                        val storeHistory = hState.data.filter { it.pharmacyId == primaryPharmacy.id }
                                        if (storeHistory.isEmpty()) {
                                            item {
                                                Text(
                                                    text = "No stock changes recorded for ${primaryPharmacy.name} yet.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        } else {
                                            items(storeHistory, key = { it.id }) { h ->
                                                HistoryAuditRow(h)
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                when (val rState = pharmacyReportsState) {
                                    is UiState.Loading -> item { LoadingStateBox() }
                                    is UiState.Error -> item { ErrorStateCard(rState.message) }
                                    is UiState.Success -> {
                                        if (storeReports.isEmpty()) {
                                            item {
                                                Text(
                                                    text = "No customer correction reports for ${primaryPharmacy.name}.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        } else {
                                            items(storeReports, key = { it.id }) { rep ->
                                                Card(
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(14.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(rep.medicineName, style = MaterialTheme.typography.titleSmall)
                                                        Text(
                                                            "Reported: ${rep.typeEnum().label} (${rep.statusEnum().label})",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        if (rep.message.isNotBlank()) {
                                                            Text(
                                                                rep.message,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRegisterDialog) {
        PharmacyFormDialog(
            title = "Register Medical Store",
            existing = null,
            onDismiss = { showRegisterDialog = false },
            onSave = { name, lic, addr, loc, pin, phone, hours ->
                viewModel.registerPharmacy(name, lic, addr, loc, pin, phone, hours) {
                    showRegisterDialog = false
                }
            }
        )
    }

    editingPharmacy?.let { pharm ->
        PharmacyFormDialog(
            title = "Edit Pharmacy Profile",
            existing = pharm,
            onDismiss = { editingPharmacy = null },
            onSave = { name, _, addr, loc, pin, phone, hours ->
                viewModel.updatePharmacyProfile(pharm.id, name, addr, loc, pin, phone, hours) {
                    editingPharmacy = null
                }
            }
        )
    }

    if (showAddMedicineDialog) {
        val approvedStoreId = (primaryPharmacy?.takeIf {
            it.statusEnum() == PharmacyVerificationStatus.APPROVED
        } ?: pharmacies.firstOrNull { it.statusEnum() == PharmacyVerificationStatus.APPROVED })?.id
        SubmitMedicineDialog(
            isAdmin = isAdmin,
            onDismiss = { showAddMedicineDialog = false },
            onSubmit = { gen, brand, str, form, cat, kw ->
                viewModel.submitMedicine(
                    genericName = gen,
                    brandName = brand,
                    strength = str,
                    dosageForm = form,
                    category = cat,
                    keywordsRaw = kw,
                    submittedPharmacyId = approvedStoreId
                ) {
                    showAddMedicineDialog = false
                }
            }
        )
    }

    if (showStockDialog) {
        val approvedPharmacy = primaryPharmacy?.takeIf {
            it.statusEnum() == PharmacyVerificationStatus.APPROVED
        } ?: pharmacies.firstOrNull { it.statusEnum() == PharmacyVerificationStatus.APPROVED }
        if (approvedPharmacy != null) {
            UpsertStockDialog(
                medicines = verifiedMeds,
                initialMedicine = preselectedMedForStock,
                initialInventory = preselectedInvForStock,
                onOpenAddMedicine = {
                    showStockDialog = false
                    showAddMedicineDialog = true
                },
                onDismiss = { showStockDialog = false },
                onSave = { med, status, qty, unit ->
                    viewModel.upsertInventory(approvedPharmacy, med, status, qty, unit) {
                        showStockDialog = false
                    }
                }
            )
        }
    }
}

@Composable
private fun PharmacyHeroBanner(
    store: Pharmacy?,
    totalListings: Int,
    freshListings: Int,
    hasApprovedStore: Boolean,
    isApprovedPrimary: Boolean,
    hasAnyStore: Boolean,
    onAddMedicine: () -> Unit,
    onInsertStock: () -> Unit,
    onRegisterStore: () -> Unit
) {
    val heroGradient = Brush.linearGradient(
        colors = listOf(PharmacyHeroStart, PharmacyHeroEnd)
    )
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGradient)
                .padding(18.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(96.dp)
            ) {
                rotate(degrees = -30f) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.14f),
                        topLeft = Offset(size.width * 0.15f, size.height * 0.25f),
                        size = Size(size.width * 0.72f, size.height * 0.42f),
                        cornerRadius = CornerRadius(size.height * 0.21f, size.height * 0.21f),
                        style = Stroke(width = 7f)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.14f),
                        start = Offset(size.width * 0.51f, size.height * 0.25f),
                        end = Offset(size.width * 0.51f, size.height * 0.67f),
                        strokeWidth = 6f
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StatusAvailableBg)
                    )
                    Text(
                        text = if (isApprovedPrimary) "VERIFIED PARTNER WORKSPACE" else "PHARMACY STORE PORTAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Text(
                    text = store?.name ?: "Pharmacy Store Portal",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Publish verified stock availability, manage medicine entries & keep freshness timestamps accurate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.86f)
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isApprovedPrimary) {
                        Button(
                            onClick = onInsertStock,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = PharmacyHeroStart
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_inventory_button")
                        ) {
                            Icon(Icons.Outlined.Inventory2, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Insert / Update Stock")
                        }
                    }
                    OutlinedButton(
                        onClick = onAddMedicine,
                        enabled = hasApprovedStore,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("pharmacy_add_medicine_button")
                    ) {
                        Icon(Icons.Outlined.Medication, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Medicine")
                    }
                    OutlinedButton(
                        onClick = onRegisterStore,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("register_pharmacy_button")
                    ) {
                        Icon(Icons.Outlined.AddBusiness, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (hasAnyStore) "Add Store" else "Register Store")
                    }
                }

                if (store != null) {
                    Text(
                        text = "$totalListings live listings • $freshListings fresh (<=6h) • ${store.locality}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = StatusAvailableBg
                    )
                }
            }
        }
    }
}

@Composable
private fun LowStockAlertCard(
    lowStockItems: List<InventoryRecord>,
    canRestock: Boolean,
    onRestock: (InventoryRecord?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentAmberBg,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.WarningAmber, null, tint = AccentAmberFg, modifier = Modifier.size(18.dp))
                    }
                }
                Column {
                    Text("Low Stock", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${lowStockItems.size} item(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (lowStockItems.isEmpty()) {
                Text(
                    text = "All active items have healthy stock.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                lowStockItems.take(3).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.genericName.ifBlank { item.medicineName },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${item.quantity} ${item.unit}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AccentAmberFg
                        )
                    }
                }
            }
            Button(
                onClick = { onRestock(lowStockItems.firstOrNull()) },
                enabled = canRestock,
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmberFg),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restock Now", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun StaleFreshnessAlertCard(
    staleItems: List<InventoryRecord>,
    totalActive: Int,
    canRefresh: Boolean,
    onRefresh: (InventoryRecord?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentRoseBg,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Schedule, null, tint = AccentRoseFg, modifier = Modifier.size(18.dp))
                    }
                }
                Column {
                    Text("Stale >24h", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${staleItems.size} need timestamp",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Segmented freshness blocks inspired by reference card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val activeDenominator = totalActive.coerceAtLeast(1)
                val staleBars = ((staleItems.size.toFloat() / activeDenominator) * 8).toInt().coerceIn(
                    if (staleItems.isNotEmpty()) 1 else 0,
                    8
                )
                for (i in 0 until 8) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (i < staleBars) AccentRoseFg
                                else AccentMintBg
                            )
                    )
                }
            }

            Text(
                text = if (staleItems.isEmpty()) "All listings fresh within 24h"
                else "${staleItems.size} listing(s) need fresh confirmation",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { onRefresh(staleItems.firstOrNull()) },
                enabled = canRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRoseFg),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Update, null, Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Confirm Stock", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun StoreReportsSummaryCard(
    reports: List<FeedbackReport>,
    onViewReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val openCount = reports.count {
        it.statusEnum() == ReportStatus.OPEN || it.statusEnum() == ReportStatus.UNDER_REVIEW
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentBlueBg,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Flag, null, tint = AccentBlueFg, modifier = Modifier.size(18.dp))
                    }
                }
                Column {
                    Text("Customer Reports", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "$openCount open • ${reports.size} total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (reports.isEmpty()) {
                Text(
                    text = "No customer mismatch reports recorded for this store.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                reports.take(2).forEach { rep ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = rep.medicineName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = rep.statusEnum().label,
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentBlueFg
                        )
                    }
                }
            }
            Button(
                onClick = onViewReports,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlueFg),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Review Reports", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun HistoryAuditRow(history: InventoryHistoryRecord) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${history.pharmacyName} • ${history.medicineName}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${history.oldStatus} (${history.oldQuantity}) → ${history.newStatus} (${history.newQuantity} ${history.unit})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "By ${history.changedByName} • ${GeoAndTimeUtils.formatLocalDate(history.changedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PharmacyFormDialog(
    title: String,
    existing: Pharmacy?,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var license by remember { mutableStateOf(existing?.licenseNumber ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var locality by remember { mutableStateOf(existing?.locality ?: "Indiranagar") }
    var pincode by remember { mutableStateOf(existing?.pincode ?: "560038") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var hours by remember { mutableStateOf(existing?.openingHours ?: "08:00 AM - 10:00 PM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Pharmacy Name *") }, singleLine = true)
                if (existing == null) {
                    OutlinedTextField(value = license, onValueChange = { license = it }, label = { Text("Drug License Number *") }, singleLine = true)
                }
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Full Street Address *") })
                OutlinedTextField(value = locality, onValueChange = { locality = it }, label = { Text("Locality *") }, singleLine = true)
                OutlinedTextField(value = pincode, onValueChange = { pincode = it }, label = { Text("Pincode *") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Contact Phone *") }, singleLine = true)
                OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Opening Hours *") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, license, address, locality, pincode, phone, hours) },
                enabled = name.trim().length >= 2 &&
                    (existing != null || license.trim().length >= 2) &&
                    address.trim().length >= 5 &&
                    phone.trim().length >= 5
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun UpsertStockDialog(
    medicines: List<Medicine>,
    initialMedicine: Medicine?,
    initialInventory: InventoryRecord?,
    onOpenAddMedicine: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (Medicine, AvailabilityStatus, Long, String) -> Unit
) {
    var medFilter by remember { mutableStateOf("") }
    var selectedMed by remember { mutableStateOf(initialMedicine ?: medicines.firstOrNull()) }
    var status by remember {
        mutableStateOf(initialInventory?.availabilityEnum() ?: AvailabilityStatus.REPORTED_AVAILABLE)
    }
    var qtyText by remember { mutableStateOf((initialInventory?.quantity ?: 20L).toString()) }
    var unit by remember { mutableStateOf(initialInventory?.unit ?: "strips") }

    val filteredMedicines = remember(medicines, medFilter) {
        val q = medFilter.trim().lowercase()
        if (q.isBlank()) medicines
        else medicines.filter {
            it.genericName.lowercase().contains(q) ||
                it.brandName.lowercase().contains(q) ||
                it.strength.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert / Update Pharmacy Stock") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (medicines.isEmpty() && initialInventory == null) {
                    Text(
                        text = "No verified catalogue medicines available yet. Submit a medicine to the catalogue or ask an admin to seed pilot data.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(onClick = onOpenAddMedicine) {
                        Icon(Icons.Outlined.Medication, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Medicine to Catalogue")
                    }
                } else if (initialInventory == null) {
                    OutlinedTextField(
                        value = medFilter,
                        onValueChange = { medFilter = it },
                        label = { Text("Filter verified medicines") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredMedicines.forEach { med ->
                            FilterChip(
                                selected = selectedMed?.id == med.id,
                                onClick = { selectedMed = med },
                                label = { Text("${med.genericName} ${med.strength} (${med.brandName})") }
                            )
                        }
                    }
                    TextButton(onClick = onOpenAddMedicine) {
                        Text("+ Medicine missing? Submit new entry")
                    }
                } else {
                    Text(initialInventory.medicineName, style = MaterialTheme.typography.titleSmall)
                }

                Text("Availability Status:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AvailabilityStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = {
                                status = st
                                if (st == AvailabilityStatus.OUT_OF_STOCK) qtyText = "0"
                            },
                            label = { Text(st.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { raw -> qtyText = raw.filter { it.isDigit() } },
                    enabled = status != AvailabilityStatus.OUT_OF_STOCK,
                    label = {
                        Text(
                            if (status == AvailabilityStatus.OUT_OF_STOCK) {
                                "Quantity (locked at 0 for Out of Stock)"
                            } else {
                                "Reported Quantity"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (e.g. strips, bottles, boxes)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val medToSave = selectedMed ?: initialInventory?.let {
                Medicine(
                    id = it.medicineId,
                    genericName = it.genericName,
                    brandName = it.brandName,
                    strength = it.strength,
                    dosageForm = it.dosageForm
                )
            }
            val parsedQty = qtyText.toLongOrNull() ?: 0L
            Button(
                onClick = {
                    if (medToSave != null) {
                        onSave(medToSave, status, parsedQty, unit)
                    }
                },
                enabled = medToSave != null && unit.isNotBlank()
            ) {
                Text("Publish Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
