package com.muzi.desktop.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Pure AMOLED Black (Exact Android Muzi Pure Black)
val PureBlack = Color(0xFF000000)
val SurfaceDark = Color(0xFF000000)
val SurfaceElevated = Color(0xFF141416)
val SurfaceCard = Color(0xFF1C1D21)
val SurfaceBorder = Color(0xFF2B2D33)

// Exact Android Muzi Brand Accent (Electric Blue #3B82F6)
val MuziBlue = Color(0xFF3B82F6)
val MuziBlueContainer = Color(0xFF1E3A8A)
val MuziAccent = MuziBlue
val PrimaryBlue = MuziBlue

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A3A8)
val TextTertiary = Color(0xFF6B7280)
val ChipBackground = Color(0xFF1C1D21)
val ChipSelected = Color(0xFF3B82F6)

val MuziDarkColorScheme = darkColorScheme(
    primary = MuziBlue,
    onPrimary = Color.White,
    primaryContainer = MuziBlueContainer,
    onPrimaryContainer = Color.White,
    secondary = MuziBlue,
    onSecondary = Color.White,
    background = PureBlack,
    surface = PureBlack,
    surfaceVariant = SurfaceElevated,
    surfaceContainer = SurfaceCard,
    surfaceContainerHighest = SurfaceElevated,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun MuziTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MuziDarkColorScheme,
        typography = AppTypography,
        shapes = androidx.compose.material3.MaterialTheme.shapes.copy(
            extraSmall = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}
