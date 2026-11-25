package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.UiState
import com.example.data.model.FeedbackReport
import com.example.data.model.GeoAndTimeUtils
import com.example.data.model.Pharmacy
import com.example.data.model.ReportStatus
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingStateBox
import com.example.ui.components.StatusPill
import com.example.ui.components.VerificationBadge
import com.example.ui.components.dialPharmacy
import com.example.ui.components.openPharmacyDirections
import com.example.ui.theme.StatusAvailableBg
import com.example.ui.theme.StatusAvailableFg
import com.example.ui.theme.StatusLowStockBg
import com.example.ui.theme.StatusLowStockFg
import com.example.ui.theme.StatusOutOfStockBg
import com.example.ui.theme.StatusOutOfStockFg
import com.example.ui.theme.StatusStaleBg
import com.example.ui.theme.StatusStaleFg
import com.example.ui.viewmodel.MedFindViewModel

@Composable
fun SavedAndReportsScreen(
    viewModel: MedFindViewModel,
    onOpenPharmacy: (Pharmacy) -> Unit
) {
    val context = LocalContext.current
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val pharmaciesState by viewModel.approvedPharmaciesState.collectAsStateWithLifecycle()
    val reportsState by viewModel.myReportsState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val savedIds = (profileState as? UiState.Success)?.data?.savedPharmacyIds.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Saved & Reports",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Quickly re-contact saved pharmacies or track stock correction reports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Saved Pharmacies (${savedIds.size})") },
                    modifier = Modifier.testTag("saved_pharmacies_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("My Reports") },
                    modifier = Modifier.testTag("my_reports_tab")
                )
            }
        }

        if (selectedTab == 0) {
            when (val pState = pharmaciesState) {
                is UiState.Loading -> item { LoadingStateBox() }
                is UiState.Error -> item { ErrorStateCard(pState.message) }
                is UiState.Success -> {
                    val savedPharmacies = pState.data.filter { it.id in savedIds }
                    if (savedPharmacies.isEmpty()) {
                        item {
                            EmptySavedCard(
                                icon = Icons.Outlined.LocalPharmacy,
                                title = "No saved pharmacies yet",
                                subtitle = "Tap the bookmark icon on any pharmacy result or detail sheet to keep it handy."
                            )
                        }
                    } else {
                        items(savedPharmacies, key = { it.id }) { pharmacy ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pharmacy.name,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "${pharmacy.locality} • ${pharmacy.pincode}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.toggleSavedPharmacy(pharmacy.id) },
                                            modifier = Modifier.testTag("unsave_pharmacy_${pharmacy.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Bookmark,
                                                contentDescription = "Remove saved pharmacy",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    VerificationBadge(pharmacy.statusEnum())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = pharmacy.address,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Hours: ${pharmacy.openingHours}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { dialPharmacy(context, pharmacy.phone) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Outlined.Call, null, Modifier.size(16.dp))
                                            Spacer(Modifier.size(6.dp))
                                            Text("Call")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                openPharmacyDirections(
                                                    context = context,
                                                    lat = pharmacy.latitude,
                                                    lon = pharmacy.longitude,
                                                    label = pharmacy.name,
                                                    address = pharmacy.address
                                                )
                                            },
                                            modifier = Modifier.weight(1.1f)
                                        ) {
                                            Icon(Icons.Outlined.Directions, null, Modifier.size(16.dp))
                                            Spacer(Modifier.size(4.dp))
                                            Text("Google Maps", maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = { onOpenPharmacy(pharmacy) }
                                        ) {
                                            Text("Stock")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            when (val rState = reportsState) {
                is UiState.Loading -> item { LoadingStateBox() }
                is UiState.Error -> item { ErrorStateCard(rState.message) }
                is UiState.Success -> {
                    if (rState.data.isEmpty()) {
                        item {
                            EmptySavedCard(
                                icon = Icons.Outlined.Flag,
                                title = "No stock correction reports",
                                subtitle = "If a pharmacy listing seems out of date after you visit or call, submit a correction report for admin review."
                            )
                        }
                    } else {
                        items(rState.data, key = { it.id }) { report ->
                            MyReportCard(report)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyReportCard(report: FeedbackReport) {
    val status = report.statusEnum()
    val (bg, fg) = when (status) {
        ReportStatus.OPEN -> StatusLowStockBg to StatusLowStockFg
        ReportStatus.UNDER_REVIEW -> StatusLowStockBg to StatusLowStockFg
        ReportStatus.RESOLVED -> StatusAvailableBg to StatusAvailableFg
        ReportStatus.DISMISSED -> StatusStaleBg to StatusStaleFg
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.pharmacyName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(text = status.label, bgColor = bg, fgColor = fg)
            }
            Text(
                text = report.medicineName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Issue: ${report.typeEnum().label}",
                style = MaterialTheme.typography.labelMedium
            )
            if (report.message.isNotBlank()) {
                Text(
                    text = report.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (report.resolutionNote.isNotBlank()) {
                Text(
                    text = "Admin note: ${report.resolutionNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusAvailableFg
                )
            }
            Text(
                text = "Submitted ${GeoAndTimeUtils.formatRelativeAndLocalTime(report.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySavedCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
