package com.example.empati.data.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerGuardrailTest {

    @Test
    fun `markdown emphasis is removed`() {
        val result = AnswerGuardrail.enforce("**Kurangi garam** dan *jalan kaki* setiap pagi.")

        assertEquals("Kurangi garam dan jalan kaki setiap pagi.", result.text)
    }

    @Test
    fun `internal acronyms and percentages are replaced`() {
        val result = AnswerGuardrail.enforce("Nilai CF Anda 0.62, dengan MB 0.7 dan MD 0.1, sekitar 62%.")

        assertFalse(result.text.contains("CF"))
        assertFalse(result.text.contains("MB"))
        assertFalse(result.text.contains("MD"))
        assertFalse(result.text.contains("62%"))
        assertTrue(result.violations.isNotEmpty())
    }

    @Test
    fun `clinical numbers from the guideline survive`() {
        val result = AnswerGuardrail.enforce("WHO menyebut hipertensi saat tekanan darah 140/90 atau lebih.")

        assertTrue(result.text.contains("140/90"))
    }

    @Test
    fun `violence context appends the exact SAPA contact once`() {
        val first = AnswerGuardrail.enforce("Ceritakan pelan-pelan.", gbvIndicated = true)

        assertTrue(first.text.contains("129"))
        assertTrue(first.text.contains("08111-129-129"))

        val second = AnswerGuardrail.enforce(first.text, gbvIndicated = true)
        assertEquals(first.text, second.text)
    }

    @Test
    fun `violence contact stays out of unrelated answers`() {
        val result = AnswerGuardrail.enforce("Minum air cukup dan tidur teratur.")

        assertFalse(result.text.contains("SAPA"))
    }

    @Test
    fun `diagnosis wording is flagged without inventing a new claim`() {
        val result = AnswerGuardrail.enforce("Anda menderita hipertensi dan perlu minum obat ini.")

        assertTrue(result.violations.contains("diagnosis"))
        assertTrue(result.text.isNotEmpty())
    }

    @Test
    fun `text always ends with terminal punctuation`() {
        val result = AnswerGuardrail.enforce("Kurangi garam pelan-pelan")

        assertTrue(result.text.endsWith("."))
    }

    @Test
    fun `reasoning trace is removed whether complete unclosed or orphaned`() {
        val complete = AnswerGuardrail.enforce("<think>hitung dulu skornya</think>Kurangi garam pelan-pelan.")
        assertEquals("Kurangi garam pelan-pelan.", complete.text)
        assertTrue(complete.violations.contains("reasoning-trace"))

        val streamingTail = AnswerGuardrail.enforce("<think>masih berpikir tentang tensi")
        assertFalse(streamingTail.text.contains("think"))
        assertFalse(streamingTail.text.contains("berpikir"))

        val orphan = AnswerGuardrail.enforce("penalaran internal</think>Tidur cukup membantu.")
        assertEquals("Tidur cukup membantu.", orphan.text)
    }

    @Test
    fun `internal confidence decimals are hidden but clinical decimals survive`() {
        val leaked = AnswerGuardrail.enforce("Kekuatan indikasi Anda 0.62 dari 1.00, jadi perlu perhatian.")
        assertFalse(leaked.text.contains("0.62"))
        assertFalse(leaked.text.contains("1.00"))
        assertTrue(leaked.violations.contains("internal-confidence"))

        val labelled = AnswerGuardrail.enforce("Keyakinan sistem sekitar 0,7 untuk hal ini.")
        assertFalse(labelled.text.contains("0,7"))

        val clinical = AnswerGuardrail.enforce("Panduan WHO menyarankan garam kurang dari 5 gram dan tekanan 140/90.")
        assertTrue(clinical.text.contains("140/90"))
        assertTrue(clinical.violations.isEmpty())
    }

    @Test
    fun `clean answer reports no violations`() {
        val result = AnswerGuardrail.enforce(
            "Panduan WHO menyarankan mengurangi garam secara bertahap."
        )

        assertTrue(result.violations.isEmpty())
    }
}
