package com.example.empati.data.lemonade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WavCodecTest {

    @Test
    fun `wraps pcm16 into a valid mono 16k wav container`() {
        val samples = shortArrayOf(0, 100, -100, 32000)
        val wav = WavCodec.wrapPcm16(samples, sampleRate = 16_000)

        assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
        assertEquals(36 + samples.size * 2, readInt(wav, 4))
        assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))
        assertEquals("fmt ", String(wav, 12, 4, Charsets.US_ASCII))
        assertEquals(16, readInt(wav, 16))
        assertEquals(1, readShort(wav, 20))
        assertEquals(1, readShort(wav, 22))
        assertEquals(16_000, readInt(wav, 24))
        assertEquals(32_000, readInt(wav, 28))
        assertEquals(2, readShort(wav, 32))
        assertEquals(16, readShort(wav, 34))
        assertEquals("data", String(wav, 36, 4, Charsets.US_ASCII))
        assertEquals(samples.size * 2, readInt(wav, 40))
        assertEquals(44 + samples.size * 2, wav.size)

        assertEquals(0, wav[44].toInt())
        assertEquals(0, wav[45].toInt())
        assertEquals(100, readShort(wav, 46))
        assertEquals(-100, readShort(wav, 48).toShort().toInt())
        assertEquals(32000, readShort(wav, 50))
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun readShort(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
}
