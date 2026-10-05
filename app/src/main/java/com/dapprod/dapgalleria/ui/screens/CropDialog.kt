package com.dapprod.dapgalleria.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dapprod.dapgalleria.data.CropRect
import com.dapprod.dapgalleria.data.ImageCropper
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private enum class Handle { NONE, MOVE, TL, TR, BL, BR, LEFT, RIGHT, TOP, BOTTOM }

private enum class AspectPreset(val label: String, val ratio: Float?) {
    FREE("Libero", null),
    SQUARE("1:1", 1f),
    R43("4:3", 4f / 3f),
    R34("3:4", 3f / 4f),
    R169("16:9", 16f / 9f),
    R916("9:16", 9f / 16f),
}

/**
 * Ritaglio di una foto. [onSave] salva la versione ritagliata (che diventa la foto "tenuta")
 * e ritorna true se è andato a buon fine; l'originale viene mandato tra i contenuti da eliminare.
 */
@Composable
fun CropDialog(
    entry: MediaEntry,
    onDismiss: () -> Unit,
    onSave: suspend (Bitmap, CropRect) -> Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var crop by remember { mutableStateOf(Rect.Zero) }
    var preset by remember { mutableStateOf(AspectPreset.FREE) }

    LaunchedEffect(entry.key) {
        val bmp = ImageCropper.load(context, entry.uri)
        if (bmp == null) {
            failed = true
        } else {
            bitmap = bmp
            crop = Rect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
        }
    }

    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !saving, dismissOnClickOutside = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss, enabled = !saving) {
                    Icon(Icons.Filled.Close, contentDescription = "Chiudi", tint = Color.White)
                }
                Text(
                    "Ritaglia",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    enabled = bitmap != null && !saving,
                    onClick = { crop = Rect(0f, 0f, bitmap!!.width.toFloat(), bitmap!!.height.toFloat()); preset = AspectPreset.FREE },
                ) { Text("Reimposta") }
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val bmp = bitmap
                when {
                    failed -> Text("Non riesco ad aprire questa foto.", color = Color.White)
                    bmp == null -> CircularProgressIndicator(color = Palette.Pink)
                    else -> CropCanvas(
                        bitmap = bmp,
                        crop = crop,
                        ratio = preset.ratio,
                        enabled = !saving,
                        onCropChange = { crop = it },
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AspectPreset.entries.forEach { p ->
                    FilterChip(
                        selected = preset == p,
                        enabled = bitmap != null && !saving,
                        onClick = {
                            preset = p
                            val bmp = bitmap
                            val r = p.ratio
                            if (bmp != null && r != null) crop = fitRatio(crop, r, bmp.width.toFloat(), bmp.height.toFloat())
                        },
                        label = { Text(p.label) },
                    )
                }
            }

            if (saveFailed) {
                Text(
                    "Salvataggio non riuscito. Riprova.",
                    color = Palette.Delete,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            Text(
                "Salvo la foto ritagliata e tengo solo quella: l'originale va tra i contenuti da eliminare.",
                color = Palette.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Button(
                enabled = bitmap != null && !saving,
                onClick = {
                    val bmp = bitmap ?: return@Button
                    saving = true
                    saveFailed = false
                    scope.launch {
                        val ok = onSave(
                            bmp,
                            CropRect(crop.left.toInt(), crop.top.toInt(), crop.width.toInt(), crop.height.toInt()),
                        )
                        saving = false
                        if (ok) onDismiss() else saveFailed = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Keep, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Salva e tieni questa", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CropCanvas(
    bitmap: Bitmap,
    crop: Rect,
    ratio: Float?,
    enabled: Boolean,
    onCropChange: (Rect) -> Unit,
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val bw = bitmap.width.toFloat()
    val bh = bitmap.height.toFloat()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val layout = remember(canvasSize, bw, bh) {
        val scale = min(canvasSize.width / bw, canvasSize.height / bh)
        CropLayout(scale, Offset((canvasSize.width - bw * scale) / 2f, (canvasSize.height - bh * scale) / 2f))
    }
    // il gestore dei gesti vive a lungo: deve leggere sempre i valori più recenti
    val latestCrop by rememberUpdatedState(crop)
    val latestLayout by rememberUpdatedState(layout)
    val latestOnChange by rememberUpdatedState(onCropChange)

    Canvas(
        Modifier
            .fillMaxSize()
            .padding(12.dp)
            .onSizeChanged { canvasSize = it }
            .pointerInput(bitmap, ratio, enabled) {
                if (!enabled) return@pointerInput
                val slop = 32.dp.toPx()
                var handle = Handle.NONE
                detectDragGestures(
                    onDragStart = { start ->
                        handle = hitTest(toCanvas(latestCrop, latestLayout), start, slop, ratio != null)
                    },
                    onDragEnd = { handle = Handle.NONE },
                    onDragCancel = { handle = Handle.NONE },
                    onDrag = { change, drag ->
                        change.consume()
                        val scale = latestLayout.scale
                        val d = Offset(drag.x / scale, drag.y / scale)
                        latestOnChange(applyDrag(latestCrop, handle, d, bw, bh, ratio, minSize = 64f))
                    },
                )
            },
    ) {
        if (layout.scale <= 0f || layout.scale.isNaN()) return@Canvas
        val origin = layout.origin
        drawImage(
            image,
            dstOffset = IntOffset(origin.x.toInt(), origin.y.toInt()),
            dstSize = IntSize((bw * layout.scale).toInt(), (bh * layout.scale).toInt()),
        )

        val r = toCanvas(crop, layout)
        val imageRect = Rect(origin, Size(bw * layout.scale, bh * layout.scale))
        // zona esclusa, scurita
        val dim = Color.Black.copy(alpha = 0.6f)
        drawRect(dim, Offset(imageRect.left, imageRect.top), Size(imageRect.width, r.top - imageRect.top))
        drawRect(dim, Offset(imageRect.left, r.bottom), Size(imageRect.width, imageRect.bottom - r.bottom))
        drawRect(dim, Offset(imageRect.left, r.top), Size(r.left - imageRect.left, r.height))
        drawRect(dim, Offset(r.right, r.top), Size(imageRect.right - r.right, r.height))

        drawRect(Color.White, r.topLeft, r.size, style = Stroke(width = 2.dp.toPx()))
        // griglia dei terzi
        val gridColor = Color.White.copy(alpha = 0.45f)
        for (i in 1..2) {
            val gx = r.left + r.width * i / 3f
            val gy = r.top + r.height * i / 3f
            drawLine(gridColor, Offset(gx, r.top), Offset(gx, r.bottom), 1.dp.toPx())
            drawLine(gridColor, Offset(r.left, gy), Offset(r.right, gy), 1.dp.toPx())
        }
        drawCorners(r)
    }
}

private class CropLayout(val scale: Float, val origin: Offset)

private fun toCanvas(r: Rect, l: CropLayout): Rect = Rect(
    l.origin.x + r.left * l.scale,
    l.origin.y + r.top * l.scale,
    l.origin.x + r.right * l.scale,
    l.origin.y + r.bottom * l.scale,
)

private fun DrawScope.drawCorners(r: Rect) {
    val len = 22.dp.toPx()
    val w = 5.dp.toPx()
    val c = Color.White
    // alto-sinistra
    drawLine(c, Offset(r.left, r.top), Offset(r.left + len, r.top), w)
    drawLine(c, Offset(r.left, r.top), Offset(r.left, r.top + len), w)
    // alto-destra
    drawLine(c, Offset(r.right, r.top), Offset(r.right - len, r.top), w)
    drawLine(c, Offset(r.right, r.top), Offset(r.right, r.top + len), w)
    // basso-sinistra
    drawLine(c, Offset(r.left, r.bottom), Offset(r.left + len, r.bottom), w)
    drawLine(c, Offset(r.left, r.bottom), Offset(r.left, r.bottom - len), w)
    // basso-destra
    drawLine(c, Offset(r.right, r.bottom), Offset(r.right - len, r.bottom), w)
    drawLine(c, Offset(r.right, r.bottom), Offset(r.right, r.bottom - len), w)
}

/** Quale parte del riquadro viene afferrata (angoli, lati o l'interno per spostarlo). */
private fun hitTest(r: Rect, p: Offset, slop: Float, locked: Boolean): Handle {
    fun near(a: Float, b: Float) = abs(a - b) <= slop
    val nl = near(p.x, r.left)
    val nr = near(p.x, r.right)
    val nt = near(p.y, r.top)
    val nb = near(p.y, r.bottom)
    return when {
        nl && nt -> Handle.TL
        nr && nt -> Handle.TR
        nl && nb -> Handle.BL
        nr && nb -> Handle.BR
        // con il rapporto bloccato i lati non si possono trascinare da soli
        !locked && nl && p.y in r.top..r.bottom -> Handle.LEFT
        !locked && nr && p.y in r.top..r.bottom -> Handle.RIGHT
        !locked && nt && p.x in r.left..r.right -> Handle.TOP
        !locked && nb && p.x in r.left..r.right -> Handle.BOTTOM
        p.x in r.left..r.right && p.y in r.top..r.bottom -> Handle.MOVE
        else -> Handle.NONE
    }
}

private fun applyDrag(r: Rect, handle: Handle, d: Offset, bw: Float, bh: Float, ratio: Float?, minSize: Float): Rect {
    val minW = min(minSize, bw)
    val minH = min(minSize, bh)
    return when (handle) {
        Handle.NONE -> r
        Handle.MOVE -> {
            val dx = d.x.coerceIn(-r.left, bw - r.right)
            val dy = d.y.coerceIn(-r.top, bh - r.bottom)
            r.translate(dx, dy)
        }
        Handle.LEFT -> Rect(r.left.plus(d.x).coerceIn(0f, r.right - minW), r.top, r.right, r.bottom)
        Handle.RIGHT -> Rect(r.left, r.top, r.right.plus(d.x).coerceIn(r.left + minW, bw), r.bottom)
        Handle.TOP -> Rect(r.left, r.top.plus(d.y).coerceIn(0f, r.bottom - minH), r.right, r.bottom)
        Handle.BOTTOM -> Rect(r.left, r.top, r.right, r.bottom.plus(d.y).coerceIn(r.top + minH, bh))
        Handle.TL, Handle.TR, Handle.BL, Handle.BR -> {
            val left = handle == Handle.TL || handle == Handle.BL
            val top = handle == Handle.TL || handle == Handle.TR
            if (ratio == null) {
                Rect(
                    if (left) (r.left + d.x).coerceIn(0f, r.right - minW) else r.left,
                    if (top) (r.top + d.y).coerceIn(0f, r.bottom - minH) else r.top,
                    if (!left) (r.right + d.x).coerceIn(r.left + minW, bw) else r.right,
                    if (!top) (r.bottom + d.y).coerceIn(r.top + minH, bh) else r.bottom,
                )
            } else {
                // con rapporto bloccato l'angolo opposto resta fermo e la larghezza guida l'altezza
                val ax = if (left) r.right else r.left
                val ay = if (top) r.bottom else r.top
                val cx = (if (left) r.left else r.right) + d.x
                var w = abs(cx - ax)
                val maxW = if (left) ax else bw - ax
                val maxH = if (top) ay else bh - ay
                w = min(w, min(maxW, maxH * ratio))
                w = max(w, min(minW, min(maxW, maxH * ratio)))
                val h = w / ratio
                Rect(
                    if (left) ax - w else ax,
                    if (top) ay - h else ay,
                    if (left) ax else ax + w,
                    if (top) ay else ay + h,
                )
            }
        }
    }
}

/** Il rettangolo più grande con il rapporto dato, centrato sul riquadro attuale e dentro l'immagine. */
private fun fitRatio(r: Rect, ratio: Float, bw: Float, bh: Float): Rect {
    var w = r.width
    var h = w / ratio
    if (h > r.height) {
        h = r.height
        w = h * ratio
    }
    val c = r.center
    val left = (c.x - w / 2f).coerceIn(0f, bw - w)
    val top = (c.y - h / 2f).coerceIn(0f, bh - h)
    return Rect(left, top, left + w, top + h)
}
