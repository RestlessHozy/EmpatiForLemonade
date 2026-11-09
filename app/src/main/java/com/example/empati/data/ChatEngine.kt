package com.example.empati.data

import com.example.empati.data.presentation.UserResultPresenter
import com.example.empati.data.rag.OllamaRagService
import com.example.empati.data.rag.RagDocumentChunk
import com.example.empati.data.rag.RagKnowledgeRepository
import com.example.empati.data.rag.RagRetrievalResult
import com.example.empati.data.rag.ScreeningContextSnapshot
import java.util.Locale

data class ChatReplyResult(
    val text: String,
    val retrievedChunks: List<RagDocumentChunk> = emptyList(),
    val retrievedChunkIds: List<String> = emptyList(),
    val usedOllamaLlm: Boolean = false,
    val fetchedFromInternet: Boolean = false,
    val modelName: String = OllamaRagService.DEFAULT_MODEL
)

object ChatEngine {

    fun reply(message: String, lastResult: ScreeningResult?): String =
        replyWithRag(message, lastResult).text

    /**
     * Menghasilkan respons chatbot menggunakan LLM Murni (tanpa korpus dan tanpa sitasi dokumen),
     * dengan pembatasan ruang lingkup pada Kesehatan, Psikologi, dan Perlindungan Kekerasan.
     *
     * - Pertanyaan cek status cepat (mis. "Lihat hasil skrining saya") mengembalikan ringkasan
     *   deterministik dari `ScreeningContextSnapshot` (`snapshot-zero-leak`).
     * - Pertanyaan analitis/kesimpulan (mis. "analisis hasil screening saya", "apa yang bisa anda
     *   simpulkan dari hasil screening saya", "jelaskan hasil skrining saya") diteruskan ke LLM
     *   dengan konteks kualitatif hasil skrining pengguna agar LLM menghasilkan analisis dan
     *   kesimpulan yang mendalam serta relevan.
     */
    fun replyWithRag(
        message: String,
        lastResult: ScreeningResult?,
        corpus: List<RagDocumentChunk> = emptyList(),
        integratedCategoryLabel: String? = null,
        enableNetworkLlm: Boolean = false,
        enableLiveWebSearch: Boolean = false,
        contextSnapshot: ScreeningContextSnapshot? = null
    ): ChatReplyResult {
        val sanitized = OllamaRagService.sanitizePrivacyInput(message)
        val m = sanitized.lowercase(Locale.forLanguageTag("id-ID"))
        val snapshot = contextSnapshot ?: UserResultPresenter.buildContextSnapshot(
            lastResult = lastResult,
            fallbackCategoryLabel = integratedCategoryLabel
        )

        val isSimpleLookupIntent = RagKnowledgeRepository.isSimpleResultLookupQuery(m)
        val isScreeningAnalysisIntent = RagKnowledgeRepository.isScreeningAnalysisQuery(m)
        val isIdentityIntent = RagKnowledgeRepository.isIdentityQuery(m)
        val isGreetingIntent = m == "halo" || m == "hai" || m.startsWith("halo ") ||
            m.startsWith("hai ") || m.startsWith("selamat ") || m.startsWith("assalam")

        // 1a. Intent Cek Ringkasan Cepat ("Lihat hasil skrining saya", "Berapa skor skrining saya"):
        //     Deterministik 100% Zero-Leak dari ScreeningContextSnapshot.
        if (isSimpleLookupIntent) {
            val resultText = if (snapshot != null) {
                val catInfo = snapshot.integratedCategoryLabel?.let { " ($it)" } ?: ""
                "Dari hasil skrining Anda pada ${snapshot.screeningDate}, kondisi tekanan darah Anda saat ini berada pada kategori ${snapshot.riskLevelLabel}$catInfo. " +
                    "Hasil ini merupakan gambaran awal untuk membantu Anda menjaga kesehatan sehari-hari, bukan diagnosis medis. Ada bagian dari hasil tersebut yang ingin kita bahas bersama?"
            } else {
                "Sepertinya Anda belum melakukan skrining di akun ini. Kalau sudah siap, Anda bisa memulainya kapan saja lewat menu Mulai Skrining di Beranda, lalu kita bisa ngobrol lagi membahas hasilnya di sini."
            }
            return ChatReplyResult(
                text = resultText,
                retrievedChunks = emptyList(),
                retrievedChunkIds = emptyList(),
                usedOllamaLlm = false,
                fetchedFromInternet = false,
                modelName = "snapshot-zero-leak"
            )
        }

        // 1b. Bila pengguna meminta analisis/kesimpulan hasil skrining namun BELUM pernah skrining:
        //     Berikan arahan hangat untuk melakukan skrining terlebih dahulu agar LLM tidak mengarang hasil.
        if (isScreeningAnalysisIntent && snapshot == null) {
            return ChatReplyResult(
                text = "Sepertinya Anda belum melakukan skrining di akun ini, jadi saya belum memiliki data hasil skrining Anda untuk dianalisis. Kalau sudah siap, silakan lakukan skrining terlebih dahulu lewat menu Mulai Skrining di Beranda, lalu kembali ke sini agar kita bisa menganalisis dan menyimpulkan hasilnya bersama.",
                retrievedChunks = emptyList(),
                retrievedChunkIds = emptyList(),
                usedOllamaLlm = false,
                fetchedFromInternet = false,
                modelName = "snapshot-zero-leak"
            )
        }

        // 2. Intent Identitas Diri & Kemampuan SRYA Emora ("siapa anda", "siapa kamu", "kamu bisa apa", dll.)
        if (isIdentityIntent) {
            return ChatReplyResult(
                text = OllamaRagService.SRYA_EMORA_SELF_INTRO,
                retrievedChunks = emptyList(),
                retrievedChunkIds = emptyList(),
                usedOllamaLlm = false,
                fetchedFromInternet = false,
                modelName = "identity-self-intro"
            )
        }

        // 3. Intent Sapaan
        if (isGreetingIntent) {
            return ChatReplyResult(
                text = "Halo! Senang bisa menemani Anda hari ini. Saya SRYA Emora, teman bicara Anda untuk urusan tekanan darah dan gaya hidup sehat, pengelolaan stres atau kualitas tidur, hingga dukungan ruang aman. Ada yang sedang ingin Anda ceritakan atau tanyakan?",
                retrievedChunks = emptyList(),
                retrievedChunkIds = emptyList(),
                usedOllamaLlm = false,
                fetchedFromInternet = false,
                modelName = "greeting-guard"
            )
        }

        // 3. Delegasi langsung ke LLM Murni (tanpa korpus / rujukan)
        val outcome = OllamaRagService.generateRagResponse(
            rawQuery = sanitized,
            retrieval = RagRetrievalResult(
                query = sanitized,
                matchedChunks = emptyList(),
                contextSnapshot = snapshot
            ),
            enableNetworkLlm = enableNetworkLlm,
            preferredModel = OllamaRagService.DEFAULT_MODEL
        )

        return ChatReplyResult(
            text = outcome.text,
            retrievedChunks = emptyList(),
            retrievedChunkIds = emptyList(),
            usedOllamaLlm = outcome.usedOllamaLlm,
            fetchedFromInternet = false,
            modelName = outcome.modelName
        )
    }
}
