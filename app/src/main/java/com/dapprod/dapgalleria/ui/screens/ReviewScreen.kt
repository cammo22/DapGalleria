package com.dapprod.dapgalleria.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.MediaType
import com.dapprod.dapgalleria.data.Stats
import com.dapprod.dapgalleria.ui.DeleteResult
import com.dapprod.dapgalleria.ui.components.formatDuration
import com.dapprod.dapgalleria.ui.components.formatSize
import com.dapprod.dapgalleria.ui.components.rememberMediaRequest
import com.dapprod.dapgalleria.ui.theme.Palette
import kotlinx.coroutines.launch

private enum class ReviewFilter(val label: String) { ALL("Tutti"), PHOTOS("Foto"), VIDEOS("Video") }

/**
 * Lista dei contenuti marchiati come "da eliminare" nella sessione.
 * Da qui si controllano a colpo d'occhio, si tolgono dalla lista quelli swipati per sbaglio
 * e si conferma l'eliminazione definitiva.
 */
@Composable
fun ReviewScreen(
    pending: List<MediaEntry>,
    onBack: () -> Unit,
    onRestore: (Collection<String>) -> Unit,
    /** Elimina davvero i file; ritorna chi è stato eliminato, lo spazio liberato e i punti guadagnati. */
    onDelete: suspend (List<MediaEntry>) -> DeleteResult,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var filter by rememberSaveable { mutableStateOf(ReviewFilter.ALL) }
    var preview by remember { mutableStateOf<MediaEntry?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    val visible = remember(pending, filter) {
        when (filter) {
            ReviewFilter.ALL -> pending
            ReviewFilter.PHOTOS -> pending.filter { it.type == MediaType.PHOTO }
            ReviewFilter.VIDEOS -> pending.filter { it.type == MediaType.VIDEO }
        }
    }
    val totalBytes = remember(visible) { visible.sumOf { it.sizeBytes } }
    val potentialPoints = remember(visible) { visible.sumOf { Stats.pointsFor(it.sizeBytes) } }

    fun startDelete() {
        val toDelete = visible
        scope.launch {
            deleting = true
            val result = onDelete(toDelete)
            deleting = false
            snackbar.showSnackbar(
                if (result.deletedKeys.isEmpty()) {
                    "Nessun elemento eliminato"
                } else {
                    "Eliminati ${result.deletedKeys.size} elementi · liberati ${formatSize(context, result.freedBytes)} · +${result.points} punti"
                },
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Palette.TextPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text("Da eliminare", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${visible.size} elementi · ${formatSize(context, totalBytes)} · +$potentialPoints punti",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextSecondary,
                    )
                }
            }

            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewFilter.entries.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                }
            }

            if (visible.isEmpty()) {
                EmptyReview(Modifier.weight(1f))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visible, key = { it.key }) { entry ->
                        PendingTile(entry, onOpen = { preview = entry }, onRemove = { onRestore(listOf(entry.key)) })
                    }
                }
            }

            if (visible.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Palette.Surface)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { onRestore(visible.map { it.key }) }, enabled = !deleting) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Svuota")
                    }
                    Button(
                        onClick = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) startDelete() else confirmDelete = true },
                        enabled = !deleting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Palette.Delete, contentColor = Color.White),
                    ) {
                        if (deleting) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.size(8.dp))
                        Text("Elimina definitivamente (${visible.size})")
                    }
                }
            }
        }

        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp),
        )
    }

    preview?.let { entry ->
        MediaPreviewDialog(
            entry = entry,
            onDismiss = { preview = null },
            onRestore = {
                preview = null
                onRestore(listOf(entry.key))
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminare definitivamente?") },
            text = { Text("${visible.size} elementi verranno cancellati dal telefono. L'operazione non si può annullare.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; startDelete() }) { Text("Elimina", color = Palette.Delete) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun PendingTile(entry: MediaEntry, onOpen: () -> Unit, onRemove: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.SurfaceHigh)
            .clickable(onClick = onOpen),
    ) {
        AsyncImage(
            model = rememberMediaRequest(entry),
            contentDescription = entry.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (entry.type == MediaType.VIDEO) {
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(formatDuration(entry.durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
        // toglie l'elemento dalla lista: resta nella galleria
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Togli dalla lista", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun EmptyReview(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Delete, contentDescription = null, tint = Palette.TextSecondary, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("Nessun contenuto da eliminare", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            "Scorri a sinistra una foto o un video per aggiungerlo qui.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
