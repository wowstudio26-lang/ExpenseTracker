package com.wowstudio.expensetracker.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Design tokens distilled from the supplied reference app; no branding/assets are copied. */
object ReferenceColors {
    val Background = Color(0xFF0B0D10)
    val Surface = Color(0xFF15181D)
    val SurfaceElevated = Color(0xFF1C2026)
    val Border = Color(0xFF2A2F36)
    val Primary = Color(0xFF6C63FF)
    val PrimarySoft = Color(0xFFB8B3FF)
    val TextPrimary = Color(0xFFF2F3F5)
    val TextSecondary = Color(0xFF8D949E)
    val Positive = Color(0xFF4BD59A)
    val Negative = Color(0xFFFF737F)
}

@Composable
fun referenceDarkColors() = darkColorScheme(
    primary = ReferenceColors.Primary,
    onPrimary = Color.White,
    primaryContainer = ReferenceColors.Primary,
    onPrimaryContainer = Color.White,
    secondary = ReferenceColors.PrimarySoft,
    background = ReferenceColors.Background,
    onBackground = ReferenceColors.TextPrimary,
    surface = ReferenceColors.Surface,
    onSurface = ReferenceColors.TextPrimary,
    surfaceVariant = ReferenceColors.SurfaceElevated,
    onSurfaceVariant = ReferenceColors.TextSecondary,
    outline = ReferenceColors.Border
)
