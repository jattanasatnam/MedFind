package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.EditLocationAlt
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.firebase.UiState
import com.example.data.model.AvailabilityStatus
import com.example.data.model.GeoAndTimeUtils
import com.example.data.model.InventoryRecord
import com.example.data.model.Medicine
import com.example.data.model.Pharmacy
import com.example.data.model.ReportType
import com.example.ui.components.AvailabilityBadge
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.FreshnessBadge
import com.example.ui.components.InteractiveNearbyMapCard
import com.example.ui.components.LoadingStateBox
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.MedicinePackThumbnail
import com.example.ui.components.StatusPill
import com.example.ui.components.dialPharmacy
import com.example.ui.components.openPharmacyDirections
import com.example.ui.viewmodel.MedFindViewModel
import com.example.ui.viewmodel.PharmacyInventoryItem
import com.example.ui.viewmodel.PharmacySortOrder
import com.google.android.gms.location.LocationServices

@Composable
fun DiscoverScreen(
    viewModel: MedFindViewModel
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val medicinesState by viewModel.verifiedMedicinesState.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val selectedMedicine by viewModel.selectedMedicine.collectAsStateWithLifecycle()
    val selectedPharmacy by viewModel.selectedPharmacy.collectAsStateWithLifecycle()
    val resultsState by viewModel.selectedMedicineResultsState.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val hideStale by viewModel.hideStaleReports.collectAsStateWithLifecycle()
    val availableOnly by viewModel.availableOnly.collectAsStateWithLifecycle()
    val pharmaciesState by viewModel.approvedPharmaciesState.collectAsStateWithLifecycle()
    val selectedPharmacyStockState by viewModel.selectedPharmacyInventoryState.collectAsStateWithLifecycle()

    var showLocationDialog by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<InventoryRecord?>(null) }

    val profile = (profileState as? UiState.Success)?.data
    val savedPharmacyIds = profile?.savedPharmacyIds.orEmpty()

    @SuppressLint("MissingPermission")
    fun fetchCoarseLocation() {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                viewModel.updateDeviceLocation(loc.latitude, loc.longitude)
            } else {
                showLocationDialog = true
            }
        }.addOnFailureListener {
            showLocationDialog = true
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchCoarseLocation()
        } else {
            showLocationDialog = true
        }
    }

    fun requestNearbyGps() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            fetchCoarseLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp

        if (!isWide) {
            BackHandler(enabled = selectedMedicine != null) {
                viewModel.selectMedicine(null)
            }
        }

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                ) {
                    MedicineCataloguePane(
                        searchQuery = searchQuery,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        selectedCategory = selectedCategory,
                        onSelectCategory = viewModel::setSelectedCategory,
                        medicinesState = medicinesState,
                        recentSearches = profile?.recentSearches.orEmpty(),
                        localityLabel = profile?.preferredLocality?.ifBlank { profile.preferredPincode } ?: "Set Area",
                        locationSharingEnabled = profile?.locationSharingEnabled == true,
                        selectedMedicineId = selectedMedicine?.id,
                        onRequestGps = ::requestNearbyGps,
                        onOpenManualLocation = { showLocationDialog = true },
                        onSelectMedicine = { viewModel.selectMedicine(it) }
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                ) {
                    val med = selectedMedicine
                    if (med == null) {
                        EmptyDetailPlaceholder()
                    } else {
                        MedicinePharmacyResultsPane(
                            medicine = med,
                            resultsState = resultsState,
                            sortOrder = sortOrder,
                            hideStale = hideStale,
                            availableOnly = availableOnly,
                            savedPharmacyIds = savedPharmacyIds,
                            showBackButton = false,
                            onBack = { viewModel.selectMedicine(null) },
                            onSortChange = viewModel::setSortOrder,
                            onToggleHideStale = viewModel::setHideStaleReports,
                            onToggleAvailableOnly = viewModel::setAvailableOnly,
                            onToggleSavePharmacy = viewModel::toggleSavedPharmacy,
                            onOpenPharmacyDetail = { inv ->
                                val pharm = (pharmaciesState as? UiState.Success)?.data
                                    ?.firstOrNull { it.id == inv.pharmacyId }
                                    ?: Pharmacy(
                                        id = inv.pharmacyId,
                                        ownerUserId = inv.ownerUserId,
                                        name = inv.pharmacyName,
                                        address = inv.pharmacyAddress,
                                        locality = inv.pharmacyLocality,
                                        pincode = inv.pharmacyPincode,
                                        latitude = inv.pharmacyLat,
                                        longitude = inv.pharmacyLon,
                                        phone = inv.pharmacyPhone,
                                        openingHours = inv.pharmacyHours,
                                        verificationStatus = "APPROVED"
                                    )
                                viewModel.selectPharmacy(pharm)
                            },
                            onReportIssue = { reportTarget = it }
                        )
                    }
                }
            }
        } else {
            val med = selectedMedicine
            if (med == null) {
                MedicineCataloguePane(
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    selectedCategory = selectedCategory,
                    onSelectCategory = viewModel::setSelectedCategory,
                    medicinesState = medicinesState,
                    recentSearches = profile?.recentSearches.orEmpty(),
                    localityLabel = profile?.preferredLocality?.ifBlank { profile.preferredPincode } ?: "Set Area",
                    locationSharingEnabled = profile?.locationSharingEnabled == true,
                    selectedMedicineId = null,
                    onRequestGps = ::requestNearbyGps,
                    onOpenManualLocation = { showLocationDialog = true },
                    onSelectMedicine = { viewModel.selectMedicine(it) }
                )
            } else {
                MedicinePharmacyResultsPane(
                    medicine = med,
                    resultsState = resultsState,
                    sortOrder = sortOrder,
                    hideStale = hideStale,
                    availableOnly = availableOnly,
                    savedPharmacyIds = savedPharmacyIds,
                    showBackButton = true,
                    onBack = { viewModel.selectMedicine(null) },
                    onSortChange = viewModel::setSortOrder,
                    onToggleHideStale = viewModel::setHideStaleReports,
                    onToggleAvailableOnly = viewModel::setAvailableOnly,
                    onToggleSavePharmacy = viewModel::toggleSavedPharmacy,
                    onOpenPharmacyDetail = { inv ->
                        val pharm = (pharmaciesState as? UiState.Success)?.data
                            ?.firstOrNull { it.id == inv.pharmacyId }
                            ?: Pharmacy(
                                id = inv.pharmacyId,
                                ownerUserId = inv.ownerUserId,
                                name = inv.pharmacyName,
                                address = inv.pharmacyAddress,
                                locality = inv.pharmacyLocality,
                                pincode = inv.pharmacyPincode,
                                latitude = inv.pharmacyLat,
                                longitude = inv.pharmacyLon,
                                phone = inv.pharmacyPhone,
                                openingHours = inv.pharmacyHours,
                                verificationStatus = "APPROVED"
                            )
                        viewModel.selectPharmacy(pharm)
                    },
                    onReportIssue = { reportTarget = it }
                )
            }
        }
    }

    if (showLocationDialog) {
        ManualLocationDialog(
            initialLocality = profile?.preferredLocality ?: "Indiranagar",
            initialPincode = profile?.preferredPincode ?: "560038",
            onDismiss = { showLocationDialog = false },
            onSave = { loc, pin ->
                viewModel.saveManualLocation(loc, pin)
                showLocationDialog = false
            }
        )
    }

    reportTarget?.let { inv ->
        ReportCorrectionDialog(
            inventory = inv,
            onDismiss = { reportTarget = null },
            onSubmit = { type, msg ->
                viewModel.submitFeedbackReport(inv, type, msg) {
                    reportTarget = null
                }
            }
        )
    }

    selectedPharmacy?.let { pharmacy ->
        PharmacyDetailDialog(
            pharmacy = pharmacy,
            isSaved = pharmacy.id in savedPharmacyIds,
            stockState = selectedPharmacyStockState,
            onToggleSave = { viewModel.toggleSavedPharmacy(pharmacy.id) },
            onDismiss = { viewModel.selectPharmacy(null) }
        )
    }
}

@Composable
private fun MedicineCataloguePane(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    medicinesState: UiState<List<Medicine>>,
    recentSearches: List<String>,
    localityLabel: String,
    locationSharingEnabled: Boolean,
    selectedMedicineId: String?,
    onRequestGps: () -> Unit,
    onOpenManualLocation: () -> Unit,
    onSelectMedicine: (Medicine) -> Unit
) {
    var useGridView by rememberSaveable { mutableStateOf(false) }

    val categories = remember(medicinesState) {
        val dynamic = (medicinesState as? UiState.Success)?.data
            ?.map { it.category }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?.sorted()
            .orEmpty()
        listOf("All") + dynamic
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Find Medicine Stock",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Search verified medicines & inspect nearby pharmacy stock",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Clean Segmented Area / GPS Bar (inspired by 144734 & 144658)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!locationSharingEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenManualLocation)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EditLocationAlt,
                                contentDescription = "Selected area",
                                tint = if (!locationSharingEnabled) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Area: $localityLabel",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (!locationSharingEnabled) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (locationSharingEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .clickable(onClick = onRequestGps)
                            .testTag("use_gps_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MyLocation,
                                contentDescription = "Use GPS",
                                tint = if (locationSharingEnabled) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Near Me GPS",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (locationSharingEnabled) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search generic, brand (e.g. Dolo, Salbutamol)...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medicine_search_input")
            )
        }

        // Clean Recent Search rows (inspired by 144627) when query is blank
        if (recentSearches.isNotEmpty() && searchQuery.isBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        recentSearches.take(4).forEachIndexed { idx, recent ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSearchQueryChange(recent) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = recent, style = MaterialTheme.typography.bodyMedium)
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search $recent",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (idx < recentSearches.take(4).lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }

        if (categories.size > 1) {
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        categories.forEach { cat ->
                            val isCatSelected = selectedCategory == cat
                            Column(
                                modifier = Modifier
                                    .clickable { onSelectCategory(cat) }
                                    .padding(top = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isCatSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(if (isCatSelected) 32.dp else 0.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (isCatSelected) MaterialTheme.colorScheme.primary
                                            else androidx.compose.ui.graphics.Color.Transparent
                                        )
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
        }

        item {
            MedicalDisclaimerBanner()
        }

        when (medicinesState) {
            is UiState.Loading -> item { LoadingStateBox("Loading verified catalogue...") }
            is UiState.Error -> item { ErrorStateCard(medicinesState.message) }
            is UiState.Success -> {
                val q = searchQuery.trim().lowercase()
                val filtered = medicinesState.data.filter { med ->
                    val matchesCategory = selectedCategory == "All" || med.category.equals(selectedCategory, ignoreCase = true)
                    val matchesQuery = q.isBlank() ||
                        med.genericName.lowercase().contains(q) ||
                        med.brandName.lowercase().contains(q) ||
                        med.strength.lowercase().contains(q) ||
                        med.category.lowercase().contains(q) ||
                        med.keywords.any { it.lowercase().contains(q) }
                    matchesCategory && matchesQuery
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Results (${filtered.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !useGridView,
                                onClick = { useGridView = false },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Outlined.FormatListBulleted, null, Modifier.size(16.dp))
                                },
                                label = { Text("List") }
                            )
                            FilterChip(
                                selected = useGridView,
                                onClick = { useGridView = true },
                                leadingIcon = {
                                    Icon(Icons.Outlined.GridView, null, Modifier.size(16.dp))
                                },
                                label = { Text("Grid") }
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_empty_discovery_1795628161301),
                                    contentDescription = "Empty discovery illustration",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = if (medicinesState.data.isEmpty()) {
                                        "No verified medicines in catalogue yet"
                                    } else {
                                        "No matching medicines found"
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (medicinesState.data.isEmpty()) {
                                        "Participating pharmacies and admins are updating the medicine catalogue. Please check back shortly."
                                    } else {
                                        "Try searching by generic name, brand name, or selecting a different category."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (useGridView) {
                    val pairs = filtered.chunked(2)
                    items(pairs, key = { row -> row.joinToString("_") { it.id } }) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { medicine ->
                                val isSelected = medicine.id == selectedMedicineId
                                MedicineGridCard(
                                    medicine = medicine,
                                    isSelected = isSelected,
                                    onClick = { onSelectMedicine(medicine) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { medicine ->
                        val isSelected = medicine.id == selectedMedicineId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectMedicine(medicine) }
                                .testTag("medicine_card_${medicine.id}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MedicinePackThumbnail(
                                    dosageForm = medicine.dosageForm,
                                    modifier = Modifier.size(62.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    StatusPill(
                                        text = medicine.category,
                                        bgColor = MaterialTheme.colorScheme.secondaryContainer,
                                        fgColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "${medicine.genericName} ${medicine.strength}",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${medicine.brandName} • ${medicine.dosageForm}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                            contentDescription = "Check stores",
                                            tint = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.size(18.dp)
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

@Composable
private fun MedicineGridCard(
    medicine: Medicine,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("medicine_card_${medicine.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                MedicinePackThumbnail(
                    dosageForm = medicine.dosageForm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    StatusPill(
                        text = medicine.dosageForm,
                        bgColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        fgColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "${medicine.genericName} ${medicine.strength}",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${medicine.brandName} • ${medicine.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check Stores", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun EmptyDetailPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalPharmacy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Select a medicine to inspect nearby pharmacies",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "View reported stock status, interactive nearby map pins, freshness timestamps, and call/Google Maps buttons.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MedicinePharmacyResultsPane(
    medicine: Medicine,
    resultsState: UiState<List<PharmacyInventoryItem>>,
    sortOrder: PharmacySortOrder,
    hideStale: Boolean,
    availableOnly: Boolean,
    savedPharmacyIds: List<String>,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSortChange: (PharmacySortOrder) -> Unit,
    onToggleHideStale: (Boolean) -> Unit,
    onToggleAvailableOnly: (Boolean) -> Unit,
    onToggleSavePharmacy: (String) -> Unit,
    onOpenPharmacyDetail: (InventoryRecord) -> Unit,
    onReportIssue: (InventoryRecord) -> Unit
) {
    val context = LocalContext.current
    var showMapView by rememberSaveable { mutableStateOf(false) }
    var selectedMapRecordId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("results_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back to medicine search"
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.displayTitle(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${medicine.dosageForm} • ${medicine.category} • Catalogue verified",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // List / Map Segmented Toggle (PDF Section 7 + screenshots 144658 & 144747)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!showMapView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showMapView = false }
                                .testTag("toggle_results_list")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                                    contentDescription = null,
                                    tint = if (!showMapView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Store List",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (!showMapView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (showMapView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showMapView = true }
                                .testTag("toggle_results_map")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Map,
                                    contentDescription = null,
                                    tint = if (showMapView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Map View",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (showMapView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Only show pharmacies reporting in stock",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = availableOnly,
                            onCheckedChange = onToggleAvailableOnly
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PharmacySortOrder.entries.forEach { order ->
                        FilterChip(
                            selected = sortOrder == order,
                            onClick = { onSortChange(order) },
                            label = { Text("Sort: ${order.label}") }
                        )
                    }
                    FilterChip(
                        selected = hideStale,
                        onClick = { onToggleHideStale(!hideStale) },
                        label = { Text("Hide Stale (>24h)") }
                    )
                }
            }
        }

        item {
            MedicalDisclaimerBanner()
        }

        when (resultsState) {
            is UiState.Loading -> item { LoadingStateBox("Fetching live pharmacy reports...") }
            is UiState.Error -> item { ErrorStateCard(resultsState.message) }
            is UiState.Success -> {
                if (resultsState.data.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No current pharmacy reports match these filters",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "No reliable stock report exists for this medicine under your current filter. Try toggling off 'Hide Stale' or check another locality.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    if (showMapView) {
                        item(key = "interactive_nearby_map") {
                            InteractiveNearbyMapCard(
                                items = resultsState.data,
                                selectedRecordId = selectedMapRecordId,
                                onSelectRecord = { selectedMapRecordId = it.record.id },
                                onOpenDetail = { onOpenPharmacyDetail(it.record) }
                            )
                        }
                    } else {
                        val nearestAvailable = resultsState.data.firstOrNull {
                            it.record.availabilityEnum() == AvailabilityStatus.REPORTED_AVAILABLE ||
                                it.record.availabilityEnum() == AvailabilityStatus.LOW_STOCK
                        }
                        if (nearestAvailable != null) {
                            item(key = "nearest_google_maps_card") {
                                val topRec = nearestAvailable.record
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
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
                                            Text(
                                                text = "Fastest Route to Available Stock",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (nearestAvailable.distanceKm != null) {
                                                Text(
                                                    text = String.format("%.1f km away", nearestAvailable.distanceKm),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Text(
                                            text = topRec.pharmacyName,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "${topRec.pharmacyAddress} • Qty: ${topRec.quantity} ${topRec.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = {
                                                openPharmacyDirections(
                                                    context = context,
                                                    lat = topRec.pharmacyLat,
                                                    lon = topRec.pharmacyLon,
                                                    label = topRec.pharmacyName,
                                                    address = topRec.pharmacyAddress
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("nearest_store_google_maps_button")
                                        ) {
                                            Icon(Icons.Outlined.Directions, null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Navigate via Google Maps")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(resultsState.data, key = { it.record.id }) { item ->
                        val rec = item.record
                        val isSaved = rec.pharmacyId in savedPharmacyIds
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPharmacyDetail(rec) }
                                .testTag("pharmacy_result_${rec.pharmacyId}")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = rec.pharmacyName,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Icon(
                                                imageVector = Icons.Outlined.VerifiedUser,
                                                contentDescription = "Verified pharmacy",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = rec.pharmacyAddress,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Hours: ${rec.pharmacyHours} • Tel: ${rec.pharmacyPhone}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onToggleSavePharmacy(rec.pharmacyId) },
                                        modifier = Modifier.testTag("bookmark_pharmacy_${rec.pharmacyId}")
                                    ) {
                                        Icon(
                                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                            contentDescription = if (isSaved) "Remove bookmark" else "Save pharmacy",
                                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvailabilityBadge(rec.availabilityEnum())
                                    FreshnessBadge(item.freshnessTier)
                                    if (item.distanceKm != null) {
                                        StatusPill(
                                            text = String.format("%.1f km away", item.distanceKm),
                                            bgColor = MaterialTheme.colorScheme.surfaceVariant,
                                            fgColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = "Reported qty: ${rec.quantity} ${rec.unit} • Updated ${GeoAndTimeUtils.formatRelativeAndLocalTime(rec.effectiveTimestamp())}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "Availability is an indication only — call to confirm stock before travelling.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { dialPharmacy(context, rec.pharmacyPhone) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("call_pharmacy_${rec.pharmacyId}")
                                    ) {
                                        Icon(Icons.Outlined.Call, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Call")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            openPharmacyDirections(
                                                context = context,
                                                lat = rec.pharmacyLat,
                                                lon = rec.pharmacyLon,
                                                label = rec.pharmacyName,
                                                address = rec.pharmacyAddress
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1.15f)
                                            .testTag("maps_pharmacy_${rec.pharmacyId}")
                                    ) {
                                        Icon(Icons.Outlined.Directions, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Google Maps", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(
                                        onClick = { onReportIssue(rec) },
                                        modifier = Modifier.testTag("report_listing_${rec.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Flag,
                                            contentDescription = "Report inaccurate stock"
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

@Composable
private fun ManualLocationDialog(
    initialLocality: String,
    initialPincode: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var locality by remember { mutableStateOf(initialLocality) }
    var pincode by remember { mutableStateOf(initialPincode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Discovery Area") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter a neighborhood or pincode (e.g. Indiranagar 560038, Koramangala 560034, Jayanagar 560011).",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = locality,
                    onValueChange = { locality = it },
                    label = { Text("Locality") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { pincode = it },
                    label = { Text("Pincode") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(locality, pincode) },
                enabled = locality.isNotBlank() || pincode.isNotBlank()
            ) {
                Text("Save Area")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ReportCorrectionDialog(
    inventory: InventoryRecord,
    onDismiss: () -> Unit,
    onSubmit: (ReportType, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(ReportType.OUT_OF_STOCK_ON_VISIT) }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Stock Inaccuracy") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${inventory.pharmacyName} • ${inventory.medicineName}",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "Reports are sent to administrators for review rather than silently altering pharmacy stock.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ReportType.entries.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Text(text = type.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Optional details (e.g. visited at 6 PM)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedType, message) }) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PharmacyDetailDialog(
    pharmacy: Pharmacy,
    isSaved: Boolean,
    stockState: UiState<List<InventoryRecord>>,
    onToggleSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pharmacy.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save pharmacy"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = pharmacy.address, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${pharmacy.locality} • ${pharmacy.pincode}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = "Hours: ${pharmacy.openingHours}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Phone: ${pharmacy.phone}", style = MaterialTheme.typography.bodySmall)
                if (pharmacy.licenseNumber.isNotBlank()) {
                    Text(
                        text = "License: ${pharmacy.licenseNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Reported Medicines at this Pharmacy",
                    style = MaterialTheme.typography.titleSmall
                )
                when (stockState) {
                    is UiState.Loading -> Text("Loading stock...", style = MaterialTheme.typography.bodySmall)
                    is UiState.Error -> Text(stockState.message, style = MaterialTheme.typography.bodySmall)
                    is UiState.Success -> {
                        if (stockState.data.isEmpty()) {
                            Text("No items reported yet.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            stockState.data.take(8).forEach { rec ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rec.medicineName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                        Text("${rec.quantity} ${rec.unit}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    AvailabilityBadge(rec.availabilityEnum())
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { dialPharmacy(context, pharmacy.phone) }) {
                Icon(Icons.Outlined.Call, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Call to Confirm")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = {
                        openPharmacyDirections(
                            context = context,
                            lat = pharmacy.latitude,
                            lon = pharmacy.longitude,
                            label = pharmacy.name,
                            address = pharmacy.address
                        )
                    }
                ) {
                    Text("Google Maps")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
