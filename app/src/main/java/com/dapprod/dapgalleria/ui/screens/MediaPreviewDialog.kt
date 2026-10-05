package com.dapprod.dapgalleria.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.MediaType
import com.dapprod.dapgalleria.ui.components.VideoSurface
import com.dapprod.dapgalleria.ui.components.rememberVideoPlayer
import com.dapprod.dapgalleria.ui.components.describe
import com.dapprod.dapgalleria.ui.components.rememberMediaRequest

/** Anteprima a schermo intero di un contenuto marchiato (i video partono con l'audio e i controlli). */
@Composable
fun MediaPreviewDialog(entry: MediaEntry, onDismiss: () -> Unit, onRestore: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            if (entry.type == MediaType.PHOTO) {
                AsyncImage(
                    model = rememberMediaRequest(entry),
                    contentDescription = entry.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val video = rememberVideoPlayer(entry.uri, entry.durationMs, playing = true, muted = false)
                VideoSurface(video, Modifier.fillMaxSize(), showController = true)
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Chiudi", tint = Color.White)
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (entry.type == MediaType.VIDEO) 72.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(entry.describe(LocalContext.current), color = Color.White.copy(alpha = 0.8f))
                Button(onClick = onRestore, modifier = Modifier.padding(top = 8.dp)) { Text("Togli dalla lista") }
            }
        }
    }
}
