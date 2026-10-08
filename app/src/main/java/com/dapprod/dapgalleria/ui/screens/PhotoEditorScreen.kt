package com.dapprod.dapgalleria.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapprod.dapgalleria.data.Adjustments
import com.dapprod.dapgalleria.data.CropRect
import com.dapprod.dapgalleria.data.MediaEntry
import com.dapprod.dapgalleria.data.PhotoEdit
import com.dapprod.dapgalleria.data.PhotoEditing
import com.dapprod.dapgalleria.data.PhotoFilter
import com.dapprod.dapgalleria.ui.theme.BrandGradient
import com.dapprod.dapgalleria.ui.theme.KeepGradient
import com.dapprod.dapgalleria.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

private enum class Handle { NONE, MOVE, TL, TR, BL, BR, LEFT, RIGHT, TOP, BOTTOM }

private enum class PhotoTab(val label: String, val icon: ImageVector) {
    CROP("Ritaglia", Icons.Filled.Crop),
    ADJUST("Regola", Icons.Filled.Tune),
    FILTER("Filtri", Icons.Filled.AutoAwesome),
}

/** Formati del ritaglio. ORIGINAL usa le proporzioni della foto. */
private enum class CropAspect(val label: String, val ratio: Float?) {
    FREE("Libero", null),
    ORIGINAL("Originale", -1f),
    SQUARE("1:1", 1f),
    P45("4:5", 4f / 5f),
    P34("3:4", 3f / 4f),
    P916("9:16", 9f / 16f),
    L43("4:3", 4f / 3f),
    L169("16:9", 16f / 9f),
}

private enum class AdjustKind(val label: String, val emoji: String, val bipolar: Boolean) {
    LIGHT("Luce", "☀️", true),
    CONTRAST("Contrasto", "◐", true),
    SATURATION("Saturazione", "🎨", true),
    WARMTH("Calore", "🌡️", true),
    FADE("Sbiadito", "🌫️", false),
    VIGNETTE("Vignetta", "⭕", false);

    fun get(a: Adjustments): Float = when (this) {
        LIGHT -> a.brightness
        CONTRAST -> a.contrast
        SATURATION -> a.saturation
        WARMTH -> a.warmth
        FADE -> a.fade
        VIGNETTE -> a.vignette
    }

    fun set(a: Adjustments, v: Float): Adjustments = when (this) {
        LIGHT -> a.copy(brightness = v)
        CONTRAST -> a.copy(contrast = v)
        SATURATION -> a.copy(saturation = v)
        WARMTH -> a.copy(warmth = v)
        FADE -> a.copy(fade = v)
        VIGNETTE -> a.copy(vignette = v)
    }
}

/**
 * L'editor delle foto: ritaglio (libero o a formato), rotazione, specchio, regolazioni, filtri e prima/dopo.
 * [onSave] riceve la foto di lavoro, le modifiche e se sostituire l'originale (che va tra i da eliminare).
 */
@Composable
fun PhotoEditorScreen(
    entry: MediaEntry,
    onDismiss: () -> Unit,
    onSave: suspend (Bitmap, PhotoEdit, Boolean) -> Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<Bitmap?>(null) }
    var working by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var crop by remember { mutableStateOf(Rect.Zero) }
    var aspect by remember { mutableStateOf(CropAspect.FREE) }
    var filter by remember { mutableStateOf(PhotoFilter.NONE) }
    var intensity by remember { mutableFloatStateOf(1f) }
    var adjustments by remember { mutableStateOf(Adjustments()) }
    var tab by remember { mutableStateOf(PhotoTab.CROP) }
    var adjustKind by remember { mutableStateOf(AdjustKind.LIGHT) }
    var comparing by remember { mutableStateOf(false) }

    LaunchedEffect(entry.key) {
        val bmp = PhotoEditing.load(context, entry.uri)
        if (bmp == null) {
            failed = true
        } else {
            loaded = bmp
            working = bmp
            crop = fullRect(bmp)
        }
    }

    fun ratioFor(a: CropAspect, bmp: Bitmap): Float? = when (a.ratio) {
        null -> null
        -1f -> bmp.width.toFloat() / bmp.height
        else -> a.ratio
    }

    fun applyAspect(a: CropAspect, bmp: Bitmap) {
        aspect = a
        val r = ratioFor(a, bmp)
        crop = if (r == null) fullRect(bmp) else centeredRect(r, bmp.width.toFloat(), bmp.height.toFloat())
    }

    fun transform(rotate: Int, mirror: Boolean) {
        val bmp = working ?: return
        busy = true
        scope.launch {
            PhotoEditing.transform(bmp, rotate, mirror)?.let { out ->
                working = out
                // un formato verticale diventa orizzontale girando: si tiene il formato e si ricentra
                applyAspect(aspect, out)
            }
            busy = false
        }
    }

    val matrix = remember(filter, intensity, adjustments) { adjustments.matrix(filter, intensity) }
    val colorFilter = remember(matrix) { ColorFilter.colorMatrix(ColorMatrix(matrix)) }
    val shownFilter = if (comparing) null else colorFilter
    val shownVignette = if (comparing) 0f else adjustments.vignette
    val dirty = working !== loaded || aspect != CropAspect.FREE || filter != PhotoFilter.NONE || !adjustments.isNeutral ||
        (working?.let { crop != fullRect(it) } ?: false)

    BackHandler { if (!saving) onDismiss() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Night)
            .pointerInput(Unit) {} // blocca i tocchi verso la schermata sotto
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss, enabled = !saving) {
                Icon(Icons.Filled.Close, contentDescription = "Chiudi", tint = Color.White)
            }
            Text(
                "Modifica foto",
                style = MaterialTheme.typography.titleLarge.copy(brush = BrandGradient),
                modifier = Modifier.weight(1f),
            )
            // tieni premuto per vedere l'originale
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (comparing) Palette.Magenta else Color.Transparent)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            comparing = true
                            tryAwaitRelease()
                            comparing = false
                        })
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Compare, contentDescription = "Tieni premuto per vedere l'originale", tint = Color.White)
            }
            TextButton(
                enabled = working != null && !saving,
                onClick = {
                    loaded?.let {
                        working = it
                        aspect = CropAspect.FREE
                        crop = fullRect(it)
                    }
                    filter = PhotoFilter.NONE
                    intensity = 1f
                    adjustments = Adjustments()
                },
            ) { Text("Reimposta", fontSize = 15.sp, color = Palette.Cyan) }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val bmp = working
            when {
                failed -> Text("Non riesco ad aprire questa foto.", color = Color.White)
                bmp == null -> CircularProgressIndicator(color = Palette.Magenta)
                else -> Crossfade(tab == PhotoTab.CROP, label = "canvas") { cropping ->
                    if (cropping) {
                        CropCanvas(
                            bitmap = bmp,
                            crop = crop,
                            ratio = ratioFor(aspect, bmp),
                            colorFilter = shownFilter,
                            vignette = shownVignette,
                            enabled = !saving && !busy,
                            onCropChange = { crop = it },
                        )
                    } else {
                        PreviewCanvas(bmp, crop, shownFilter, shownVignette)
                    }
                }
            }
            if (busy) CircularProgressIndicator(color = Palette.Magenta)
            if (comparing) {
                Text(
                    "ORIGINALE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }

        // pannello degli strumenti
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Palette.Surface)
                .padding(top = 14.dp, bottom = 12.dp),
        ) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { (fadeIn() + slideInVertically { it / 4 }) togetherWith fadeOut() },
                label = "tools",
            ) { t ->
                when (t) {
                    PhotoTab.CROP -> CropTools(
                        aspect = aspect,
                        enabled = working != null && !busy,
                        onAspect = { a -> working?.let { applyAspect(a, it) } },
                        onRotate = { transform(90, false) },
                        onMirror = { transform(0, true) },
                    )
                    PhotoTab.ADJUST -> AdjustTools(adjustments, adjustKind, onKind = { adjustKind = it }, onChange = { adjustments = it })
                    PhotoTab.FILTER -> FilterTools(working, filter, intensity, onFilter = { filter = it }, onIntensity = { intensity = it })
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoTab.entries.forEach { t ->
                    ToolTab(t.label, t.icon, selected = tab == t, onClick = { tab = t }, modifier = Modifier.weight(1f))
                }
            }

            if (saveFailed) {
                Text(
                    "Salvataggio non riuscito. Riprova.",
                    color = Palette.Delete,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                )
            }
            SaveButtons(
                enabled = working != null && !saving && !busy && dirty,
                saving = saving,
                hint = "Sostituisci: l'originale va tra i da eliminare · Copia: le tieni tutte e due",
                onSave = { replace ->
                    val bmp = working ?: return@SaveButtons
                    saving = true
                    saveFailed = false
                    scope.launch {
                        val edit = PhotoEdit(
                            crop = CropRect(crop.left.roundToInt(), crop.top.roundToInt(), crop.width.roundToInt(), crop.height.roundToInt()),
                            filter = filter,
                            intensity = intensity,
                            adjustments = adjustments,
                        )
                        val ok = onSave(bmp, edit, replace)
                        saving = false
                        if (ok) onDismiss() else saveFailed = true
                    }
                },
            )
        }
    }
}

/** I due pulsanti di salvataggio, uguali per foto e video. */
@Composable
internal fun SaveButtons(enabled: Boolean, saving: Boolean, hint: String, onSave: (replace: Boolean) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp).padding(top = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { onSave(false) },
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(54.dp),
            ) { Text("Salva copia", fontWeight = FontWeight.Bold) }
            Button(
                onClick = { onSave(true) },
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    contentColor = Color.Black,
                    disabledContentColor = Color.Black.copy(alpha = 0.5f),
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(KeepGradient, alpha = if (enabled) 1f else 0.35f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.5.dp)
                    } else {
                        Text("Sostituisci e tieni", fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        Text(hint, color = Palette.TextDim, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
internal fun ToolTab(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .then(if (selected) Modifier.background(BrandGradient, shape, alpha = 0.9f) else Modifier.background(Palette.SurfaceHigh))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Color.White else Palette.TextSecondary, modifier = Modifier.size(20.dp))
        Text(label, color = if (selected) Color.White else Palette.TextSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun Chip(label: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    val shape = RoundedCornerShape(50)
    Text(
        label,
        color = when {
            !enabled -> Palette.TextDim
            selected -> Color.White
            else -> Palette.TextSecondary
        },
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(shape)
            .then(if (selected) Modifier.background(BrandGradient, shape) else Modifier.border(1.dp, Palette.Outline, shape))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
internal fun ToolButton(label: String, icon: ImageVector, enabled: Boolean = true, selected: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Palette.Magenta.copy(alpha = 0.25f) else Palette.SurfaceHigh)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) Color.White else Palette.TextDim, modifier = Modifier.size(22.dp))
        Text(label, color = if (enabled) Palette.TextSecondary else Palette.TextDim, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CropTools(aspect: CropAspect, enabled: Boolean, onAspect: (CropAspect) -> Unit, onRotate: () -> Unit, onMirror: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CropAspect.entries.forEach { a -> Chip(a.label, selected = a == aspect, enabled = enabled, onClick = { onAspect(a) }) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolButton("Ruota", Icons.Filled.RotateRight, enabled = enabled, onClick = onRotate)
            ToolButton("Specchia", Icons.Filled.Flip, enabled = enabled, onClick = onMirror)
        }
    }
}

@Composable
private fun AdjustTools(a: Adjustments, kind: AdjustKind, onKind: (AdjustKind) -> Unit, onChange: (Adjustments) -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AdjustKind.entries.forEach { k ->
                val v = k.get(a)
                Chip("${k.emoji} ${k.label}${if (v != 0f) " •" else ""}", selected = k == kind, onClick = { onKind(k) })
            }
        }
        Spacer(Modifier.height(6.dp))
        ValueSlider(
            value = kind.get(a),
            bipolar = kind.bipolar,
            onChange = { onChange(kind.set(a, it)) },
        )
    }
}

/** Cursore con il valore scritto accanto; un tocco sul numero lo azzera. */
@Composable
internal fun ValueSlider(value: Float, bipolar: Boolean, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = if (bipolar) -1f..1f else 0f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Palette.Magenta,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f),
            ),
        )
        val n = (value * 100).roundToInt()
        Text(
            if (n > 0 && bipolar) "+$n" else "$n",
            color = if (n == 0) Palette.TextDim else Color.White,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .width(52.dp)
                .clickable { onChange(0f) }
                .padding(start = 10.dp),
        )
    }
}

@Composable
private fun FilterTools(working: Bitmap?, filter: PhotoFilter, intensity: Float, onFilter: (PhotoFilter) -> Unit, onIntensity: (Float) -> Unit) {
    val thumb: ImageBitmap? = remember(working) {
        working?.let {
            val scale = 180f / maxOf(it.width, it.height)
            Bitmap.createScaledBitmap(it, (it.width * scale).toInt().coerceAtLeast(1), (it.height * scale).toInt().coerceAtLeast(1), true).asImageBitmap()
        }
    }
    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(PhotoFilter.entries) { f ->
                val selected = f == filter
                val cf = remember(f) { ColorFilter.colorMatrix(ColorMatrix(f.matrix())) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onFilter(f) },
                ) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .then(if (selected) Modifier.border(3.dp, BrandGradient, RoundedCornerShape(16.dp)) else Modifier),
                    ) {
                        if (thumb != null) {
                            Image(thumb, contentDescription = f.label, contentScale = ContentScale.Crop, colorFilter = cf, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        f.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White else Palette.TextSecondary,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
                    )
                }
            }
        }
        if (filter != PhotoFilter.NONE) {
            Spacer(Modifier.height(4.dp))
            ValueSlider(intensity, bipolar = false, onChange = onIntensity)
        }
    }
}

// ------------------------------------------------------------------ disegno

private fun fullRect(bmp: Bitmap) = Rect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())

private fun centeredRect(ratio: Float, bw: Float, bh: Float): Rect = if (bw / bh > ratio) {
    val w = bh * ratio
    Rect((bw - w) / 2f, 0f, (bw + w) / 2f, bh)
} else {
    val h = bw / ratio
    Rect(0f, (bh - h) / 2f, bw, (bh + h) / 2f)
}

private fun DrawScope.drawVignette(r: Rect, amount: Float) {
    if (amount <= 0f) return
    drawRect(
        Brush.radialGradient(
            0f to Color.Transparent,
            PhotoEditing.VIGNETTE_INNER to Color.Transparent,
            1f to Color.Black.copy(alpha = PhotoEditing.vignetteAlpha(amount)),
            center = r.center,
            radius = hypot(r.width, r.height) / 2f,
        ),
        topLeft = r.topLeft,
        size = r.size,
    )
}

/** Solo la parte ritagliata, grande: per regolazioni e filtri. */
@Composable
private fun PreviewCanvas(bitmap: Bitmap, crop: Rect, colorFilter: ColorFilter?, vignette: Float) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Canvas(
        Modifier
            .fillMaxSize()
            .padding(12.dp),
    ) {
        val left = floor(crop.left).toInt().coerceIn(0, image.width - 1)
        val top = floor(crop.top).toInt().coerceIn(0, image.height - 1)
        val cw = crop.width.toInt().coerceIn(1, image.width - left)
        val ch = crop.height.toInt().coerceIn(1, image.height - top)
        val scale = min(size.width / cw, size.height / ch)
        val dw = cw * scale
        val dh = ch * scale
        val ox = (size.width - dw) / 2f
        val oy = (size.height - dh) / 2f
        drawImage(
            image,
            srcOffset = IntOffset(left, top),
            srcSize = IntSize(cw, ch),
            dstOffset = IntOffset(ox.toInt(), oy.toInt()),
            dstSize = IntSize(dw.toInt(), dh.toInt()),
            colorFilter = colorFilter,
        )
        drawVignette(Rect(ox, oy, ox + dw, oy + dh), vignette)
    }
}

@Composable
private fun CropCanvas(
    bitmap: Bitmap,
    crop: Rect,
    ratio: Float?,
    colorFilter: ColorFilter?,
    vignette: Float,
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
    val latestRatio by rememberUpdatedState(ratio)
    val latestOnChange by rememberUpdatedState(onCropChange)

    Canvas(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .onSizeChanged { canvasSize = it }
            .pointerInput(bitmap, enabled) {
                if (!enabled) return@pointerInput
                val slop = 32.dp.toPx()
                var handle = Handle.NONE
                detectDragGestures(
                    onDragStart = { start ->
                        handle = hitTest(toCanvas(latestCrop, latestLayout), start, slop, edges = latestRatio == null)
                    },
                    onDragEnd = { handle = Handle.NONE },
                    onDragCancel = { handle = Handle.NONE },
                    onDrag = { change, drag ->
                        change.consume()
                        val scale = latestLayout.scale
                        val d = Offset(drag.x / scale, drag.y / scale)
                        latestOnChange(applyDrag(latestCrop, handle, d, bw, bh, minSize = 64f, ratio = latestRatio))
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
            colorFilter = colorFilter,
        )

        val r = toCanvas(crop, layout)
        drawVignette(r, vignette)
        val imageRect = Rect(origin, Size(bw * layout.scale, bh * layout.scale))
        // zona esclusa, scurita
        val dim = Color.Black.copy(alpha = 0.62f)
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
    val c = Palette.Magenta
    drawLine(c, Offset(r.left, r.top), Offset(r.left + len, r.top), w)
    drawLine(c, Offset(r.left, r.top), Offset(r.left, r.top + len), w)
    drawLine(c, Offset(r.right, r.top), Offset(r.right - len, r.top), w)
    drawLine(c, Offset(r.right, r.top), Offset(r.right, r.top + len), w)
    drawLine(c, Offset(r.left, r.bottom), Offset(r.left + len, r.bottom), w)
    drawLine(c, Offset(r.left, r.bottom), Offset(r.left, r.bottom - len), w)
    drawLine(c, Offset(r.right, r.bottom), Offset(r.right - len, r.bottom), w)
    drawLine(c, Offset(r.right, r.bottom), Offset(r.right, r.bottom - len), w)
}

/** Quale parte del riquadro viene afferrata (angoli, lati o l'interno per spostarlo). Con un formato fisso niente lati. */
private fun hitTest(r: Rect, p: Offset, slop: Float, edges: Boolean): Handle {
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
        edges && nl && p.y in r.top..r.bottom -> Handle.LEFT
        edges && nr && p.y in r.top..r.bottom -> Handle.RIGHT
        edges && nt && p.x in r.left..r.right -> Handle.TOP
        edges && nb && p.x in r.left..r.right -> Handle.BOTTOM
        p.x in r.left..r.right && p.y in r.top..r.bottom -> Handle.MOVE
        else -> Handle.NONE
    }
}

private fun applyDrag(r: Rect, handle: Handle, d: Offset, bw: Float, bh: Float, minSize: Float, ratio: Float?): Rect {
    if (handle == Handle.NONE) return r
    if (handle == Handle.MOVE) return r.translate(d.x.coerceIn(-r.left, bw - r.right), d.y.coerceIn(-r.top, bh - r.bottom))
    val left = handle == Handle.TL || handle == Handle.BL || handle == Handle.LEFT
    val right = handle == Handle.TR || handle == Handle.BR || handle == Handle.RIGHT
    val top = handle == Handle.TL || handle == Handle.TR || handle == Handle.TOP
    val bottom = handle == Handle.BL || handle == Handle.BR || handle == Handle.BOTTOM

    if (ratio == null) {
        val minW = min(minSize, bw)
        val minH = min(minSize, bh)
        return Rect(
            if (left) (r.left + d.x).coerceIn(0f, r.right - minW) else r.left,
            if (top) (r.top + d.y).coerceIn(0f, r.bottom - minH) else r.top,
            if (right) (r.right + d.x).coerceIn(r.left + minW, bw) else r.right,
            if (bottom) (r.bottom + d.y).coerceIn(r.top + minH, bh) else r.bottom,
        )
    }

    // formato fisso: l'angolo opposto resta fermo e il riquadro mantiene le proporzioni
    val ax = if (left) r.right else r.left
    val ay = if (top) r.bottom else r.top
    val cx = (if (left) r.left else r.right) + d.x
    val cy = (if (top) r.top else r.bottom) + d.y
    var w = abs(cx - ax)
    var h = abs(cy - ay)
    if (w / ratio > h) h = w / ratio else w = h * ratio
    val maxW = if (left) ax else bw - ax
    val maxH = if (top) ay else bh - ay
    if (w > maxW) { w = maxW; h = w / ratio }
    if (h > maxH) { h = maxH; w = h * ratio }
    if (w < minSize || h < minSize) return r
    return Rect(
        if (left) ax - w else ax,
        if (top) ay - h else ay,
        if (left) ax else ax + w,
        if (top) ay else ay + h,
    )
}
