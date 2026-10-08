package com.dapprod.dapgalleria.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** I colori della sala DaProd: fondo viola notte, magenta, oro e ciano al neon. */
object Palette {
    val Background = Color(0xFF0B0912)
    val Night = Color(0xFF07060D)
    val Surface = Color(0xFF16121F)
    val SurfaceHigh = Color(0xFF221C2E)
    val Outline = Color(0xFF332A44)
    val CardBackground = Color(0xFF120F19)
    val Pink = Color(0xFFFF4D8D)
    val Magenta = Color(0xFFFF3DF2)
    val Violet = Color(0xFF8B5CF6)
    val Cyan = Color(0xFF35E8FF)
    val Gold = Color(0xFFFFD54A)
    val GoldDeep = Color(0xFFFFAB00)
    val Keep = Color(0xFF2EE59D)
    val Delete = Color(0xFFFF4D5E)
    val Amber = Color(0xFFFFB74D)
    val Sky = Color(0xFF4DB8FF)
    val Fire = Color(0xFFFF7A1A)
    val TextPrimary = Color(0xFFF4F1FA)
    val TextSecondary = Color(0xFFB0A6C6)
    val TextDim = Color(0xFF6E6585)
}

val BrandGradient: Brush = Brush.linearGradient(listOf(Palette.Magenta, Palette.Violet, Palette.Cyan))
val HotGradient: Brush = Brush.linearGradient(listOf(Palette.Pink, Palette.Magenta))
val GoldGradient: Brush = Brush.linearGradient(listOf(Palette.Gold, Palette.GoldDeep, Palette.Gold))
val FireGradient: Brush = Brush.linearGradient(listOf(Palette.Gold, Palette.Fire, Palette.Pink))
val KeepGradient: Brush = Brush.linearGradient(listOf(Palette.Keep, Palette.Cyan))
val DeleteGradient: Brush = Brush.linearGradient(listOf(Palette.Delete, Palette.Pink))

/** I colori dei coriandoli. */
val ConfettiColors = listOf(Palette.Magenta, Palette.Gold, Palette.Cyan, Palette.Keep, Palette.Violet, Palette.Pink)

private val Scheme = darkColorScheme(
    primary = Palette.Magenta,
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
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHigh,
    outline = Palette.Outline,
    error = Palette.Delete,
    onError = Color.White,
)

private val Base = Typography()
private val AppTypography = Base.copy(
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.Bold),
)

/** Numeri grandi da sala giochi. */
val ArcadeNumber = TextStyle(fontWeight = FontWeight.Black, fontSize = 44.sp, letterSpacing = (-1).sp)

@Composable
fun DapGalleriaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
}
