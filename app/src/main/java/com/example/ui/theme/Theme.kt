package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = MedicalBluePrimary,
    onPrimary = MedicalBlueOnPrimary,
    primaryContainer = MedicalBluePrimaryContainer,
    onPrimaryContainer = MedicalBlueOnPrimaryContainer,
    secondary = ClinicalTealSecondary,
    onSecondary = ClinicalTealOnSecondary,
    secondaryContainer = ClinicalTealContainer,
    onSecondaryContainer = ClinicalTealOnContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = AmberOnTertiaryContainer,
    background = IceBackground,
    onBackground = DeepNavyOnBackground,
    surface = PureWhiteSurface,
    onSurface = DeepNavyOnBackground,
    surfaceVariant = SoftSlateSurfaceVariant,
    onSurfaceVariant = MutedSlateOnSurfaceVariant,
    surfaceContainerLowest = PureWhiteSurface,
    surfaceContainerLow = SubtleSurfaceLow,
    surfaceContainer = SubtleSurfaceMid,
    surfaceContainerHigh = SubtleSurfaceHigh,
    surfaceContainerHighest = SoftSlateSurfaceVariant,
    outline = HairlineOutline,
    outlineVariant = HairlineOutline,
    error = StatusOutOfStockFg,
    onError = MedicalBlueOnPrimary,
    errorContainer = StatusOutOfStockBg,
    onErrorContainer = StatusOutOfStockFg
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBluePrimary,
    onPrimary = DarkBlueOnPrimary,
    primaryContainer = DarkBluePrimaryContainer,
    onPrimaryContainer = DarkBlueOnPrimaryContainer,
    secondary = DarkTealSecondary,
    onSecondary = DarkTealOnSecondary,
    secondaryContainer = DarkTealContainer,
    onSecondaryContainer = DarkTealOnContainer,
    tertiary = DarkAmberTertiary,
    onTertiary = DarkAmberOnTertiary,
    tertiaryContainer = DarkAmberTertiaryContainer,
    onTertiaryContainer = DarkAmberOnTertiaryContainer,
    background = ObsidianBackground,
    onBackground = ObsidianOnBackground,
    surface = ObsidianSurface,
    onSurface = ObsidianOnBackground,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianOnSurfaceVariant,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianSurfaceVariant,
    surfaceContainerHighest = DarkOutline,
    outline = DarkOutline,
    outlineVariant = DarkOutline
)

val MedFindShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MedFindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MedFindTypography,
        shapes = MedFindShapes,
        content = content
    )
}
