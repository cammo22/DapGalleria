package com.dapprod.dapgalleria.ui.components

import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Riproduce un video in loop. Il player vive solo finché la composable è nello schermo
 * e si mette in pausa quando l'app va in background.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoSurface(
    uri: Uri,
    playing: Boolean,
    muted: Boolean,
    modifier: Modifier = Modifier,
    showController: Boolean = false,
    onProgress: (Float) -> Unit = {},
) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }
    val currentPlaying by rememberUpdatedState(playing)

    DisposableEffect(player) { onDispose { player.release() } }

    LaunchedEffect(player, muted) {
        player.volume = if (muted) 0f else 1f
        // da muto non si deve rubare il focus audio (altrimenti la musica in sottofondo si ferma)
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            !muted,
        )
    }
    LaunchedEffect(player, playing) { player.playWhenReady = playing }

    LaunchedEffect(player) {
        while (isActive) {
            val duration = player.duration
            if (duration > 0) onProgress((player.currentPosition.toFloat() / duration).coerceIn(0f, 1f))
            delay(150)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> player.playWhenReady = currentPlaying
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = showController
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                // trasparente: finché non arriva il primo fotogramma resta visibile la miniatura sotto
                setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                this.player = player
            }
        },
        update = { view ->
            view.useController = showController
            if (view.player !== player) view.player = player
        },
    )
}
