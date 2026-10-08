package com.dapprod.dapgalleria.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.ui.Decision
import com.dapprod.dapgalleria.ui.theme.Palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

/** Stato del trascinamento della carta in cima: posizione, animazione di uscita, soglie di decisione. */
@Stable
class CardSwipeState(
    private val scope: CoroutineScope,
    private val widthPx: Float,
    initial: Offset,
    private val onThreshold: () -> Unit,
    private val onDecision: (Decision) -> Unit,
) {
    val offset = Animatable(initial, Offset.VectorConverter)
    private var finished = false
    private var beyond = 0

    /** Se la carta è stata presa nella metà bassa gira al contrario (come una carta vera). */
    var grabSign by mutableIntStateOf(1)

    /** -1 (tutto a sinistra, elimina) ... +1 (tutto a destra, tieni). */
    val progress: Float get() = (offset.value.x / (widthPx * 0.4f)).coerceIn(-1f, 1f)

    suspend fun enter() {
        offset.animateTo(Offset.Zero, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
    }

    fun start(y: Float, height: Float) {
        grabSign = if (y > height * 0.55f) -1 else 1
    }

    fun drag(delta: Offset) {
        if (finished) return
        scope.launch { offset.snapTo(offset.value + delta) }
        val x = offset.value.x + delta.x
        val now = when {
            x > widthPx * THRESHOLD -> 1
            x < -widthPx * THRESHOLD -> -1
            else -> 0
        }
        if (now != beyond) {
            if (now != 0) onThreshold()
            beyond = now
        }
    }

    fun release(velocityX: Float) {
        if (finished) return
        val x = offset.value.x
        when {
            x > widthPx * THRESHOLD || (velocityX > FLING_VELOCITY && x > 24f) -> fling(Decision.KEEP, velocityX)
            x < -widthPx * THRESHOLD || (velocityX < -FLING_VELOCITY && x < -24f) -> fling(Decision.DELETE, velocityX)
            else -> {
                beyond = 0
                scope.launch {
                    offset.animateTo(Offset.Zero, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    /** Fa uscire la carta dallo schermo e poi comunica la decisione. Usata anche dai pulsanti. */
    fun fling(decision: Decision, velocityX: Float = 0f) {
        if (finished) return
        finished = true
        scope.launch {
            val sign = if (decision == Decision.KEEP) 1f else -1f
            val target = Offset(sign * widthPx * 1.7f, offset.value.y + (if (velocityX == 0f) -widthPx * 0.15f else 0f))
            val speed = (abs(velocityX) / 4000f).coerceIn(0f, 1f)
            offset.animateTo(target, tween(durationMillis = (300 - 120 * speed).toInt(), easing = FastOutLinearInEasing))
            onDecision(decision)
        }
    }

    private companion object {
        const val FLING_VELOCITY = 1800f
        const val THRESHOLD = 0.28f
    }
}

@Composable
fun SwipeDeck(
    items: List<MediaEntry>,
    index: Int,
    canUndo: Boolean,
    lastUndone: Decision?,
    isGolden: (MediaEntry) -> Boolean,
    onDecision: (Decision) -> Unit,
    onUndo: () -> Unit,
    onEdit: () -> Unit,
    onOpenPhoto: (MediaEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = items[index]
    val next = items.getOrNull(index + 1)
    val third = items.getOrNull(index + 2)
    var muted by rememberSaveable { mutableStateOf(true) }
    val feedback = LocalFeedback.current

    val scope = rememberCoroutineScope()
    val latestOnDecision by rememberUpdatedState(onDecision)
    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    // annulla: la carta rientra dal lato da cui era uscita
    var lastIndex by remember { mutableIntStateOf(index) }
    val fromUndo = index < lastIndex
    SideEffect { lastIndex = index }
    val swipe = remember(top.key) {
        val start = if (fromUndo) {
            Offset(if (lastUndone == Decision.KEEP) widthPx * 1.5f else -widthPx * 1.5f, -60f)
        } else {
            Offset.Zero
        }
        CardSwipeState(scope, widthPx, start, onThreshold = { feedback?.threshold() }) { latestOnDecision(it) }
    }
    LaunchedEffect(swipe) { if (swipe.offset.value != Offset.Zero) swipe.enter() }

    var deckHeight by remember { mutableIntStateOf(1) }

    Column(modifier) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .onSizeChanged { deckHeight = it.height },
        ) {
            if (third != null) {
                key(third.key) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val p = abs(swipe.progress)
                                val s = 0.84f + 0.06f * p
                                scaleX = s
                                scaleY = s
                                translationY = (1f - p) * 40.dp.toPx() + p * 22.dp.toPx()
                                alpha = 0.55f + 0.25f * p
                            },
                    ) {
                        MediaCard(third, active = false, muted = true, onToggleMute = {}, golden = isGolden(third))
                    }
                }
            }
            if (next != null) {
                key(next.key) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val p = abs(swipe.progress)
                                val s = 0.92f + 0.08f * p
                                scaleX = s
                                scaleY = s
                                translationY = (1f - p) * 22.dp.toPx()
                                alpha = 0.85f + 0.15f * p
                            },
                    ) {
                        MediaCard(next, active = false, muted = muted, onToggleMute = {}, golden = isGolden(next))
                    }
                }
            }

            key(top.key) {
                val golden = isGolden(top)
                Box(
                    Modifier
                        .fillMaxSize()
                        // il rilevatore sta fuori dal graphicsLayer: cosi' le coordinate non si muovono con la carta
                        .pointerInput(top.key) {
                            val tracker = VelocityTracker()
                            detectDragGestures(
                                onDragStart = { start ->
                                    tracker.resetTracking()
                                    swipe.start(start.y, deckHeight.toFloat())
                                },
                                onDragEnd = { swipe.release(tracker.calculateVelocity().x) },
                                onDragCancel = { swipe.release(0f) },
                                onDrag = { change, dragAmount ->
                                    tracker.addPosition(change.uptimeMillis, change.position)
                                    change.consume()
                                    swipe.drag(dragAmount)
                                },
                            )
                        }
                        .graphicsLayer {
                            val o = swipe.offset.value
                            translationX = o.x
                            translationY = o.y * 0.5f
                            rotationZ = o.x / widthPx * 18f * swipe.grabSign
                            val lift = 1f + 0.03f * abs(swipe.progress)
                            scaleX = lift
                            scaleY = lift
                        },
                ) {
                    MediaCard(
                        top,
                        active = true,
                        muted = muted,
                        onToggleMute = { muted = !muted },
                        onOpenPhoto = { onOpenPhoto(top) },
                        golden = golden,
                    )

                    // velo colorato che si intensifica trascinando
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(28.dp))
                            .drawBehind {
                                val p = swipe.progress
                                val color = if (p >= 0f) Palette.Keep else Palette.Delete
                                drawRect(
                                    Brush.horizontalGradient(
                                        if (p >= 0f) listOf(Color.Transparent, color) else listOf(color, Color.Transparent),
                                    ),
                                    alpha = abs(p) * 0.55f,
                                )
                            },
                    )
                    SwipeStamp(
                        text = "TIENI",
                        color = Palette.Keep,
                        rotation = -14f,
                        progress = { max(swipe.progress, 0f) },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(28.dp),
                    )
                    SwipeStamp(
                        text = "ELIMINA",
                        color = Palette.Delete,
                        rotation = 14f,
                        progress = { max(-swipe.progress, 0f) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(28.dp),
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(Icons.AutoMirrored.Filled.Undo, "Annulla l'ultimo swipe", Palette.Amber, 50.dp, enabled = canUndo) {
                feedback?.undo()
                onUndo()
            }
            ActionButton(
                Icons.Filled.Close, "Elimina", Palette.Delete, 70.dp,
                lean = { max(-swipe.progress, 0f) },
            ) { swipe.fling(Decision.DELETE) }
            ActionButton(
                Icons.Filled.Check, "Tieni", Palette.Keep, 70.dp,
                lean = { max(swipe.progress, 0f) },
            ) { swipe.fling(Decision.KEEP) }
            // modifica (foto e video): la versione modificata viene tenuta
            ActionButton(Icons.Filled.AutoFixHigh, "Modifica", Palette.Cyan, 50.dp, onClick = onEdit)
        }
    }
}

@Composable
private fun SwipeStamp(text: String, color: Color, rotation: Float, progress: () -> Float, modifier: Modifier = Modifier) {
    Text(
        text,
        color = color,
        fontSize = 32.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 3.sp,
        modifier = modifier
            .graphicsLayer {
                val p = progress()
                alpha = (p * 1.6f).coerceIn(0f, 1f)
                // timbro che "batte": cresce oltre e torna
                val s = 0.6f + 0.55f * p.coerceIn(0f, 1f) - 0.15f * ((p - 0.7f).coerceIn(0f, 0.3f) / 0.3f)
                scaleX = s
                scaleY = s
                rotationZ = rotation
            }
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .border(4.dp, color, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    )
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    size: Dp,
    enabled: Boolean = true,
    lean: () -> Float = { 0f },
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val alpha by animateFloatAsState(if (enabled) 1f else 0.3f, label = "enabled")
    Box(
        Modifier
            .size(size)
            .pressBounce(interaction)
            .graphicsLayer {
                val l = lean()
                val s = 1f + 0.22f * l
                scaleX = s
                scaleY = s
                this.alpha = alpha
            }
            .clip(CircleShape)
            .drawBehind {
                val l = lean()
                drawCircle(Palette.Surface)
                drawCircle(tint.copy(alpha = 0.10f + 0.38f * l))
            }
            .border(2.5.dp, tint, CircleShape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}
