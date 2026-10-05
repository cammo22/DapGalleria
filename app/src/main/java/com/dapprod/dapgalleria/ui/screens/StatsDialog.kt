package com.dapprod.dapgalleria.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dapprod.dapgalleria.data.Stats
import com.dapprod.dapgalleria.ui.components.formatSize
import com.dapprod.dapgalleria.ui.theme.Palette

/** Punteggio, livello e totale dello spazio liberato. */
@Composable
fun StatsDialog(stats: Stats, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val next = stats.nextLevel
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
        icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Palette.Amber, modifier = Modifier.size(40.dp)) },
        title = { Text(stats.level.title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("${stats.score} punti", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Palette.Amber)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { stats.progressToNext },
                    modifier = Modifier.fillMaxWidth(),
                    color = Palette.Pink,
                    trackColor = Palette.SurfaceHigh,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (next != null) "Prossimo livello: ${next.title} (${next.minScore} punti)" else "Hai raggiunto il livello massimo!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary,
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("Spazio liberato", formatSize(context, stats.freedBytes))
                    Stat("Eliminati", stats.deletedCount.toString())
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Ogni elemento eliminato definitivamente vale 1 punto per ogni MB liberato (minimo 1).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary,
                )
            }
        },
    )
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
    }
}
