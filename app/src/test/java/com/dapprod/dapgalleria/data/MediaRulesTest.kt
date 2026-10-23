package com.dapprod.dapgalleria.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaRulesTest {

    @Test
    fun `i file ts non sono video da mostrare`() {
        assertTrue(MediaRules.isHiddenVideo("app.ts", "video/vnd.dlna.mpeg-tts"))
        assertTrue(MediaRules.isHiddenVideo("index.TS", null))
        assertTrue(MediaRules.isHiddenVideo("clip", "video/mp2t"))
        assertFalse(MediaRules.isHiddenVideo("VID_2024.mp4", "video/mp4"))
        assertFalse(MediaRules.isHiddenVideo("lots.mov", "video/quicktime"))
    }

    @Test
    fun `le copie identiche diventano una carta sola`() {
        val twins = MediaRules.twins(
            listOf(
                "a" to "P:100:10x10:0",
                "b" to "P:200:10x10:0",
                "c" to "P:100:10x10:0",
                "d" to "P:100:10x10:0",
            ),
        )
        assertEquals(mapOf("a" to listOf("c", "d")), twins)
    }

    @Test
    fun `senza firma non si cercano copie`() {
        val twins = MediaRules.twins(listOf("a" to null, "b" to null))
        assertTrue(twins.isEmpty())
    }

    @Test
    fun `la firma richiede dati sicuri`() {
        assertNull(MediaRules.signature(MediaType.PHOTO, 0, 10, 10, 0))
        assertNull(MediaRules.signature(MediaType.PHOTO, 100, 0, 10, 0))
        assertEquals(
            MediaRules.signature(MediaType.VIDEO, 100, 10, 20, 5000),
            MediaRules.signature(MediaType.VIDEO, 100, 10, 20, 5000),
        )
        // stesso peso e dimensioni ma una foto e un video non sono la stessa cosa
        assertFalse(
            MediaRules.signature(MediaType.PHOTO, 100, 10, 20, 0) == MediaRules.signature(MediaType.VIDEO, 100, 10, 20, 0),
        )
    }
}
