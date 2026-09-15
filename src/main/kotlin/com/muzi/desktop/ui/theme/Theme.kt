package com.muzi.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PureBlack = Color(0xFF000000)
val SurfaceDark = Color(0xFF121212)
val SurfaceElevated = Color(0xFF1E1E1E)
val PrimaryRed = Color(0xFF8B1E1E)
val SecondaryRed = Color(0xFF4A1010)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB3B3B3)
val TextTertiary = Color(0xFF757575)
val ChipBackground = Color(0xFF262626)

private val MuziDarkColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = PrimaryRed,
    background = PureBlack,
    surface = SurfaceDark,
    onPrimary = PureBlack,
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
