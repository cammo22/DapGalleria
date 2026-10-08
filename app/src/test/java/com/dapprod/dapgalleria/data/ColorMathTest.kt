package com.dapprod.dapgalleria.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathTest {

    @Test
    fun identitaNonCambiaNiente() {
        val c = ColorMath.apply(ColorMath.identity(), 10f, 120f, 240f)
        assertArrayEquals(floatArrayOf(10f, 120f, 240f, 255f), c, 0.001f)
    }

    @Test
    fun concatApplicaInOrdine() {
        val m = ColorMath.concat(ColorMath.brightness(1f), ColorMath.contrast(0.5f))
        val direct = ColorMath.apply(ColorMath.contrast(0.5f), ColorMath.apply(ColorMath.brightness(1f), 100f, 100f, 100f).let { it[0] }, 100f + 0.3f * 255f, 100f + 0.3f * 255f)
        val combined = ColorMath.apply(m, 100f, 100f, 100f)
        assertEquals(direct[1], combined[1], 0.01f)
    }

    @Test
    fun biancoENeroToglieIlColore() {
        val c = ColorMath.apply(ColorMath.saturation(-1f), 255f, 0f, 0f)
        assertEquals(c[0], c[1], 0.01f)
        assertEquals(c[1], c[2], 0.01f)
    }

    @Test
    fun caldoAlzaIlRossoEAbbassaIlBlu() {
        val c = ColorMath.apply(ColorMath.warmth(1f), 128f, 128f, 128f)
        assertTrue(c[0] > 128f)
        assertTrue(c[2] < 128f)
    }

    @Test
    fun regolazioniNeutreSonoIdentita() {
        val m = Adjustments().matrix(PhotoFilter.NONE, 1f)
        assertArrayEquals(ColorMath.identity(), m, 0.0001f)
        assertTrue(Adjustments().isNeutral)
    }

    @Test
    fun intensitaZeroIgnoraIlFiltro() {
        PhotoFilter.entries.forEach { f ->
            assertArrayEquals(f.name, ColorMath.identity(), Adjustments().matrix(f, 0f), 0.0001f)
        }
    }
}
