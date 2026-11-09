package com.example.empati.cf

import com.example.empati.data.cf.AnswerValue
import com.example.empati.data.cf.CfInferenceEngine
import com.example.empati.data.cf.CfInputParser
import com.example.empati.data.cf.CfLevel
import com.example.empati.data.cf.CfResultGenerator
import com.example.empati.data.cf.CfRule
import com.example.empati.data.cf.Hypothesis
import com.example.empati.data.cf.IntegratedCategory
import com.example.empati.data.cf.QuestionKind
import com.example.empati.data.cf.ScreeningModule
import com.example.empati.data.cf.allCfQuestions
import com.example.empati.data.cf.gbvQuestions
import com.example.empati.data.cf.hypertensionQuestions
import com.example.empati.data.cf.questionsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CfEngineTest {

    // ---------- Struktur pertanyaan ----------

    @Test
    fun `all 26 questions present with correct ids`() {
        assertEquals(16, hypertensionQuestions.size)
        assertEquals(10, gbvQuestions.size)
        assertEquals(26, allCfQuestions.size)
        assertEquals((1..16).map { "M%02d".format(it) }, hypertensionQuestions.map { it.id })
        assertEquals((1..10).map { "K%02d".format(it) }, gbvQuestions.map { it.id })
    }

    @Test
    fun `medical question kinds are correct`() {
        val likertIds = setOf("M07", "M08", "M10", "M12", "M14")
        hypertensionQuestions.forEach { q ->
            val expected = if (q.id in likertIds) QuestionKind.LIKERT else QuestionKind.DICHOTOMY
            assertEquals("kind for ${q.id}", expected, q.kind)
        }
        assertTrue("M08 must be reverse scored", hypertensionQuestions.first { it.id == "M08" }.reverseScored)
        assertEquals(1, hypertensionQuestions.count { it.reverseScored })
    }

    @Test
    fun `gbv questions are likert sensitive`() {
        gbvQuestions.forEach { q ->
            assertEquals(QuestionKind.LIKERT, q.kind)
            assertTrue(q.sensitive)
        }
    }

    // ---------- Pemetaan CF_User ----------

    @Test
    fun `dichotomy mapping`() {
        val q = hypertensionQuestions.first { it.id == "M01" }
        assertEquals(1.0, CfInputParser.parseSingle(q, AnswerValue.Dichotomy(true))!!, 1e-9)
        assertEquals(0.0, CfInputParser.parseSingle(q, AnswerValue.Dichotomy(false))!!, 1e-9)
    }

    @Test
    fun `likert mapping all five levels`() {
        val q = hypertensionQuestions.first { it.id == "M07" }
        val expected = mapOf(1 to 0.0, 2 to 0.2, 3 to 0.5, 4 to 0.8, 5 to 1.0)
        expected.forEach { (scale, cf) ->
            assertEquals("scale $scale", cf, CfInputParser.parseSingle(q, AnswerValue.Likert(scale))!!, 1e-9)
        }
    }

    @Test
    fun `reverse scored m08 mapping`() {
        val q = hypertensionQuestions.first { it.id == "M08" }
        val expected = mapOf(1 to 1.0, 2 to 0.8, 3 to 0.5, 4 to 0.2, 5 to 0.0)
        expected.forEach { (scale, cf) ->
            assertEquals("scale $scale", cf, CfInputParser.parseSingle(q, AnswerValue.Likert(scale))!!, 1e-9)
        }
    }

    @Test
    fun `empty answers remain unanswered not zero`() {
        val result = CfInputParser.parse(hypertensionQuestions, emptyMap())
        assertTrue(result.cfUser.isEmpty())
        assertNull(result.cfUser["M01"])
        assertTrue(result.isValid)
    }

    @Test
    fun `partially answered only contains answered ids`() {
        val result = CfInputParser.parse(
            hypertensionQuestions,
            mapOf("M01" to AnswerValue.Dichotomy(true))
        )
        assertEquals(1, result.cfUser.size)
        assertEquals(1.0, result.cfUser["M01"]!!, 1e-9)
        assertFalse(result.cfUser.containsKey("M07"))
    }

    @Test
    fun `validation reports wrong answer kind`() {
        val result = CfInputParser.parse(
            hypertensionQuestions,
            mapOf("M07" to AnswerValue.Dichotomy(true))
        )
        assertFalse(result.isValid)
        assertEquals("M07", result.issues.first().questionId)
    }

    @Test
    fun `validation reports likert out of range`() {
        val result = CfInputParser.parse(
            hypertensionQuestions,
            mapOf("M07" to AnswerValue.Likert(0))
        )
        assertFalse(result.isValid)
        assertEquals("M07", result.issues.first().questionId)
    }

    // ---------- Python reference cases ----------

    @Test
    fun `python sample medical parser`() {
        val answers = mapOf(
            "M01" to AnswerValue.Dichotomy(true),
            "M07" to AnswerValue.Likert(4),
            "M08" to AnswerValue.Likert(1)
        )
        val result = CfInputParser.parse(hypertensionQuestions, answers)
        assertEquals(3, result.cfUser.size)
        assertEquals(1.0, result.cfUser["M01"]!!, 1e-9)
        assertEquals(0.8, result.cfUser["M07"]!!, 1e-9)
        assertEquals(1.0, result.cfUser["M08"]!!, 1e-9)
    }

    @Test
    fun `python sample kbg parser`() {
        val answers = mapOf(
            "K03" to AnswerValue.Likert(4),
            "K01" to AnswerValue.Likert(1)
        )
        val result = CfInputParser.parse(gbvQuestions, answers)
        assertEquals(2, result.cfUser.size)
        assertEquals(0.8, result.cfUser["K03"]!!, 1e-9)
        assertEquals(0.0, result.cfUser["K01"]!!, 1e-9)
    }

    // ---------- Formula CF ----------

    @Test
    fun `cf evidence is cf user times cf rule`() {
        val rule = CfRule("R-Q", "Q", Hypothesis.HT_MEDICAL, mb = 0.9, md = 0.1, source = "test")
        assertEquals(0.8, rule.cfPakar, 1e-9)
        assertEquals(0.4, CfInferenceEngine.cfEvidence(0.5, rule), 1e-9)
    }

    @Test
    fun `combine positive evidence`() {
        // 0.6 + 0.5 * (1 - 0.6) = 0.8
        assertEquals(0.8, CfInferenceEngine.combine(0.6, 0.5), 1e-9)
    }

    @Test
    fun `combine negative evidence`() {
        // -0.4 + (-0.5) * (1 + (-0.4)) = -0.7
        assertEquals(-0.7, CfInferenceEngine.combine(-0.4, -0.5), 1e-9)
    }

    @Test
    fun `combine conflicting evidence`() {
        // (0.6 + (-0.3)) / (1 - min(0.6, 0.3)) = 0.3 / 0.7
        assertEquals(0.3 / 0.7, CfInferenceEngine.combine(0.6, -0.3), 1e-9)
    }

    @Test
    fun `combine identity with zero`() {
        assertEquals(0.42, CfInferenceEngine.combine(0.42, 0.0), 1e-9)
        assertEquals(0.42, CfInferenceEngine.combine(0.0, 0.42), 1e-9)
    }

    @Test
    fun `combineAll is deterministic`() {
        val values = listOf(0.5, 0.3, 0.2, 0.7)
        val first = CfInferenceEngine.combineAll(values)
        val second = CfInferenceEngine.combineAll(values)
        assertEquals(first, second, 0.0)
    }

    // ---------- Hasil modul ----------

    @Test
    fun `hypertension high answers produce very high certainty`() {
        val answers = hypertensionQuestions.associate { q ->
            q.id to when (q.kind) {
                QuestionKind.DICHOTOMY -> AnswerValue.Dichotomy(true)
                QuestionKind.LIKERT -> AnswerValue.Likert(if (q.reverseScored) 1 else 5)
            }
        }
        val result = CfResultGenerator.buildModuleResult(
            ScreeningModule.HYPERTENSION, hypertensionQuestions, answers
        )
        assertTrue(result.complete)
        assertTrue(result.cf > 0.75)
        assertEquals(CfLevel.SANGAT_TINGGI, result.level)
        assertTrue(result.hasDiagnosis)
        assertTrue(result.dominantFactors.isNotEmpty())
    }

    @Test
    fun `hypertension low answers produce near zero certainty`() {
        val answers = hypertensionQuestions.associate { q ->
            q.id to when (q.kind) {
                QuestionKind.DICHOTOMY -> AnswerValue.Dichotomy(false)
                QuestionKind.LIKERT -> AnswerValue.Likert(if (q.reverseScored) 5 else 1)
            }
        }
        val result = CfResultGenerator.buildModuleResult(
            ScreeningModule.HYPERTENSION, hypertensionQuestions, answers
        )
        assertTrue(result.complete)
        assertEquals(0.0, result.cf, 1e-9)
        assertEquals(CfLevel.RENDAH, result.level)
        assertFalse(result.hasDiagnosis)
    }

    @Test
    fun `gbv module result builds categories`() {
        val answers = gbvQuestions.associate { it.id to AnswerValue.Likert(5) }
        val result = CfResultGenerator.buildModuleResult(
            ScreeningModule.GBV, gbvQuestions, answers
        )
        assertTrue(result.complete)
        assertTrue(result.cf > 0.75)
        assertEquals(5, result.categories.size)
    }

    @Test
    fun `module marked incomplete when some answers missing`() {
        val partial = hypertensionQuestions.associate { q ->
            q.id to when (q.kind) {
                QuestionKind.DICHOTOMY -> AnswerValue.Dichotomy(false)
                QuestionKind.LIKERT -> AnswerValue.Likert(1)
            }
        } - setOf("M01", "M02", "M03")
        val result = CfResultGenerator.buildModuleResult(
            ScreeningModule.HYPERTENSION, hypertensionQuestions, partial
        )
        assertFalse(result.complete)
        assertEquals(13, result.answeredCount)
    }

    // ---------- Integrasi dua modul ----------

    private fun highHt() = CfResultGenerator.buildModuleResult(
        ScreeningModule.HYPERTENSION,
        hypertensionQuestions,
        hypertensionQuestions.associate { q ->
            q.id to when (q.kind) {
                QuestionKind.DICHOTOMY -> AnswerValue.Dichotomy(true)
                QuestionKind.LIKERT -> AnswerValue.Likert(if (q.reverseScored) 1 else 5)
            }
        }
    )

    private fun lowHt() = CfResultGenerator.buildModuleResult(
        ScreeningModule.HYPERTENSION,
        hypertensionQuestions,
        hypertensionQuestions.associate { q ->
            q.id to when (q.kind) {
                QuestionKind.DICHOTOMY -> AnswerValue.Dichotomy(false)
                QuestionKind.LIKERT -> AnswerValue.Likert(if (q.reverseScored) 5 else 1)
            }
        }
    )

    private fun lifestyleHt() = CfResultGenerator.buildModuleResult(
        ScreeningModule.HYPERTENSION,
        hypertensionQuestions,
        mapOf(
            "M01" to AnswerValue.Dichotomy(false),
            "M02" to AnswerValue.Dichotomy(false),
            "M03" to AnswerValue.Dichotomy(false),
            "M04" to AnswerValue.Dichotomy(false),
            "M05" to AnswerValue.Dichotomy(false),
            "M06" to AnswerValue.Dichotomy(false),
            "M07" to AnswerValue.Likert(5),
            "M08" to AnswerValue.Likert(1),
            "M09" to AnswerValue.Dichotomy(true),
            "M10" to AnswerValue.Likert(5),
            "M11" to AnswerValue.Dichotomy(true),
            "M12" to AnswerValue.Likert(5),
            "M13" to AnswerValue.Dichotomy(true),
            "M14" to AnswerValue.Likert(5),
            "M15" to AnswerValue.Dichotomy(false),
            "M16" to AnswerValue.Dichotomy(true)
        )
    )

    private fun highGbv() = CfResultGenerator.buildModuleResult(
        ScreeningModule.GBV, gbvQuestions, gbvQuestions.associate { it.id to AnswerValue.Likert(5) }
    )

    private fun lowGbv() = CfResultGenerator.buildModuleResult(
        ScreeningModule.GBV, gbvQuestions, gbvQuestions.associate { it.id to AnswerValue.Likert(1) }
    )

    @Test
    fun `integration low low is low risk`() {
        val analysis = CfResultGenerator.integrate(lowHt(), lowGbv())
        assertEquals(IntegratedCategory.LOW_RISK, analysis.category)
    }

    @Test
    fun `integration high ht low gbv is medical history`() {
        val analysis = CfResultGenerator.integrate(highHt(), lowGbv())
        assertEquals(IntegratedCategory.HYPERTENSION_MEDICAL_HISTORY, analysis.category)
    }

    @Test
    fun `integration lifestyle dominant is lifestyle only`() {
        val analysis = CfResultGenerator.integrate(lifestyleHt(), lowGbv())
        assertEquals(IntegratedCategory.HYPERTENSION_LIFESTYLE_ONLY, analysis.category)
    }

    @Test
    fun `integration both high flags gbv relationship`() {
        val analysis = CfResultGenerator.integrate(highHt(), highGbv())
        assertEquals(IntegratedCategory.HYPERTENSION_WITH_GBV_INDICATION, analysis.category)
        assertTrue(analysis.relationship.contains("BUKAN bukti kausalitas"))
    }

    @Test
    fun `integration gbv only stays independent`() {
        val analysis = CfResultGenerator.integrate(lowHt(), highGbv())
        assertEquals(IntegratedCategory.GBV_ONLY, analysis.category)
    }

    @Test
    fun `integration incomplete is inconclusive`() {
        val partialHt = CfResultGenerator.buildModuleResult(
            ScreeningModule.HYPERTENSION,
            hypertensionQuestions,
            mapOf("M01" to AnswerValue.Dichotomy(false))
        )
        val analysis = CfResultGenerator.integrate(partialHt, lowGbv())
        assertEquals(IntegratedCategory.INCONCLUSIVE, analysis.category)
        assertTrue(analysis.inconclusiveReasons.isNotEmpty())
    }

    @Test
    fun `same input yields identical result`() {
        val a = CfResultGenerator.buildModuleResult(
            ScreeningModule.GBV,
            gbvQuestions,
            gbvQuestions.associate { it.id to AnswerValue.Likert(3) }
        )
        val b = CfResultGenerator.buildModuleResult(
            ScreeningModule.GBV,
            gbvQuestions,
            gbvQuestions.associate { it.id to AnswerValue.Likert(3) }
        )
        assertEquals(a.cf, b.cf, 0.0)
        assertEquals(a.level, b.level)
    }

    @Test
    fun `questionsFor returns matching module`() {
        assertNotNull(questionsFor(ScreeningModule.HYPERTENSION))
        assertEquals(16, questionsFor(ScreeningModule.HYPERTENSION).size)
        assertEquals(10, questionsFor(ScreeningModule.GBV).size)
    }
}
