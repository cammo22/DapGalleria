package com.dapprod.dapgalleria.ui.components

import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapprod.dapgalleria.game.Level
import com.dapprod.dapgalleria.game.Outcome
import com.dapprod.dapgalleria.game.Quests
import com.dapprod.dapgalleria.ui.Decision
import com.dapprod.dapgalleria.ui.GameEvent
import com.dapprod.dapgalleria.ui.theme.ArcadeNumber
import com.dapprod.dapgalleria.ui.theme.BrandGradient
import com.dapprod.dapgalleria.ui.theme.FireGradient
import com.dapprod.dapgalleria.ui.theme.GoldGradient
import com.dapprod.dapgalleria.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

private class FloatItem(val id: Long, val text: String, val color: Color, val x: Float, val y: Float, val big: Boolean)
private class ToastItem(val id: Long, val title: String, val subtitle: String, val reward: String?, val gold: Boolean)

/** Quello che sta sopra a tutto: punti che volano, avvisi in alto, festa delle eliminazioni, livello nuovo. */
@Stable
class HudState {
    internal val floats = mutableStateListOf<FloatItem>()
    internal val toasts = mutableStateListOf<ToastItem>()
    var celebration by mutableStateOf<GameEvent.Deleted?>(null)
    var levelUp by mutableStateOf<Level?>(null)
    private var nextId = 0L
    internal var lastMultiplier = 1

    internal fun float(text: String, color: Color, x: Float, y: Float, big: Boolean = false) {
        floats += FloatItem(nextId++, text, color, x, y, big)
    }

    fun toast(title: String, subtitle: String, reward: String? = null, gold: Boolean = false) {
        toasts += ToastItem(nextId++, title, subtitle, reward, gold)
    }
}

@Composable
fun rememberHudState(): HudState = remember { HudState() }

/** Ascolta il gioco e festeggia: suoni, vibrazioni, coriandoli, punti, avvisi. */
@Composable
fun GameHud(events: Flow<GameEvent>, hud: HudState, confetti: ConfettiState, modifier: Modifier = Modifier) {
    val feedback = LocalFeedback.current

    fun rewards(o: Outcome) {
        o.quests.forEach { q ->
            hud.toast("🎯 Missione compiuta", q.title, "+${q.reward}")
            feedback?.reward()
        }
        if (o.chest) {
            hud.toast("🎁 Forziere del giorno!", "Tutte le missioni di oggi fatte", "+${Quests.CHEST_REWARD}", gold = true)
            confetti.rain(140)
        }
        o.achievements.forEach { a ->
            hud.toast("${a.emoji} Traguardo sbloccato", a.title, "+${formatThousands(a.reward)}", gold = true)
            confetti.burst(0.5f, 0.12f, count = 50, power = 0.7f, direction = 90f, spread = 140f)
            feedback?.reward()
        }
        o.levelUp?.let {
            hud.levelUp = it
            feedback?.levelUp()
            confetti.rain(220)
        }
    }

    LaunchedEffect(events) {
        events.collect { e ->
            when (e) {
                is GameEvent.Swiped -> {
                    val o = e.outcome
                    val keep = e.decision == Decision.KEEP
                    if (keep) feedback?.keep(o.combo) else feedback?.delete(o.combo)
                    val x = if (keep) 0.72f else 0.28f
                    val label = buildString {
                        append("+").append(o.points)
                        if (o.multiplier > 1) append("  ×").append(o.multiplier)
                    }
                    hud.float(label, if (o.golden) Palette.Gold else if (keep) Palette.Keep else Palette.Delete, x, 0.5f, big = o.golden)
                    confetti.burst(
                        if (keep) 0.98f else 0.02f, 0.5f, count = 16,
                        colors = if (keep) listOf(Palette.Keep, Palette.Cyan) else listOf(Palette.Delete, Palette.Pink),
                        power = 0.45f, direction = if (keep) 200f else -20f, spread = 80f,
                    )
                    if (o.golden) {
                        feedback?.golden()
                        confetti.burst(0.5f, 0.42f, count = 90, colors = listOf(Palette.Gold, Palette.GoldDeep, Color.White), power = 0.9f)
                    }
                    if (o.multiplier > hud.lastMultiplier && o.multiplier > 1) {
                        feedback?.comboUp(o.multiplier)
                        hud.float("COMBO ×${o.multiplier}!", Palette.Fire, 0.5f, 0.3f, big = true)
                    }
                    hud.lastMultiplier = o.multiplier
                    rewards(o)
                }
                is GameEvent.Edited -> {
                    feedback?.reward()
                    hud.float("+${e.outcome.points} 🎨", Palette.Cyan, 0.5f, 0.45f, big = true)
                    confetti.burst(0.5f, 0.45f, count = 50, colors = listOf(Palette.Cyan, Palette.Magenta, Color.White), power = 0.7f)
                    hud.lastMultiplier = e.outcome.multiplier
                    rewards(e.outcome)
                }
                is GameEvent.Deleted -> {
                    hud.celebration = e
                    feedback?.boom()
                    confetti.burst(0.5f, 0.4f, count = 160, power = 1.2f)
                    rewards(e.outcome)
                }
                is GameEvent.RoundDone -> {
                    feedback?.levelUp()
                    confetti.rain(240)
                    hud.lastMultiplier = 1
                    rewards(e.outcome)
                }
                is GameEvent.Message -> hud.toast(e.text, "")
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        // punti che salgono
        BoxWithConstraints(Modifier.fillMaxSize()) {
            hud.floats.toList().forEach { item ->
                key(item.id) {
                    FloatingLabel(item, maxWidth.value, maxHeight.value) { hud.floats.remove(item) }
                }
            }
        }

        // avvisi in alto, uno alla volta
        val current = hud.toasts.firstOrNull()
        LaunchedEffect(current?.id) {
            if (current != null) {
                delay(2_300)
                hud.toasts.remove(current)
            }
        }
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (slideInVertically(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) { -it * 2 } + fadeIn()) togetherWith
                    (slideOutVertically(tween(220)) { -it * 2 } + fadeOut(tween(220)))
            },
            contentKey = { it?.id },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
            label = "toast",
        ) { t ->
            if (t != null) ToastCard(t)
        }

        hud.levelUp?.let { level -> LevelUpOverlay(level) { hud.levelUp = null } }
        hud.celebration?.let { c -> DeleteCelebration(c) { hud.celebration = null } }
    }
}

@Composable
private fun FloatingLabel(item: FloatItem, widthDp: Float, heightDp: Float, onDone: () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        p.animateTo(1f, tween(if (item.big) 1300 else 900, easing = LinearEasing))
        onDone()
    }
    val v = p.value
    val pop = if (v < 0.15f) 0.6f + v / 0.15f * 0.6f else 1.2f - 0.2f * ((v - 0.15f) / 0.85f)
    Text(
        item.text,
        color = item.color,
        fontWeight = FontWeight.Black,
        fontSize = if (item.big) 34.sp else 26.sp,
        modifier = Modifier
            .offset(x = (widthDp * item.x - 60f).dp, y = (heightDp * item.y - 120f * v).dp)
            .width(120.dp)
            .graphicsLayer {
                alpha = 1f - v * v
                scaleX = pop
                scaleY = pop
                shadowElevation = 0f
            },
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall.copy(
            shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 3f), 8f),
        ),
    )
}

@Composable
private fun ToastCard(t: ToastItem) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Surface.copy(alpha = 0.96f))
            .border(1.5.dp, if (t.gold) GoldGradient else BrandGradient, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(t.title, fontWeight = FontWeight.ExtraBold, color = if (t.gold) Palette.Gold else Palette.TextPrimary)
            if (t.subtitle.isNotEmpty()) Text(t.subtitle, color = Palette.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        if (t.reward != null) {
            Text(
                t.reward,
                fontWeight = FontWeight.Black,
                color = Color(0xFF2A1A00),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GoldGradient)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

/** La combo in corso: fiammella, moltiplicatore e la barretta del tempo che resta. */
@Composable
fun ComboMeter(combo: Int, multiplier: Int, expiresAt: Long, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val alive = combo >= 2 && now < expiresAt
    LaunchedEffect(expiresAt, combo) {
        now = System.currentTimeMillis()
        while (now < expiresAt) {
            delay(50)
            now = System.currentTimeMillis()
        }
    }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(multiplier) {
        if (multiplier > 1) {
            pop.snapTo(1.5f)
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
    }
    val bump = remember { Animatable(1f) }
    LaunchedEffect(combo) {
        if (combo >= 2) {
            bump.snapTo(1.15f)
            bump.animateTo(1f, spring(dampingRatio = 0.5f))
        }
    }
    AnimatedVisibility(
        visible = alive,
        enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
        exit = fadeOut(tween(300)),
        modifier = modifier,
    ) {
        val left = ((expiresAt - now).toFloat() / com.dapprod.dapgalleria.game.GameEngine.COMBO_WINDOW_MS).coerceIn(0f, 1f)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val s = pop.value * bump.value
                scaleX = s
                scaleY = s
            },
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (multiplier > 1) FireGradient else BrandGradient)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔥 $combo", color = Color.White, fontWeight = FontWeight.Black)
                if (multiplier > 1) {
                    Spacer(Modifier.width(8.dp))
                    Text("×$multiplier", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .width(90.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.15f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(left)
                        .height(4.dp)
                        .background(if (left < 0.3f) Palette.Delete else Palette.Gold),
                )
            }
        }
    }
}

@Composable
private fun LevelUpOverlay(level: Level, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    LaunchedEffect(level) {
        delay(3_500)
        onDismiss()
    }
    val scale = remember { Animatable(0.2f) }
    LaunchedEffect(level) { scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow)) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
            Text("LIVELLO SUPERIORE", style = MaterialTheme.typography.titleMedium.copy(brush = GoldGradient), fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Spacer(Modifier.height(12.dp))
            ProgressRing(1f, 150.dp, stroke = 8.dp) { Text(level.emoji, fontSize = 72.sp) }
            Spacer(Modifier.height(16.dp))
            Text(level.title, style = MaterialTheme.typography.headlineLarge.copy(brush = BrandGradient), textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("da ${formatThousands(level.minScore)} punti", color = Palette.TextSecondary)
        }
    }
}

/** Spazio libero e totale del telefono. */
fun storageStats(): Pair<Long, Long> = try {
    val st = StatFs(Environment.getDataDirectory().path)
    st.availableBytes to st.totalBytes
} catch (_: Exception) {
    0L to 0L
}

/** La festa dopo un'eliminazione: i MB liberati che contano in su, i punti, il bonus dei giorni di fila. */
@Composable
private fun DeleteCelebration(e: GameEvent.Deleted, onDismiss: () -> Unit) {
    val context = LocalContext.current
    BackHandler(onBack = onDismiss)
    val appear = remember { Animatable(0f) }
    val count = remember { Animatable(0f) }
    LaunchedEffect(e) {
        appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(e) {
        delay(250)
        count.animateTo(1f, tween(1_600, easing = FastOutSlowInEasing))
    }
    val (free, total) = remember(e) { storageStats() }
    val bytes = (e.freedBytes * count.value).toLong()
    val points = (e.outcome.points * count.value).toLong()
    val photos = e.freedBytes / (3 * 1024 * 1024)
    val songs = e.freedBytes / (4 * 1024 * 1024)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f * appear.value.coerceIn(0f, 1f)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(28.dp)
                .graphicsLayer {
                    val s = 0.5f + 0.5f * appear.value
                    scaleX = s
                    scaleY = s
                    alpha = appear.value.coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(32.dp))
                .background(Palette.Surface)
                .border(2.dp, BrandGradient, RoundedCornerShape(32.dp))
                .padding(28.dp),
        ) {
            Text("💥", fontSize = 64.sp)
            Text("SPAZIO LIBERATO", style = MaterialTheme.typography.titleSmall, color = Palette.TextSecondary, letterSpacing = 3.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(formatSize(context, bytes), style = ArcadeNumber.copy(brush = BrandGradient, fontSize = 52.sp))
            Text("${e.count} ${if (e.count == 1) "contenuto eliminato" else "contenuti eliminati"}", color = Palette.TextSecondary)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("+${formatThousands(points)}", style = ArcadeNumber.copy(fontSize = 34.sp), color = Palette.Gold)
                Spacer(Modifier.width(8.dp))
                Text("punti", color = Palette.Gold, fontWeight = FontWeight.Bold)
            }
            if (e.streakMultiplier > 1f) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "🔥 bonus giorni di fila ×${"%.1f".format(e.streakMultiplier)}",
                    color = Palette.Fire,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (photos > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "È lo spazio di ~${formatThousands(photos)} foto nuove o ~${formatThousands(songs)} canzoni",
                    color = Palette.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
            if (total > 0) {
                Spacer(Modifier.height(18.dp))
                StorageBar(free, total, e.freedBytes, Modifier.width(240.dp))
            }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Magenta, contentColor = Color.White),
                shape = RoundedCornerShape(16.dp),
            ) { Text("Continua a pulire", fontWeight = FontWeight.ExtraBold) }
        }
    }
}

/** Barra della memoria: la parte appena liberata si accende in verde. */
@Composable
fun StorageBar(free: Long, total: Long, justFreed: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val usedFraction = ((total - free).toFloat() / total).coerceIn(0f, 1f)
    val freedFraction = (justFreed.toFloat() / total).coerceIn(0f, usedFraction)
    val anim by animateFloatAsState(freedFraction, tween(1_400, delayMillis = 400, easing = FastOutSlowInEasing), label = "freed")
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Palette.SurfaceHigh)
                .drawBehind {
                    val w = size.width
                    val used = w * usedFraction
                    drawRoundRect(BrandGradient, size = Size(used, size.height), cornerRadius = CornerRadius(size.height / 2))
                    val fw = w * anim
                    drawRect(Palette.Keep, topLeft = Offset(used - fw, 0f), size = Size(fw, size.height))
                },
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Liberi ${formatSize(context, free)}", color = Palette.Keep, style = MaterialTheme.typography.labelSmall)
            Text("su ${formatSize(context, total)}", color = Palette.TextDim, style = MaterialTheme.typography.labelSmall)
        }
    }
}
