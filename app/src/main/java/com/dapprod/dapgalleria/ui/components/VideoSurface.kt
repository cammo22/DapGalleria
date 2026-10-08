package com.dapprod.dapgalleria.ui.components

import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import com.dapprod.dapgalleria.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Il player di un video più la posizione corrente, per mostrare barra di avanzamento e tempi. */
@Stable
class VideoPlayerState(val player: ExoPlayer, private val fallbackDurationMs: Long) {
    var positionMs by mutableLongStateOf(0L)
        internal set
    var durationMs by mutableLongStateOf(fallbackDurationMs)
        internal set

    /** Larghezza / altezza del video come si vede (0 finché non arriva il primo fotogramma). */
    var aspect by mutableFloatStateOf(0f)
        internal set

    fun seekTo(ms: Long) {
        val max = if (durationMs > 0) durationMs else Long.MAX_VALUE
        val target = ms.coerceIn(0L, max)
        player.seekTo(target)
        positionMs = target
    }

    /** Avanti (valori positivi) o indietro (negativi) di [deltaMs]. */
    fun seekBy(deltaMs: Long) = seekTo(player.currentPosition + deltaMs)

    internal fun refresh() {
        val duration = player.duration
        if (duration > 0) durationMs = duration
        positionMs = player.currentPosition.coerceAtLeast(0L)
        val size = player.videoSize
        if (size.width > 0 && size.height > 0) {
            val w = size.width * size.pixelWidthHeightRatio
            val h = size.height.toFloat()
            aspect = if (size.unappliedRotationDegrees % 180 != 0) h / w else w / h
        }
    }
}

/**
 * Crea il player per [uri] e lo tiene in sync con [playing] e [muted].
 * Viene rilasciato quando esce dallo schermo e si mette in pausa quando l'app va in background.
 */
@Composable
fun rememberVideoPlayer(uri: Uri, durationHintMs: Long, playing: Boolean, muted: Boolean): VideoPlayerState {
    val context = LocalContext.current
    val state = remember(uri) {
        val player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
        VideoPlayerState(player, durationHintMs)
    }
    val player = state.player
    val currentPlaying by rememberUpdatedState(playing)

    DisposableEffect(state) { onDispose { player.release() } }

    LaunchedEffect(state, muted) {
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
    LaunchedEffect(state, playing) { player.playWhenReady = playing }

    LaunchedEffect(state) {
        while (isActive) {
            state.refresh()
            delay(120)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state) {
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
    return state
}

/**
 * La superficie video. Con [showController] usa i controlli standard di Media3 (anteprima a schermo intero).
 * Con [texture] usa una TextureView: si può girare e colorare da Compose (editor).
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoSurface(state: VideoPlayerState, modifier: Modifier = Modifier, showController: Boolean = false, texture: Boolean = false) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val view = if (texture) {
                LayoutInflater.from(ctx).inflate(R.layout.player_texture, null) as PlayerView
            } else {
                PlayerView(ctx)
            }
            view.apply {
                useController = showController
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                // trasparente: finché non arriva il primo fotogramma resta visibile la miniatura sotto
                setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                player = state.player
            }
        },
        update = { view ->
            view.useController = showController
            if (view.player !== state.player) view.player = state.player
        },
    )
}
