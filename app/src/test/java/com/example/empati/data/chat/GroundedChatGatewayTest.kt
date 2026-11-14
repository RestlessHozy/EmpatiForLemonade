package com.example.empati.data.chat

import com.example.empati.data.ChatReplyResult
import com.example.empati.data.lemonade.LemonadeFailure
import com.example.empati.data.triage.EmpatiPromptBuilder
import com.example.empati.data.triage.ReferenceChunk
import com.example.empati.data.triage.TriageContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroundedChatGatewayTest {

    private class FakeModel(
        private val output: String = "Panduan World Health Organization menyarankan mengurangi garam."
    ) : ChatModel {
        var systemPrompt: String? = null
        var userPrompt: String? = null
        var calls = 0
        var failWith: Throwable? = null

        override fun complete(systemPrompt: String, userPrompt: String): String {
            calls++
            this.systemPrompt = systemPrompt
            this.userPrompt = userPrompt
            failWith?.let { throw it }
            return output
        }
    }

    private fun chunk() = ReferenceChunk(
        id = "who-salt-2023#intake",
        title = "Salt reduction",
        publisher = "World Health Organization",
        year = 2023,
        section = "Recommendation",
        text = "Kurangi garam kurang dari 5 gram per hari.",
        url = "https://www.who.int/news-room/fact-sheets/detail/salt-reduction"
    )

    private fun context(chunks: List<ReferenceChunk> = listOf(chunk()), triage: TriageContext? = null) =
        ChatRequestContext(snapshot = null, chunks = chunks, triage = triage)

    @Test
    fun `prompt carries the citations and the system rules`() {
        val model = FakeModel()
        val gateway = GroundedChatGateway(model)

        gateway.generate("bagaimana mengurangi garam", context())

        val userPrompt = model.userPrompt!!
        assertTrue(userPrompt.contains("who-salt-2023#intake"))
        assertTrue(userPrompt.contains("World Health Organization"))
        assertTrue(userPrompt.contains("bagaimana mengurangi garam"))
        assertEquals(EmpatiPromptBuilder.SYSTEM_PROMPT, model.systemPrompt)
    }

    @Test
    fun `saved snapshot reaches the prompt when no engine context exists`() {
        val model = FakeModel()
        val snapshot = com.example.empati.data.rag.ScreeningContextSnapshot(
            riskLevelLabel = "Tinggi",
            integratedCategoryLabel = "Risiko Hipertensi dengan Dominasi Faktor Kesehatan dan Gaya Hidup",
            screeningDate = "05 Nov 2026"
        )

        GroundedChatGateway(model).generate(
            "analisis hasil saya",
            ChatRequestContext(snapshot = snapshot, chunks = listOf(chunk()), triage = null)
        )

        assertTrue(model.userPrompt!!.contains("Tinggi"))
        assertFalse(model.userPrompt!!.contains("Belum ada hasil skrining pada sesi ini"))
    }

    @Test
    fun `prompt asks for refusal when there is no citation`() {
        val model = FakeModel()
        GroundedChatGateway(model).generate("pertanyaan", context(chunks = emptyList()))

        assertTrue(model.userPrompt!!.lowercase().contains("belum punya rujukan"))
    }

    @Test
    fun `model answer is passed through the guardrail`() {
        val model = FakeModel("Nilai CF Anda 0.62, sekitar 62%.")
        val result = GroundedChatGateway(model).generate("tanya", context())

        assertFalse(result.text.contains("CF"))
        assertFalse(result.text.contains("62%"))
        assertTrue(result.text.isNotEmpty())
    }

    @Test
    fun `violence context appends the crisis contact`() {
        val model = FakeModel("Ceritakan pelan-pelan.")
        val result = GroundedChatGateway(model).generate(
            query = "saya merasa tidak aman di rumah",
            context = context(),
            gbvIndicated = true
        )

        assertTrue(result.text.contains("08111-129-129"))
    }

    @Test
    fun `cloud answers when the local model fails`() {
        val model = FakeModel().apply { failWith = IllegalStateException("server mati") }

        val result = GroundedChatGateway(model, cloud = { _, _ -> "Jawaban dari cloud." })
            .generate("tanya", context())

        assertEquals("cloud-fallback", result.modelName)
        assertTrue(result.text.contains("cloud"))
    }

    @Test
    fun `cloud receives the same grounded prompt the local model would`() {
        val model = FakeModel().apply { failWith = LemonadeFailure.Unreachable(null) }
        var cloudSystem: String? = null
        var cloudUser: String? = null

        GroundedChatGateway(
            model,
            cloud = { system, user ->
                cloudSystem = system
                cloudUser = user
                "Jawaban dari cloud."
            }
        ).generate("bagaimana mengurangi garam", context())

        assertEquals(EmpatiPromptBuilder.SYSTEM_PROMPT, cloudSystem)
        assertTrue(cloudUser!!.contains("bagaimana mengurangi garam"))
        assertTrue(cloudUser!!.contains("who-salt-2023#intake"))
    }

    @Test
    fun `local failure is reported so the UI can name the missing capability`() {
        val model = FakeModel().apply {
            failWith = LemonadeFailure.ModelNotReady("empati-llm", null)
        }

        val result = GroundedChatGateway(model, cloud = null).generate("tanya", context())

        assertTrue(result.failure is LemonadeFailure.ModelNotReady)
    }

    @Test
    fun `model failure returns a calm fallback instead of an exception`() {
        val model = FakeModel().apply { failWith = IllegalStateException("server mati") }

        val result = GroundedChatGateway(model).generate("tanya", context())

        assertTrue(result.text.isNotBlank())
        assertFalse(result.text.contains("CF"))
        assertEquals("fallback-bounded", result.modelName)
    }
}
