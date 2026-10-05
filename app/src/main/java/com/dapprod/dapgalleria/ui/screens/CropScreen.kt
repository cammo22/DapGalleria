package com.dapprod.dapgalleria.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.sp
import com.dapprod.dapgalleria.data.CropRect
import com.dapprod.dapgalleria.data.ImageCropper
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

private enum class Handle { NONE, MOVE, TL, TR, BL, BR, LEFT, RIGHT, TOP, BOTTOM }

/**
 * Ritaglio libero di una foto, a schermo intero. [onSave] salva la versione ritagliata
 * (che diventa la foto "tenuta") e ritorna true se è andato a buon fine; l'originale viene
 * mandato tra i contenuti da eliminare.
 */
@Composable
fun CropScreen(
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

    LaunchedEffect(entry.key) {
        val bmp = ImageCropper.load(context, entry.uri)
        if (bmp == null) {
            failed = true
        } else {
            bitmap = bmp
            crop = Rect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
        }
    }

    BackHandler { if (!saving) onDismiss() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {} // blocca i tocchi verso la schermata sotto
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                onClick = { bitmap?.let { crop = Rect(0f, 0f, it.width.toFloat(), it.height.toFloat()) } },
            ) { Text("Reimposta", fontSize = 16.sp) }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = bitmap
            when {
                failed -> Text("Non riesco ad aprire questa foto.", color = Color.White)
                bmp == null -> CircularProgressIndicator(color = Palette.Pink)
                else -> CropCanvas(bmp, crop, enabled = !saving, onCropChange = { crop = it })
            }
        }

        // pannello in basso: sempre visibile, con il pulsante grande e leggibile
        Column(
            Modifier
                .fillMaxWidth()
                .background(Palette.Surface)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                "Trascina gli angoli o i lati per ritagliare. Tengo solo la versione ritagliata: l'originale va tra i contenuti da eliminare.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
            )
            if (saveFailed) {
                Text(
                    "Salvataggio non riuscito. Riprova.",
                    color = Palette.Delete,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Palette.Keep,
                    contentColor = Color.Black,
                    disabledContainerColor = Palette.Keep.copy(alpha = 0.4f),
                    disabledContentColor = Color.Black.copy(alpha = 0.6f),
                ),
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .height(58.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.5.dp)
                } else {
                    Text("Salva e tieni", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun CropCanvas(
    bitmap: Bitmap,
    crop: Rect,
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
            .pointerInput(bitmap, enabled) {
                if (!enabled) return@pointerInput
                val slop = 32.dp.toPx()
                var handle = Handle.NONE
                detectDragGestures(
                    onDragStart = { start ->
                        handle = hitTest(toCanvas(latestCrop, latestLayout), start, slop)
                    },
                    onDragEnd = { handle = Handle.NONE },
                    onDragCancel = { handle = Handle.NONE },
                    onDrag = { change, drag ->
                        change.consume()
                        val scale = latestLayout.scale
                        val d = Offset(drag.x / scale, drag.y / scale)
                        latestOnChange(applyDrag(latestCrop, handle, d, bw, bh, minSize = 64f))
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
    drawLine(c, Offset(r.left, r.top), Offset(r.left + len, r.top), w)
    drawLine(c, Offset(r.left, r.top), Offset(r.left, r.top + len), w)
    drawLine(c, Offset(r.right, r.top), Offset(r.right - len, r.top), w)
    drawLine(c, Offset(r.right, r.top), Offset(r.right, r.top + len), w)
    drawLine(c, Offset(r.left, r.bottom), Offset(r.left + len, r.bottom), w)
    drawLine(c, Offset(r.left, r.bottom), Offset(r.left, r.bottom - len), w)
    drawLine(c, Offset(r.right, r.bottom), Offset(r.right - len, r.bottom), w)
    drawLine(c, Offset(r.right, r.bottom), Offset(r.right, r.bottom - len), w)
}

/** Quale parte del riquadro viene afferrata (angoli, lati o l'interno per spostarlo). */
private fun hitTest(r: Rect, p: Offset, slop: Float): Handle {
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
        nl && p.y in r.top..r.bottom -> Handle.LEFT
        nr && p.y in r.top..r.bottom -> Handle.RIGHT
        nt && p.x in r.left..r.right -> Handle.TOP
        nb && p.x in r.left..r.right -> Handle.BOTTOM
        p.x in r.left..r.right && p.y in r.top..r.bottom -> Handle.MOVE
        else -> Handle.NONE
    }
}

private fun applyDrag(r: Rect, handle: Handle, d: Offset, bw: Float, bh: Float, minSize: Float): Rect {
    val minW = min(minSize, bw)
    val minH = min(minSize, bh)
    val left = handle == Handle.TL || handle == Handle.BL || handle == Handle.LEFT
    val right = handle == Handle.TR || handle == Handle.BR || handle == Handle.RIGHT
    val top = handle == Handle.TL || handle == Handle.TR || handle == Handle.TOP
    val bottom = handle == Handle.BL || handle == Handle.BR || handle == Handle.BOTTOM
    return when (handle) {
        Handle.NONE -> r
        Handle.MOVE -> r.translate(d.x.coerceIn(-r.left, bw - r.right), d.y.coerceIn(-r.top, bh - r.bottom))
        else -> Rect(
            if (left) (r.left + d.x).coerceIn(0f, r.right - minW) else r.left,
            if (top) (r.top + d.y).coerceIn(0f, r.bottom - minH) else r.top,
            if (right) (r.right + d.x).coerceIn(r.left + minW, bw) else r.right,
            if (bottom) (r.bottom + d.y).coerceIn(r.top + minH, bh) else r.bottom,
        )
    }
}
