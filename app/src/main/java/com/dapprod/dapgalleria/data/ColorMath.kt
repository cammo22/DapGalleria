package com.dapprod.dapgalleria.data

/**
 * Matrici colore 4x5 nello stesso formato di android.graphics.ColorMatrix
 * (righe R, G, B, A; l'ultima colonna è lo spostamento, in 0..255).
 * Kotlin puro: la stessa matrice serve all'anteprima in Compose e al salvataggio su Bitmap.
 */
object ColorMath {

    fun identity(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    /** Prima [first], poi [then]: il risultato è then ∘ first. */
    fun concat(first: FloatArray, then: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (row in 0 until 4) {
            for (col in 0 until 5) {
                var v = if (col == 4) then[row * 5 + 4] else 0f
                for (k in 0 until 4) v += then[row * 5 + k] * first[k * 5 + col]
                out[row * 5 + col] = v
            }
        }
        return out
    }

    fun chain(vararg matrices: FloatArray): FloatArray = matrices.fold(identity()) { acc, m -> concat(acc, m) }

    /** Da [a] (t = 0) a [b] (t = 1). */
    fun lerp(a: FloatArray, b: FloatArray, t: Float): FloatArray = FloatArray(20) { a[it] + (b[it] - a[it]) * t }

    /** -1 (scuro) .. +1 (chiaro). */
    fun brightness(amount: Float): FloatArray = identity().also {
        val off = amount * 0.3f * 255f
        it[4] = off; it[9] = off; it[14] = off
    }

    /** -1 (piatto) .. +1 (deciso). */
    fun contrast(amount: Float): FloatArray {
        val s = 1f + amount * 0.8f
        val off = 128f * (1f - s)
        return floatArrayOf(
            s, 0f, 0f, 0f, off,
            0f, s, 0f, 0f, off,
            0f, 0f, s, 0f, off,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    /** -1 (bianco e nero) .. +1 (colori il doppio più accesi). */
    fun saturation(amount: Float): FloatArray {
        val s = (1f + amount).coerceAtLeast(0f)
        val ir = 0.213f * (1f - s)
        val ig = 0.715f * (1f - s)
        val ib = 0.072f * (1f - s)
        return floatArrayOf(
            ir + s, ig, ib, 0f, 0f,
            ir, ig + s, ib, 0f, 0f,
            ir, ig, ib + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    /** -1 (freddo, azzurro) .. +1 (caldo, arancio). */
    fun warmth(amount: Float): FloatArray = identity().also {
        it[0] = 1f + 0.14f * amount
        it[4] = 10f * amount
        it[12] = 1f - 0.14f * amount
        it[14] = -10f * amount
    }

    /** Sposta le ombre verso il grigio: l'effetto "pellicola sbiadita". 0..1. */
    fun fade(amount: Float): FloatArray {
        val s = 1f - 0.18f * amount
        val off = 255f * 0.12f * amount
        return floatArrayOf(
            s, 0f, 0f, 0f, off,
            0f, s, 0f, 0f, off,
            0f, 0f, s, 0f, off,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    fun sepia(): FloatArray = floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    /** Moltiplica i canali (tinta). */
    fun tint(r: Float, g: Float, b: Float, offR: Float = 0f, offG: Float = 0f, offB: Float = 0f): FloatArray = floatArrayOf(
        r, 0f, 0f, 0f, offR,
        0f, g, 0f, 0f, offG,
        0f, 0f, b, 0f, offB,
        0f, 0f, 0f, 1f, 0f,
    )

    /** Applica la matrice a un colore 0..255 (serve ai test e ai conti sui pixel). */
    fun apply(m: FloatArray, r: Float, g: Float, b: Float, a: Float = 255f): FloatArray = FloatArray(4) { row ->
        (m[row * 5] * r + m[row * 5 + 1] * g + m[row * 5 + 2] * b + m[row * 5 + 3] * a + m[row * 5 + 4]).coerceIn(0f, 255f)
    }
}

/** I look pronti, uguali per foto e (in versione semplificata) per i video. */
enum class PhotoFilter(val label: String) {
    NONE("Originale"),
    VIVID("Vivace"),
    WARM("Caldo"),
    COOL("Freddo"),
    MONO("B/N"),
    NOIR("Noir"),
    VINTAGE("Vintage"),
    FILM("Pellicola"),
    CINEMA("Cinema"),
    NEON("Neon DaProd"),
    DRAMA("Dramma");

    /** Matrice del filtro al 100%. */
    fun matrix(): FloatArray = when (this) {
        NONE -> ColorMath.identity()
        VIVID -> ColorMath.chain(ColorMath.saturation(0.45f), ColorMath.contrast(0.15f))
        WARM -> ColorMath.chain(ColorMath.warmth(0.75f), ColorMath.saturation(0.1f))
        COOL -> ColorMath.chain(ColorMath.warmth(-0.7f), ColorMath.brightness(0.03f))
        MONO -> ColorMath.chain(ColorMath.saturation(-1f), ColorMath.contrast(0.12f))
        NOIR -> ColorMath.chain(ColorMath.saturation(-1f), ColorMath.contrast(0.55f), ColorMath.brightness(-0.08f))
        VINTAGE -> ColorMath.chain(ColorMath.lerp(ColorMath.identity(), ColorMath.sepia(), 0.65f), ColorMath.fade(0.6f))
        FILM -> ColorMath.chain(ColorMath.fade(0.8f), ColorMath.warmth(0.3f), ColorMath.saturation(-0.15f))
        CINEMA -> ColorMath.chain(
            ColorMath.contrast(0.25f),
            ColorMath.tint(1.06f, 0.98f, 0.94f, offB = 14f, offG = 4f),
            ColorMath.saturation(-0.1f),
        )
        NEON -> ColorMath.chain(ColorMath.saturation(0.6f), ColorMath.tint(1.08f, 0.92f, 1.12f, offR = 6f, offB = 10f), ColorMath.contrast(0.2f))
        DRAMA -> ColorMath.chain(ColorMath.contrast(0.45f), ColorMath.saturation(-0.35f), ColorMath.brightness(-0.04f))
    }
}

/** Le regolazioni a cursore dell'editor (tutte 0 = neutre). */
data class Adjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val fade: Float = 0f,
    /** 0..1: bordi scuriti. Non è una matrice: si disegna sopra. */
    val vignette: Float = 0f,
) {
    val isNeutral: Boolean
        get() = brightness == 0f && contrast == 0f && saturation == 0f && warmth == 0f && fade == 0f && vignette == 0f

    /** Filtro (con la sua intensità) e poi le regolazioni. */
    fun matrix(filter: PhotoFilter, intensity: Float): FloatArray = ColorMath.chain(
        ColorMath.lerp(ColorMath.identity(), filter.matrix(), intensity.coerceIn(0f, 1f)),
        ColorMath.warmth(warmth),
        ColorMath.saturation(saturation),
        ColorMath.contrast(contrast),
        ColorMath.brightness(brightness),
        ColorMath.fade(fade),
    )
}
