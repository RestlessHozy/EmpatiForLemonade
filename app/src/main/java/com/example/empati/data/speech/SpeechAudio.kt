package com.example.empati.data.speech

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File

/**
 * Menyimpan audio balasan sebagai berkas mp3 di cache internal aplikasi.
 * Nama berkas berasal dari [SpeechCacheKey], jadi teks yang sama selalu
 * memakai berkas yang sama.
 */
class FileSpeechAudioStore(cacheDir: File) : SpeechAudioStore {

    private val directory = File(cacheDir, "empati-tts").apply { mkdirs() }

    override fun exists(fileName: String): Boolean {
        val file = file(fileName)
        return file.isFile && file.length() > 0
    }

    override fun read(fileName: String): ByteArray? = file(fileName).takeIf { it.isFile }?.readBytes()

    override fun write(fileName: String, bytes: ByteArray) {
        file(fileName).writeBytes(bytes)
    }

    override fun pathOf(fileName: String): String = file(fileName).absolutePath

    private fun file(fileName: String): File = File(directory, fileName)
}

/**
 * Pemutar audio platform. Penyiapan dilakukan asinkron supaya UI tidak
 * tertahan saat berkas dibuka, dan hanya satu berkas yang berbunyi.
 */
class MediaPlayerSpeechAudioPlayer : SpeechAudioPlayer {

    private var player: MediaPlayer? = null

    override fun play(filePath: String, onCompleted: () -> Unit) {
        stop()
        val media = MediaPlayer()
        media.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        media.setDataSource(filePath)
        media.setOnPreparedListener { it.start() }
        media.setOnCompletionListener {
            onCompleted()
            it.release()
            if (player === it) player = null
        }
        media.setOnErrorListener { failed, _, _ ->
            onCompleted()
            failed.release()
            if (player === failed) player = null
            true
        }
        player = media
        runCatching { media.prepareAsync() }.onFailure {
            media.release()
            if (player === media) player = null
            onCompleted()
        }
    }

    override fun stop() {
        player?.let { media ->
            runCatching { media.stop() }
            media.release()
        }
        player = null
    }

    fun release() {
        stop()
    }
}
