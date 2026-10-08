package com.dapprod.dapgalleria.ui.screens

import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.VideoAspect
import com.dapprod.dapgalleria.data.VideoEdit
import com.dapprod.dapgalleria.data.VideoEditing
import com.dapprod.dapgalleria.data.VideoFilter
import com.dapprod.dapgalleria.ui.components.ProgressRing
import com.dapprod.dapgalleria.ui.components.VideoSurface
import com.dapprod.dapgalleria.ui.components.formatDuration
import com.dapprod.dapgalleria.ui.components.rememberVideoPlayer
import com.dapprod.dapgalleria.ui.theme.BrandGradient
import com.dapprod.dapgalleria.ui.theme.Palette
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

private enum class VideoTab(val label: String, val icon: ImageVector) {
    TRIM("Taglia", Icons.Filled.ContentCut),
    FRAME("Formato", Icons.Filled.AspectRatio),
    LOOK("Look", Icons.Filled.AutoAwesome),
    AUDIO("Audio", Icons.AutoMirrored.Filled.VolumeUp),
}

private const val MIN_LENGTH_MS = 500L

/**
 * L'editor dei video: taglio con la striscia dei fotogrammi, rotazione, formato, look, audio e "foto da qui".
 * [onExported] riceve il file pronto e se sostituire l'originale; [onSaveFrame] salva il fotogramma come foto.
 */
@Composable
fun VideoEditorScreen(
    entry: MediaEntry,
    onDismiss: () -> Unit,
    onExported: suspend (File, Boolean, Long) -> Boolean,
    onSaveFrame: suspend (Long) -> Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var playing by remember { mutableStateOf(true) }
    var edit by remember { mutableStateOf<VideoEdit?>(null) }
    var tab by remember { mutableStateOf(VideoTab.TRIM) }
    var exporting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var exportJob by remember { mutableStateOf<Job?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var frameSaved by remember { mutableStateOf<String?>(null) }
    var frames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var scrubbing by remember { mutableStateOf(false) }
    val stripPx = with(LocalDensity.current) { 56.dp.roundToPx() }

    val video = rememberVideoPlayer(entry.uri, entry.durationMs, playing && !exporting && !scrubbing, muted = edit?.mute ?: false)
    val duration = video.durationMs

    LaunchedEffect(duration) {
        if (edit == null && duration > 0) edit = VideoEdit(durationMs = duration)
    }
    LaunchedEffect(entry.key, duration) {
        if (duration > 0 && frames.isEmpty()) {
            frames = VideoEditing.filmstrip(context, entry.uri, duration, 10, stripPx).map { it.asImageBitmap() }
        }
    }
    // gira in tondo dentro il pezzo scelto
    val latestEdit by rememberUpdatedState(edit)
    LaunchedEffect(video) {
        while (true) {
            val e = latestEdit
            if (e != null && !scrubbing) {
                val pos = video.player.currentPosition
                if (pos >= e.endMs || pos < e.startMs - 250) video.seekTo(e.startMs)
            }
            delay(40)
        }
    }
    LaunchedEffect(frameSaved) {
        if (frameSaved != null) {
            delay(2_000)
            frameSaved = null
        }
    }

    BackHandler {
        if (exporting) exportJob?.cancel() else onDismiss()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Night)
            .pointerInput(Unit) {},
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss, enabled = !exporting) {
                    Icon(Icons.Filled.Close, contentDescription = "Chiudi", tint = Color.White)
                }
                Text("Modifica video", style = MaterialTheme.typography.titleLarge.copy(brush = BrandGradient), modifier = Modifier.weight(1f))
                TextButton(
                    enabled = edit != null && !exporting,
                    onClick = { edit = VideoEdit(durationMs = duration) },
                ) { Text("Reimposta", fontSize = 15.sp, color = Palette.Cyan) }
            }

            // anteprima: girata, colorata e con la maschera del formato
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                val e = edit
                val baseAspect = if (video.aspect > 0f) video.aspect else 16f / 9f
                val rotated = (e?.rotation ?: 0) % 180 != 0
                val shownAspect = if (rotated) 1f / baseAspect else baseAspect
                val rotation by animateFloatAsState((e?.rotation ?: 0).toFloat(), tween(350), label = "rot")
                val matrix = remember(e?.filter, e?.brightness, e?.contrast) { e?.previewMatrix() }
                BoxWithConstraints(
                    Modifier
                        .aspectRatio(shownAspect)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { playing = !playing },
                    contentAlignment = Alignment.Center,
                ) {
                    val w = if (rotated) maxHeight else maxWidth
                    val h = if (rotated) maxWidth else maxHeight
                    Box(
                        Modifier
                            .requiredSize(w, h)
                            .graphicsLayer {
                                rotationZ = rotation
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && matrix != null) {
                                    renderEffect = RenderEffect
                                        .createColorFilterEffect(ColorMatrixColorFilter(matrix))
                                        .asComposeRenderEffect()
                                }
                            },
                    ) {
                        VideoSurface(video, Modifier.fillMaxSize(), texture = true)
                    }
                    val ratio = e?.aspect?.ratio
                    if (ratio != null) AspectMask(ratio, shownAspect)
                    if (!playing) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Riproduci", tint = Color.White, modifier = Modifier.size(38.dp))
                        }
                    }
                }
                frameSaved?.let {
                    Text(
                        it,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(50))
                            .background(Palette.Keep.copy(alpha = 0.9f))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Palette.Surface)
                    .padding(top = 14.dp, bottom = 12.dp),
            ) {
                val e = edit
                if (e == null) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Palette.Magenta)
                    }
                } else {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = { (fadeIn() + slideInVertically { it / 4 }) togetherWith fadeOut() },
                        label = "vtools",
                    ) { t ->
                        when (t) {
                            VideoTab.TRIM -> TrimTools(
                                edit = e,
                                frames = frames,
                                position = video.positionMs,
                                playing = playing,
                                onTogglePlay = { playing = !playing },
                                onChange = { edit = it },
                                onScrub = { ms, active ->
                                    scrubbing = active
                                    video.seekTo(ms)
                                },
                                onFrame = {
                                    val at = video.positionMs
                                    scope.launch {
                                        frameSaved = if (onSaveFrame(at)) "📸 Foto salvata (${formatDuration(at)})" else "Foto non salvata"
                                    }
                                },
                            )
                            VideoTab.FRAME -> FrameTools(e, onChange = { edit = it })
                            VideoTab.LOOK -> LookTools(e, onChange = { edit = it })
                            VideoTab.AUDIO -> AudioTools(e, onChange = { edit = it })
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoTab.entries.forEach { t ->
                        val icon = if (t == VideoTab.AUDIO && e?.mute == true) Icons.AutoMirrored.Filled.VolumeOff else t.icon
                        ToolTab(t.label, icon, selected = tab == t, onClick = { tab = t }, modifier = Modifier.weight(1f))
                    }
                }
                error?.let {
                    Text(it, color = Palette.Delete, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 20.dp, top = 8.dp))
                }
                SaveButtons(
                    enabled = e != null && !e.isUnchanged && !exporting,
                    saving = exporting,
                    hint = "Sostituisci: l'originale va tra i da eliminare · Copia: li tieni tutti e due",
                    onSave = { replace ->
                        val current = e ?: return@SaveButtons
                        exporting = true
                        progress = 0f
                        error = null
                        playing = false
                        exportJob = scope.launch {
                            try {
                                val file = VideoEditing.export(context, entry.uri, current) { progress = it }
                                if (file == null) {
                                    error = "Non sono riuscito a creare il video. Prova senza formato o filtri."
                                } else if (onExported(file, replace, current.lengthMs)) {
                                    onDismiss()
                                } else {
                                    error = "Salvataggio non riuscito. Riprova."
                                }
                            } finally {
                                exporting = false
                            }
                        }
                    },
                )
            }
        }

        if (exporting) ExportOverlay(progress) { exportJob?.cancel() }
    }
}

/** Scurisce quello che il formato taglierà via. */
@Composable
private fun AspectMask(ratio: Float, containerAspect: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val (cw, ch) = if (containerAspect > ratio) (h * ratio) to h else w to (w / ratio)
        val left = (w - cw) / 2f
        val top = (h - ch) / 2f
        val dim = Color.Black.copy(alpha = 0.6f)
        drawRect(dim, Offset(0f, 0f), Size(w, top))
        drawRect(dim, Offset(0f, top + ch), Size(w, h - top - ch))
        drawRect(dim, Offset(0f, top), Size(left, ch))
        drawRect(dim, Offset(left + cw, top), Size(w - left - cw, ch))
        drawRect(Palette.Magenta, Offset(left, top), Size(cw, ch), style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun TrimTools(
    edit: VideoEdit,
    frames: List<ImageBitmap>,
    position: Long,
    playing: Boolean,
    onTogglePlay: () -> Unit,
    onChange: (VideoEdit) -> Unit,
    onScrub: (Long, Boolean) -> Unit,
    onFrame: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Palette.SurfaceHigh)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (playing) "Pausa" else "Riproduci", tint = Color.White)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${formatDuration(edit.startMs)} → ${formatDuration(edit.endMs)}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "durata ${formatDuration(edit.lengthMs)}${if (edit.lengthMs < edit.durationMs) " · tolti ${formatDuration(edit.durationMs - edit.lengthMs)}" else ""}",
                    color = Palette.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            ToolButton("Foto da qui", Icons.Filled.PhotoCamera, onClick = onFrame)
        }
        Spacer(Modifier.height(10.dp))
        TrimBar(edit, frames, position, onChange, onScrub)
        Text(
            "Trascina le maniglie per tenere solo il pezzo che ti serve.",
            color = Palette.TextDim,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** La striscia dei fotogrammi con le due maniglie e la linea della posizione. */
@Composable
private fun TrimBar(
    edit: VideoEdit,
    frames: List<ImageBitmap>,
    position: Long,
    onChange: (VideoEdit) -> Unit,
    onScrub: (Long, Boolean) -> Unit,
) {
    val latest by rememberUpdatedState(edit)
    val latestOnChange by rememberUpdatedState(onChange)
    val latestOnScrub by rememberUpdatedState(onScrub)
    val handleW = with(LocalDensity.current) { 16.dp.toPx() }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(60.dp),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Palette.SurfaceHigh),
        ) {
            frames.forEach { f ->
                Image(f, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var grab = 0 // -1 inizio, 1 fine, 0 posizione
                    val inner = widthPx - handleW * 2
                    fun toMs(x: Float): Long = (((x - handleW) / inner).coerceIn(0f, 1f) * latest.durationMs).toLong()
                    fun toX(ms: Long): Float = handleW + inner * (ms.toFloat() / latest.durationMs.coerceAtLeast(1))
                    detectDragGestures(
                        onDragStart = { p ->
                            val sx = toX(latest.startMs)
                            val ex = toX(latest.endMs)
                            grab = when {
                                abs(p.x - sx) < handleW * 2f && abs(p.x - sx) <= abs(p.x - ex) -> -1
                                abs(p.x - ex) < handleW * 2f -> 1
                                else -> 0
                            }
                            latestOnScrub(toMs(p.x).coerceIn(latest.startMs, latest.endMs), true)
                        },
                        onDragEnd = { latestOnScrub(latest.startMs, false) },
                        onDragCancel = { latestOnScrub(latest.startMs, false) },
                        onDrag = { change, _ ->
                            change.consume()
                            val ms = toMs(change.position.x)
                            val e = latest
                            when (grab) {
                                -1 -> {
                                    val s = ms.coerceIn(0, e.endMs - MIN_LENGTH_MS)
                                    latestOnChange(e.copy(startMs = s))
                                    latestOnScrub(s, true)
                                }
                                1 -> {
                                    val end = ms.coerceIn(e.startMs + MIN_LENGTH_MS, e.durationMs)
                                    latestOnChange(e.copy(endMs = end))
                                    latestOnScrub(end, true)
                                }
                                else -> latestOnScrub(ms.coerceIn(e.startMs, e.endMs), true)
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { p ->
                        val inner = widthPx - handleW * 2
                        val ms = (((p.x - handleW) / inner).coerceIn(0f, 1f) * latest.durationMs).toLong()
                        latestOnScrub(ms.coerceIn(latest.startMs, latest.endMs), false)
                    }
                },
        ) {
            val inner = size.width - handleW * 2
            val d = edit.durationMs.coerceAtLeast(1).toFloat()
            val sx = handleW + inner * (edit.startMs / d)
            val ex = handleW + inner * (edit.endMs / d)
            val dim = Color.Black.copy(alpha = 0.65f)
            drawRect(dim, Offset(handleW, 0f), Size((sx - handleW).coerceAtLeast(0f), size.height))
            drawRect(dim, Offset(ex, 0f), Size((size.width - handleW - ex).coerceAtLeast(0f), size.height))
            // cornice del pezzo tenuto
            drawRect(Palette.Gold, Offset(sx, 0f), Size(ex - sx, 3.dp.toPx()))
            drawRect(Palette.Gold, Offset(sx, size.height - 3.dp.toPx()), Size(ex - sx, 3.dp.toPx()))
            drawRoundRect(Palette.Gold, Offset(sx - handleW, 0f), Size(handleW, size.height), CornerRadius(6.dp.toPx()))
            drawRoundRect(Palette.Gold, Offset(ex, 0f), Size(handleW, size.height), CornerRadius(6.dp.toPx()))
            // tacche sulle maniglie
            val notch = Color(0xFF2A1A00)
            drawLine(notch, Offset(sx - handleW / 2, size.height * 0.35f), Offset(sx - handleW / 2, size.height * 0.65f), 2.dp.toPx())
            drawLine(notch, Offset(ex + handleW / 2, size.height * 0.35f), Offset(ex + handleW / 2, size.height * 0.65f), 2.dp.toPx())
            // posizione
            val px = handleW + inner * (position.coerceIn(0, edit.durationMs) / d)
            drawLine(Color.White, Offset(px, -2f), Offset(px, size.height + 2f), 3.dp.toPx())
        }
    }
}

@Composable
private fun FrameTools(edit: VideoEdit, onChange: (VideoEdit) -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VideoAspect.entries.forEach { a -> Chip(a.label, selected = edit.aspect == a, onClick = { onChange(edit.copy(aspect = a)) }) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            ToolButton("Ruota", Icons.Filled.RotateRight) { onChange(edit.copy(rotation = (edit.rotation + 90) % 360)) }
            Spacer(Modifier.width(12.dp))
            Text(
                if (edit.rotation == 0) "Dritto" else "Girato di ${edit.rotation}°",
                color = Palette.TextSecondary,
            )
        }
    }
}

@Composable
private fun LookTools(edit: VideoEdit, onChange: (VideoEdit) -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VideoFilter.entries.forEach { f -> Chip(f.label, selected = edit.filter == f, onClick = { onChange(edit.copy(filter = f)) }) }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("☀️", modifier = Modifier.width(24.dp))
            ValueSlider(edit.brightness, bipolar = true, onChange = { onChange(edit.copy(brightness = it)) }, modifier = Modifier.weight(1f))
        }
        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("◐", color = Color.White, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
            ValueSlider(edit.contrast, bipolar = true, onChange = { onChange(edit.copy(contrast = it)) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AudioTools(edit: VideoEdit, onChange: (VideoEdit) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        ToolButton(
            if (edit.mute) "Audio tolto" else "Togli l'audio",
            if (edit.mute) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            selected = edit.mute,
        ) { onChange(edit.copy(mute = !edit.mute)) }
        Spacer(Modifier.width(14.dp))
        Text(
            if (edit.mute) "Il video verrà salvato muto." else "Il video tiene il suo audio.",
            color = Palette.TextSecondary,
        )
    }
}

@Composable
private fun ExportOverlay(progress: Float, onCancel: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ProgressRing(progress, 140.dp, stroke = 10.dp) {
                Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Black, fontSize = 30.sp, color = Color.White)
            }
            Spacer(Modifier.height(18.dp))
            Text("Sto montando il tuo video…", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Resta su questa schermata", color = Palette.TextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onCancel) { Text("Annulla", color = Palette.Delete) }
        }
    }
}
