package com.dapprod.dapgalleria.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.hypot

/** Area di ritaglio in pixel della foto già raddrizzata (come la si vede a schermo). */
data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)

/** Tutto quello che l'editor ha deciso: si applica in un colpo solo al salvataggio. */
data class PhotoEdit(
    val crop: CropRect,
    val filter: PhotoFilter = PhotoFilter.NONE,
    val intensity: Float = 1f,
    val adjustments: Adjustments = Adjustments(),
) {
    val colorMatrix: FloatArray get() = adjustments.matrix(filter, intensity)
}

/** Carica, gira e salva le foto dell'editor. */
object PhotoEditing {
    private const val MAX_SIDE = 4096

    /** Vignetta: fino a dove resta pulita (frazione del raggio) e quanto scurisce il bordo. */
    const val VIGNETTE_INNER = 0.45f
    fun vignetteAlpha(amount: Float): Float = 0.85f * amount.coerceIn(0f, 1f)

    /** Decodifica la foto (ridimensionata se enorme) già ruotata secondo l'EXIF. Null se non leggibile. */
    suspend fun load(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@withContext null

            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
                else -> return@withContext decoded
            }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    /** Gira di 90° (a sinistra con [clockwise] false) o specchia in orizzontale. */
    suspend fun transform(source: Bitmap, rotate: Int = 0, mirror: Boolean = false): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val m = Matrix()
            if (rotate != 0) m.postRotate(rotate.toFloat())
            if (mirror) m.postScale(-1f, 1f)
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, m, true)
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    /** Applica ritaglio, colore e vignetta. */
    suspend fun render(source: Bitmap, edit: PhotoEdit): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val c = edit.crop
            val x = c.left.coerceIn(0, source.width - 1)
            val y = c.top.coerceIn(0, source.height - 1)
            val w = c.width.coerceIn(1, source.width - x)
            val h = c.height.coerceIn(1, source.height - y)
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            paint.colorFilter = ColorMatrixColorFilter(edit.colorMatrix)
            canvas.drawBitmap(source, Rect(x, y, x + w, y + h), Rect(0, 0, w, h), paint)

            val v = edit.adjustments.vignette
            if (v > 0f) {
                val radius = hypot(w.toFloat(), h.toFloat()) / 2f
                val edgeAlpha = (vignetteAlpha(v) * 255).toInt()
                val shader = RadialGradient(
                    w / 2f, h / 2f, radius,
                    intArrayOf(0, 0, android.graphics.Color.argb(edgeAlpha, 0, 0, 0)),
                    floatArrayOf(0f, VIGNETTE_INNER, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { this.shader = shader })
            }
            out
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    /** Applica le modifiche e salva la nuova foto. Ritorna l'URI nuovo o null. */
    suspend fun save(context: Context, original: MediaEntry, source: Bitmap, edit: PhotoEdit): Uri? {
        val rendered = render(source, edit) ?: return null
        val png = original.name.endsWith(".png", ignoreCase = true)
        val name = MediaSaver.editedName(original.name, if (png) "png" else "jpg")
        return MediaSaver.saveImage(context, rendered, name, png, original.dateMillis).also { rendered.recycle() }
    }
}
