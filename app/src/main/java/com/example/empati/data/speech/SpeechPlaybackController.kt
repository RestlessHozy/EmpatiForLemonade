package com.example.empati.data.speech

sealed interface SpeechPlaybackState {
    data object Idle : SpeechPlaybackState
    data class Preparing(val messageId: String) : SpeechPlaybackState
    data class Playing(val messageId: String) : SpeechPlaybackState
    data class Error(val messageId: String, val message: String) : SpeechPlaybackState
}

interface SpeechAudioStore {
    suspend fun exists(fileName: String): Boolean
    suspend fun write(fileName: String, bytes: ByteArray)
    fun pathOf(fileName: String): String
}

interface SpeechAudioPlayer {
    fun play(filePath: String, onCompleted: () -> Unit)

    /** Pemutar yang bisa melaporkan kegagalan dekode menimpa ini. Bawaan: kegagalan dianggap selesai. */
    fun play(filePath: String, onCompleted: () -> Unit, onError: () -> Unit) = play(filePath, onCompleted)

    fun stop()
}

/**
 * Mengatur pemutaran suara balasan. Audio diminta sekali per teks, disimpan di
 * [store], lalu diputar ulang dari berkas yang sama tanpa memanggil server lagi.
 * Hanya satu pesan yang boleh berbunyi pada satu waktu.
 */
class SpeechPlaybackController(
    private val synthesize: suspend (String) -> Result<ByteArray>,
    private val store: SpeechAudioStore,
    private val player: SpeechAudioPlayer,
    private val voice: String = DEFAULT_VOICE,
    private val onStateChange: (SpeechPlaybackState) -> Unit = {}
) {

    var state: SpeechPlaybackState = SpeechPlaybackState.Idle
        private set(value) {
            field = value
            onStateChange(value)
        }

    private var requestToken = 0

    suspend fun toggle(messageId: String, text: String): SpeechPlaybackState {
        when (val current = state) {
            is SpeechPlaybackState.Playing ->
                if (current.messageId == messageId) return stop()

            is SpeechPlaybackState.Preparing ->
                if (current.messageId == messageId) return stop()

            else -> Unit
        }

        val token = ++requestToken
        player.stop()
        state = SpeechPlaybackState.Idle

        val prepared = SpeechTextPreparer.prepare(text)
        if (prepared.isBlank()) return state

        val fileName = SpeechCacheKey.fileName(prepared, voice)
        if (!store.exists(fileName)) {
            state = SpeechPlaybackState.Preparing(messageId)
            val audio = synthesize(prepared).getOrElse {
                if (token == requestToken) {
                    state = SpeechPlaybackState.Error(
                        messageId,
                        "Suara balasan belum bisa disiapkan. Periksa server Lemonade lalu coba lagi."
                    )
                }
                return state
            }
            if (token != requestToken) return state
            store.write(fileName, audio)
        }
        if (token != requestToken) return state

        state = SpeechPlaybackState.Playing(messageId)
        runCatching {
            player.play(
                filePath = store.pathOf(fileName),
                onCompleted = { finish(messageId) },
                onError = { fail(messageId) }
            )
        }
            .onFailure {
                state = SpeechPlaybackState.Error(
                    messageId,
                    "Suara balasan tidak bisa diputar di perangkat ini."
                )
            }
        return state
    }

    fun stop(): SpeechPlaybackState {
        requestToken++
        player.stop()
        state = SpeechPlaybackState.Idle
        return state
    }

    private fun fail(messageId: String) {
        if (state == SpeechPlaybackState.Playing(messageId)) {
            state = SpeechPlaybackState.Error(
                messageId,
                "Suara balasan tidak bisa diputar di perangkat ini."
            )
        }
    }

    private fun finish(messageId: String) {
        if (state == SpeechPlaybackState.Playing(messageId)) {
            state = SpeechPlaybackState.Idle
        }
    }

    companion object {
        const val DEFAULT_VOICE = "af_sky"
    }
}
