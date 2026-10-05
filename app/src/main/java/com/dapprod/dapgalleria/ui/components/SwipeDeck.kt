package com.dapprod.dapgalleria.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.MediaType
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
    private val onDecision: (Decision) -> Unit,
) {
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)
    private var finished = false

    /** -1 (tutto a sinistra, elimina) ... +1 (tutto a destra, tieni). */
    val progress: Float get() = (offset.value.x / (widthPx * 0.4f)).coerceIn(-1f, 1f)

    fun drag(delta: Offset) {
        if (finished) return
        scope.launch { offset.snapTo(offset.value + delta) }
    }

    fun release(velocityX: Float) {
        if (finished) return
        val x = offset.value.x
        val threshold = widthPx * 0.28f
        when {
            x > threshold || (velocityX > FLING_VELOCITY && x > 24f) -> fling(Decision.KEEP)
            x < -threshold || (velocityX < -FLING_VELOCITY && x < -24f) -> fling(Decision.DELETE)
            else -> scope.launch {
                offset.animateTo(Offset.Zero, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
            }
        }
    }

    /** Fa uscire la carta dallo schermo e poi comunica la decisione. Usata anche dai pulsanti. */
    fun fling(decision: Decision) {
        if (finished) return
        finished = true
        scope.launch {
            val target = if (decision == Decision.KEEP) widthPx * 1.6f else -widthPx * 1.6f
            offset.animateTo(Offset(target, offset.value.y), tween(durationMillis = 240, easing = FastOutLinearInEasing))
            onDecision(decision)
        }
    }

    private companion object {
        const val FLING_VELOCITY = 1800f
    }
}

@Composable
fun SwipeDeck(
    items: List<MediaEntry>,
    index: Int,
    canUndo: Boolean,
    onDecision: (Decision) -> Unit,
    onUndo: () -> Unit,
    onCrop: () -> Unit,
    onOpenPhoto: (MediaEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = items[index]
    val next = items.getOrNull(index + 1)
    var muted by rememberSaveable { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val latestOnDecision by rememberUpdatedState(onDecision)
    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val swipe = remember(top.key) { CardSwipeState(scope, widthPx) { latestOnDecision(it) } }

    Column(modifier) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (next != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = abs(swipe.progress)
                            val s = 0.92f + 0.08f * p
                            scaleX = s
                            scaleY = s
                            translationY = (1f - p) * 22.dp.toPx()
                        },
                ) {
                    MediaCard(next, active = false, muted = muted, onToggleMute = {})
                }
            }

            key(top.key) {
                Box(
                    Modifier
                        .fillMaxSize()
                        // il rilevatore sta fuori dal graphicsLayer: cosi' le coordinate non si muovono con la carta
                        .pointerInput(top.key) {
                            val tracker = VelocityTracker()
                            detectDragGestures(
                                onDragStart = { tracker.resetTracking() },
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
                            translationY = o.y * 0.6f
                            rotationZ = o.x / widthPx * 16f
                        },
                ) {
                    MediaCard(
                        top,
                        active = true,
                        muted = muted,
                        onToggleMute = { muted = !muted },
                        onOpenPhoto = { onOpenPhoto(top) },
                    )

                    // velo colorato che si intensifica trascinando
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(28.dp))
                            .drawBehind {
                                val p = swipe.progress
                                drawRect(if (p >= 0f) Palette.Keep else Palette.Delete, alpha = abs(p) * 0.3f)
                            },
                    )
                    SwipeStamp(
                        text = "TIENI",
                        color = Palette.Keep,
                        rotation = -14f,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(28.dp)
                            .graphicsLayer { alpha = max(swipe.progress, 0f) },
                    )
                    SwipeStamp(
                        text = "ELIMINA",
                        color = Palette.Delete,
                        rotation = 14f,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(28.dp)
                            .graphicsLayer { alpha = max(-swipe.progress, 0f) },
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(Icons.AutoMirrored.Filled.Undo, "Annulla l'ultimo swipe", Palette.Amber, 48.dp, enabled = canUndo, onClick = onUndo)
            ActionButton(Icons.Filled.Close, "Elimina", Palette.Delete, 66.dp) { swipe.fling(Decision.DELETE) }
            ActionButton(Icons.Filled.Check, "Tieni", Palette.Keep, 66.dp) { swipe.fling(Decision.KEEP) }
            // ritaglia e tieni solo la versione ritagliata (solo foto)
            ActionButton(Icons.Filled.Crop, "Ritaglia e tieni", Palette.Sky, 48.dp, enabled = top.type == MediaType.PHOTO, onClick = onCrop)
        }
    }
}

@Composable
private fun SwipeStamp(text: String, color: Color, rotation: Float, modifier: Modifier = Modifier) {
    Text(
        text,
        color = color,
        fontSize = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.sp,
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
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
    onClick: () -> Unit,
) {
    val color = if (enabled) tint else tint.copy(alpha = 0.3f)
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Palette.Surface)
            .border(2.dp, color, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = color, modifier = Modifier.size(size * 0.46f))
    }
}
