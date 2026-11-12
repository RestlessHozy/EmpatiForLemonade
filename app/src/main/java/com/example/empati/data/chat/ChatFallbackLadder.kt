package com.example.empati.data.chat

enum class ChatRoute { LEMONADE, CLOUD, OFFLINE }

data class ChatOutcome(val text: String, val route: ChatRoute)

/**
 * Urutan jalur jawaban: Lemonade lokal, lalu cloud bila diizinkan, lalu teks
 * offline. Setiap jalur dibungkus supaya kegagalan satu langkah tidak
 * menghentikan langkah berikutnya.
 */
class ChatFallbackLadder(
    private val lemonade: () -> String,
    private val cloud: (() -> String)? = null,
    private val offline: () -> String = { DEFAULT_OFFLINE_TEXT }
) {

    fun answer(): ChatOutcome {
        attempt(lemonade)?.let { return ChatOutcome(it, ChatRoute.LEMONADE) }
        if (cloud != null) {
            attempt(cloud)?.let { return ChatOutcome(it, ChatRoute.CLOUD) }
        }
        return ChatOutcome(offline(), ChatRoute.OFFLINE)
    }

    private fun attempt(block: () -> String): String? =
        runCatching { block() }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        const val DEFAULT_OFFLINE_TEXT =
            "Jawaban lengkap belum bisa disusun karena model lokal belum tersambung. " +
                "Saya tetap bisa membantu membaca hasil skrining Anda di menu riwayat, " +
                "dan saran umum tetap berlaku: jaga asupan garam, cukup tidur, dan periksa " +
                "tekanan darah secara berkala."
    }
}
