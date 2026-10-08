package com.dapprod.dapgalleria.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** I suoni della sala, sintetizzati al primo avvio (niente file nell'APK). */
enum class Sfx { KEEP, DELETE, UNDO, COIN, COMBO, LEVEL, CHEST, BOOM, TICK }

class SoundFx(context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = HashMap<Sfx, Int>()

    init {
        val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
        // i file si preparano in un altro thread: il primo suono può arrivare un attimo dopo, mai bloccare l'avvio
        Thread {
            for (s in Sfx.entries) {
                try {
                    val f = File(dir, "${s.name.lowercase()}_v1.wav")
                    if (!f.exists()) writeWav(f, synth(s))
                    val id = pool.load(f.path, 1)
                    synchronized(ids) { ids[s] = id }
                } catch (_: Exception) {
                }
            }
        }.start()
    }

    fun play(s: Sfx, pitch: Float = 1f, volume: Float = 0.55f) {
        val id = synchronized(ids) { ids[s] } ?: return
        pool.play(id, volume, volume, 1, 0, pitch.coerceIn(0.5f, 2f))
    }

    fun release() = pool.release()

    private companion object {
        const val RATE = 44_100

        fun synth(s: Sfx): FloatArray = when (s) {
            Sfx.KEEP -> sweep(0.09f, 620f, 1040f, decay = 30f)
            Sfx.UNDO -> sweep(0.13f, 760f, 380f, decay = 18f)
            Sfx.TICK -> sweep(0.025f, 1800f, 1800f, decay = 120f, volume = 0.5f)
            Sfx.DELETE -> mix(noise(0.18f, decay = 16f, smooth = 0.82f), sweep(0.18f, 220f, 70f, decay = 14f, volume = 0.6f))
            Sfx.COIN -> concat(square(0.07f, 988f, decay = 6f), square(0.26f, 1319f, decay = 10f))
            Sfx.COMBO -> concat(sweep(0.05f, 523f, 523f, 20f), sweep(0.05f, 659f, 659f, 20f), sweep(0.12f, 784f, 784f, 12f))
            Sfx.LEVEL -> concat(
                square(0.09f, 523f, 8f), square(0.09f, 659f, 8f), square(0.09f, 784f, 8f),
                mix(square(0.5f, 1047f, 4f), sweep(0.5f, 1568f, 1568f, 5f, volume = 0.3f)),
            )
            Sfx.CHEST -> concat(*Array(6) { i -> sweep(0.06f, 1200f + i * 180f + Random.nextFloat() * 100f, 1500f + i * 200f, 25f, 0.6f) })
            Sfx.BOOM -> mix(noise(0.6f, decay = 6f, smooth = 0.93f), sweep(0.6f, 140f, 40f, decay = 5f, volume = 0.9f))
        }

        fun sweep(sec: Float, f0: Float, f1: Float, decay: Float, volume: Float = 0.8f): FloatArray {
            val n = (sec * RATE).toInt()
            var phase = 0.0
            return FloatArray(n) { i ->
                val t = i.toFloat() / n
                val f = f0 + (f1 - f0) * t
                phase += 2 * PI * f / RATE
                val attack = (i / (RATE * 0.004f)).coerceAtMost(1f)
                (sin(phase) * volume * attack * exp(-decay * t * sec)).toFloat()
            }
        }

        fun square(sec: Float, f: Float, decay: Float, volume: Float = 0.35f): FloatArray {
            val n = (sec * RATE).toInt()
            return FloatArray(n) { i ->
                val t = i.toFloat() / RATE
                val v = if (sin(2 * PI * f * t) >= 0) 1f else -1f
                val attack = (i / (RATE * 0.003f)).coerceAtMost(1f)
                v * volume * attack * exp(-decay * t)
            }
        }

        fun noise(sec: Float, decay: Float, smooth: Float): FloatArray {
            val n = (sec * RATE).toInt()
            var last = 0f
            return FloatArray(n) { i ->
                val t = i.toFloat() / RATE
                last = last * smooth + (Random.nextFloat() * 2f - 1f) * (1f - smooth)
                last * 3f * exp(-decay * t)
            }
        }

        fun mix(a: FloatArray, b: FloatArray): FloatArray = FloatArray(maxOf(a.size, b.size)) { i ->
            (a.getOrElse(i) { 0f } + b.getOrElse(i) { 0f }).coerceIn(-1f, 1f)
        }

        fun concat(vararg parts: FloatArray): FloatArray {
            val out = FloatArray(parts.sumOf { it.size })
            var pos = 0
            for (p in parts) { p.copyInto(out, pos); pos += p.size }
            return out
        }

        fun writeWav(file: File, samples: FloatArray) {
            val data = samples.size * 2
            DataOutputStream(FileOutputStream(file).buffered()).use { out ->
                fun le32(v: Int) { out.write(v and 0xff); out.write(v shr 8 and 0xff); out.write(v shr 16 and 0xff); out.write(v shr 24 and 0xff) }
                fun le16(v: Int) { out.write(v and 0xff); out.write(v shr 8 and 0xff) }
                out.writeBytes("RIFF"); le32(36 + data); out.writeBytes("WAVE")
                out.writeBytes("fmt "); le32(16); le16(1); le16(1); le32(RATE); le32(RATE * 2); le16(2); le16(16)
                out.writeBytes("data"); le32(data)
                for (s in samples) le16((s.coerceIn(-1f, 1f) * 32_000).toInt())
            }
        }
    }
}

/** Suoni e vibrazioni insieme, con gli interruttori del menu. */
class Feedback(private val view: View, private val sound: SoundFx?) {
    var soundOn: Boolean = true
    var hapticsOn: Boolean = true

    private fun haptic(constant: Int) {
        if (hapticsOn) view.performHapticFeedback(constant)
    }

    private fun sfx(s: Sfx, pitch: Float = 1f, volume: Float = 0.55f) {
        if (soundOn) sound?.play(s, pitch, volume)
    }

    /** Si passa la soglia trascinando la carta. */
    fun threshold() {
        haptic(HapticFeedbackConstants.CLOCK_TICK)
        sfx(Sfx.TICK, volume = 0.25f)
    }

    /** Il suono sale con la combo. */
    fun keep(combo: Int) {
        haptic(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP)
        sfx(Sfx.KEEP, pitch = 1f + (combo.coerceAtMost(30) * 0.025f))
    }

    fun delete(combo: Int) {
        haptic(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.KEYBOARD_TAP)
        sfx(Sfx.DELETE, pitch = 1f + (combo.coerceAtMost(30) * 0.015f))
    }

    fun undo() {
        haptic(HapticFeedbackConstants.KEYBOARD_TAP)
        sfx(Sfx.UNDO)
    }

    fun golden() {
        haptic(HapticFeedbackConstants.LONG_PRESS)
        sfx(Sfx.COIN, volume = 0.6f)
    }

    fun comboUp(multiplier: Int) {
        haptic(HapticFeedbackConstants.VIRTUAL_KEY)
        sfx(Sfx.COMBO, pitch = 0.9f + multiplier * 0.1f)
    }

    fun reward() {
        haptic(HapticFeedbackConstants.LONG_PRESS)
        sfx(Sfx.CHEST)
    }

    fun levelUp() {
        haptic(HapticFeedbackConstants.LONG_PRESS)
        sfx(Sfx.LEVEL, volume = 0.6f)
    }

    fun boom() {
        haptic(HapticFeedbackConstants.LONG_PRESS)
        sfx(Sfx.BOOM, volume = 0.7f)
    }

    fun tap() = haptic(HapticFeedbackConstants.KEYBOARD_TAP)
}

val LocalFeedback = staticCompositionLocalOf<Feedback?> { null }
