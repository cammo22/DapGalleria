package com.dapprod.dapgalleria.data

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Legge foto e video dalla galleria tramite MediaStore. */
class MediaRepository(private val resolver: ContentResolver) {

    /** Tutti i contenuti, dal più recente al più vecchio. Lancia SecurityException senza permessi. */
    suspend fun loadAll(): List<MediaEntry> = withContext(Dispatchers.IO) {
        (query(MediaType.PHOTO) + query(MediaType.VIDEO)).sortedByDescending { it.dateMillis }
    }

    private fun query(type: MediaType): List<MediaEntry> {
        val isVideo = type == MediaType.VIDEO
        val collection: Uri = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(if (isVideo) MediaStore.Video.VideoColumns.DATE_TAKEN else MediaStore.Images.ImageColumns.DATE_TAKEN)
            if (isVideo) add(MediaStore.Video.VideoColumns.DURATION)
        }.toTypedArray()

        val result = ArrayList<MediaEntry>()
        resolver.query(collection, projection, null, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val takenCol = c.getColumnIndexOrThrow(
                if (isVideo) MediaStore.Video.VideoColumns.DATE_TAKEN else MediaStore.Images.ImageColumns.DATE_TAKEN,
            )
            val durationCol = if (isVideo) c.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION) else -1

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val taken = c.getLong(takenCol)
                val added = c.getLong(addedCol) * 1000L
                result += MediaEntry(
                    id = id,
                    type = type,
                    uri = ContentUris.withAppendedId(collection, id),
                    name = c.getString(nameCol) ?: "",
                    sizeBytes = c.getLong(sizeCol),
                    dateMillis = if (taken > 0) taken else added,
                    durationMs = if (durationCol >= 0) c.getLong(durationCol) else 0L,
                )
            }
        }
        return result
    }
}
