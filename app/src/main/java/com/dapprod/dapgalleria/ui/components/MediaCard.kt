package com.dapprod.dapgalleria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * un tocco mette in pausa, il pulsante in basso a destra attiva/disattiva l'audio.
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
    val progress = remember(entry.key) { mutableFloatStateOf(0f) }
    val isVideo = entry.type == MediaType.VIDEO

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

        if (isVideo && active) {
            VideoSurface(
                uri = entry.uri,
                playing = playing,
                muted = muted,
                modifier = Modifier.fillMaxSize(),
                onProgress = { progress.floatValue = it },
            )
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
            LinearProgressIndicator(
                progress = { progress.floatValue },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
            )
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

        // informazioni sul contenuto
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                .padding(start = 20.dp, end = if (isVideo && active) 76.dp else 20.dp, top = 40.dp, bottom = 18.dp),
        ) {
            Text(
                entry.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(entry.describe(context), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
        }

        if (isVideo && active) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = onToggleMute),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (muted) "Attiva audio" else "Disattiva audio",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
