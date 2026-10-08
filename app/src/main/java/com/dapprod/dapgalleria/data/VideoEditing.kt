package com.dapprod.dapgalleria.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class VideoAspect(val label: String, val ratio: Float?) {
    ORIGINAL("Originale", null),
    SQUARE("1:1", 1f),
    PORTRAIT("9:16", 9f / 16f),
    FOUR_FIVE("4:5", 4f / 5f),
    LANDSCAPE("16:9", 16f / 9f),
}

enum class VideoFilter(val label: String) {
    NONE("Originale"),
    VIVID("Vivace"),
    WARM("Caldo"),
    COOL("Freddo"),
    MONO("B/N"),
    DRAMA("Dramma");

    /** Matrice per l'anteprima dal vivo: la stessa idea degli effetti Media3 usati nel salvataggio. */
    fun previewMatrix(): FloatArray = when (this) {
        NONE -> ColorMath.identity()
        VIVID -> ColorMath.chain(ColorMath.saturation(0.35f), VideoEditing.contrastPreview(0.15f))
        WARM -> ColorMath.tint(1.12f, 1f, 0.88f)
        COOL -> ColorMath.tint(0.9f, 1f, 1.12f)
        MONO -> ColorMath.saturation(-1f)
        DRAMA -> ColorMath.chain(VideoEditing.contrastPreview(0.3f), ColorMath.saturation(-0.3f))
    }
}

/** Le scelte dell'editor video. I tempi sono in millisecondi. */
data class VideoEdit(
    val durationMs: Long,
    val startMs: Long = 0,
    val endMs: Long = durationMs,
    /** Gradi in senso orario: 0, 90, 180, 270. */
    val rotation: Int = 0,
    val aspect: VideoAspect = VideoAspect.ORIGINAL,
    val filter: VideoFilter = VideoFilter.NONE,
    /** -1..1 */
    val brightness: Float = 0f,
    /** -1..1 */
    val contrast: Float = 0f,
    val mute: Boolean = false,
) {
    val lengthMs: Long get() = (endMs - startMs).coerceAtLeast(0)

    val isUnchanged: Boolean
        get() = startMs <= 0 && endMs >= durationMs && rotation == 0 && aspect == VideoAspect.ORIGINAL &&
            filter == VideoFilter.NONE && brightness == 0f && contrast == 0f && !mute

    fun previewMatrix(): FloatArray = ColorMath.chain(
        filter.previewMatrix(),
        VideoEditing.contrastPreview(contrast * 0.6f),
        ColorMath.brightness(brightness),
    )
}

object VideoEditing {

    /** Il Contrast di Media3 ((c+1)/(1-c)) tradotto nella matrice dell'anteprima. */
    fun contrastPreview(c: Float): FloatArray {
        if (c == 0f) return ColorMath.identity()
        val s = (c + 1f) / (1.0001f - c)
        return ColorMath.contrast((s - 1f) / 0.8f)
    }

    @OptIn(UnstableApi::class)
    private fun videoEffects(edit: VideoEdit): List<Effect> = buildList {
        if (edit.rotation % 360 != 0) {
            // Media3 gira in senso antiorario
            add(ScaleAndRotateTransformation.Builder().setRotationDegrees(((360 - edit.rotation) % 360).toFloat()).build())
        }
        edit.aspect.ratio?.let { add(Presentation.createForAspectRatio(it, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP)) }
        when (edit.filter) {
            VideoFilter.NONE -> Unit
            VideoFilter.VIVID -> {
                add(HslAdjustment.Builder().adjustSaturation(35f).build())
                add(Contrast(0.15f))
            }
            VideoFilter.WARM -> add(RgbAdjustment.Builder().setRedScale(1.12f).setBlueScale(0.88f).build())
            VideoFilter.COOL -> add(RgbAdjustment.Builder().setRedScale(0.9f).setBlueScale(1.12f).build())
            VideoFilter.MONO -> add(RgbFilter.createGrayscaleFilter())
            VideoFilter.DRAMA -> {
                add(Contrast(0.3f))
                add(HslAdjustment.Builder().adjustSaturation(-30f).build())
            }
        }
        if (edit.contrast != 0f) add(Contrast((edit.contrast * 0.6f).coerceIn(-1f, 1f)))
        if (edit.brightness != 0f) add(Brightness((edit.brightness * 0.3f).coerceIn(-1f, 1f)))
    }

    /**
     * Esporta il video modificato in un file temporaneo (MP4 H.264/AAC).
     * [onProgress] riceve 0..1. Ritorna il file o null se non riesce. Si può annullare cancellando la coroutine.
     */
    @OptIn(UnstableApi::class)
    suspend fun export(context: Context, source: Uri, edit: VideoEdit, onProgress: (Float) -> Unit): File? =
        withContext(Dispatchers.Main) {
            val out = File(context.cacheDir, "dap_export_${System.currentTimeMillis()}.mp4")
            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(edit.startMs.coerceAtLeast(0))
                .apply { if (edit.endMs < edit.durationMs) setEndPositionMs(edit.endMs) }
                .build()
            val item = MediaItem.Builder().setUri(source).setClippingConfiguration(clipping).build()
            val edited = EditedMediaItem.Builder(item)
                .setRemoveAudio(edit.mute)
                .setEffects(Effects(emptyList(), videoEffects(edit)))
                .build()

            val done = CompletableDeferred<Boolean>()
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        done.complete(true)
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        done.complete(false)
                    }
                })
                .build()

            transformer.start(edited, out.absolutePath)
            val holder = ProgressHolder()
            val poll = launch {
                while (isActive) {
                    if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress / 100f)
                    delay(120)
                }
            }
            try {
                val ok = done.await()
                if (ok && out.length() > 0) {
                    onProgress(1f)
                    out
                } else {
                    out.delete()
                    null
                }
            } catch (e: CancellationException) {
                transformer.cancel()
                out.delete()
                throw e
            } finally {
                poll.cancel()
            }
        }

    /** Il fotogramma a [timeMs], a piena grandezza (per salvarlo come foto). */
    suspend fun frameAt(context: Context, uri: Uri, timeMs: Long): Bitmap? = withContext(Dispatchers.IO) {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            r.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        } finally {
            try { r.release() } catch (_: Exception) {}
        }
    }

    /** [count] fotogrammi piccoli distribuiti su tutto il video, per la striscia del taglio. */
    suspend fun filmstrip(context: Context, uri: Uri, durationMs: Long, count: Int, heightPx: Int): List<Bitmap> =
        withContext(Dispatchers.IO) {
            val r = MediaMetadataRetriever()
            val frames = ArrayList<Bitmap>()
            try {
                r.setDataSource(context, uri)
                val step = durationMs.coerceAtLeast(1) / count
                for (i in 0 until count) {
                    val us = (step * i + step / 2) * 1000
                    val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        r.getScaledFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, heightPx * 2, heightPx)
                    } else {
                        r.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { full ->
                            val w = (full.width * heightPx / full.height.coerceAtLeast(1)).coerceAtLeast(1)
                            Bitmap.createScaledBitmap(full, w, heightPx, true).also { if (it !== full) full.recycle() }
                        }
                    }
                    if (frame != null) frames += frame
                }
            } catch (_: Exception) {
            } catch (_: OutOfMemoryError) {
            } finally {
                try { r.release() } catch (_: Exception) {}
            }
            frames
        }

    /** Salva il video esportato nella galleria e cancella il file temporaneo. */
    suspend fun saveExport(context: Context, original: MediaEntry, file: File): Uri? {
        val uri = MediaSaver.saveVideo(context, file, MediaSaver.editedName(original.name, "mp4"), original.dateMillis)
        file.delete()
        return uri
    }

    /** Salva il fotogramma a [timeMs] come foto nuova. */
    suspend fun saveFrame(context: Context, original: MediaEntry, timeMs: Long): Uri? {
        val frame = frameAt(context, original.uri, timeMs) ?: return null
        val base = original.name.substringBeforeLast('.', original.name).ifEmpty { "video" }
        val uri = MediaSaver.saveImage(context, frame, "${base}_fotogramma_${timeMs / 1000}s.jpg", png = false, dateMillis = original.dateMillis)
        frame.recycle()
        return uri
    }
}
