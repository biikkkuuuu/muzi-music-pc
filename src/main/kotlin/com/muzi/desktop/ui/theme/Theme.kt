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

// Exact Android Muzi Brand Accent (DefaultThemeColor = Color(0xFFED5564))
val MuziAccent = Color(0xFFED5564)
val PrimaryRed = MuziAccent
val SecondaryRed = Color(0xFFD64554)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A0A0)
val TextTertiary = Color(0xFF666666)
val ChipBackground = Color(0xFF1C1C1C)
val ChipSelected = MuziAccent

private val MuziDarkColorScheme = darkColorScheme(
    primary = MuziAccent,
    secondary = MuziAccent,
    background = PureBlack,
    surface = SurfaceDark,
    surfaceVariant = SurfaceElevated,
    onPrimary = Color.White,
    onSecondary = Color.White,
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
