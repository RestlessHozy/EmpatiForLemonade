package com.example.empati.data.chat

import com.example.empati.data.triage.ReferenceChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingChatTest {

    private class FakeModel(
        private val answer: String? = "jawaban lengkap"
    ) : ChatModel {
        var completeCalls = 0

        override fun complete(systemPrompt: String, userPrompt: String): String {
            completeCalls++
            return answer ?: throw IllegalStateException("model mati")
        }
    }

    private fun context() = ChatRequestContext(
        snapshot = null,
        chunks = listOf(
            ReferenceChunk(
                id = "who-salt-2023#intake",
                title = "Salt reduction",
                publisher = "World Health Organization",
                year = 2023,
                section = "Recommendation",
                text = "Kurangi garam.",
                url = "https://www.who.int/news-room/fact-sheets/detail/salt-reduction"
            )
        )
    )

    @Test
    fun `normal stream emits every delta and keeps the joined text`() {
        val model = FakeModel()
        val deltas = mutableListOf<String>()
        val gateway = GroundedChatGateway(model) { _, _, onDelta ->
            listOf("Halo", ", ", "dunia").forEach { part ->
                onDelta(part)
            }
            "Halo, dunia"
        }

        val result = gateway.generateStreaming("halo", context()) { deltas += it }

        assertEquals(listOf("Halo", ", ", "dunia"), deltas)
        assertEquals("Halo, dunia.", result.text)
        assertFalse(result.isPartial)
        assertEquals(0, model.completeCalls)
    }

    @Test
    fun `stream broken after the first token keeps the partial text`() {
        val gateway = GroundedChatGateway(FakeModel()) { _, _, onDelta ->
            onDelta("Halo")
            throw IllegalStateException("koneksi putus")
        }

        val result = gateway.generateStreaming("halo", context()) { }

        assertTrue(result.isPartial)
        assertTrue(result.text.contains("Halo"))
    }

    @Test
    fun `stream broken before any token is retried once without streaming`() {
        val model = FakeModel(answer = "jawaban lengkap")
        val gateway = GroundedChatGateway(model) { _, _, _ ->
            throw IllegalStateException("koneksi putus")
        }

        val result = gateway.generateStreaming("halo", context()) { }

        assertEquals(1, model.completeCalls)
        assertEquals("jawaban lengkap.", result.text)
        assertFalse(result.isPartial)
    }

    @Test
    fun `retry failure falls back without throwing`() {
        val gateway = GroundedChatGateway(FakeModel(answer = null)) { _, _, _ ->
            throw IllegalStateException("koneksi putus")
        }

        val result = gateway.generateStreaming("halo", context()) { }

        assertTrue(result.text.isNotBlank())
        assertFalse(result.text.contains("CF"))
    }
}
