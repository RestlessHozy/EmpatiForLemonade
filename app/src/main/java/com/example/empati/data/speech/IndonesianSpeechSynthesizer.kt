package com.example.empati.data.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * Kontrak penyedia audio ucapan untuk memisahkan logika pemilihan mesin suara
 * berbahasa Indonesia dari implementasi Android agar mudah diuji di JVM.
 */
fun interface SpeechSynthesizer {
    suspend fun synthesize(text: String): Result<ByteArray>
}

enum class SpeechSynthesisSource {
    ANDROID_INDONESIAN_OFFLINE,
    LEMONADE_SERVER
}

data class SpeechSynthesisOutcome(
    val bytes: ByteArray,
    val source: SpeechSynthesisSource
)

internal data class LocalTtsVoiceCandidate(
    val name: String,
    val language: String,
    val country: String,
    val isNetworkRequired: Boolean,
    val isInstalled: Boolean,
    val quality: Int
)

internal fun isIndonesianLocaleLanguage(language: String): Boolean =
    language.equals("id", ignoreCase = true) || language.equals("in", ignoreCase = true)

internal fun selectBestIndonesianVoice(
    candidates: Collection<LocalTtsVoiceCandidate>
): LocalTtsVoiceCandidate? = candidates
    .asSequence()
    .filter { candidate ->
        isIndonesianLocaleLanguage(candidate.language) &&
            candidate.isInstalled &&
            !candidate.isNetworkRequired
    }
    .sortedWith(
        compareByDescending<LocalTtsVoiceCandidate> { it.country.equals("ID", ignoreCase = true) }
            .thenByDescending { it.quality }
            .thenBy { it.name }
    )
    .firstOrNull()

/**
 * Mengutamakan TTS offline berbahasa Indonesia milik perangkat agar pelafalan
 * Indonesia terdengar alami, lalu jatuh kembali ke Lemonade (`empati-tts`)
 * hanya bila perangkat belum memiliki paket suara `id-ID` offline.
 */
class HybridSpeechSynthesizer(
    private val indonesianEngine: SpeechSynthesizer,
    private val lemonadeFallback: SpeechSynthesizer
) : SpeechSynthesizer {

    suspend fun synthesizeWithSource(text: String): Result<SpeechSynthesisOutcome> {
        val local = indonesianEngine.synthesize(text)
        if (local.isSuccess) {
            return Result.success(
                SpeechSynthesisOutcome(
                    bytes = local.getOrThrow(),
                    source = SpeechSynthesisSource.ANDROID_INDONESIAN_OFFLINE
                )
            )
        }
        return lemonadeFallback.synthesize(text).map { bytes ->
            SpeechSynthesisOutcome(
                bytes = bytes,
                source = SpeechSynthesisSource.LEMONADE_SERVER
            )
        }
    }

    override suspend fun synthesize(text: String): Result<ByteArray> =
        synthesizeWithSource(text).map { it.bytes }
}

/**
 * Pembungkus `android.speech.tts.TextToSpeech` dengan lokalisasi `id-ID`
 * offline dan konversi ke berkas WAV agar tetap memakai alur kontrol putar
 * dan cache [SpeechPlaybackController].
 */
class AndroidIndonesianTtsSynthesizer(
    context: Context,
    private val cacheDir: File
) : SpeechSynthesizer {

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var engine: TextToSpeech? = null
    private var engineReady: Boolean = false

    override suspend fun synthesize(text: String): Result<ByteArray> = mutex.withLock {
        val tts = ensureReadyEngine()
            ?: return@withLock Result.failure(IllegalStateException("TTS Android belum siap."))

        if (!configureIndonesianVoice(tts)) {
            return@withLock Result.failure(
                IllegalStateException("Paket suara offline bahasa Indonesia tidak tersedia.")
            )
        }

        val tempFile = withContext(Dispatchers.IO) {
            cacheDir.mkdirs()
            File.createTempFile("empati-id-tts-", ".wav", cacheDir)
        }

        try {
            val utteranceId = "empati-${UUID.randomUUID()}"
            val done = CompletableDeferred<Boolean>()
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit

                override fun onDone(id: String?) {
                    if (id == utteranceId) done.complete(true)
                }

                @Deprecated("Dipertahankan untuk kompatibilitas engine Android lama.")
                override fun onError(id: String?) {
                    if (id == utteranceId) done.complete(false)
                }

                override fun onError(id: String?, errorCode: Int) {
                    if (id == utteranceId) done.complete(false)
                }

                override fun onStop(id: String?, interrupted: Boolean) {
                    if (id == utteranceId) done.complete(false)
                }
            })

            tts.setSpeechRate(INDONESIAN_SPEECH_RATE)
            tts.setPitch(1.0f)

            val queued = withContext(Dispatchers.Main.immediate) {
                tts.synthesizeToFile(text, Bundle(), tempFile, utteranceId)
            }
            if (queued != TextToSpeech.SUCCESS) {
                return@withLock Result.failure(IllegalStateException("Gagal mengantrekan TTS lokal."))
            }

            val completed = withTimeoutOrNull(SYNTHESIS_TIMEOUT_MS) { done.await() } == true
            if (!completed) {
                return@withLock Result.failure(IllegalStateException("Sintesis TTS lokal gagal atau habis waktu."))
            }

            val bytes = withContext(Dispatchers.IO) {
                if (!tempFile.exists() || tempFile.length() <= MIN_WAV_HEADER_BYTES) {
                    ByteArray(0)
                } else {
                    tempFile.readBytes()
                }
            }
            if (bytes.size <= MIN_WAV_HEADER_BYTES) {
                return@withLock Result.failure(IllegalStateException("Keluaran audio TTS lokal kosong."))
            }
            Result.success(bytes)
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            tts.setOnUtteranceProgressListener(null)
            withContext(Dispatchers.IO) { tempFile.delete() }
        }
    }

    fun shutdown() {
        engineReady = false
        val current = engine
        engine = null
        runCatching {
            current?.stop()
            current?.shutdown()
        }
    }

    private suspend fun ensureReadyEngine(): TextToSpeech? {
        val existing = engine
        if (existing != null && engineReady) return existing
        shutdown()

        val initStatus = CompletableDeferred<Int>()
        val created = withContext(Dispatchers.Main.immediate) {
            TextToSpeech(appContext) { status ->
                if (!initStatus.isCompleted) initStatus.complete(status)
            }
        }
        val status = withTimeoutOrNull(INIT_TIMEOUT_MS) { initStatus.await() }
        if (status != TextToSpeech.SUCCESS) {
            runCatching { created.shutdown() }
            return null
        }
        engine = created
        engineReady = true
        return created
    }

    private fun configureIndonesianVoice(tts: TextToSpeech): Boolean {
        val availableVoices = runCatching { tts.voices }.getOrNull().orEmpty()
        val voiceMap = availableVoices.associateBy { it.name }
        val candidates = availableVoices.map { it.toCandidate() }
        val bestVoice = selectBestIndonesianVoice(candidates)?.let { voiceMap[it.name] }
        if (bestVoice != null) {
            val status = tts.setVoice(bestVoice)
            if (status == TextToSpeech.SUCCESS) return true
        }

        val locales = listOf(Locale.forLanguageTag("id-ID"), Locale.forLanguageTag("in-ID"))
        for (locale in locales) {
            val langResult = tts.setLanguage(locale)
            if (langResult >= TextToSpeech.LANG_AVAILABLE) {
                val activeVoice = runCatching { tts.voice }.getOrNull()
                if (activeVoice == null || !activeVoice.isNetworkConnectionRequired) {
                    return true
                }
            }
        }
        return false
    }

    private fun Voice.toCandidate(): LocalTtsVoiceCandidate = LocalTtsVoiceCandidate(
        name = name,
        language = locale?.language.orEmpty(),
        country = locale?.country.orEmpty(),
        isNetworkRequired = isNetworkConnectionRequired,
        isInstalled = !features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED),
        quality = quality
    )

    companion object {
        const val INDONESIAN_VOICE_KEY = "empati-id-tts"
        private const val INDONESIAN_SPEECH_RATE = 0.95f
        private const val INIT_TIMEOUT_MS = 5_000L
        private const val SYNTHESIS_TIMEOUT_MS = 20_000L
        private const val MIN_WAV_HEADER_BYTES = 44L
    }
}
