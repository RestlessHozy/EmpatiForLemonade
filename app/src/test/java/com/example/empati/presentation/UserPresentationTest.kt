package com.example.empati.presentation

import com.example.empati.data.cf.AnswerValue
import com.example.empati.data.cf.CfResultGenerator
import com.example.empati.data.cf.QuestionKind
import com.example.empati.data.cf.ScreeningModule
import com.example.empati.data.cf.gbvQuestions
import com.example.empati.data.cf.hypertensionQuestions
import com.example.empati.data.presentation.UserResultPresenter
import com.example.empati.data.research.ResearchRecordFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPresentationTest {

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

    private fun highGbv() = CfResultGenerator.buildModuleResult(
        ScreeningModule.GBV,
        gbvQuestions,
        gbvQuestions.associate { it.id to AnswerValue.Likert(5) }
    )

    private fun lowGbv() = CfResultGenerator.buildModuleResult(
        ScreeningModule.GBV,
        gbvQuestions,
        gbvQuestions.associate { it.id to AnswerValue.Likert(1) }
    )

    private fun partialHt() = CfResultGenerator.buildModuleResult(
        ScreeningModule.HYPERTENSION,
        hypertensionQuestions,
        mapOf("M01" to AnswerValue.Dichotomy(false))
    )

    private fun assertNoInternalLeak(vararg texts: String) {
        val joined = texts.joinToString(" ")
        assertFalse("Should not expose CF", joined.contains("CF"))
        assertFalse("Should not expose MB", joined.contains("MB"))
        assertFalse("Should not expose MD", joined.contains("MD"))
        assertFalse("Should not expose percentage", joined.contains("%"))
        assertFalse("Should not expose decimals", Regex("\\d+\\.\\d+").containsMatchIn(joined))
    }

    @Test
    fun `hypertension presentation uses plain language`() {
        val p = UserResultPresenter.presentModule(highHt())
        assertTrue(p.isHypertension)
        assertTrue(p.factorsToNote.isNotEmpty())
        assertTrue(p.conclusion.contains("hipertensi"))
        assertNoInternalLeak(
            p.title, p.statusLabel, p.conclusion, p.disclaimer,
            p.factorsToNote.joinToString(" "),
            p.recommendations.joinToString(" ")
        )
    }

    @Test
    fun `gbv presentation lists plain categories and no cf`() {
        val answers = gbvQuestions.associate {
            it.id to AnswerValue.Likert(if (it.id == "K03") 4 else 1)
        }
        val result = CfResultGenerator.buildModuleResult(ScreeningModule.GBV, gbvQuestions, answers)
        val p = UserResultPresenter.presentModule(result)
        assertFalse(p.isHypertension)
        assertTrue(p.identifiedCategories.any { it.contains("Kekerasan psikologis") })
        assertTrue(p.conclusion.contains("kekerasan psikologis"))
        assertNoInternalLeak(
            p.title, p.statusLabel, p.conclusion, p.disclaimer,
            p.identifiedCategories.joinToString(" "),
            p.recommendations.joinToString(" ")
        )
    }

    @Test
    fun `integrated presentation keeps modules separate`() {
        val ht = highHt()
        val gbv = highGbv()
        val analysis = CfResultGenerator.integrate(ht, gbv)
        val p = UserResultPresenter.presentIntegrated(ht, gbv, analysis)
        assertTrue(p.hypertension.isHypertension)
        assertFalse(p.gbv.isHypertension)
        assertNoInternalLeak(
            p.categoryLabel, p.summary, p.relationshipExplanation, p.disclaimer,
            p.hypertension.conclusion, p.gbv.conclusion,
            p.factors.joinToString(" "), p.recommendations.joinToString(" ")
        )
    }

    @Test
    fun `integrated category labels match internal classification`() {
        val lowLow = UserResultPresenter.presentIntegrated(
            lowHt(), lowGbv(), CfResultGenerator.integrate(lowHt(), lowGbv())
        )
        assertEquals("Risiko Hipertensi Rendah", lowLow.categoryLabel)

        val htHigh = highHt()
        val gbvLow = lowGbv()
        val medical = UserResultPresenter.presentIntegrated(
            htHigh, gbvLow, CfResultGenerator.integrate(htHigh, gbvLow)
        )
        assertEquals(
            "Risiko Hipertensi dengan Dominasi Faktor Kesehatan dan Gaya Hidup",
            medical.categoryLabel
        )

        val htLow = lowHt()
        val gbvHigh = highGbv()
        val gbvOnly = UserResultPresenter.presentIntegrated(
            htLow, gbvHigh, CfResultGenerator.integrate(htLow, gbvHigh)
        )
        assertEquals("Indikasi KBG Tanpa Indikasi Risiko Hipertensi", gbvOnly.categoryLabel)

        val both = UserResultPresenter.presentIntegrated(
            highHt(), highGbv(), CfResultGenerator.integrate(highHt(), highGbv())
        )
        assertEquals("Risiko Hipertensi dengan Indikasi Keterkaitan Pengalaman KBG", both.categoryLabel)

        val htPartial = partialHt()
        val incomplete = UserResultPresenter.presentIntegrated(
            htPartial, lowGbv(), CfResultGenerator.integrate(htPartial, lowGbv())
        )
        assertEquals("Hasil Belum Dapat Disimpulkan", incomplete.categoryLabel)
        assertTrue(incomplete.isInconclusive)
    }

    @Test
    fun `risk summary exposes only level and date`() {
        val result = highHt()
        val summary = CfResultGenerator.toScreeningResult(
            CfResultGenerator.integrate(result, highGbv())
        )
        val user = UserResultPresenter.presentRiskSummary(summary)
        assertEquals(summary.level, user.level)
        assertEquals(summary.date, user.date)

        // Controlled hypertension (M01=Ya, all other factors controlled/low) aligns as Sedang · Perlu Pemantauan
        val controlledAnswers = hypertensionQuestions.associate { q ->
            q.id to when {
                q.id == "M01" -> AnswerValue.Dichotomy(true)
                q.kind == QuestionKind.DICHOTOMY -> AnswerValue.Dichotomy(false)
                else -> AnswerValue.Likert(if (q.reverseScored) 5 else 1)
            }
        }
        val controlledHt = CfResultGenerator.buildModuleResult(
            ScreeningModule.HYPERTENSION,
            hypertensionQuestions,
            controlledAnswers
        )
        val controlledPresentation = UserResultPresenter.presentModule(controlledHt)
        val controlledSummary = CfResultGenerator.toScreeningResult(
            CfResultGenerator.integrate(controlledHt, lowGbv())
        )
        assertEquals("Sedang · Perlu Pemantauan", controlledPresentation.statusLabel)
        assertEquals(com.example.empati.data.RiskLevel.MEDIUM, controlledSummary.level)
    }

    @Test
    fun `internal research record retains cf and evidence`() {
        val ht = highHt()
        val gbv = highGbv()
        val analysis = CfResultGenerator.integrate(ht, gbv)
        val record = ResearchRecordFactory.create(ht, gbv, analysis)

        assertTrue(record.screeningId.isNotBlank())
        assertTrue(record.hypertensionCf > 0.0)
        assertTrue(record.gbvCf > 0.0)
        assertTrue(record.hypertension.cf > 0.0)
        assertTrue(record.hypertension.categories.isNotEmpty())

        val firstEvidence = record.hypertension.categories.first().evidence.first()
        assertTrue(firstEvidence.questionId.isNotBlank())
        assertTrue(firstEvidence.cfEvidence >= 0.0)
        assertTrue(record.integratedCategory.isNotBlank())
    }

    @Test
    fun `protective factors and clinical tag explanations are zero leak`() {
        val lowModule = UserResultPresenter.presentModule(lowHt())
        val integrated = UserResultPresenter.presentIntegrated(
            lowHt(), lowGbv(), CfResultGenerator.integrate(lowHt(), lowGbv())
        )
        assertTrue(lowModule.protectiveFactors.isNotEmpty())
        assertEquals(lowModule.protectiveFactors, integrated.protectiveFactors)
        assertNoInternalLeak(lowModule.protectiveFactors.joinToString(" "))

        val tags = UserResultPresenter.allKnownClinicalTags()
        assertTrue(tags.size >= 9)
        tags.forEach { tag ->
            val detail = UserResultPresenter.explainClinicalTag(tag)
            assertTrue(detail.explanation.isNotBlank())
            assertTrue(detail.categoryBadge.isNotBlank())
            assertTrue(detail.linkageReminder.isNotBlank())
            assertNoInternalLeak(
                detail.tag,
                detail.categoryBadge,
                detail.explanation,
                detail.linkageReminder
            )
        }
    }
}
