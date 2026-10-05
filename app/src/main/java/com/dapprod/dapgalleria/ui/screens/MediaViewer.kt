package com.dapprod.dapgalleria.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.MediaType
import com.dapprod.dapgalleria.ui.components.VideoSurface
import com.dapprod.dapgalleria.ui.components.describe
import com.dapprod.dapgalleria.ui.components.rememberVideoPlayer

private const val MAX_ZOOM = 6f

/**
 * Visualizzatore a schermo intero. Le foto si ingrandiscono con il pizzico (o doppio tocco) e si
 * spostano trascinando; i video partono con audio e controlli. Con [actionLabel] compare un pulsante
 * in basso (es. "Togli dalla lista").
 */
@Composable
fun MediaViewer(
    entry: MediaEntry,
    onDismiss: () -> Unit,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val isPhoto = entry.type == MediaType.PHOTO

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {}, // blocca i tocchi verso la schermata sotto
    ) {
        if (isPhoto) {
            ZoomableImage(entry)
        } else {
            val video = rememberVideoPlayer(entry.uri, entry.durationMs, playing = true, muted = false)
            VideoSurface(video, Modifier.fillMaxSize(), showController = true)
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Chiudi", tint = Color.White)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (isPhoto) 16.dp else 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(entry.describe(context), color = Color.White.copy(alpha = 0.8f))
            if (actionLabel != null) {
                Button(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(actionLabel) }
            }
        }
    }
}

/** Foto con zoom a pizzico, doppio tocco e trascinamento. */
@Composable
private fun ZoomableImage(entry: MediaEntry) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    // risoluzione alta (ma limitata) per restare nitida quando si ingrandisce
    val request = remember(entry.key) {
        ImageRequest.Builder(context).data(entry.uri).size(3072).build()
    }

    // nuova scala mantenendo fermo il punto [focus] sotto le dita; l'offset resta dentro i bordi
    fun zoomTo(newScale: Float, focus: Offset, pan: Offset = Offset.Zero) {
        val s = newScale.coerceIn(1f, MAX_ZOOM)
        if (s <= 1f) {
            scale = 1f
            offset = Offset.Zero
            return
        }
        val center = Offset(box.width / 2f, box.height / 2f)
        val moved = offset + (focus - center - offset) * (1f - s / scale) + pan
        val maxX = box.width * (s - 1f) / 2f
        val maxY = box.height * (s - 1f) / 2f
        scale = s
        offset = Offset(moved.x.coerceIn(-maxX, maxX), moved.y.coerceIn(-maxY, maxY))
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { box = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap -> if (scale > 1.05f) zoomTo(1f, tap) else zoomTo(2.5f, tap) },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    zoomTo(scale * zoom, centroid, pan)
                }
            },
    ) {
        AsyncImage(
            model = request,
            contentDescription = entry.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}
