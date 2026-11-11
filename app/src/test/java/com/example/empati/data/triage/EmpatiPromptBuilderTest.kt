package com.example.empati.data.triage

import com.example.empati.data.cf.CfLevel
import com.example.empati.data.cf.Hypothesis
import com.example.empati.data.cf.ModuleResult
import com.example.empati.data.cf.ScreeningModule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmpatiPromptBuilderTest {

    private fun context() = HybridTriageBridge.build(
        hypertension = ModuleResult(
            module = ScreeningModule.HYPERTENSION,
            cf = 0.62,
            level = CfLevel.SEDANG,
            interpretation = "internal",
            categories = emptyList(),
            dominantFactors = listOf("Gaya Hidup"),
            protectiveFactors = emptyList(),
            hasDiagnosis = false,
            answeredCount = 16,
            totalCount = 16,
            complete = true
        ),
        gbv = null,
        integrated = null,
        kbVersion = "cf-kb-v2.0",
        capturedAt = "now"
    )

    private fun chunk(id: String = "who-htn-2023#sodium", title: String = "WHO guideline for hypertension") =
        ReferenceChunk(
            id = id,
            title = title,
            publisher = "World Health Organization",
            year = 2023,
            section = "Lifestyle",
            text = "Reduce dietary sodium intake to less than 2 g per day.",
            url = "https://www.who.int/publications/i/item/9789240049094"
        )

    @Test
    fun `system prompt avoids chatbot cliches`() {
        val prompt = EmpatiPromptBuilder.SYSTEM_PROMPT.lowercase()

        listOf(
            "tentu saja",
            "sebagai asisten ai",
            "pertanyaan yang bagus",
            "sebagai model bahasa",
            "saya hanya ai",
            "great question",
            "i hope this helps"
        ).forEach { banned ->
            assertFalse("system prompt must not contain the cliche: $banned", prompt.contains(banned))
        }
    }

    @Test
    fun `system prompt keeps the exact crisis contact formatting`() {
        val prompt = EmpatiPromptBuilder.SYSTEM_PROMPT

        assertTrue(prompt.contains("SAPA 129"))
        assertTrue(prompt.contains("129"))
        assertTrue(prompt.contains("08111-129-129"))
    }

    @Test
    fun `system prompt asks for spoken friendly prose without markdown`() {
        val prompt = EmpatiPromptBuilder.SYSTEM_PROMPT.lowercase()

        assertTrue(prompt.contains("dibacakan"))
        assertTrue(prompt.contains("kalimat"))
        assertTrue(prompt.contains("markdown"))
        assertTrue(prompt.contains("poin") || prompt.contains("daftar"))
    }

    @Test
    fun `system prompt uses the new brand and never the old one`() {
        assertTrue(EmpatiPromptBuilder.SYSTEM_PROMPT.contains("Empati"))
        assertFalse(EmpatiPromptBuilder.SYSTEM_PROMPT.contains("SRYA"))
        assertFalse(EmpatiPromptBuilder.SYSTEM_PROMPT.lowercase().contains("emora"))
    }

    @Test
    fun `system prompt states that risk labels come from the engine`() {
        val prompt = EmpatiPromptBuilder.SYSTEM_PROMPT.lowercase()

        assertTrue(prompt.contains("sistem pakar") || prompt.contains("perhitungan"))
        assertTrue(prompt.contains("jangan") && prompt.contains("menghitung"))
        assertTrue(prompt.contains("diagnosis"))
    }

    @Test
    fun `user prompt carries the query the triage block and every citation`() {
        val prompt = EmpatiPromptBuilder.buildUserPrompt(
            query = "apa yang sebaiknya saya lakukan",
            context = context(),
            chunks = listOf(chunk(), chunk(id = "kemenkes-garam", title = "Pedoman gizi seimbang"))
        )

        assertTrue(prompt.contains("apa yang sebaiknya saya lakukan"))
        assertTrue(prompt.contains("Sedang"))
        assertTrue(prompt.contains("who-htn-2023#sodium"))
        assertTrue(prompt.contains("WHO guideline for hypertension"))
        assertTrue(prompt.contains("World Health Organization"))
        assertTrue(prompt.contains("kemenkes-garam"))
        assertFalse(prompt.contains("CF"))
    }

    @Test
    fun `user prompt instructs refusal when no reference is available`() {
        val prompt = EmpatiPromptBuilder.buildUserPrompt(
            query = "bagaimana cara menurunkan tensi",
            context = null,
            chunks = emptyList()
        )

        assertTrue(prompt.lowercase().contains("belum punya rujukan"))
        assertTrue(prompt.lowercase().contains("jangan mengarang"))
    }
}
