package com.example.empati.data.speech

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechPlaybackControllerTest {

    private class FakeStore : SpeechAudioStore {
        val files = mutableMapOf<String, ByteArray>()

        override fun exists(fileName: String): Boolean = files.containsKey(fileName)
        override fun read(fileName: String): ByteArray? = files[fileName]
        override fun write(fileName: String, bytes: ByteArray) {
            files[fileName] = bytes
        }

        override fun pathOf(fileName: String): String = "/cache/$fileName"
    }

    private class FakePlayer : SpeechAudioPlayer {
        val played = mutableListOf<String>()
        var stops = 0
        private var completion: (() -> Unit)? = null

        override fun play(filePath: String, onCompleted: () -> Unit) {
            played += filePath
            completion = onCompleted
        }

        override fun stop() {
            stops++
            completion = null
        }

        fun finishPlayback() {
            completion?.invoke()
        }
    }

    private class FakeSynthesizer(
        private val result: Result<ByteArray> = Result.success(byteArrayOf(1, 2, 3))
    ) {
        val texts = mutableListOf<String>()

        suspend fun synthesize(text: String): Result<ByteArray> {
            texts += text
            return result
        }
    }

    private fun controller(
        synthesizer: FakeSynthesizer = FakeSynthesizer(),
        store: FakeStore = FakeStore(),
        player: FakePlayer = FakePlayer()
    ) = SpeechPlaybackController(
        synthesize = synthesizer::synthesize,
        store = store,
        player = player,
        voice = "af_sky"
    )

    @Test
    fun `first play synthesizes a cleaned script caches it and starts playback`() = runBlocking {
        val synthesizer = FakeSynthesizer()
        val store = FakeStore()
        val player = FakePlayer()
        val controller = controller(synthesizer, store, player)

        controller.toggle("msg-1", "  **Kurangi garam**   dan jalan kaki.  ")

        assertEquals(listOf("Kurangi garam dan jalan kaki."), synthesizer.texts)
        val fileName = SpeechCacheKey.fileName("Kurangi garam dan jalan kaki.", "af_sky")
        assertTrue(store.files.containsKey(fileName))
        assertEquals(listOf("/cache/$fileName"), player.played)
        assertEquals(SpeechPlaybackState.Playing("msg-1"), controller.state)
    }

    @Test
    fun `replaying the same message never asks the server again`() = runBlocking {
        val synthesizer = FakeSynthesizer()
        val player = FakePlayer()
        val controller = controller(synthesizer = synthesizer, player = player)

        controller.toggle("msg-1", "Hasil skrining Anda sedang.")
        controller.toggle("msg-1", "Hasil skrining Anda sedang.")
        controller.toggle("msg-1", "Hasil skrining Anda sedang.")

        assertEquals(1, synthesizer.texts.size)
        assertEquals(2, player.played.size)
        assertEquals(SpeechPlaybackState.Playing("msg-1"), controller.state)
    }

    @Test
    fun `tapping the playing message stops the audio`() = runBlocking {
        val player = FakePlayer()
        val controller = controller(player = player)

        controller.toggle("msg-1", "Teks balasan.")
        controller.toggle("msg-1", "Teks balasan.")

        assertTrue(player.stops >= 1)
        assertEquals(SpeechPlaybackState.Idle, controller.state)
    }

    @Test
    fun `playing another message stops the previous audio`() = runBlocking {
        val player = FakePlayer()
        val controller = controller(player = player)

        controller.toggle("msg-1", "Balasan pertama.")
        controller.toggle("msg-2", "Balasan kedua.")

        assertTrue(player.stops >= 1)
        assertEquals(SpeechPlaybackState.Playing("msg-2"), controller.state)
        assertEquals(2, player.played.size)
    }

    @Test
    fun `playback completion returns to idle`() = runBlocking {
        val player = FakePlayer()
        val controller = controller(player = player)

        controller.toggle("msg-1", "Teks balasan.")
        player.finishPlayback()

        assertEquals(SpeechPlaybackState.Idle, controller.state)
    }

    @Test
    fun `synthesis failure shows a clear error and plays nothing`() = runBlocking {
        val player = FakePlayer()
        val store = FakeStore()
        val controller = controller(
            synthesizer = FakeSynthesizer(Result.failure(IllegalStateException("server mati"))),
            store = store,
            player = player
        )

        controller.toggle("msg-1", "Teks balasan.")

        val state = controller.state
        assertTrue(state is SpeechPlaybackState.Error)
        assertEquals("msg-1", (state as SpeechPlaybackState.Error).messageId)
        assertTrue(state.message.isNotBlank())
        assertFalse(state.message.contains("server mati"))
        assertTrue(store.files.isEmpty())
        assertTrue(player.played.isEmpty())
    }

    @Test
    fun `empty script never reaches the server`() = runBlocking {
        val synthesizer = FakeSynthesizer()
        val player = FakePlayer()
        val controller = controller(synthesizer = synthesizer, player = player)

        controller.toggle("msg-1", "   **   **  ")

        assertTrue(synthesizer.texts.isEmpty())
        assertTrue(player.played.isEmpty())
        assertEquals(SpeechPlaybackState.Idle, controller.state)
    }
}
