package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.UiState
import com.example.data.model.CatalogueStatus
import com.example.data.model.FeedbackReport
import com.example.data.model.GeoAndTimeUtils
import com.example.data.model.PharmacyVerificationStatus
import com.example.data.model.ReportStatus
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingStateBox
import com.example.ui.components.StatusPill
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.AdminSidebarNavy
import com.example.ui.theme.StatusAvailableBg
import com.example.ui.theme.StatusAvailableFg
import com.example.ui.theme.StatusLowStockBg
import com.example.ui.theme.StatusLowStockFg
import com.example.ui.theme.StatusStaleBg
import com.example.ui.theme.StatusStaleFg
import com.example.ui.viewmodel.MedFindViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: MedFindViewModel
) {
    val pharmaciesState by viewModel.adminPharmaciesState.collectAsStateWithLifecycle()
    val reportsState by viewModel.adminReportsState.collectAsStateWithLifecycle()
    val medicinesState by viewModel.adminMedicinesState.collectAsStateWithLifecycle()
    val historyState by viewModel.adminHistoryState.collectAsStateWithLifecycle()
    val actionsState by viewModel.adminActionsState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var resolvingReport by remember { mutableStateOf<FeedbackReport?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "admin_header") {
                val adminHeaderGradient = Brush.linearGradient(
                    colors = listOf(AdminSidebarNavy, MaterialTheme.colorScheme.primary)
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(adminHeaderGradient)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin Governance",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Verify pharmacies, resolve stock disputes, and inspect audit logs",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = { viewModel.seedPilotData() },
                            enabled = !isBusy,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                            modifier = Modifier.testTag("admin_seed_button")
                        ) {
                            Icon(Icons.Outlined.Dataset, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Seed Pilot")
                        }
                    }
                }
            }

            item(key = "admin_tabs") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 8.dp,
                        containerColor = Color.Transparent
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Pharmacies") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Reports") })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Catalogue") })
                        Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Audit Logs") })
                    }
                }
            }

        when (selectedTab) {
            0 -> {
                when (val state = pharmaciesState) {
                    is UiState.Loading -> item { LoadingStateBox() }
                    is UiState.Error -> item { ErrorStateCard(state.message) }
                    is UiState.Success -> {
                        val sorted = state.data.sortedBy {
                            if (it.statusEnum() == PharmacyVerificationStatus.PENDING) 0 else 1
                        }
                        if (sorted.isEmpty()) {
                            item { Text("No pharmacies registered yet.", modifier = Modifier.padding(12.dp)) }
                        } else {
                            items(sorted, key = { it.id }) { p ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(p.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                            VerificationBadge(p.statusEnum())
                                        }
                                        Text("License: ${p.licenseNumber} • Phone: ${p.phone}", style = MaterialTheme.typography.bodySmall)
                                        Text("${p.address} (${p.locality} ${p.pincode})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (p.statusEnum() != PharmacyVerificationStatus.APPROVED) {
                                                Button(
                                                    onClick = {
                                                        viewModel.moderatePharmacy(p.id, PharmacyVerificationStatus.APPROVED, "Verified by Admin")
                                                    },
                                                    enabled = !isBusy
                                                ) {
                                                    Text("Approve")
                                                }
                                            }
                                            if (p.statusEnum() != PharmacyVerificationStatus.SUSPENDED) {
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.moderatePharmacy(p.id, PharmacyVerificationStatus.SUSPENDED, "Suspended by Admin")
                                                    },
                                                    enabled = !isBusy
                                                ) {
                                                    Text("Suspend")
                                                }
                                            }
                                            if (p.statusEnum() == PharmacyVerificationStatus.PENDING) {
                                                TextButton(
                                                    onClick = {
                                                        viewModel.moderatePharmacy(p.id, PharmacyVerificationStatus.REJECTED, "Rejected during review")
                                                    },
                                                    enabled = !isBusy
                                                ) {
                                                    Text("Reject")
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
                when (val state = reportsState) {
                    is UiState.Loading -> item { LoadingStateBox() }
                    is UiState.Error -> item { ErrorStateCard(state.message) }
                    is UiState.Success -> {
                        val sorted = state.data.sortedByDescending { it.createdAt?.seconds ?: 0L }
                        if (sorted.isEmpty()) {
                            item { Text("No user stock reports submitted.", modifier = Modifier.padding(12.dp)) }
                        } else {
                            items(sorted, key = { it.id }) { rep ->
                                val st = rep.statusEnum()
                                val (bg, fg) = when (st) {
                                    ReportStatus.OPEN, ReportStatus.UNDER_REVIEW -> StatusLowStockBg to StatusLowStockFg
                                    ReportStatus.RESOLVED -> StatusAvailableBg to StatusAvailableFg
                                    ReportStatus.DISMISSED -> StatusStaleBg to StatusStaleFg
                                }
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(rep.pharmacyName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                            StatusPill(st.label, bg, fg)
                                        }
                                        Text(rep.medicineName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        Text("${rep.typeEnum().label} • By ${rep.reporterName}", style = MaterialTheme.typography.bodySmall)
                                        if (rep.message.isNotBlank()) {
                                            Text(rep.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (st == ReportStatus.OPEN || st == ReportStatus.UNDER_REVIEW) {
                                            Button(
                                                onClick = { resolvingReport = rep },
                                                enabled = !isBusy
                                            ) {
                                                Text("Review & Resolve")
                                            }
                                        } else if (rep.resolutionNote.isNotBlank()) {
                                            Text("Resolution: ${rep.resolutionNote}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                when (val state = medicinesState) {
                    is UiState.Loading -> item { LoadingStateBox() }
                    is UiState.Error -> item { ErrorStateCard(state.message) }
                    is UiState.Success -> {
                        val sorted = state.data.sortedBy {
                            if (it.statusEnum() == CatalogueStatus.PENDING_REVIEW) 0 else 1
                        }
                        if (sorted.isEmpty()) {
                            item { Text("No medicines in catalogue yet.", modifier = Modifier.padding(12.dp)) }
                        } else {
                            items(sorted, key = { it.id }) { med ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(med.displayTitle(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                            StatusPill(
                                                text = med.statusEnum().name,
                                                bgColor = if (med.statusEnum() == CatalogueStatus.VERIFIED) StatusAvailableBg else StatusLowStockBg,
                                                fgColor = if (med.statusEnum() == CatalogueStatus.VERIFIED) StatusAvailableFg else StatusLowStockFg
                                            )
                                        }
                                        Text("${med.dosageForm} • ${med.category}", style = MaterialTheme.typography.bodySmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (med.statusEnum() != CatalogueStatus.VERIFIED) {
                                                Button(
                                                    onClick = {
                                                        viewModel.moderateMedicine(med.id, CatalogueStatus.VERIFIED, "Verified catalogue entry")
                                                    },
                                                    enabled = !isBusy
                                                ) {
                                                    Text("Verify")
                                                }
                                            }
                                            if (med.statusEnum() != CatalogueStatus.ARCHIVED) {
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.moderateMedicine(med.id, CatalogueStatus.ARCHIVED, "Archived duplicate/invalid entry")
                                                    },
                                                    enabled = !isBusy
                                                ) {
                                                    Text("Archive")
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

            else -> {
                item {
                    Text("Recent Admin Actions", style = MaterialTheme.typography.titleMedium)
                }
                when (val aState = actionsState) {
                    is UiState.Loading -> item { LoadingStateBox() }
                    is UiState.Error -> item { ErrorStateCard(aState.message) }
                    is UiState.Success -> {
                        items(aState.data.sortedByDescending { it.createdAt?.seconds ?: 0L }, key = { "act_${it.id}" }) { act ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${act.targetType} • ${act.action}", style = MaterialTheme.typography.titleSmall)
                                    if (act.reason.isNotBlank()) {
                                        Text(act.reason, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        text = "${act.adminEmail} • ${GeoAndTimeUtils.formatLocalDate(act.createdAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Inventory Audit Trail", style = MaterialTheme.typography.titleMedium)
                }
                when (val hState = historyState) {
                    is UiState.Loading -> item { LoadingStateBox() }
                    is UiState.Error -> item { ErrorStateCard(hState.message) }
                    is UiState.Success -> {
                        items(hState.data.sortedByDescending { it.changedAt?.seconds ?: 0L }, key = { "hist_${it.id}" }) { h ->
                            HistoryAuditRow(h)
                        }
                    }
                }
            }
        }
    }
    }

    resolvingReport?.let { rep ->
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { resolvingReport = null },
            title = { Text("Resolve Stock Report") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${rep.pharmacyName} • ${rep.medicineName}", style = MaterialTheme.typography.titleSmall)
                    Text(rep.typeEnum().label, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Resolution note") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resolveReport(rep.id, ReportStatus.RESOLVED, note.ifBlank { "Reviewed and verified with pharmacy" })
                        resolvingReport = null
                    }
                ) {
                    Text("Mark Resolved")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.resolveReport(rep.id, ReportStatus.DISMISSED, note.ifBlank { "Dismissed after review" })
                            resolvingReport = null
                        }
                    ) {
                        Text("Dismiss")
                    }
                    TextButton(onClick = { resolvingReport = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
