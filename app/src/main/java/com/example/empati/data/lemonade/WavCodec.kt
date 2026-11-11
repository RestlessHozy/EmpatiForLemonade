package com.example.empati.data.lemonade

/**
 * Pembungkus PCM16 menjadi kontainer WAV 44 byte. Lemonade mendokumentasikan WAV
 * sebagai format file untuk `/v1/audio/transcriptions`, sedangkan `AudioRecord`
 * menghasilkan PCM mentah.
 */
object WavCodec {

    fun wrapPcm16(samples: ShortArray, sampleRate: Int = 16_000, channels: Int = 1): ByteArray {
        val dataSize = samples.size * 2
        val out = ByteArray(44 + dataSize)
        writeAscii(out, 0, "RIFF")
        writeInt(out, 4, 36 + dataSize)
        writeAscii(out, 8, "WAVE")
        writeAscii(out, 12, "fmt ")
        writeInt(out, 16, 16)
        writeShort(out, 20, 1)
        writeShort(out, 22, channels)
        writeInt(out, 24, sampleRate)
        writeInt(out, 28, sampleRate * channels * 2)
        writeShort(out, 32, channels * 2)
        writeShort(out, 34, 16)
        writeAscii(out, 36, "data")
        writeInt(out, 40, dataSize)

        var offset = 44
        for (sample in samples) {
            out[offset] = (sample.toInt() and 0xFF).toByte()
            out[offset + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
            offset += 2
        }
        return out
    }

    private fun writeAscii(out: ByteArray, offset: Int, value: String) {
        for (i in value.indices) out[offset + i] = value[i].code.toByte()
    }

    private fun writeInt(out: ByteArray, offset: Int, value: Int) {
        out[offset] = (value and 0xFF).toByte()
        out[offset + 1] = ((value shr 8) and 0xFF).toByte()
        out[offset + 2] = ((value shr 16) and 0xFF).toByte()
        out[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShort(out: ByteArray, offset: Int, value: Int) {
        out[offset] = (value and 0xFF).toByte()
        out[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }
}
