package com.example.empati.data.speech

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceInputControllerTest {

    private class FakeRecorder(private val samples: ShortArray = shortArrayOf(120, -240, 480)) : VoiceRecorder {
        var starts = 0
        var stops = 0

        override fun start() {
            starts++
        }

        override fun stop(): ShortArray {
            stops++
            return samples
        }
    }

    private class RecordingTranscriber(private val result: Result<String>) {
        val wavUploads = mutableListOf<ByteArray>()

        suspend fun transcribe(wav: ByteArray): Result<String> {
            wavUploads += wav
            return result
        }
    }

    private fun controller(
        recorder: FakeRecorder = FakeRecorder(),
        transcriber: RecordingTranscriber = RecordingTranscriber(Result.success("Saya pusing sejak kemarin.")),
        clock: () -> Long = { 1_000L }
    ) = VoiceInputController(
        recorder = recorder,
        transcribe = transcriber::transcribe,
        nowMs = clock
    )

    private fun VoiceInputController.startRecording() {
        requestPermission()
        onPermissionResult(granted = true)
    }

    private fun intAt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun shortAt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    @Test
    fun `granted permission starts a recording`() {
        val recorder = FakeRecorder()
        val controller = controller(recorder = recorder)

        controller.startRecording()

        assertEquals(1, recorder.starts)
        assertTrue(controller.state is VoiceInputState.Recording)
    }

    @Test
    fun `denied permission never starts the recorder`() {
        val recorder = FakeRecorder()
        val controller = controller(recorder = recorder)

        controller.requestPermission()
        controller.onPermissionResult(granted = false)

        assertEquals(0, recorder.starts)
        assertEquals(VoiceInputState.PermissionDenied, controller.state)
    }

    @Test
    fun `finishing sends a 16 kHz mono wav and keeps the transcript`() = runBlocking {
        val transcriber = RecordingTranscriber(Result.success("Saya pusing sejak kemarin."))
        val controller = controller(transcriber = transcriber)

        controller.startRecording()
        controller.stop()

        assertEquals(VoiceInputState.Ready("Saya pusing sejak kemarin."), controller.state)
        val wav = transcriber.wavUploads.single()
        assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))
        assertEquals(1, shortAt(wav, 22))
        assertEquals(16_000, intAt(wav, 24))
        assertEquals(16, shortAt(wav, 34))
        assertEquals(6, intAt(wav, 40))
    }

    @Test
    fun `every transition is published before the next step runs`() = runBlocking {
        val seen = mutableListOf<VoiceInputState>()
        val controller = VoiceInputController(
            recorder = FakeRecorder(),
            transcribe = { Result.success("selesai") },
            nowMs = { 0L },
            onStateChange = { seen += it }
        )

        controller.startRecording()
        controller.stop()

        assertEquals(
            listOf(
                VoiceInputState.RequestingPermission,
                VoiceInputState.Recording(startedAtMs = 0),
                VoiceInputState.Transcribing(timedOut = false),
                VoiceInputState.Ready("selesai")
            ),
            seen
        )
    }

    @Test
    fun `cancel discards the audio without transcribing`() = runBlocking {
        val recorder = FakeRecorder()
        val transcriber = RecordingTranscriber(Result.success("tidak boleh dipakai"))
        val controller = controller(recorder = recorder, transcriber = transcriber)

        controller.startRecording()
        controller.cancel()

        assertEquals(1, recorder.stops)
        assertTrue(transcriber.wavUploads.isEmpty())
        assertEquals(VoiceInputState.Cancelled, controller.state)
    }

    @Test
    fun `cancelling while the permission prompt is open returns to idle`() {
        val recorder = FakeRecorder()
        val controller = controller(recorder = recorder)

        controller.requestPermission()
        controller.cancel()

        assertEquals(VoiceInputState.Idle, controller.state)
        assertEquals(0, recorder.starts)
    }

    @Test
    fun `recording transcribes itself at the sixty second limit`() = runBlocking {
        var now = 5_000L
        val recorder = FakeRecorder()
        val transcriber = RecordingTranscriber(Result.success("cerita panjang"))
        val controller = controller(recorder = recorder, transcriber = transcriber) { now }

        controller.startRecording()
        now = 64_999L
        controller.onTick()
        assertTrue("still recording before the limit", controller.state is VoiceInputState.Recording)

        now = 65_000L
        controller.onTick()

        assertEquals(1, recorder.stops)
        assertEquals(1, transcriber.wavUploads.size)
        assertEquals(VoiceInputState.Ready("cerita panjang"), controller.state)
    }

    @Test
    fun `timeout is visible while the audio is processing`() = runBlocking {
        var now = 0L
        lateinit var controller: VoiceInputController
        val transcriber = RecordingTranscriber(Result.success("selesai"))
        controller = VoiceInputController(
            recorder = FakeRecorder(),
            transcribe = { wav ->
                assertEquals(VoiceInputState.Transcribing(timedOut = true), controller.state)
                transcriber.transcribe(wav)
            },
            nowMs = { now }
        )

        controller.startRecording()
        now = 60_000L
        controller.onTick()

        assertEquals(VoiceInputState.Ready("selesai"), controller.state)
    }

    @Test
    fun `audio beyond the sixty second limit is truncated before upload`() = runBlocking {
        val samples = ShortArray(VoiceInputController.MAX_SAMPLES + 1_000)
        val transcriber = RecordingTranscriber(Result.success("dipotong"))
        val controller = controller(recorder = FakeRecorder(samples), transcriber = transcriber)

        controller.startRecording()
        controller.stop()

        val wav = transcriber.wavUploads.single()
        assertEquals(VoiceInputController.MAX_SAMPLES * 2, intAt(wav, 40))
    }

    @Test
    fun `transcription failure becomes a calm error state`() = runBlocking {
        val transcriber = RecordingTranscriber(Result.failure(IllegalStateException("server mati")))
        val controller = controller(transcriber = transcriber)

        controller.startRecording()
        controller.stop()

        assertEquals(
            VoiceInputState.Error("Transkripsi gagal. Periksa koneksi ke server lalu coba lagi."),
            controller.state
        )
    }

    @Test
    fun `empty recording is rejected before it reaches the server`() = runBlocking {
        val transcriber = RecordingTranscriber(Result.success("tidak mungkin"))
        val controller = controller(recorder = FakeRecorder(ShortArray(0)), transcriber = transcriber)

        controller.startRecording()
        controller.stop()

        assertTrue(transcriber.wavUploads.isEmpty())
        assertTrue(controller.state is VoiceInputState.Error)
    }

    @Test
    fun `a blank transcript asks the user to try again`() = runBlocking {
        val controller = controller(transcriber = RecordingTranscriber(Result.success("   ")))

        controller.startRecording()
        controller.stop()

        val state = controller.state
        assertTrue(state is VoiceInputState.Error)
        assertFalse((state as VoiceInputState.Error).message.isBlank())
    }

    @Test
    fun `reset returns to idle after a ready transcript`() = runBlocking {
        val controller = controller()

        controller.startRecording()
        controller.stop()
        controller.reset()

        assertEquals(VoiceInputState.Idle, controller.state)
    }
}
