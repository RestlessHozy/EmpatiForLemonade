package com.example.empati.data.speech

/**
 * Kunci cache audio balasan. Memakai hash supaya memutar ulang pesan yang sama
 * tidak memanggil Lemonade lagi, sementara voice yang berbeda tetap disimpan
 * terpisah.
 */
object SpeechCacheKey {

    fun of(text: String, voice: String): String {
        val payload = "$voice\u0000$text"
        var hash = 0x811C9DC5.toInt()
        for (character in payload) {
            hash = hash xor character.code
            hash *= 0x01000193
        }
        return hash.toUInt().toString(16).padStart(8, '0')
    }

    fun fileName(text: String, voice: String): String = "empati-tts-${of(text, voice)}.mp3"
}

/**
 * Membersihkan teks sebelum dikirim ke mesin suara. Penanda markdown akan
 * dibacakan sebagai karakter oleh sebagian mesin TTS, jadi dihapus lebih dulu.
 */
object SpeechTextPreparer {

    fun prepare(text: String): String = text
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        .replace("*", "")
        .replace(Regex("\\s+"), " ")
        .trim()
}
