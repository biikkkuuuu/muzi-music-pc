package com.muzi.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pure AMOLED Black (Exact Android Muzi Pure Black)
val PureBlack = Color(0xFF000000)
val SurfaceDark = Color(0xFF101010)
val SurfaceElevated = Color(0xFF181818)
val SurfaceCard = Color(0xFF202020)
val SurfaceBorder = Color(0xFF2B2B2B)

// Exact Android Muzi Brand Accent from Screenshot (Material 3 Dynamic Electric Blue)
val MuziBlue = Color(0xFF3B82F6)
val MuziBlueContainer = Color(0xFF2A3D66)
val MuziAccent = MuziBlue
val PrimaryBlue = MuziBlue

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A0A0)
val TextTertiary = Color(0xFF666666)
val ChipBackground = Color(0xFF242424)
val ChipSelected = Color(0xFF333333)

private val MuziDarkColorScheme = darkColorScheme(
    primary = MuziBlue,
    onPrimary = Color.White,
    primaryContainer = MuziBlueContainer,
    onPrimaryContainer = Color.White,
    secondary = MuziBlue,
    background = PureBlack,
    surface = SurfaceDark,
    surfaceVariant = SurfaceElevated,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun MuziTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MuziDarkColorScheme,
        content = content
    )
}
