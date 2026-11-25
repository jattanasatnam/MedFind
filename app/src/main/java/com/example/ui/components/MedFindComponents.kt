package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AvailabilityStatus
import com.example.data.model.FreshnessTier
import com.example.data.model.GeoAndTimeUtils
import com.example.data.model.PharmacyVerificationStatus
import com.example.ui.theme.StatusAvailableBg
import com.example.ui.theme.StatusAvailableFg
import com.example.ui.theme.StatusLowStockBg
import com.example.ui.theme.StatusLowStockFg
import com.example.ui.theme.StatusOutOfStockBg
import com.example.ui.theme.StatusOutOfStockFg
import com.example.ui.theme.StatusStaleBg
import com.example.ui.theme.StatusStaleFg
import com.example.ui.viewmodel.PharmacyInventoryItem
import kotlin.math.roundToInt

@Composable
fun MedFindBrandLogo(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tealColor = MaterialTheme.colorScheme.secondary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            rotate(degrees = -35f, pivot = Offset(w * 0.42f, h * 0.54f)) {
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(w * 0.14f, h * 0.34f),
                    size = Size(w * 0.56f, h * 0.34f),
                    cornerRadius = CornerRadius(h * 0.17f, h * 0.17f)
                )
            }
            rotate(degrees = 35f, pivot = Offset(w * 0.60f, h * 0.52f)) {
                drawRoundRect(
                    color = tealColor,
                    topLeft = Offset(w * 0.32f, h * 0.34f),
                    size = Size(w * 0.54f, h * 0.34f),
                    cornerRadius = CornerRadius(h * 0.17f, h * 0.17f),
                    style = Stroke(width = w * 0.08f)
                )
            }
            drawCircle(
                color = onPrimaryColor.copy(alpha = 0.9f),
                radius = w * 0.06f,
                center = Offset(w * 0.36f, h * 0.48f)
            )
        }
    }
}

@Composable
fun MedicinePackThumbnail(
    dosageForm: String,
    modifier: Modifier = Modifier
) {
    val form = dosageForm.trim().lowercase()
    val primary = MaterialTheme.colorScheme.primary
    val teal = MaterialTheme.colorScheme.secondary
    val bgTint = when {
        form.contains("syrup") || form.contains("liquid") || form.contains("suspension") -> Color(0xFFE6F7F5)
        form.contains("inhaler") || form.contains("spray") -> Color(0xFFE0F2FE)
        form.contains("inject") || form.contains("vial") -> Color(0xFFEDE9FE)
        else -> Color(0xFFEAF2FF)
    }
    val accent = when {
        form.contains("syrup") || form.contains("liquid") || form.contains("suspension") -> teal
        form.contains("inhaler") || form.contains("spray") -> Color(0xFF0284C7)
        form.contains("inject") || form.contains("vial") -> Color(0xFF6D28D9)
        else -> primary
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bgTint
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val w = size.width
            val h = size.height
            when {
                form.contains("syrup") || form.contains("liquid") || form.contains("suspension") -> {
                    // Stylized medicine bottle
                    drawRoundRect(
                        color = accent.copy(alpha = 0.35f),
                        topLeft = Offset(w * 0.37f, h * 0.12f),
                        size = Size(w * 0.26f, h * 0.14f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(w * 0.28f, h * 0.26f),
                        size = Size(w * 0.44f, h * 0.62f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset(w * 0.35f, h * 0.42f),
                        size = Size(w * 0.30f, h * 0.28f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.50f, h * 0.48f),
                        end = Offset(w * 0.50f, h * 0.64f),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.42f, h * 0.56f),
                        end = Offset(w * 0.58f, h * 0.56f),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }

                form.contains("inhaler") || form.contains("spray") -> {
                    // Stylized inhaler silhouette
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(w * 0.36f, h * 0.16f),
                        size = Size(w * 0.26f, h * 0.56f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    drawRoundRect(
                        color = accent.copy(alpha = 0.85f),
                        topLeft = Offset(w * 0.36f, h * 0.56f),
                        size = Size(w * 0.42f, h * 0.24f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.7f),
                        topLeft = Offset(w * 0.43f, h * 0.24f),
                        size = Size(w * 0.12f, h * 0.24f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }

                form.contains("capsule") -> {
                    // Dual-tone tilted capsule
                    rotate(degrees = -30f, pivot = Offset(w * 0.5f, h * 0.5f)) {
                        drawRoundRect(
                            color = accent,
                            topLeft = Offset(w * 0.20f, h * 0.34f),
                            size = Size(w * 0.60f, h * 0.32f),
                            cornerRadius = CornerRadius(h * 0.16f, h * 0.16f)
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.88f),
                            topLeft = Offset(w * 0.50f, h * 0.34f),
                            size = Size(w * 0.30f, h * 0.32f),
                            cornerRadius = CornerRadius(h * 0.16f, h * 0.16f)
                        )
                    }
                }

                else -> {
                    // Blister tablet pack
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.20f, h * 0.16f),
                        size = Size(w * 0.60f, h * 0.68f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = accent.copy(alpha = 0.28f),
                        topLeft = Offset(w * 0.20f, h * 0.16f),
                        size = Size(w * 0.60f, h * 0.68f),
                        cornerRadius = CornerRadius(12f, 12f),
                        style = Stroke(width = 3f)
                    )
                    val cols = listOf(0.36f, 0.64f)
                    val rows = listOf(0.35f, 0.65f)
                    rows.forEach { ry ->
                        cols.forEach { cx ->
                            drawCircle(
                                color = accent.copy(alpha = 0.85f),
                                radius = w.coerceAtMost(h) * 0.11f,
                                center = Offset(w * cx, h * ry)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.6f),
                                radius = w.coerceAtMost(h) * 0.04f,
                                center = Offset(w * cx - 2f, h * ry - 2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryGridTile(
    title: String,
    icon: ImageVector,
    bgColor: Color,
    fgColor: Color,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = if (selected) 2.dp else 1.dp,
            border = BorderStroke(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
            ),
            modifier = Modifier.size(62.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = fgColor,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun InteractiveNearbyMapCard(
    items: List<PharmacyInventoryItem>,
    selectedRecordId: String?,
    onSelectRecord: (PharmacyInventoryItem) -> Unit,
    onOpenDetail: (PharmacyInventoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedItem = remember(items, selectedRecordId) {
        items.firstOrNull { it.record.id == selectedRecordId } ?: items.firstOrNull()
    }

    // Normalize coordinates inside map viewport relative to min/max bounds
    val coordPairs = remember(items) {
        val valid = items.filter { it.record.pharmacyLat != 0.0 || it.record.pharmacyLon != 0.0 }
        if (valid.isEmpty()) emptyMap<String, Pair<Float, Float>>()
        else {
            val lats = valid.map { it.record.pharmacyLat }
            val lons = valid.map { it.record.pharmacyLon }
            val minLat = lats.min()
            val maxLat = lats.max()
            val minLon = lons.min()
            val maxLon = lons.max()
            val latSpan = (maxLat - minLat).coerceAtLeast(0.015)
            val lonSpan = (maxLon - minLon).coerceAtLeast(0.015)
            valid.associate { item ->
                val x = ((item.record.pharmacyLon - minLon) / lonSpan).toFloat().coerceIn(0.15f, 0.85f)
                val y = (1f - ((item.record.pharmacyLat - minLat) / latSpan).toFloat()).coerceIn(0.18f, 0.78f)
                item.record.id to (x to y)
            }
        }
    }

    val roadColor = MaterialTheme.colorScheme.surface
    val parkColor = Color(0xFFD5F0E3)
    val waterColor = Color(0xFFD0E7FB)
    val mapBgColor = Color(0xFFEDF3F8)
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val userDotColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(mapBgColor)
            ) {
                val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
                val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Decorative river & park blocks
                    val river = Path().apply {
                        moveTo(0f, size.height * 0.25f)
                        cubicTo(
                            size.width * 0.28f, size.height * 0.14f,
                            size.width * 0.55f, size.height * 0.44f,
                            size.width, size.height * 0.22f
                        )
                        lineTo(size.width, size.height * 0.34f)
                        cubicTo(
                            size.width * 0.55f, size.height * 0.56f,
                            size.width * 0.28f, size.height * 0.26f,
                            0f, size.height * 0.37f
                        )
                        close()
                    }
                    drawPath(river, color = waterColor)

                    drawRoundRect(
                        color = parkColor,
                        topLeft = Offset(size.width * 0.08f, size.height * 0.58f),
                        size = Size(size.width * 0.24f, size.height * 0.22f),
                        cornerRadius = CornerRadius(18f, 18f)
                    )
                    drawRoundRect(
                        color = parkColor,
                        topLeft = Offset(size.width * 0.68f, size.height * 0.52f),
                        size = Size(size.width * 0.22f, size.height * 0.26f),
                        cornerRadius = CornerRadius(18f, 18f)
                    )

                    // Clean street grid
                    val roadXs = listOf(0.18f, 0.45f, 0.74f)
                    roadXs.forEach { rx ->
                        drawLine(
                            color = roadColor,
                            start = Offset(size.width * rx, 0f),
                            end = Offset(size.width * rx, size.height),
                            strokeWidth = 10f
                        )
                    }
                    val roadYs = listOf(0.28f, 0.55f, 0.80f)
                    roadYs.forEach { ry ->
                        drawLine(
                            color = roadColor,
                            start = Offset(0f, size.height * ry),
                            end = Offset(size.width, size.height * ry),
                            strokeWidth = 10f
                        )
                    }
                    drawLine(
                        color = roadColor,
                        start = Offset(0f, size.height * 0.88f),
                        end = Offset(size.width, size.height * 0.14f),
                        strokeWidth = 12f
                    )

                    // Center user discovery radius ring
                    val center = Offset(size.width * 0.5f, size.height * 0.5f)
                    drawCircle(
                        color = userDotColor.copy(alpha = 0.08f),
                        radius = size.minDimension * 0.36f,
                        center = center
                    )
                    drawCircle(
                        color = gridLineColor,
                        radius = size.minDimension * 0.36f,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 12f,
                        center = center
                    )
                    drawCircle(
                        color = userDotColor,
                        radius = 7f,
                        center = center
                    )
                }

                // Tappable Pharmacy Pins
                items.take(8).forEachIndexed { idx, item ->
                    val fallbackX = (0.20f + (idx % 4) * 0.20f)
                    val fallbackY = (0.24f + (idx / 4) * 0.40f)
                    val (normX, normY) = coordPairs[item.record.id] ?: (fallbackX to fallbackY)
                    val isSelected = selectedItem?.record?.id == item.record.id
                    val pinColor = when {
                        item.freshnessTier == FreshnessTier.STALE -> StatusStaleFg
                        item.record.availabilityEnum() == AvailabilityStatus.REPORTED_AVAILABLE -> StatusAvailableFg
                        item.record.availabilityEnum() == AvailabilityStatus.LOW_STOCK -> StatusLowStockFg
                        else -> StatusOutOfStockFg
                    }
                    val pinPxX = (widthPx * normX).roundToInt()
                    val pinPxY = (heightPx * normY).roundToInt()

                    Surface(
                        modifier = Modifier
                            .offset { IntOffset(pinPxX - 44, pinPxY - 44) }
                            .shadow(if (isSelected) 6.dp else 2.dp, CircleShape)
                            .clickable { onSelectRecord(item) }
                            .testTag("map_pin_${item.record.pharmacyId}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) pinColor else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(if (isSelected) 2.dp else 1.5.dp, pinColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalPharmacy,
                                contentDescription = item.record.pharmacyName,
                                tint = if (isSelected) Color.White else pinColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = item.distanceKm?.let { String.format("%.1f km", it) } ?: "#${idx + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else pinColor
                            )
                        }
                    }
                }

                // Top floating map legend pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Tap a store pin • Launch Google Maps below",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (selectedItem != null) {
                val rec = selectedItem.record
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rec.pharmacyName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${rec.pharmacyAddress} • ${rec.pharmacyHours}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (selectedItem.distanceKm != null) {
                            StatusPill(
                                text = String.format("%.1f km", selectedItem.distanceKm),
                                bgColor = MaterialTheme.colorScheme.primaryContainer,
                                fgColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AvailabilityBadge(rec.availabilityEnum())
                        FreshnessBadge(selectedItem.freshnessTier)
                        StatusPill(
                            text = "${rec.quantity} ${rec.unit}",
                            bgColor = MaterialTheme.colorScheme.surfaceVariant,
                            fgColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Reported ${GeoAndTimeUtils.formatRelativeAndLocalTime(rec.effectiveTimestamp())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { dialPharmacy(context, rec.pharmacyPhone) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Call, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Call Store")
                        }
                        OutlinedButton(
                            onClick = {
                                openPharmacyDirections(
                                    context,
                                    rec.pharmacyLat,
                                    rec.pharmacyLon,
                                    rec.pharmacyName,
                                    rec.pharmacyAddress
                                )
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Outlined.Directions, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Google Maps")
                        }
                        TextButton(
                            onClick = { onOpenDetail(selectedItem) },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Details")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedicalDisclaimerBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Safety disclaimer",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )
            Text(
                text = stringResource(R.string.medical_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun AvailabilityBadge(status: AvailabilityStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        AvailabilityStatus.REPORTED_AVAILABLE -> StatusAvailableBg to StatusAvailableFg
        AvailabilityStatus.LOW_STOCK -> StatusLowStockBg to StatusLowStockFg
        AvailabilityStatus.OUT_OF_STOCK -> StatusOutOfStockBg to StatusOutOfStockFg
        AvailabilityStatus.PAUSED -> StatusStaleBg to StatusStaleFg
    }
    StatusPill(text = status.label, bgColor = bg, fgColor = fg, modifier = modifier)
}

@Composable
fun FreshnessBadge(tier: FreshnessTier, modifier: Modifier = Modifier) {
    val (bg, fg, icon) = when (tier) {
        FreshnessTier.FRESH -> Triple(StatusAvailableBg, StatusAvailableFg, Icons.Outlined.Schedule)
        FreshnessTier.AGING -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Icons.Outlined.Schedule)
        FreshnessTier.STALE -> Triple(StatusStaleBg, StatusStaleFg, Icons.Outlined.WarningAmber)
    }
    StatusPill(
        text = tier.label,
        bgColor = bg,
        fgColor = fg,
        icon = icon,
        modifier = modifier
    )
}

@Composable
fun VerificationBadge(status: PharmacyVerificationStatus, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (status) {
        PharmacyVerificationStatus.APPROVED -> Triple(StatusAvailableBg, StatusAvailableFg, "Verified")
        PharmacyVerificationStatus.PENDING -> Triple(StatusLowStockBg, StatusLowStockFg, "Pending Review")
        PharmacyVerificationStatus.REJECTED -> Triple(StatusOutOfStockBg, StatusOutOfStockFg, "Rejected")
        PharmacyVerificationStatus.SUSPENDED -> Triple(StatusOutOfStockBg, StatusOutOfStockFg, "Suspended")
    }
    StatusPill(
        text = label,
        bgColor = bg,
        fgColor = fg,
        icon = if (status == PharmacyVerificationStatus.APPROVED) Icons.Outlined.VerifiedUser else null,
        modifier = modifier
    )
}

@Composable
fun StatusPill(
    text: String,
    bgColor: Color,
    fgColor: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fgColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = fgColor
            )
        }
    }
}

@Composable
fun LoadingStateBox(label: String = "Syncing live records...") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorStateCard(message: String, onRetry: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = StatusOutOfStockBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sync Notice",
                style = MaterialTheme.typography.titleSmall,
                color = StatusOutOfStockFg
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = StatusOutOfStockFg
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag("retry_button")
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

fun dialPharmacy(context: Context, phone: String) {
    val sanitized = phone.trim()
    if (sanitized.isBlank()) return
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized"))
    runCatching { context.startActivity(intent) }
}

fun openPharmacyDirections(
    context: Context,
    lat: Double,
    lon: Double,
    label: String,
    address: String = ""
) {
    val hasCoords = lat != 0.0 || lon != 0.0
    val destinationQuery = if (hasCoords) {
        "$lat,$lon"
    } else {
        Uri.encode(listOf(label, address).filter { it.isNotBlank() }.joinToString(", "))
    }

    val mapsDirectionsUrl = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&destination=$destinationQuery&travelmode=driving"
    )
    val mapsAppIntent = Intent(Intent.ACTION_VIEW, mapsDirectionsUrl).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (mapsAppIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapsAppIntent)
        return
    }

    if (hasCoords) {
        val navIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lon")).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (navIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(navIntent)
            return
        }
    }

    val geoUri = if (hasCoords) {
        Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})")
    } else {
        Uri.parse("geo:0,0?q=${Uri.encode(listOf(label, address).filter { it.isNotBlank() }.joinToString(", "))}")
    }
    val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
    if (geoIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(geoIntent)
        return
    }

    val browserIntent = Intent(Intent.ACTION_VIEW, mapsDirectionsUrl)
    runCatching { context.startActivity(browserIntent) }.onFailure {
        android.widget.Toast.makeText(
            context,
            "No maps app or browser found to open directions.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
fun SubmitMedicineDialog(
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, String) -> Unit
) {
    var genericName by remember { mutableStateOf("") }
    var brandName by remember { mutableStateOf("") }
    var strength by remember { mutableStateOf("") }
    var dosageForm by remember { mutableStateOf("Tablet") }
    var category by remember { mutableStateOf("General") }
    var keywords by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAdmin) "Add Verified Medicine" else "Add Medicine to Catalogue") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isAdmin) {
                        "Entries added by Admin are immediately verified in the search catalogue."
                    } else {
                        "Submitted medicines are sent for catalogue verification before appearing in customer search."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = genericName,
                    onValueChange = { genericName = it },
                    label = { Text("Generic Name *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = it },
                    label = { Text("Brand Name *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = strength,
                    onValueChange = { strength = it },
                    label = { Text("Strength (e.g. 500 mg) *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = dosageForm,
                    onValueChange = { dosageForm = it },
                    label = { Text("Dosage Form (e.g. Tablet, Syrup) *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Keywords (comma-separated)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(genericName, brandName, strength, dosageForm, category, keywords) },
                enabled = genericName.trim().length >= 2 && brandName.isNotBlank() && strength.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
