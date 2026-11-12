package com.example.empati.data.chat

import com.example.empati.data.ChatReplyResult
import com.example.empati.data.rag.ScreeningContextSnapshot
import com.example.empati.data.triage.ReferenceChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmpatiChatUseCaseTest {

    private class RecordingGateway(
        private val reply: ChatReplyResult = ChatReplyResult(text = "jawaban dari model")
    ) : ConversationGateway {
        var calls = 0
        var lastQuery: String? = null
        var lastContext: ChatRequestContext? = null

        override fun generate(query: String, context: ChatRequestContext): ChatReplyResult {
            calls++
            lastQuery = query
            lastContext = context
            return reply
        }
    }

    private class FixedRetriever(private val chunks: List<ReferenceChunk>) : KnowledgeRetriever {
        var lastQuery: String? = null
        var lastLimit: Int = 0

        override fun retrieve(query: String, limit: Int): List<ReferenceChunk> {
            lastQuery = query
            lastLimit = limit
            return chunks.take(limit)
        }
    }

    private fun chunk(id: String) = ReferenceChunk(
        id = id,
        title = "Judul $id",
        publisher = "Penerbit $id",
        year = 2024,
        section = "Bagian",
        text = "isi $id",
        url = "https://example.org/$id"
    )

    private fun snapshot() = ScreeningContextSnapshot(
        screeningDate = "11 November 2026",
        riskLevelLabel = "Sedang",
        hypertensionStatusLabel = "Sedang",
        gbvStatusLabel = "Tidak terindikasi",
        integratedCategoryLabel = "Hipertensi Murni Faktor Gaya Hidup",
        keyFactors = listOf("Gaya Hidup"),
        protectiveFactors = listOf("Aktivitas fisik"),
        summaryNarrative = null,
        relationshipExplanation = null,
        keyRecommendations = listOf("Kurangi garam")
    )

    @Test
    fun `identity question answers without touching the model`() {
        val gateway = RecordingGateway()
        val useCase = EmpatiChatUseCase(gateway = gateway, retriever = FixedRetriever(emptyList()))

        val reply = useCase.reply("siapa kamu", snapshot = null)

        assertEquals(0, gateway.calls)
        assertTrue(reply.text.contains("Empati") || reply.text.contains("teman bicara"))
        assertEquals("identity-self-intro", reply.modelName)
    }

    @Test
    fun `greeting answers locally`() {
        val gateway = RecordingGateway()
        val useCase = EmpatiChatUseCase(gateway = gateway, retriever = FixedRetriever(emptyList()))

        val reply = useCase.reply("halo ", snapshot = null)

        assertEquals(0, gateway.calls)
        assertEquals("greeting-guard", reply.modelName)
    }

    @Test
    fun `quick result lookup uses the snapshot without the model`() {
        val gateway = RecordingGateway()
        val useCase = EmpatiChatUseCase(gateway = gateway, retriever = FixedRetriever(emptyList()))

        val reply = useCase.reply("lihat hasil skrining saya", snapshot())

        assertEquals(0, gateway.calls)
        assertTrue(reply.text.contains("Sedang"))
        assertEquals("snapshot-zero-leak", reply.modelName)
    }

    @Test
    fun `analysis request without a snapshot asks for screening first`() {
        val gateway = RecordingGateway()
        val useCase = EmpatiChatUseCase(gateway = gateway, retriever = FixedRetriever(emptyList()))

        val reply = useCase.reply("analisis hasil skrining saya", snapshot = null)

        assertEquals(0, gateway.calls)
        assertTrue(reply.text.contains("belum melakukan skrining"))
    }

    @Test
    fun `general question retrieves context and delegates to the model once`() {
        val gateway = RecordingGateway()
        val retriever = FixedRetriever(listOf(chunk("a"), chunk("b")))
        val useCase = EmpatiChatUseCase(gateway = gateway, retriever = retriever)

        val reply = useCase.reply("bagaimana cara mengurangi garam", snapshot())

        assertEquals(1, gateway.calls)
        assertEquals("bagaimana cara mengurangi garam", gateway.lastQuery)
        assertEquals(listOf("a", "b"), gateway.lastContext!!.chunks.map { it.id })
        assertEquals("Sedang", gateway.lastContext!!.snapshot?.riskLevelLabel)
        assertTrue(reply.text.isNotEmpty())
        assertFalse(reply.referenceChunks.isEmpty())
    }

    @Test
    fun `personal identifiers never reach the model`() {
        val gateway = RecordingGateway()
        val useCase = EmpatiChatUseCase(gateway = gateway, retriever = FixedRetriever(emptyList()))

        useCase.reply("NIP saya 198503122010012014, tensi saya tinggi", snapshot = null)

        assertFalse(gateway.lastQuery!!.contains("198503122010012014"))
        assertTrue(gateway.lastQuery!!.contains("NIP-DIRAHASIAKAN"))
    }

    private class StreamingGateway : ConversationGateway {
        var streamingCalls = 0

        override fun generate(query: String, context: ChatRequestContext): ChatReplyResult =
            ChatReplyResult(text = "biasa")

        override fun generateStreaming(
            query: String,
            context: ChatRequestContext,
            onDelta: (String) -> Unit
        ): ChatReplyResult {
            streamingCalls++
            onDelta("Halo")
            onDelta(" dunia")
            return ChatReplyResult(text = "Halo dunia")
        }
    }

    @Test
    fun `general question streams deltas through the gateway`() {
        val gateway = StreamingGateway()
        val deltas = mutableListOf<String>()
        val useCase = EmpatiChatUseCase(gateway = gateway, retriever = FixedRetriever(emptyList()))

        val reply = useCase.replyStreaming("bagaimana cara mengurangi garam", null) { deltas += it }

        assertEquals(1, gateway.streamingCalls)
        assertEquals(listOf("Halo", " dunia"), deltas)
        assertEquals("Halo dunia", reply.text)
    }

    @Test
    fun `deterministic intents never open a stream`() {
        val gateway = StreamingGateway()
        val useCase = EmpatiChatUseCase(gateway = gateway, retriever = FixedRetriever(emptyList()))

        val reply = useCase.replyStreaming("halo", null) { }

        assertEquals(0, gateway.streamingCalls)
        assertEquals("greeting-guard", reply.modelName)
    }
}
