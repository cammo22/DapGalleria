package com.dapprod.dapgalleria.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.MediaType
import com.dapprod.dapgalleria.game.Quests
import com.dapprod.dapgalleria.ui.Decision
import com.dapprod.dapgalleria.ui.GalleryUiState
import com.dapprod.dapgalleria.ui.components.AnimatedNumber
import com.dapprod.dapgalleria.ui.components.ComboMeter
import com.dapprod.dapgalleria.ui.components.ProgressRing
import com.dapprod.dapgalleria.ui.components.SwipeDeck
import com.dapprod.dapgalleria.ui.components.formatThousands
import com.dapprod.dapgalleria.ui.components.pressBounce
import com.dapprod.dapgalleria.ui.theme.BrandGradient
import com.dapprod.dapgalleria.ui.theme.FireGradient
import com.dapprod.dapgalleria.ui.theme.Palette

class SwipeActions(
    val onMode: (MediaType) -> Unit,
    val onDecision: (Decision) -> Unit,
    val onUndo: () -> Unit,
    val onEdit: () -> Unit,
    val onOpenPhoto: (MediaEntry) -> Unit,
    val onOpenTrophies: () -> Unit,
    val onOpenReview: () -> Unit,
    val onRestartRound: () -> Unit,
    val onSound: (Boolean) -> Unit,
    val onHaptics: (Boolean) -> Unit,
)

@Composable
fun SwipeScreen(state: GalleryUiState, actions: SwipeActions) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TopBar(state, actions)
        RoundBar(state, onClick = actions.onOpenTrophies)
        ModeToggle(state, actions.onMode)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val current = state.current
            AnimatedContent(
                targetState = when {
                    state.loading -> DeckPhase.LOADING
                    current == null -> DeckPhase.DONE
                    else -> DeckPhase.DECK
                },
                transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(200)) },
                label = "deck",
            ) { phase ->
                when (phase) {
                    DeckPhase.LOADING -> LoadingDeck()
                    DeckPhase.DONE -> FinishedState(state, actions)
                    DeckPhase.DECK -> if (state.current != null) {
                        SwipeDeck(
                            items = state.deck,
                            index = state.index,
                            canUndo = state.canUndo,
                            lastUndone = state.lastUndone,
                            isGolden = state::isGolden,
                            onDecision = actions.onDecision,
                            onUndo = actions.onUndo,
                            onEdit = actions.onEdit,
                            onOpenPhoto = actions.onOpenPhoto,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            ComboMeter(
                combo = state.combo,
                multiplier = state.multiplier,
                expiresAt = state.comboExpiresAt,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp),
            )
        }
    }
}

private enum class DeckPhase { LOADING, DONE, DECK }

@Composable
private fun TopBar(state: GalleryUiState, actions: SwipeActions) {
    var menuOpen by remember { mutableStateOf(false) }
    val profile = state.profile
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ringInteraction = remember { MutableInteractionSource() }
        ProgressRing(
            progress = profile.levelProgress,
            size = 46.dp,
            modifier = Modifier
                .pressBounce(ringInteraction)
                .clickable(interactionSource = ringInteraction, indication = null, onClick = actions.onOpenTrophies),
        ) { Text(profile.level.emoji, fontSize = 20.sp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "DapGalleria",
                style = MaterialTheme.typography.titleLarge.copy(brush = BrandGradient, fontWeight = FontWeight.Black),
            )
            Text(profile.level.title, style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
        }
        if (profile.streakDays > 0) StreakChip(profile.streakDays, onClick = actions.onOpenTrophies)
        Spacer(Modifier.width(6.dp))
        ScorePill(profile.score, onClick = actions.onOpenTrophies)
        TrashButton(state.pending.size, onClick = actions.onOpenReview)
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Altre opzioni", tint = Palette.TextPrimary)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (state.soundOn) "🔊  Suoni accesi" else "🔇  Suoni spenti") },
                    onClick = { actions.onSound(!state.soundOn) },
                )
                DropdownMenuItem(
                    text = { Text(if (state.hapticsOn) "📳  Vibrazione accesa" else "📴  Vibrazione spenta") },
                    onClick = { actions.onHaptics(!state.hapticsOn) },
                )
                DropdownMenuItem(
                    text = { Text("🔁  Ricomincia il giro adesso") },
                    onClick = { menuOpen = false; actions.onRestartRound() },
                )
            }
        }
    }
}

@Composable
private fun StreakChip(days: Int, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "flame")
    val flicker by t.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "flicker")
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Palette.Surface)
            .border(1.dp, FireGradient, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔥", fontSize = 14.sp, modifier = Modifier.graphicsLayer { scaleX = flicker; scaleY = flicker })
        Spacer(Modifier.width(2.dp))
        Text("$days", color = Palette.Fire, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ScorePill(score: Long, onClick: () -> Unit) {
    // quando i punti salgono la pillola fa un saltello
    val bump = remember { Animatable(1f) }
    var last by remember { mutableStateOf(score) }
    LaunchedEffect(score) {
        if (score > last) {
            bump.snapTo(1.18f)
            bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
        }
        last = score
    }
    Row(
        Modifier
            .graphicsLayer { scaleX = bump.value; scaleY = bump.value }
            .clip(RoundedCornerShape(50))
            .background(Palette.Surface)
            .border(1.dp, Palette.Gold.copy(alpha = 0.5f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🏆", fontSize = 13.sp)
        Spacer(Modifier.size(4.dp))
        AnimatedNumber(score, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black), color = Palette.Gold)
    }
}

@Composable
private fun TrashButton(count: Int, onClick: () -> Unit) {
    val shake = remember { Animatable(0f) }
    var last by remember { mutableStateOf(count) }
    LaunchedEffect(count) {
        if (count > last) {
            shake.snapTo(1f)
            shake.animateTo(0f, spring(dampingRatio = 0.25f, stiffness = Spring.StiffnessMediumLow))
        }
        last = count
    }
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge(containerColor = Palette.Delete, contentColor = Color.White) { Text(formatThousands(count.toLong())) }
                }
            },
            modifier = Modifier.graphicsLayer {
                rotationZ = shake.value * 18f
                val s = 1f + shake.value * 0.25f
                scaleX = s
                scaleY = s
            },
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Contenuti da eliminare", tint = Palette.TextPrimary)
        }
    }
}

/** Il giro in corso: quanto manca a vedere tutta la galleria, e le missioni di oggi. */
@Composable
private fun RoundBar(state: GalleryUiState, onClick: () -> Unit) {
    val progress by animateFloatAsState(state.roundProgress, tween(700, easing = FastOutSlowInEasing), label = "round")
    val quests = remember(state.profile.day) { Quests.forDay(state.profile.day) }
    val questsDone = quests.count { it.id in state.profile.questsClaimed }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("GIRO ${state.profile.round}", color = Palette.TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.5.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                if (state.totalItems > 0) "${(progress * 100).toInt()}% · ${formatThousands((state.totalItems - state.reviewedItems).toLong())} da vedere" else "",
                color = Palette.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (questsDone == quests.size) "🎁 fatte" else "🎯 $questsDone/${quests.size}",
                color = if (questsDone == quests.size) Palette.Gold else Palette.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(5.dp))
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Palette.SurfaceHigh),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(50))
                    .background(BrandGradient),
            )
            // scintilla che corre sulla punta della barra
            if (progress > 0.01f) {
                val t = rememberInfiniteTransition(label = "spark")
                val glow by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "glow")
                Box(
                    Modifier
                        .offset(x = maxWidth * progress.coerceIn(0f, 1f) - 6.dp)
                        .size(6.dp)
                        .graphicsLayer { alpha = glow }
                        .clip(RoundedCornerShape(50))
                        .background(Color.White),
                )
            }
        }
    }
}

@Composable
private fun ModeToggle(state: GalleryUiState, onMode: (MediaType) -> Unit) {
    val mode = state.mode
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(Palette.Surface)
            .border(1.dp, Palette.Outline, RoundedCornerShape(50))
            .padding(4.dp),
    ) {
        val half = maxWidth / 2
        // la pillola colorata scivola sotto la scelta
        val x by animateDpAsState(
            if (mode == MediaType.PHOTO) 0.dp else half,
            spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
            label = "pill",
        )
        Box(
            Modifier
                .offset(x = x)
                .width(half)
                .height(42.dp)
                .clip(RoundedCornerShape(50))
                .background(BrandGradient),
        )
        Row(Modifier.fillMaxWidth()) {
            ModeTab("Foto", state.photosLeft, Icons.Filled.PhotoLibrary, mode == MediaType.PHOTO, { onMode(MediaType.PHOTO) }, Modifier.weight(1f))
            ModeTab("Video", state.videosLeft, Icons.Filled.VideoLibrary, mode == MediaType.VIDEO, { onMode(MediaType.VIDEO) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModeTab(label: String, left: Int, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint by androidx.compose.animation.animateColorAsState(if (selected) Color.White else Palette.TextSecondary, label = "tint")
    Row(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.size(8.dp))
        Text(label, color = tint, fontWeight = FontWeight.Bold)
        if (left > 0) {
            Spacer(Modifier.size(6.dp))
            Text(
                formatThousands(left.toLong()),
                color = tint.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun LoadingDeck() {
    val t = rememberInfiniteTransition(label = "loading")
    val pulse by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse")
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 90.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Palette.Surface.copy(alpha = pulse)),
        contentAlignment = Alignment.Center,
    ) {
        Text("Mescolo il mazzo…", color = Palette.TextSecondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FinishedState(state: GalleryUiState, actions: SwipeActions) {
    val otherMode = if (state.mode == MediaType.PHOTO) MediaType.VIDEO else MediaType.PHOTO
    val otherLeft = if (otherMode == MediaType.PHOTO) state.photosLeft else state.videosLeft
    val what = if (state.mode == MediaType.PHOTO) "Foto" else "Video"
    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", fontSize = 72.sp, modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value })
        Spacer(Modifier.height(12.dp))
        Text("$what finiti!", style = MaterialTheme.typography.headlineMedium.copy(brush = BrandGradient))
        Spacer(Modifier.height(8.dp))
        Text(
            if (otherLeft > 0) {
                "Per chiudere il giro ${state.profile.round} ti mancano ${formatThousands(otherLeft.toLong())} ${if (otherMode == MediaType.PHOTO) "foto" else "video"}."
            } else {
                "Hai visto tutta la galleria."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Palette.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (otherLeft > 0) {
            Button(
                onClick = { actions.onMode(otherMode) },
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Magenta, contentColor = Color.White),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(if (otherMode == MediaType.PHOTO) "Passa alle foto →" else "Passa ai video →", fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(10.dp))
        }
        if (state.pending.isNotEmpty()) {
            OutlinedButton(onClick = actions.onOpenReview, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Controlla ed elimina (${state.pending.size})")
            }
        }
    }
}
