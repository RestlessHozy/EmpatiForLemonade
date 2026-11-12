package com.example.empati.data.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechCacheKeyTest {

    @Test
    fun `same text and voice produce the same key`() {
        val a = SpeechCacheKey.of("Hasil skrining Anda sedang.", "af_sky")
        val b = SpeechCacheKey.of("Hasil skrining Anda sedang.", "af_sky")

        assertEquals(a, b)
        assertTrue(a.matches(Regex("[0-9a-f]{8}")))
    }

    @Test
    fun `voice changes the key`() {
        assertNotEquals(
            SpeechCacheKey.of("teks", "af_sky"),
            SpeechCacheKey.of("teks", "am_echo")
        )
    }

    @Test
    fun `file name is stable and ends with mp3`() {
        val name = SpeechCacheKey.fileName("teks", "af_sky")

        assertTrue(name.startsWith("empati-tts-"))
        assertTrue(name.endsWith(".mp3"))
        assertEquals(name, SpeechCacheKey.fileName("teks", "af_sky"))
    }

    @Test
    fun `preparer strips markdown and collapses whitespace`() {
        val prepared = SpeechTextPreparer.prepare("  **Kurangi garam**   dan\n\njalan kaki.  ")

        assertEquals("Kurangi garam dan jalan kaki.", prepared)
    }

    @Test
    fun `preparer keeps the crisis contact digits intact`() {
        val prepared = SpeechTextPreparer.prepare(
            "Hubungi SAPA 129 atau WhatsApp 08111-129-129."
        )

        assertTrue(prepared.contains("129"))
        assertTrue(prepared.contains("08111-129-129"))
    }

    @Test
    fun `preparer refuses empty input`() {
        assertEquals("", SpeechTextPreparer.prepare("   **  **  "))
        assertEquals("", SpeechTextPreparer.prepare(""))
    }
}
