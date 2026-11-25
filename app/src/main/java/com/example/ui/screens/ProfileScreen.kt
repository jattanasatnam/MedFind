package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.firebase.UiState
import com.example.data.model.PharmacyVerificationStatus
import com.example.data.model.UserRole
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.StatusAvailableBg
import com.example.ui.theme.StatusAvailableFg
import com.example.ui.theme.StatusLowStockBg
import com.example.ui.theme.StatusLowStockFg
import com.example.ui.theme.StatusOutOfStockBg
import com.example.ui.theme.StatusOutOfStockFg
import com.example.ui.viewmodel.AppWorkspaceMode
import com.example.ui.viewmodel.MedFindViewModel

@Composable
fun ProfileScreen(
    viewModel: MedFindViewModel,
    userRole: UserRole = UserRole.CUSTOMER,
    canAccessPharmacyPortal: Boolean = false,
    workspaceMode: AppWorkspaceMode = AppWorkspaceMode.CUSTOMER,
    onSelectWorkspaceMode: (AppWorkspaceMode) -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val myPharmaciesState by viewModel.myPharmaciesState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val myPharmacies = (myPharmaciesState as? UiState.Success)?.data.orEmpty()
    var showApplyPharmacyDialog by remember { mutableStateOf(false) }

    var localityInput by remember(profile?.preferredLocality) {
        mutableStateOf(profile?.preferredLocality ?: "Indiranagar")
    }
    var pincodeInput by remember(profile?.preferredPincode) {
        mutableStateOf(profile?.preferredPincode ?: "560038")
    }

    val availableModes = remember(userRole, canAccessPharmacyPortal) {
        buildList {
            add(AppWorkspaceMode.CUSTOMER)
            if (canAccessPharmacyPortal) {
                add(AppWorkspaceMode.PHARMACY)
            }
            if (userRole == UserRole.ADMIN) {
                add(AppWorkspaceMode.ADMIN)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Account & Permissions",
                style = MaterialTheme.typography.headlineSmall
            )

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "User avatar",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.displayName ?: viewModel.userDisplayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = viewModel.userEmail.ifBlank { profile?.email.orEmpty() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(
                                text = "Verified Role: ${userRole.name}",
                                bgColor = StatusAvailableBg,
                                fgColor = StatusAvailableFg,
                                icon = if (userRole == UserRole.ADMIN) Icons.Outlined.AdminPanelSettings else Icons.Outlined.VerifiedUser
                            )
                            StatusPill(
                                text = "View: ${workspaceMode.label}",
                                bgColor = MaterialTheme.colorScheme.secondaryContainer,
                                fgColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            if (availableModes.size > 1) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Switch Authorized View",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "Switch between your authorized workspaces. Access is enforced by server security rules.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        availableModes.forEach { mode ->
                            val icon: ImageVector = when (mode) {
                                AppWorkspaceMode.CUSTOMER -> Icons.Outlined.Person
                                AppWorkspaceMode.PHARMACY -> Icons.Outlined.LocalPharmacy
                                AppWorkspaceMode.ADMIN -> Icons.Outlined.AdminPanelSettings
                            }
                            val selected = workspaceMode == mode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectWorkspaceMode(mode) }
                                    .testTag("switch_mode_${mode.name.lowercase()}"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = { onSelectWorkspaceMode(mode) }
                                    )
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = mode.label,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = mode.subtitle,
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

            if (userRole != UserRole.ADMIN) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalPharmacy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Pharmacy Partner Verification",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = if (canAccessPharmacyPortal) {
                                "Your store is verified. Switch to My Pharmacy Store above to manage stock and submit new medicines."
                            } else {
                                "Only verified pharmacies can publish stock or submit medicines. Submit your store & drug license for Admin review."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        myPharmacies.forEach { store ->
                            val status = store.statusEnum()
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = store.name,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = "License: ${store.licenseNumber} • ${store.locality}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        VerificationBadge(status)
                                    }
                                    val statusExplain = when (status) {
                                        PharmacyVerificationStatus.PENDING -> "Pending Admin review of your drug license before stock editing unlocks."
                                        PharmacyVerificationStatus.APPROVED -> "Verified partner store — stock publishing enabled."
                                        PharmacyVerificationStatus.REJECTED -> "Application rejected. Review admin note before re-applying."
                                        PharmacyVerificationStatus.SUSPENDED -> "Store suspended. Stock editing and public visibility are disabled."
                                    }
                                    val (noteBg, noteFg) = when (status) {
                                        PharmacyVerificationStatus.APPROVED -> StatusAvailableBg to StatusAvailableFg
                                        PharmacyVerificationStatus.PENDING -> StatusLowStockBg to StatusLowStockFg
                                        PharmacyVerificationStatus.REJECTED,
                                        PharmacyVerificationStatus.SUSPENDED -> StatusOutOfStockBg to StatusOutOfStockFg
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = noteBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (store.adminNote.isNotBlank()) {
                                                "$statusExplain Admin note: ${store.adminNote}"
                                            } else {
                                                statusExplain
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = noteFg,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showApplyPharmacyDialog = true },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("apply_partner_pharmacy_button")
                        ) {
                            Icon(Icons.Outlined.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (myPharmacies.isEmpty()) "Apply as Partner Pharmacy"
                                else "Submit Another Store Application"
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "How MedFind Roles Work",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "• Customer: Searches medicines, views freshness timestamps, calls/navigates to stores, and flags inaccurate stock.\n" +
                            "• Verified Pharmacy Partner: Granted after Admin approves a valid drug license. Can only update inventory for their own approved store.\n" +
                            "• Platform Admin: Reviews pharmacy licenses, moderates the medicine catalogue, resolves customer disputes, and audits security logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Manual Area & Pincode Preference",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "You can always discover pharmacies without enabling GPS by saving a default locality or pincode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = localityInput,
                        onValueChange = { localityInput = it },
                        label = { Text("Locality / Neighborhood") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_locality_input")
                    )
                    OutlinedTextField(
                        value = pincodeInput,
                        onValueChange = { pincodeInput = it },
                        label = { Text("Pincode / Postal Code") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_pincode_input")
                    )
                    Button(
                        onClick = { viewModel.saveManualLocation(localityInput, pincodeInput) },
                        enabled = !isBusy,
                        modifier = Modifier.testTag("save_location_pref_button")
                    ) {
                        Text("Save Location Preference")
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Freshness & Reliability Policy",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "• Fresh (<= 6h) & Recent (<= 24h): Recently reported by participating pharmacy staff.\n• Stale (> 24h): Older report, ranked below fresh listings and flagged for confirmation.\n• Out of Stock / Paused: Never presented as available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Policy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Your precise location and search history are never sold or shared publicly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            MedicalDisclaimerBanner()

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.sign_out))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showApplyPharmacyDialog) {
        PharmacyFormDialog(
            title = "Apply as Partner Pharmacy",
            existing = null,
            subtitle = "Your application starts as PENDING. Once an Admin verifies your drug license, Pharmacy Store Portal unlocks automatically.",
            onDismiss = { showApplyPharmacyDialog = false },
            onSave = { name, lic, addr, loc, pin, phone, hours ->
                viewModel.registerPharmacy(name, lic, addr, loc, pin, phone, hours) {
                    showApplyPharmacyDialog = false
                }
            }
        )
    }
}
