package com.dapprod.dapgalleria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.MediaType
import com.dapprod.dapgalleria.ui.theme.Palette

/** Richiesta Coil per un'immagine o per un fotogramma di video. */
@Composable
fun rememberMediaRequest(entry: MediaEntry): ImageRequest {
    val context = LocalContext.current
    return remember(entry.key) {
        ImageRequest.Builder(context)
            .data(entry.uri)
            .apply { if (entry.type == MediaType.VIDEO) videoFrameMillis(700) }
            .build()
    }
}

/**
 * Carta di un contenuto. Quando [active] è true (carta in cima al mazzo) i video partono da soli:
 * un tocco sul video mette in pausa; in basso ci sono durata, barra per spostarsi, ±10 secondi e audio.
 */
@Composable
fun MediaCard(
    entry: MediaEntry,
    active: Boolean,
    muted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var playing by remember(entry.key) { mutableStateOf(true) }
    val isVideo = entry.type == MediaType.VIDEO
    val video = if (isVideo && active) {
        rememberVideoPlayer(entry.uri, entry.durationMs, playing, muted)
    } else {
        null
    }

    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(Palette.CardBackground),
    ) {
        AsyncImage(
            model = rememberMediaRequest(entry),
            contentDescription = entry.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        if (video != null) {
            VideoSurface(video, Modifier.fillMaxSize())
            // livello trasparente che cattura il tocco (play/pausa) senza dare il tocco al PlayerView
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { playing = !playing },
            )
            if (!playing) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Riproduci", tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }
        } else if (isVideo) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp),
            )
        }

        // informazioni sul contenuto (+ controlli del video)
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))))
                .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 12.dp),
        ) {
            Text(
                entry.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(entry.describe(context), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
            if (video != null) {
                VideoControls(
                    video = video,
                    playing = playing,
                    muted = muted,
                    onTogglePlay = { playing = !playing },
                    onToggleMute = onToggleMute,
                )
            } else {
                Box(Modifier.height(6.dp))
            }
        }
    }
}

/** Tempo corrente / durata, barra per spostarsi nel video, ±10 secondi, pausa e audio. */
@Composable
private fun VideoControls(
    video: VideoPlayerState,
    playing: Boolean,
    muted: Boolean,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
) {
    // mentre si trascina la barra si mostra la posizione scelta, non quella del player
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = video.durationMs.coerceAtLeast(1L)
    val fraction = dragging ?: (video.positionMs.toFloat() / duration).coerceIn(0f, 1f)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatDuration((fraction * duration).toLong()),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
        )
        Slider(
            value = fraction,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { video.seekTo((it * duration).toLong()) }
                dragging = null
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Palette.Pink,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )
        Text(
            formatDuration(video.durationMs),
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelMedium,
        )
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardIconButton(Icons.Filled.Replay10, "Indietro di 10 secondi") { video.seekBy(-10_000) }
        CardIconButton(
            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (playing) "Pausa" else "Riproduci",
            size = 52,
            onClick = onTogglePlay,
        )
        CardIconButton(Icons.Filled.Forward10, "Avanti di 10 secondi") { video.seekBy(10_000) }
        CardIconButton(
            if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            if (muted) "Attiva audio" else "Disattiva audio",
            onClick = onToggleMute,
        )
    }
}

@Composable
private fun CardIconButton(icon: ImageVector, description: String, size: Int = 44, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size((size * 0.58f).dp))
    }
}
