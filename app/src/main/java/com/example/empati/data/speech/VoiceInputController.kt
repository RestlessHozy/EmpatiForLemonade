package com.example.empati.data.speech

import com.example.empati.data.lemonade.WavCodec

sealed interface VoiceInputState {
    data object Idle : VoiceInputState
    data object RequestingPermission : VoiceInputState
    data class Recording(val startedAtMs: Long, val elapsedMs: Long = 0) : VoiceInputState
    data class Transcribing(val timedOut: Boolean = false) : VoiceInputState
    data class Ready(val text: String) : VoiceInputState
    data object Cancelled : VoiceInputState
    data object PermissionDenied : VoiceInputState
    data class Error(val message: String) : VoiceInputState
}

interface VoiceRecorder {
    fun start()
    fun stop(): ShortArray
}

/**
 * State machine rekam suara tanpa dependensi Android. Pemanggil menyediakan
 * perekam, fungsi transkripsi, dan jam, sehingga seluruh transisi bisa diuji
 * dengan fake tanpa mikrofon atau server. Rekaman dibatasi [MAX_DURATION_MS]
 * dan dibungkus WAV 16 kHz mono sebelum dikirim ke [transcribe].
 */
class VoiceInputController(
    private val recorder: VoiceRecorder,
    private val transcribe: suspend (ByteArray) -> Result<String>,
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000 },
    private val onStateChange: (VoiceInputState) -> Unit = {}
) {

    var state: VoiceInputState = VoiceInputState.Idle
        private set(value) {
            field = value
            onStateChange(value)
        }

    fun requestPermission() {
        state = VoiceInputState.RequestingPermission
    }

    fun onPermissionResult(granted: Boolean) {
        if (!granted) {
            state = VoiceInputState.PermissionDenied
            return
        }
        state = try {
            recorder.start()
            VoiceInputState.Recording(startedAtMs = nowMs())
        } catch (error: Exception) {
            VoiceInputState.Error(
                "Mikrofon belum bisa dinyalakan. Coba lagi atau ketik saja cerita Anda."
            )
        }
    }

    suspend fun stop(): VoiceInputState = finishRecording(timedOut = false)

    suspend fun onTick(): VoiceInputState {
        val current = state as? VoiceInputState.Recording ?: return state
        val elapsed = (nowMs() - current.startedAtMs).coerceAtLeast(0)
        if (elapsed >= MAX_DURATION_MS) return finishRecording(timedOut = true)
        state = current.copy(elapsedMs = elapsed)
        return state
    }

    fun cancel() {
        when (state) {
            is VoiceInputState.Recording -> {
                runCatching { recorder.stop() }
                state = VoiceInputState.Cancelled
            }

            VoiceInputState.RequestingPermission -> state = VoiceInputState.Idle
            else -> Unit
        }
    }

    fun reset() {
        state = VoiceInputState.Idle
    }

    private suspend fun finishRecording(timedOut: Boolean): VoiceInputState {
        if (state !is VoiceInputState.Recording) return state
        val recorded = try {
            recorder.stop()
        } catch (error: Exception) {
            state = VoiceInputState.Error("Rekaman gagal dihentikan. Coba lagi.")
            return state
        }
        state = VoiceInputState.Transcribing(timedOut = timedOut)

        val samples = if (recorded.size > MAX_SAMPLES) recorded.copyOf(MAX_SAMPLES) else recorded
        if (samples.isEmpty()) {
            state = VoiceInputState.Error(
                "Tidak ada suara yang terekam. Coba lagi dan bicara lebih dekat ke mikrofon."
            )
            return state
        }

        val wav = WavCodec.wrapPcm16(samples, SAMPLE_RATE, CHANNELS)
        state = transcribe(wav).fold(
            onSuccess = { text ->
                if (text.isBlank()) {
                    VoiceInputState.Error(
                        "Suara Anda belum terdengar jelas. Coba rekam ulang atau ketik saja."
                    )
                } else {
                    VoiceInputState.Ready(text)
                }
            },
            onFailure = {
                VoiceInputState.Error(
                    "Transkripsi gagal. Periksa koneksi ke server lalu coba lagi."
                )
            }
        )
        return state
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val CHANNELS = 1
        const val MAX_DURATION_SECONDS = 60
        const val MAX_DURATION_MS = MAX_DURATION_SECONDS * 1_000L
        const val MAX_SAMPLES = SAMPLE_RATE * MAX_DURATION_SECONDS
    }
}
