package com.example.empati.data.speech

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * Perekam PCM16 mono 16 kHz di atas [AudioRecord]. Sampel dikumpulkan di latar
 * belakang supaya UI tidak tersendat, lalu diserahkan ke
 * [VoiceInputController] saat rekaman berhenti.
 */
class AndroidVoiceRecorder : VoiceRecorder {

    private var record: AudioRecord? = null
    private var reader: Thread? = null

    @Volatile
    private var buffer = ShortArray(SAMPLE_RATE)

    @Volatile
    private var length = 0

    @SuppressLint("MissingPermission")
    override fun start() {
        release()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        require(minBuffer > 0) { "Mikrofon tidak mendukung rekaman 16 kHz mono." }

        val audio = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(format)
            .setBufferSizeInBytes(maxOf(minBuffer, SAMPLE_RATE))
            .build()
        if (audio.state != AudioRecord.STATE_INITIALIZED) {
            audio.release()
            throw IllegalStateException("Mikrofon tidak siap dipakai.")
        }

        buffer = ShortArray(SAMPLE_RATE * 2)
        length = 0
        record = audio
        audio.startRecording()
        reader = Thread({ readUntilStopped(audio) }, "empati-voice-recorder").apply { start() }
    }

    override fun stop(): ShortArray {
        val audio = record
        runCatching { audio?.stop() }
        val thread = reader
        thread?.interrupt()
        thread?.join(JOIN_TIMEOUT_MS)
        reader = null
        record = null
        audio?.release()
        return snapshot()
    }

    fun release() {
        runCatching { stop() }
    }

    private fun readUntilStopped(audio: AudioRecord) {
        val chunk = ShortArray(CHUNK_SAMPLES)
        while (audio.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val read = audio.read(chunk, 0, chunk.size)
            if (read > 0) append(chunk, read)
        }
    }

    @Synchronized
    private fun append(source: ShortArray, count: Int) {
        val room = VoiceInputController.MAX_SAMPLES - length
        if (room <= 0) return
        val taken = minOf(count, room)
        if (length + taken > buffer.size) {
            buffer = buffer.copyOf(maxOf(buffer.size * 2, length + taken))
        }
        source.copyInto(buffer, destinationOffset = length, startIndex = 0, endIndex = taken)
        length += taken
    }

    @Synchronized
    private fun snapshot(): ShortArray = buffer.copyOf(length)

    private companion object {
        const val SAMPLE_RATE = VoiceInputController.SAMPLE_RATE
        const val CHUNK_SAMPLES = 2_048
        const val JOIN_TIMEOUT_MS = 500L
    }
}
