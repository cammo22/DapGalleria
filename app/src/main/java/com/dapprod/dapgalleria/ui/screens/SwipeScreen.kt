package com.dapprod.dapgalleria.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dapprod.dapgalleria.data.MediaType
import com.dapprod.dapgalleria.ui.Decision
import com.dapprod.dapgalleria.ui.GalleryUiState
import com.dapprod.dapgalleria.ui.components.SwipeDeck
import com.dapprod.dapgalleria.ui.theme.BrandGradient
import com.dapprod.dapgalleria.ui.theme.Palette

class SwipeActions(
    val onMode: (MediaType) -> Unit,
    val onDecision: (Decision) -> Unit,
    val onUndo: () -> Unit,
    val onCrop: () -> Unit,
    val onOpenStats: () -> Unit,
    val onOpenReview: () -> Unit,
    val onShowReviewed: (Boolean) -> Unit,
    val onResetKept: () -> Unit,
)

@Composable
fun SwipeScreen(state: GalleryUiState, actions: SwipeActions) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TopBar(state, actions)
        ModeToggle(state.mode, actions.onMode)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val current = state.current
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Palette.Pink)
                current == null -> FinishedState(state, actions)
                else -> SwipeDeck(
                    items = state.deck,
                    index = state.index,
                    canUndo = state.canUndo,
                    onDecision = actions.onDecision,
                    onUndo = actions.onUndo,
                    onCrop = actions.onCrop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun TopBar(state: GalleryUiState, actions: SwipeActions) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "DapGalleria",
                style = MaterialTheme.typography.headlineSmall.copy(brush = BrandGradient, fontWeight = FontWeight.ExtraBold),
            )
            if (!state.loading && state.deck.isNotEmpty()) {
                Text(
                    "${state.remaining} da valutare",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary,
                )
            }
        }
        ScorePill(state.stats.score, onClick = actions.onOpenStats)
        IconButton(onClick = actions.onOpenReview) {
            BadgedBox(
                badge = {
                    if (state.pending.isNotEmpty()) {
                        Badge(containerColor = Palette.Delete, contentColor = Color.White) { Text(state.pending.size.toString()) }
                    }
                },
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Contenuti da eliminare", tint = Palette.TextPrimary)
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Altre opzioni", tint = Palette.TextPrimary)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (state.showReviewed) "✓  Mostra anche quelli già tenuti" else "Mostra anche quelli già tenuti") },
                    onClick = { menuOpen = false; actions.onShowReviewed(!state.showReviewed) },
                )
                DropdownMenuItem(
                    text = { Text("Dimentica i contenuti tenuti (${state.keptCount})") },
                    enabled = state.keptCount > 0,
                    onClick = { menuOpen = false; actions.onResetKept() },
                )
            }
        }
    }
}

@Composable
private fun ScorePill(score: Long, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Palette.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = "Punteggio", tint = Palette.Amber, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(4.dp))
        Text(score.toString(), color = Palette.TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ModeToggle(mode: MediaType, onMode: (MediaType) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(Palette.Surface)
            .padding(4.dp),
    ) {
        ModeTab("Foto", Icons.Filled.PhotoLibrary, mode == MediaType.PHOTO, { onMode(MediaType.PHOTO) }, Modifier.weight(1f))
        ModeTab("Video", Icons.Filled.VideoLibrary, mode == MediaType.VIDEO, { onMode(MediaType.VIDEO) }, Modifier.weight(1f))
    }
}

@Composable
private fun ModeTab(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .then(if (selected) Modifier.background(BrandGradient, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (selected) Color.White else Palette.TextSecondary
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(label, color = tint, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FinishedState(state: GalleryUiState, actions: SwipeActions) {
    val what = if (state.mode == MediaType.PHOTO) "foto" else "video"
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Palette.Keep, modifier = Modifier.size(84.dp))
        Spacer(Modifier.height(16.dp))
        Text("Tutto fatto!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Non ci sono altri $what da valutare.",
            style = MaterialTheme.typography.bodyLarge,
            color = Palette.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        if (state.pending.isNotEmpty()) {
            Button(onClick = actions.onOpenReview) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Controlla ed elimina (${state.pending.size})")
            }
            Spacer(Modifier.height(8.dp))
        }
        if (!state.showReviewed) {
            OutlinedButton(onClick = { actions.onShowReviewed(true) }) { Text("Rivedi anche quelli già tenuti") }
        }
    }
}
