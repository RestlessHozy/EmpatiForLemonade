package com.example.empati.data.lemonade

import com.example.empati.BuildConfig

/**
 * Konfigurasi koneksi Lemonade. Alias dipakai sebagai kontrak supaya nama model
 * tidak pernah masuk ke kode aplikasi; model di balik alias diganti di server.
 *
 * Host berasal dari `LEMONADE_BASE_URL` di `local.properties` atau variabel
 * lingkungan, sehingga mesin demo bisa memakai IP LAN tanpa mengubah kode.
 */
object LemonadeConfig {

    const val DEFAULT_HOST = "10.0.2.2"
    const val DEFAULT_PORT = 13305

    val defaultBaseUrl: String = normalizeBaseUrl(
        runCatching { BuildConfig.LEMONADE_BASE_URL }.getOrDefault("")
    )

    const val ALIAS_LLM = "empati-llm"
    const val ALIAS_EMBED = "empati-embed"
    const val ALIAS_STT = "empati-stt"
    const val ALIAS_TTS = "empati-tts"

    val REQUIRED_ALIASES = listOf(ALIAS_LLM, ALIAS_EMBED, ALIAS_STT, ALIAS_TTS)

    /** Nilai awal `10.0.2.2` adalah alias loopback host dari emulator Android. */
    fun normalizeBaseUrl(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        val withScheme = when {
            trimmed.isEmpty() -> "http://$DEFAULT_HOST:$DEFAULT_PORT"
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "http://$trimmed"
        }
        return withScheme.trimEnd('/')
    }
}
