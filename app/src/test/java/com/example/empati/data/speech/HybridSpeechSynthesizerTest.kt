package com.example.empati.data.speech

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridSpeechSynthesizerTest {

    @Test
    fun `prefers offline indonesian voice and skips lemonade fallback when available`() = runBlocking {
        var lemonadeCalled = false
        val wavBytes = "RIFF....WAVE".encodeToByteArray()
        val hybrid = HybridSpeechSynthesizer(
            indonesianEngine = { Result.success(wavBytes) },
            lemonadeFallback = {
                lemonadeCalled = true
                Result.success(byteArrayOf(9, 9))
            }
        )

        val outcome = hybrid.synthesizeWithSource("Tarik napas perlahan.").getOrThrow()

        assertEquals(SpeechSynthesisSource.ANDROID_INDONESIAN_OFFLINE, outcome.source)
        assertArrayEquals(wavBytes, outcome.bytes)
        assertEquals(false, lemonadeCalled)
    }

    @Test
    fun `falls back to lemonade empati-tts when offline indonesian tts is missing`() = runBlocking {
        val mp3Bytes = byteArrayOf(0x49, 0x44, 0x33)
        val hybrid = HybridSpeechSynthesizer(
            indonesianEngine = { Result.failure(IllegalStateException("id-ID tidak terpasang")) },
            lemonadeFallback = { Result.success(mp3Bytes) }
        )

        val outcome = hybrid.synthesizeWithSource("Tarik napas perlahan.").getOrThrow()

        assertEquals(SpeechSynthesisSource.LEMONADE_SERVER, outcome.source)
        assertArrayEquals(mp3Bytes, outcome.bytes)
    }

    @Test
    fun `returns failure when both indonesian tts and lemonade fail`() = runBlocking {
        val hybrid = HybridSpeechSynthesizer(
            indonesianEngine = { Result.failure(IllegalStateException("local missing")) },
            lemonadeFallback = { Result.failure(IllegalStateException("server down")) }
        )

        val result = hybrid.synthesize("Halo")

        assertTrue(result.isFailure)
    }

    @Test
    fun `selectBestIndonesianVoice ignores english network and uninstalled voices`() {
        val candidates = listOf(
            LocalTtsVoiceCandidate("en-us-x-sfg-local", "en", "US", isNetworkRequired = false, isInstalled = true, quality = 500),
            LocalTtsVoiceCandidate("id-id-x-idd-network", "id", "ID", isNetworkRequired = true, isInstalled = true, quality = 500),
            LocalTtsVoiceCandidate("in-id-not-installed", "in", "ID", isNetworkRequired = false, isInstalled = false, quality = 500),
            LocalTtsVoiceCandidate("in-id-x-low", "in", "ID", isNetworkRequired = false, isInstalled = true, quality = 300),
            LocalTtsVoiceCandidate("id-id-x-high", "id", "ID", isNetworkRequired = false, isInstalled = true, quality = 400)
        )

        val selected = selectBestIndonesianVoice(candidates)

        assertEquals("id-id-x-high", selected?.name)
    }

    @Test
    fun `selectBestIndonesianVoice returns null when only network indonesian exists`() {
        val candidates = listOf(
            LocalTtsVoiceCandidate("id-id-x-idd-network", "id", "ID", isNetworkRequired = true, isInstalled = true, quality = 500)
        )

        assertNull(selectBestIndonesianVoice(candidates))
    }
}

