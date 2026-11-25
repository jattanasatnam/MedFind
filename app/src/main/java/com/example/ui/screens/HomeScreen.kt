package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.UiState
import com.example.ui.components.CategoryGridTile
import com.example.ui.components.MedFindBrandLogo
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.MedicinePackThumbnail
import com.example.ui.components.StatusPill
import com.example.ui.theme.AccentAmberBg
import com.example.ui.theme.AccentAmberFg
import com.example.ui.theme.AccentBlueBg
import com.example.ui.theme.AccentBlueFg
import com.example.ui.theme.AccentMintBg
import com.example.ui.theme.AccentMintFg
import com.example.ui.theme.AccentOrangeBg
import com.example.ui.theme.AccentOrangeFg
import com.example.ui.theme.AccentRoseBg
import com.example.ui.theme.AccentRoseFg
import com.example.ui.theme.AccentSkyBg
import com.example.ui.theme.AccentSkyFg
import com.example.ui.theme.AccentTealBg
import com.example.ui.theme.AccentTealFg
import com.example.ui.theme.AccentVioletBg
import com.example.ui.theme.AccentVioletFg
import com.example.ui.theme.StatusAvailableBg
import com.example.ui.theme.StatusAvailableFg
import com.example.ui.viewmodel.MedFindViewModel

private data class HomeCategorySpec(
    val queryCategory: String,
    val shortTitle: String,
    val icon: ImageVector,
    val bg: Color,
    val fg: Color
)

@Composable
fun HomeScreen(
    viewModel: MedFindViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToAgent: () -> Unit,
    onNavigateToSaved: () -> Unit
) {
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val medicinesState by viewModel.verifiedMedicinesState.collectAsStateWithLifecycle()
    val pharmaciesState by viewModel.approvedPharmaciesState.collectAsStateWithLifecycle()
    val reportsState by viewModel.myReportsState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val localityLabel = profile?.preferredLocality?.ifBlank { profile.preferredPincode } ?: "Indiranagar"
    val pincodeSuffix = profile?.preferredPincode?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
    val recentSearches = profile?.recentSearches.orEmpty()
    val savedCount = profile?.savedPharmacyIds?.size ?: 0
    val reportCount = (reportsState as? UiState.Success)?.data?.size ?: 0
    val medicines = (medicinesState as? UiState.Success)?.data.orEmpty()
    val pharmacyCount = (pharmaciesState as? UiState.Success)?.data?.size ?: 0

    val categorySpecs = listOf(
        HomeCategorySpec("All", "All Meds", Icons.Outlined.Medication, AccentBlueBg, AccentBlueFg),
        HomeCategorySpec("Fever & Pain", "Fever & Pain", Icons.Outlined.Thermostat, AccentRoseBg, AccentRoseFg),
        HomeCategorySpec("Antibiotic", "Antibiotic", Icons.Outlined.Biotech, AccentTealBg, AccentTealFg),
        HomeCategorySpec("Respiratory", "Respiratory", Icons.Outlined.Air, AccentSkyBg, AccentSkyFg),
        HomeCategorySpec("Diabetes Care", "Diabetes", Icons.Outlined.Science, AccentVioletBg, AccentVioletFg),
        HomeCategorySpec("Digestive", "Digestive", Icons.Outlined.Healing, AccentAmberBg, AccentAmberFg),
        HomeCategorySpec("Allergy", "Allergy", Icons.Outlined.Shield, AccentMintBg, AccentMintFg),
        HomeCategorySpec("Cardiac Care", "Cardiac", Icons.Outlined.FavoriteBorder, AccentOrangeBg, AccentOrangeFg)
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar inspired by MedGuide mobile UI: brand badge + active location chip + Saved/Alerts button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.size(44.dp)
                    ) {
                        MedFindBrandLogo(modifier = Modifier.padding(6.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(horizontal = 8.dp)
                            .clickable(onClick = onNavigateToSearch)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = "Active area",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$localityLabel$pincodeSuffix",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onNavigateToSaved)
                            .testTag("home_saved_reports_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsNone,
                                contentDescription = "Saved pharmacies and reports",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            if (savedCount > 0 || reportCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StatusAvailableBg)
                                )
                            }
                        }
                    }
                }
            }

            // Headline + Search Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Simplify Your Medicine Search",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Hi ${viewModel.userDisplayName.substringBefore(" ")}, locate verified local pharmacy stock before travelling",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSearch() }
                            .testTag("home_search_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Search to find your medicine...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search medicines",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Hero Discovery Card with crisp Canvas capsule/molecule geometry
            item {
                val heroGradient = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSearch() }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(heroGradient)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                        ) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = size.height * 0.65f,
                                center = Offset(size.width * 0.92f, size.height * 0.25f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.12f),
                                radius = size.height * 0.42f,
                                center = Offset(size.width * 0.85f, size.height * 0.72f),
                                style = Stroke(width = 3f)
                            )
                            rotate(degrees = -28f, pivot = Offset(size.width * 0.86f, size.height * 0.48f)) {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.16f),
                                    topLeft = Offset(size.width * 0.76f, size.height * 0.36f),
                                    size = Size(84f, 40f),
                                    cornerRadius = CornerRadius(20f, 20f)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusPill(
                                text = "$pharmacyCount Verified Stores • ${medicines.size} Medicines",
                                bgColor = Color.White.copy(alpha = 0.18f),
                                fgColor = Color.White,
                                icon = Icons.Outlined.LocalHospital
                            )
                            Text(
                                text = "Find the Right Pharmacy Stock",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Check reported availability, stock freshness timestamps & open Google Maps directions in one tap.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                            Button(
                                onClick = onNavigateToSearch,
                                shape = RoundedCornerShape(12.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Check Nearby Availability",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Top Categories 4x2 Icon Grid (inspired by MedGuide screenshot)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Categories",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Browse All >>",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                viewModel.setSelectedCategory("All")
                                onNavigateToSearch()
                            }
                        )
                    }

                    val rows = categorySpecs.chunked(4)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { spec ->
                                    CategoryGridTile(
                                        title = spec.shortTitle,
                                        icon = spec.icon,
                                        bgColor = spec.bg,
                                        fgColor = spec.fg,
                                        selected = selectedCategory.equals(spec.queryCategory, ignoreCase = true),
                                        onClick = {
                                            viewModel.setSelectedCategory(spec.queryCategory)
                                            onNavigateToSearch()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("home_cat_${spec.queryCategory.lowercase().replace(" ", "_")}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Verified Medicines Carousel (real live Firestore catalogue items, NO fake prices/cart!)
            if (medicines.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Popular Catalogue Searches",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "See All >>",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onNavigateToSearch)
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(medicines.take(6), key = { it.id }) { med ->
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .width(184.dp)
                                        .clickable {
                                            viewModel.selectMedicine(med)
                                            onNavigateToSearch()
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            MedicinePackThumbnail(
                                                dosageForm = med.dosageForm,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(78.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                            ) {
                                                StatusPill(
                                                    text = med.dosageForm,
                                                    bgColor = StatusAvailableBg,
                                                    fgColor = StatusAvailableFg
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "${med.genericName} ${med.strength}",
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${med.brandName} • ${med.category}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.selectMedicine(med)
                                                onNavigateToSearch()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            Text("Check Stores", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AI Health & Discovery Guide card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAgent() }
                        .testTag("home_ai_agent_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = "AI Health Guide",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ask AI Health & Medicine Guide",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Find medicines in local stores, check freshness & pharmacy navigation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = onNavigateToAgent) {
                            Text("Ask AI")
                        }
                    }
                }
            }

            if (recentSearches.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentSearches.take(6).forEach { term ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        viewModel.setSearchQuery(term)
                                        onNavigateToSearch()
                                    },
                                    label = { Text(term) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeStatCard(
                        icon = Icons.Outlined.BookmarkBorder,
                        count = savedCount.toString(),
                        label = "Saved Stores",
                        accentBg = AccentBlueBg,
                        accentFg = AccentBlueFg,
                        onClick = onNavigateToSaved,
                        modifier = Modifier.weight(1f)
                    )
                    HomeStatCard(
                        icon = Icons.Outlined.Flag,
                        count = reportCount.toString(),
                        label = "My Reports",
                        accentBg = AccentAmberBg,
                        accentFg = AccentAmberFg,
                        onClick = onNavigateToSaved,
                        modifier = Modifier.weight(1f)
                    )
                    HomeStatCard(
                        icon = Icons.Outlined.LocalPharmacy,
                        count = pharmacyCount.toString(),
                        label = "Pharmacies",
                        accentBg = AccentMintBg,
                        accentFg = AccentMintFg,
                        onClick = onNavigateToSearch,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                MedicalDisclaimerBanner()
            }
        }
    }
}

@Composable
private fun HomeStatCard(
    icon: ImageVector,
    count: String,
    label: String,
    accentBg: Color,
    accentFg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentBg,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = accentFg,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
