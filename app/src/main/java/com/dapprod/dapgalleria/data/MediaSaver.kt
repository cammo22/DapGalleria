package com.dapprod.dapgalleria.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/** Salva nella galleria le foto e i video creati dall'app (in Immagini/DapGalleria e Film/DapGalleria). */
object MediaSaver {
    const val FOLDER = "DapGalleria"

    /** Salva [bitmap] come JPEG (o PNG se [png]). Ritorna l'URI nuovo o null. */
    suspend fun saveImage(context: Context, bitmap: Bitmap, displayName: String, png: Boolean, dateMillis: Long): Uri? =
        withContext(Dispatchers.IO) {
            val mime = if (png) "image/png" else "image/jpeg"
            val format = if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            try {
                insert(
                    context,
                    collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    directory = Environment.DIRECTORY_PICTURES,
                    name = displayName,
                    mime = mime,
                    dateMillis = dateMillis,
                    isVideo = false,
                ) { out -> if (!bitmap.compress(format, 95, out)) throw IllegalStateException("compress") }
            } catch (_: Exception) {
                null
            } catch (_: OutOfMemoryError) {
                null
            }
        }

    /** Copia un video già pronto (in cache) nella galleria. */
    suspend fun saveVideo(context: Context, file: File, displayName: String, dateMillis: Long): Uri? =
        withContext(Dispatchers.IO) {
            try {
                insert(
                    context,
                    collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    directory = Environment.DIRECTORY_MOVIES,
                    name = displayName,
                    mime = "video/mp4",
                    dateMillis = dateMillis,
                    isVideo = true,
                ) { out -> file.inputStream().use { it.copyTo(out) } }
            } catch (_: Exception) {
                null
            }
        }

    private fun insert(
        context: Context,
        collection: Uri,
        directory: String,
        name: String,
        mime: String,
        dateMillis: Long,
        isVideo: Boolean,
        write: (OutputStream) -> Unit,
    ): Uri? {
        val resolver = context.contentResolver
        val dateColumn = if (isVideo) MediaStore.Video.VideoColumns.DATE_TAKEN else MediaStore.Images.ImageColumns.DATE_TAKEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/$FOLDER")
                put(dateColumn, dateMillis)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: return null
            return try {
                resolver.openOutputStream(uri)?.use(write) ?: throw IllegalStateException("stream")
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                null
            }
        }

        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(directory), FOLDER)
        if (!dir.exists() && !dir.mkdirs()) return null
        var file = File(dir, name)
        var n = 1
        while (file.exists()) {
            file = File(dir, name.substringBeforeLast('.') + "_$n." + name.substringAfterLast('.'))
            n++
        }
        try {
            FileOutputStream(file).use(write)
        } catch (e: Exception) {
            file.delete()
            return null
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            @Suppress("DEPRECATION")
            put(MediaStore.MediaColumns.DATA, file.absolutePath)
            put(MediaStore.MediaColumns.SIZE, file.length())
            put(dateColumn, dateMillis)
        }
        return resolver.insert(collection, values)
    }

    /** Elimina un contenuto creato dall'app (annulla di una modifica): ne è proprietaria, quindi senza conferme. */
    suspend fun deleteOwn(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (_: Exception) {
        }
    }

    /** "IMG_1234.jpg" -> "IMG_1234_dap.jpg" */
    fun editedName(original: String, extension: String): String {
        val base = original.substringBeforeLast('.', original).ifEmpty { "dap" }
        return "${base}_dap.$extension"
    }
}
