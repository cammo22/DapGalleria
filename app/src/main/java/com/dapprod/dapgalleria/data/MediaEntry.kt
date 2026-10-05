package com.dapprod.dapgalleria.data

import android.net.Uri

enum class MediaType { PHOTO, VIDEO }

/** Una foto o un video della galleria di sistema (MediaStore). */
data class MediaEntry(
    val id: Long,
    val type: MediaType,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val dateMillis: Long,
    val durationMs: Long,
) {
    /** Chiave stabile usata per salvare le decisioni (le righe di foto e video hanno id che si sovrappongono). */
    val key: String = uri.toString()
}
