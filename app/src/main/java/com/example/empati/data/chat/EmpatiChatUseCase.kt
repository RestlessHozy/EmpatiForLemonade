package com.example.empati.data.chat

import com.example.empati.data.ChatReplyResult
import com.example.empati.data.rag.OllamaRagService
import com.example.empati.data.rag.RagKnowledgeRepository
import com.example.empati.data.rag.ScreeningContextSnapshot
import com.example.empati.data.retrieval.CitationPicker
import com.example.empati.data.triage.ReferenceChunk
import com.example.empati.data.triage.TriageContext
import java.util.Locale

data class ChatRequestContext(
    val snapshot: ScreeningContextSnapshot?,
    val chunks: List<ReferenceChunk>,
    val triage: TriageContext? = null
)

interface ConversationGateway {
    fun generate(query: String, context: ChatRequestContext): ChatReplyResult

    fun generateStreaming(
        query: String,
        context: ChatRequestContext,
        onDelta: (String) -> Unit
    ): ChatReplyResult = generate(query, context)
}

fun interface KnowledgeRetriever {
    fun retrieve(query: String, limit: Int): List<ReferenceChunk>
}

/**
 * Satu titik perilaku chat Empati. Percakapan yang bisa dijawab tanpa model
 * (identitas, sapaan, ringkasan hasil skrining) diselesaikan di sini, sisanya
 * dikirim ke gateway bersama konteks hasil skrining dan potongan rujukan.
 */
class EmpatiChatUseCase(
    private val gateway: ConversationGateway,
    private val retriever: KnowledgeRetriever = KnowledgeRetriever { _, _ -> emptyList() },
    private val triageContext: () -> TriageContext? = { null },
    private val retrievalLimit: Int = 4
) {

    fun reply(message: String, snapshot: ScreeningContextSnapshot?): ChatReplyResult {
        val sanitized = sanitize(message)
        deterministicReply(sanitized, snapshot)?.let { return it }
        val chunks = retriever.retrieve(sanitized, retrievalLimit)
        return cited(gateway.generate(sanitized, contextOf(snapshot, chunks)), chunks)
    }

    /**
     * Sama seperti [reply], tetapi pertanyaan umum diteruskan lewat streaming.
     * Jawaban deterministik tetap dikembalikan utuh karena tidak ada model
     * yang perlu ditunggu.
     */
    fun replyStreaming(
        message: String,
        snapshot: ScreeningContextSnapshot?,
        onDelta: (String) -> Unit
    ): ChatReplyResult {
        val sanitized = sanitize(message)
        deterministicReply(sanitized, snapshot)?.let { return it }
        val chunks = retriever.retrieve(sanitized, retrievalLimit)
        val reply = gateway.generateStreaming(sanitized, contextOf(snapshot, chunks), onDelta)
        return cited(reply, chunks)
    }

    private fun cited(reply: ChatReplyResult, chunks: List<ReferenceChunk>): ChatReplyResult =
        reply.copy(referenceChunks = CitationPicker.usedIn(reply.text, chunks))

    private fun contextOf(snapshot: ScreeningContextSnapshot?, chunks: List<ReferenceChunk>) =
        ChatRequestContext(snapshot = snapshot, chunks = chunks, triage = triageContext())

    private fun sanitize(message: String): String =
        OllamaRagService.sanitizePrivacyInput(message)

    private fun deterministicReply(
        sanitized: String,
        snapshot: ScreeningContextSnapshot?
    ): ChatReplyResult? {
        val normalized = sanitized.lowercase(Locale.forLanguageTag("id-ID"))

        if (RagKnowledgeRepository.isSimpleResultLookupQuery(normalized)) {
            val text = if (snapshot != null) {
                val categoryInfo = snapshot.integratedCategoryLabel?.let { " ($it)" } ?: ""
                "Dari hasil skrining Anda pada ${snapshot.screeningDate}, kondisi tekanan darah Anda " +
                    "saat ini berada pada kategori ${snapshot.riskLevelLabel}$categoryInfo. " +
                    "Hasil ini merupakan gambaran awal untuk membantu Anda menjaga kesehatan sehari-hari, " +
                    "bukan diagnosis medis. Ada bagian dari hasil tersebut yang ingin kita bahas bersama?"
            } else {
                "Sepertinya Anda belum melakukan skrining di akun ini. Kalau sudah siap, Anda bisa " +
                    "memulainya kapan saja lewat menu Mulai Skrining di Beranda, lalu kita bisa ngobrol " +
                    "lagi membahas hasilnya di sini."
            }
            return ChatReplyResult(text = text, modelName = "snapshot-zero-leak")
        }

        if (RagKnowledgeRepository.isScreeningAnalysisQuery(normalized) && snapshot == null) {
            return ChatReplyResult(
                text = "Sepertinya Anda belum melakukan skrining di akun ini, jadi saya belum memiliki " +
                    "data hasil skrining Anda untuk dianalisis. Kalau sudah siap, silakan lakukan skrining " +
                    "terlebih dahulu lewat menu Mulai Skrining di Beranda, lalu kembali ke sini agar kita " +
                    "bisa menganalisis dan menyimpulkan hasilnya bersama.",
                modelName = "snapshot-zero-leak"
            )
        }

        if (RagKnowledgeRepository.isIdentityQuery(normalized)) {
            return ChatReplyResult(
                text = OllamaRagService.SRYA_EMORA_SELF_INTRO,
                modelName = "identity-self-intro"
            )
        }

        val isGreeting = normalized == "halo" || normalized == "hai" ||
            normalized.startsWith("halo ") || normalized.startsWith("hai ") ||
            normalized.startsWith("selamat ") || normalized.startsWith("assalam")
        if (isGreeting) {
            return ChatReplyResult(
                text = "Halo! Senang bisa menemani Anda hari ini. Saya Empati, teman bicara Anda untuk " +
                    "urusan tekanan darah dan gaya hidup sehat, pengelolaan stres atau kualitas tidur, " +
                    "hingga dukungan ruang aman. Ada yang sedang ingin Anda ceritakan atau tanyakan?",
                modelName = "greeting-guard"
            )
        }

        return null
    }
}
