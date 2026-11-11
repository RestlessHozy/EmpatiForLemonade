package com.example.empati.data.triage

import com.example.empati.data.cf.CategoryResult
import com.example.empati.data.cf.CfLevel
import com.example.empati.data.cf.EvidenceContribution
import com.example.empati.data.lemonade.MiniJson
import com.example.empati.data.cf.Hypothesis
import com.example.empati.data.cf.IntegratedAnalysis
import com.example.empati.data.cf.IntegratedCategory
import com.example.empati.data.cf.ModuleResult
import com.example.empati.data.cf.ScreeningModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridTriageBridgeTest {

    private fun evidence(questionId: String, category: String, cfUser: Double, cfRule: Double, cfEvidence: Double) =
        EvidenceContribution(questionId, category, cfUser, cfRule, cfEvidence)

    private fun hypertensionResult() = ModuleResult(
        module = ScreeningModule.HYPERTENSION,
        cf = 0.62,
        level = CfLevel.SEDANG,
        interpretation = "internal",
        categories = listOf(
            CategoryResult(
                hypothesis = Hypothesis.HT_GENETIC,
                cf = 0.20,
                contributions = listOf(evidence("M04", "Riwayat & Genetik", 0.5, 0.4, 0.2))
            ),
            CategoryResult(
                hypothesis = Hypothesis.HT_LIFESTYLE,
                cf = 0.71,
                contributions = listOf(
                    evidence("M09", "Gaya Hidup", 0.8, 0.6, 0.48),
                    evidence("M10", "Gaya Hidup", 0.5, 0.4, 0.2)
                )
            ),
            CategoryResult(
                hypothesis = Hypothesis.HT_MEDICAL,
                cf = 0.34,
                contributions = listOf(evidence("M05", "Kondisi Medis", 0.5, 0.35, 0.175))
            ),
            CategoryResult(
                hypothesis = Hypothesis.HT_PSYCHOSOCIAL,
                cf = 0.05,
                contributions = listOf(evidence("M12", "Faktor Psikososial", 0.2, 0.25, 0.05))
            )
        ),
        dominantFactors = listOf("Gaya Hidup"),
        protectiveFactors = listOf("M08"),
        hasDiagnosis = false,
        answeredCount = 15,
        totalCount = 16,
        complete = false
    )

    private fun gbvResult() = ModuleResult(
        module = ScreeningModule.GBV,
        cf = 0.0,
        level = CfLevel.RENDAH,
        interpretation = "internal",
        categories = emptyList(),
        dominantFactors = emptyList(),
        protectiveFactors = emptyList(),
        hasDiagnosis = false,
        answeredCount = 10,
        totalCount = 10,
        complete = true
    )

    private fun integrated() = IntegratedAnalysis(
        category = IntegratedCategory.HYPERTENSION_LIFESTYLE_ONLY,
        hypertensionCf = 0.62,
        gbvCf = 0.0,
        hypertensionLevel = CfLevel.SEDANG,
        gbvLevel = CfLevel.RENDAH,
        relationship = "internal",
        contributingFactors = listOf("Gaya Hidup"),
        recommendations = listOf("Kurangi garam"),
        inconclusiveReasons = emptyList()
    )

    @Test
    fun `context preserves module names levels and certainty values`() {
        val context = HybridTriageBridge.build(
            hypertension = hypertensionResult(),
            gbv = gbvResult(),
            integrated = integrated(),
            kbVersion = "cf-kb-v2.0",
            capturedAt = "2026-11-11T09:12:00+07:00"
        )

        assertEquals("empati.screening_context/v1", context.schema)
        assertEquals("cf-kb-v2.0", context.kbVersion)
        assertEquals(2, context.modules.size)

        val hypertension = context.modules[0]
        assertEquals(ScreeningModule.HYPERTENSION, hypertension.module)
        assertEquals(CfLevel.SEDANG, hypertension.level)
        assertEquals(0.62, hypertension.cf, 0.0)
        assertEquals(15, hypertension.answered)
        assertEquals(1, hypertension.unanswered)
        assertFalse(hypertension.complete)
        assertEquals(listOf("M08"), hypertension.protectiveFactors)

        val gbv = context.modules[1]
        assertEquals(ScreeningModule.GBV, gbv.module)
        assertEquals(0, gbv.unanswered)
        assertTrue(gbv.complete)

        assertEquals(IntegratedCategory.HYPERTENSION_LIFESTYLE_ONLY, context.integrated?.category)
        assertTrue(context.integrated!!.indication)
    }

    @Test
    fun `dominant categories are sorted capped and keep their evidence`() {
        val context = HybridTriageBridge.build(
            hypertension = hypertensionResult(),
            gbv = null,
            integrated = null,
            kbVersion = "cf-kb-v2.0",
            capturedAt = "2026-11-11T09:12:00+07:00"
        )

        val categories = context.modules.single().dominantCategories
        assertEquals(3, categories.size)
        assertEquals(Hypothesis.HT_LIFESTYLE, categories[0].hypothesis)
        assertEquals(0.71, categories[0].cf, 0.0)
        assertEquals(Hypothesis.HT_MEDICAL, categories[1].hypothesis)
        assertEquals(Hypothesis.HT_GENETIC, categories[2].hypothesis)

        val lifestyleEvidence = categories[0].evidence
        assertEquals(2, lifestyleEvidence.size)
        assertEquals("M09", lifestyleEvidence[0].questionId)
        assertEquals(0.48, lifestyleEvidence[0].cfEvidence, 0.0)
        assertEquals("M10", lifestyleEvidence[1].questionId)
    }

    @Test
    fun `evidence per category is capped at three items`() {
        val wide = hypertensionResult().copy(
            categories = listOf(
                CategoryResult(
                    hypothesis = Hypothesis.HT_LIFESTYLE,
                    cf = 0.9,
                    contributions = (1..6).map {
                        evidence("M0$it", "Gaya Hidup", 0.9, 0.9, 0.9 - it * 0.05)
                    }
                )
            )
        )

        val context = HybridTriageBridge.build(wide, null, null, "cf-kb-v2.0", "now")

        assertEquals(3, context.modules.single().dominantCategories.single().evidence.size)
    }

    @Test
    fun `integrated section is absent when both modules are missing`() {
        val context = HybridTriageBridge.build(null, null, integrated(), "cf-kb-v2.0", "now")

        assertEquals(0, context.modules.size)
        assertEquals(IntegratedCategory.HYPERTENSION_LIFESTYLE_ONLY, context.integrated?.category)
    }

    @Test
    fun `json round trip keeps enum names and numeric values`() {
        val context = HybridTriageBridge.build(
            hypertension = hypertensionResult(),
            gbv = gbvResult(),
            integrated = integrated(),
            kbVersion = "cf-kb-v2.0",
            capturedAt = "now"
        )

        val json = HybridTriageBridge.toJson(context)
        val parsed = MiniJson.parse(json) as Map<*, *>
        assertEquals("empati.screening_context/v1", parsed["schema"])
        val modules = parsed["modules"] as List<*>
        val first = modules[0] as Map<*, *>
        assertEquals("HYPERTENSION", first["module"])
        assertEquals("SEDANG", first["level"])
        assertEquals(0.62, (first["cf"] as Number).toDouble(), 0.0001)
        val categories = first["dominant_categories"] as List<*>
        assertEquals("HT_LIFESTYLE", (categories[0] as Map<*, *>)["hypothesis"])
    }

    @Test
    fun `retrieval query uses clinical vocabulary without numbers or acronyms`() {
        val context = HybridTriageBridge.build(
            hypertension = hypertensionResult(),
            gbv = gbvResult(),
            integrated = integrated(),
            kbVersion = "cf-kb-v2.0",
            capturedAt = "now"
        )

        val query = HybridTriageBridge.toRetrievalQuery(context)

        assertTrue(query.contains("hipertensi"))
        assertTrue(query.contains("gaya hidup"))
        assertTrue(query.contains("sedang"))
        assertFalse("query must not leak certainty numbers: $query", Regex("\\d").containsMatchIn(query))
        assertFalse(query.lowercase().contains("cf"))
        assertFalse(query.contains("  "))
    }

    @Test
    fun `prompt block names the level and flags unanswered questions`() {
        val context = HybridTriageBridge.build(
            hypertension = hypertensionResult(),
            gbv = gbvResult(),
            integrated = integrated(),
            kbVersion = "cf-kb-v2.0",
            capturedAt = "now"
        )

        val block = HybridTriageBridge.toPromptBlock(context)

        assertTrue(block.contains("Sedang"))
        assertTrue(block.contains("Hipertensi"))
        assertTrue(block.contains("1 pertanyaan belum dijawab"))
        assertFalse(block.contains("CF"))
    }

    @Test
    fun `prompt block reports complete screening without unanswered note`() {
        val context = HybridTriageBridge.build(
            hypertension = hypertensionResult().copy(answeredCount = 16, complete = true),
            gbv = null,
            integrated = null,
            kbVersion = "cf-kb-v2.0",
            capturedAt = "now"
        )

        val block = HybridTriageBridge.toPromptBlock(context)

        assertFalse(block.contains("belum dijawab"))
    }

    @Test
    fun `prompt block carries internal confidence for tone but forbids mentioning it`() {
        val context = HybridTriageBridge.build(
            hypertension = hypertensionResult(),
            gbv = null,
            integrated = null,
            kbVersion = "cf-kb-v2.0",
            capturedAt = "now"
        )

        val block = HybridTriageBridge.toPromptBlock(context)

        assertTrue(block.contains("0.62"))
        assertTrue(block.contains("jangan menyebut angka"))
    }

    @Test
    fun `integrated category drives indication and the chronic stress note`() {
        val linked = HybridTriageBridge.build(
            hypertension = null,
            gbv = null,
            integrated = integrated().copy(category = IntegratedCategory.HYPERTENSION_WITH_GBV_INDICATION),
            kbVersion = "cf-kb-v2.0",
            capturedAt = "now"
        )
        assertTrue(linked.integrated!!.indication)
        assertEquals("chronic_stress_pathway", linked.integrated!!.relationshipNote)

        val low = HybridTriageBridge.build(
            hypertension = null,
            gbv = null,
            integrated = integrated().copy(category = IntegratedCategory.LOW_RISK),
            kbVersion = "cf-kb-v2.0",
            capturedAt = "now"
        )
        assertFalse(low.integrated!!.indication)
        assertNull(low.integrated!!.relationshipNote)
    }

    @Test
    fun `empty module list still produces a usable query`() {
        val context = HybridTriageBridge.build(null, null, null, "cf-kb-v2.0", "now")

        val query = HybridTriageBridge.toRetrievalQuery(context)

        assertNull(context.integrated)
        assertTrue(query.isNotBlank())
    }
}
