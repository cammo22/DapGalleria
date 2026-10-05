package com.dapprod.dapgalleria.ui.components

import android.content.Context
import android.text.format.Formatter
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.MediaType
import java.text.DateFormat
import java.util.Date

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatSize(context: Context, bytes: Long): String = Formatter.formatShortFileSize(context, bytes)

fun formatDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

/** "0:42 · 12 MB · 3 mar 2025" */
fun MediaEntry.describe(context: Context): String = buildList {
    if (type == MediaType.VIDEO && durationMs > 0) add(formatDuration(durationMs))
    add(formatSize(context, sizeBytes))
    add(formatDate(dateMillis))
}.joinToString(" · ")
