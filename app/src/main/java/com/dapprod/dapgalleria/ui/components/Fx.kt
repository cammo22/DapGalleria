package com.dapprod.dapgalleria.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapprod.dapgalleria.ui.theme.ConfettiColors
import com.dapprod.dapgalleria.ui.theme.Palette
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Lo sfondo vivo della sala: tre luci al neon che si muovono piano dietro a tutto. */
@Composable
fun NeonBackdrop(modifier: Modifier = Modifier, intensity: Float = 1f) {
    val t = rememberInfiniteTransition(label = "backdrop")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(0f, 1f, infiniteRepeatable(tween(19_000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    Canvas(modifier.fillMaxSize()) {
        drawRect(Palette.Background)
        val w = size.width
        val h = size.height
        val r = maxOf(w, h) * 0.55f
        fun blob(color: Color, x: Float, y: Float, alpha: Float) {
            drawCircle(
                Brush.radialGradient(listOf(color.copy(alpha = alpha * intensity), Color.Transparent), center = Offset(x, y), radius = r),
                radius = r,
                center = Offset(x, y),
            )
        }
        blob(Palette.Magenta, w * (0.1f + 0.5f * a), h * (0.05f + 0.25f * b), 0.16f)
        blob(Palette.Violet, w * (0.95f - 0.4f * b), h * (0.45f + 0.2f * a), 0.14f)
        blob(Palette.Cyan, w * (0.2f + 0.4f * b), h * (0.95f - 0.2f * a), 0.10f)
    }
}

/** Rimpicciolisce un poco quando lo premi e torna con la molla: per tutti i pulsanti. */
fun Modifier.pressBounce(interaction: MutableInteractionSource, pressedScale: Float = 0.88f): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Un numero che scorre fino al valore nuovo, come un contatore da flipper. */
@Composable
fun AnimatedNumber(
    value: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    durationMs: Int = 700,
    format: (Long) -> String = { formatThousands(it) },
) {
    val anim = remember { Animatable(value.toFloat()) }
    LaunchedEffect(value) { anim.animateTo(value.toFloat(), tween(durationMs, easing = FastOutSlowInEasing)) }
    Text(format(anim.value.toLong()), style = style, color = color, modifier = modifier)
}

/** Anello di avanzamento con il gradiente DaProd e il contenuto al centro (es. l'emoji del livello). */
@Composable
fun ProgressRing(
    progress: Float,
    size: Dp,
    modifier: Modifier = Modifier,
    stroke: Dp = 4.dp,
    track: Color = Palette.SurfaceHigh,
    colors: List<Color> = listOf(Palette.Magenta, Palette.Violet, Palette.Cyan, Palette.Magenta),
    content: @Composable () -> Unit = {},
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
            rotate(-90f) {
                drawArc(Brush.sweepGradient(colors), 0f, 360f * p, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

/** Bordo che brilla e scorre (carta d'oro, forziere pronto). */
fun Modifier.shimmerBorder(colors: List<Color>, width: Dp, corner: Dp, speedMs: Int = 1800): Modifier = composed {
    val t = rememberInfiniteTransition(label = "shimmer")
    val shift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(speedMs, easing = LinearEasing)), label = "shift")
    drawWithContent {
        drawContent()
        val sw = width.toPx()
        val span = size.width + size.height
        val x = shift * span * 2f - span
        val brush = Brush.linearGradient(
            colors,
            start = Offset(x, 0f),
            end = Offset(x + span / 2f, size.height),
            tileMode = TileMode.Mirror,
        )
        val r = corner.toPx()
        drawRoundRect(
            brush = brush,
            topLeft = Offset(sw / 2, sw / 2),
            size = Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(sw),
        )
    }
}

// ------------------------------------------------------------------ coriandoli

private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rot: Float,
    val vr: Float,
    val color: Color,
    val w: Float,
    val h: Float,
    var life: Float,
    val maxLife: Float,
    val round: Boolean,
)

/** I coriandoli: [burst] li spara da un punto (frazioni dello schermo), il [ConfettiLayer] li disegna. */
@Stable
class ConfettiState {
    private val particles = ArrayList<Particle>()
    internal var size = Size.Zero
    internal var frame by mutableLongStateOf(0L)
    internal var generation by mutableIntStateOf(0)

    /**
     * [x], [y] in 0..1 sullo schermo. [direction] in gradi (-90 = verso l'alto), [spread] l'apertura del ventaglio.
     */
    fun burst(
        x: Float,
        y: Float,
        count: Int = 70,
        colors: List<Color> = ConfettiColors,
        power: Float = 1f,
        direction: Float = -90f,
        spread: Float = 360f,
    ) {
        if (size == Size.Zero) return
        val ox = x * size.width
        val oy = y * size.height
        val speed = size.minDimension * 1.6f * power
        repeat(count) {
            val angle = Math.toRadians((direction + (Random.nextFloat() - 0.5f) * spread).toDouble())
            val v = speed * (0.35f + Random.nextFloat() * 0.65f)
            val life = 1.2f + Random.nextFloat() * 1.0f
            particles += Particle(
                x = ox, y = oy,
                vx = (cos(angle) * v).toFloat(), vy = (sin(angle) * v).toFloat(),
                rot = Random.nextFloat() * 360f, vr = (Random.nextFloat() - 0.5f) * 720f,
                color = colors[Random.nextInt(colors.size)],
                w = 6f + Random.nextFloat() * 8f, h = 4f + Random.nextFloat() * 6f,
                life = life, maxLife = life, round = Random.nextInt(4) == 0,
            )
        }
        generation++
    }

    /** Una pioggia dall'alto su tutta la larghezza (grandi feste). */
    fun rain(count: Int = 120, colors: List<Color> = ConfettiColors) {
        if (size == Size.Zero) return
        repeat(count) {
            val life = 2.0f + Random.nextFloat() * 1.5f
            particles += Particle(
                x = Random.nextFloat() * size.width, y = -20f - Random.nextFloat() * size.height * 0.3f,
                vx = (Random.nextFloat() - 0.5f) * 120f, vy = 150f + Random.nextFloat() * 250f,
                rot = Random.nextFloat() * 360f, vr = (Random.nextFloat() - 0.5f) * 540f,
                color = colors[Random.nextInt(colors.size)],
                w = 7f + Random.nextFloat() * 8f, h = 4f + Random.nextFloat() * 6f,
                life = life, maxLife = life, round = Random.nextInt(4) == 0,
            )
        }
        generation++
    }

    internal fun step(dt: Float): Boolean {
        val gravity = size.height * 0.9f
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.vy += gravity * dt
            p.vx *= (1f - 1.2f * dt)
            p.vy *= (1f - 0.6f * dt)
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rot += p.vr * dt
            p.life -= dt
            if (p.life <= 0f || p.y > size.height + 40f) it.remove()
        }
        frame++
        return particles.isNotEmpty()
    }

    internal fun forEach(block: (x: Float, y: Float, w: Float, h: Float, rot: Float, color: Color, alpha: Float, round: Boolean) -> Unit) {
        for (p in particles) block(p.x, p.y, p.w, p.h, p.rot, p.color, (p.life / p.maxLife * 2f).coerceIn(0f, 1f), p.round)
    }
}

@Composable
fun rememberConfettiState(): ConfettiState = remember { ConfettiState() }

/** Disegna i coriandoli sopra a tutto. Non intercetta i tocchi. */
@Composable
fun ConfettiLayer(state: ConfettiState, modifier: Modifier = Modifier) {
    LaunchedEffect(state) {
        snapshotFlow { state.generation }.collectLatest {
            var last = 0L
            var alive = true
            while (alive) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                    last = now
                    alive = state.step(dt)
                }
            }
        }
    }
    Canvas(
        modifier
            .fillMaxSize()
            .onSizeChanged { state.size = Size(it.width.toFloat(), it.height.toFloat()) },
    ) {
        state.frame // legge il fotogramma: ridisegna a ogni passo
        state.forEach { x, y, w, h, rot, color, alpha, round ->
            if (round) {
                drawCircle(color, radius = w / 2, center = Offset(x, y), alpha = alpha)
            } else {
                rotate(rot, pivot = Offset(x, y)) {
                    drawRect(color, topLeft = Offset(x - w / 2, y - h / 2), size = Size(w, h), alpha = alpha)
                }
            }
        }
    }
}

fun formatThousands(n: Long): String {
    val s = kotlin.math.abs(n).toString()
    val sb = StringBuilder()
    for ((i, c) in s.withIndex()) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
        sb.append(c)
    }
    return if (n < 0) "-$sb" else sb.toString()
}

/** Stile per le etichette da sala giochi (MAIUSCOLE spaziate). */
val ArcadeLabel = TextStyle(fontSize = 11.sp, letterSpacing = 1.5.sp)
