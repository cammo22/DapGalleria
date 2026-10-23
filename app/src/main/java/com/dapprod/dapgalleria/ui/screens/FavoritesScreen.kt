package com.dapprod.dapgalleria.ui.screens

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.dapprod.dapgalleria.ui.components.formatDuration
import com.dapprod.dapgalleria.ui.components.formatSize
import com.dapprod.dapgalleria.ui.components.rememberMediaRequest
import com.dapprod.dapgalleria.ui.theme.BrandGradient
import com.dapprod.dapgalleria.ui.theme.GoldGradient
import com.dapprod.dapgalleria.ui.theme.Palette

private enum class FavoritesFilter(val label: String) { ALL("Tutti"), PHOTOS("Foto"), VIDEOS("Video") }

/**
 * I contenuti con la stella ⭐, gli ultimi aggiunti per primi. Un tocco li apre a schermo intero,
 * la stella sulla miniatura li toglie dai preferiti (restano nella galleria).
 */
@Composable
fun FavoritesScreen(
    favorites: List<MediaEntry>,
    favoriteKeys: Set<String>,
    onBack: () -> Unit,
    onToggleFavorite: (MediaEntry) -> Unit,
) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(FavoritesFilter.ALL) }
    var preview by remember { mutableStateOf<MediaEntry?>(null) }

    val visible = remember(favorites, filter) {
        when (filter) {
            FavoritesFilter.ALL -> favorites
            FavoritesFilter.PHOTOS -> favorites.filter { it.type == MediaType.PHOTO }
            FavoritesFilter.VIDEOS -> favorites.filter { it.type == MediaType.VIDEO }
        }
    }
    val totalBytes = remember(visible) { visible.sumOf { it.sizeBytes } }

    Box(Modifier.fillMaxSize()) {
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
                    Text("Preferiti", style = MaterialTheme.typography.titleLarge.copy(brush = GoldGradient), fontWeight = FontWeight.Black)
                    Text(
                        "${visible.size} elementi · ${formatSize(context, totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextSecondary,
                    )
                }
            }

            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FavoritesFilter.entries.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                }
            }

            if (visible.isEmpty()) {
                EmptyFavorites(Modifier.weight(1f))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visible, key = { it.key }) { entry ->
                        FavoriteTile(
                            entry,
                            onOpen = { preview = entry },
                            onUnstar = { onToggleFavorite(entry) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

        preview?.let { entry ->
            MediaViewer(
                entry = entry,
                onDismiss = { preview = null },
                isFavorite = entry.key in favoriteKeys,
                onToggleFavorite = { onToggleFavorite(entry) },
            )
        }
    }
}

@Composable
private fun FavoriteTile(entry: MediaEntry, onOpen: () -> Unit, onUnstar: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
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
        // la stella piena toglie il contenuto dai preferiti: resta nella galleria
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onUnstar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Star, contentDescription = "Togli dai preferiti", tint = Palette.Gold, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EmptyFavorites(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = Palette.Gold.copy(alpha = 0.6f), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("Nessun preferito", style = MaterialTheme.typography.titleMedium.copy(brush = BrandGradient), textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            "Tocca la stella ⭐ su una foto o un video per metterlo qui.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
