package com.dapprod.dapgalleria.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Area di ritaglio in pixel della foto già raddrizzata (come la si vede a schermo). */
data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)

/** Carica una foto per il ritaglio e salva il risultato come nuova foto nella galleria. */
object ImageCropper {
    private const val MAX_SIDE = 4096

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
            val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (upright !== decoded) decoded.recycle()
            upright
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    /**
     * Ritaglia [source] e salva il risultato come nuova foto in Immagini/DapGalleria.
     * Ritorna l'URI della nuova foto, o null se il salvataggio fallisce.
     */
    suspend fun saveCropped(context: Context, original: MediaEntry, source: Bitmap, crop: CropRect): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val x = crop.left.coerceIn(0, source.width - 1)
                val y = crop.top.coerceIn(0, source.height - 1)
                val w = crop.width.coerceIn(1, source.width - x)
                val h = crop.height.coerceIn(1, source.height - y)
                val cropped = Bitmap.createBitmap(source, x, y, w, h)

                val isPng = original.name.endsWith(".png", ignoreCase = true)
                val extension = if (isPng) "png" else "jpg"
                val mime = if (isPng) "image/png" else "image/jpeg"
                val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val base = original.name.substringBeforeLast('.', original.name).ifEmpty { "foto" }
                val displayName = "${base}_ritagliata.$extension"

                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    insertScoped(context, displayName, mime, original.dateMillis, cropped, format)
                } else {
                    insertLegacy(context, displayName, mime, original.dateMillis, cropped, format)
                }
                cropped.recycle()
                uri
            } catch (_: Exception) {
                null
            } catch (_: OutOfMemoryError) {
                null
            }
        }

    private fun insertScoped(
        context: Context, name: String, mime: String, dateMillis: Long, bitmap: Bitmap, format: Bitmap.CompressFormat,
    ): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DapGalleria")
            put(MediaStore.Images.ImageColumns.DATE_TAKEN, dateMillis)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            val ok = resolver.openOutputStream(uri)?.use { bitmap.compress(format, 95, it) } ?: false
            if (!ok) throw IllegalStateException("compress failed")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun insertLegacy(
        context: Context, name: String, mime: String, dateMillis: Long, bitmap: Bitmap, format: Bitmap.CompressFormat,
    ): Uri? {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DapGalleria")
        if (!dir.exists() && !dir.mkdirs()) return null
        var file = File(dir, name)
        var n = 1
        while (file.exists()) {
            file = File(dir, name.substringBeforeLast('.') + "_$n." + name.substringAfterLast('.'))
            n++
        }
        FileOutputStream(file).use { if (!bitmap.compress(format, 95, it)) return null }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.DATA, file.absolutePath)
            put(MediaStore.MediaColumns.SIZE, file.length())
            put(MediaStore.Images.ImageColumns.DATE_TAKEN, dateMillis)
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    /** Elimina una foto creata dall'app (annulla di un ritaglio): l'app ne è proprietaria, quindi senza conferme. */
    suspend fun deleteOwn(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (_: Exception) {
        }
    }
}
