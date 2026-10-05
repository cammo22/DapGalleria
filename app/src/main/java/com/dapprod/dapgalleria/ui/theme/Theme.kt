package com.dapprod.dapgalleria.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Palette {
    val Background = Color(0xFF0E0E14)
    val Surface = Color(0xFF17171F)
    val SurfaceHigh = Color(0xFF22222D)
    val CardBackground = Color(0xFF14141B)
    val Pink = Color(0xFFFF4D8D)
    val Violet = Color(0xFF8B5CF6)
    val Keep = Color(0xFF2EE59D)
    val Delete = Color(0xFFFF4D5E)
    val Amber = Color(0xFFFFB74D)
    val Sky = Color(0xFF4DB8FF)
    val TextPrimary = Color(0xFFF2F2F7)
    val TextSecondary = Color(0xFFA9A9BC)
}

val BrandGradient: Brush = Brush.linearGradient(listOf(Palette.Pink, Palette.Violet))

private val Scheme = darkColorScheme(
    primary = Palette.Pink,
    onPrimary = Color.White,
    secondary = Palette.Violet,
    onSecondary = Color.White,
    tertiary = Palette.Keep,
    background = Palette.Background,
    onBackground = Palette.TextPrimary,
    surface = Palette.Surface,
    onSurface = Palette.TextPrimary,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.TextSecondary,
    error = Palette.Delete,
    onError = Color.White,
)

@Composable
fun DapGalleriaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
