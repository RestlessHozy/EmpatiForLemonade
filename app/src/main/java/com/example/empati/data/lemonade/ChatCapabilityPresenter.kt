package com.example.empati.data.lemonade

import com.example.empati.data.chat.ChatRoute

enum class ChatCapability { SERVER_MISSING, MODEL_LOADING, MODEL_BUSY }

data class ChatCapabilityNotice(
    val capability: ChatCapability,
    val title: String,
    val detail: String
)

/**
 * Menerjemahkan kegagalan Lemonade menjadi pesan status yang berbeda untuk
 * pengguna: server tidak ada, model sedang dimuat, atau model sedang sibuk.
 * Kegagalan lain atau jawaban yang berhasil tidak menampilkan apa pun.
 */
object ChatCapabilityPresenter {

    fun forFailure(failure: Throwable?, route: ChatRoute? = null): ChatCapabilityNotice? = when (failure) {
        is LemonadeFailure.Unreachable -> ChatCapabilityNotice(
            capability = ChatCapability.SERVER_MISSING,
            title = "Server Lemonade tidak tersedia",
            detail = when (route) {
                ChatRoute.CLOUD ->
                    "Jawaban dibuat lewat jalur cloud Supabase. Pastikan server di ${LemonadeConfig.defaultBaseUrl} berjalan, lalu periksa lagi."
                ChatRoute.OFFLINE ->
                    "Jawaban dibuat oleh penyusun offline di perangkat. Pastikan server di ${LemonadeConfig.defaultBaseUrl} berjalan, lalu periksa lagi."
                else ->
                    "Jawaban memakai jalur cadangan. Pastikan server di ${LemonadeConfig.defaultBaseUrl} berjalan, lalu periksa lagi."
            }
        )

        is LemonadeFailure.ModelNotReady -> ChatCapabilityNotice(
            capability = ChatCapability.MODEL_LOADING,
            title = "Model sedang dimuat",
            detail = "Tunggu sebentar sampai model siap, lalu kirim pertanyaan Anda lagi."
        )

        is LemonadeFailure.Busy -> ChatCapabilityNotice(
            capability = ChatCapability.MODEL_BUSY,
            title = "Model sedang sibuk",
            detail = "Masih ada permintaan lain yang diproses. Coba lagi beberapa saat lagi."
        )

        else -> null
    }
}
