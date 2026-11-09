package com.example.empati.data.cf

import kotlin.math.abs
import kotlin.math.min

/**
 * Inference engine Certainty Factor (MYCIN-style, general).
 *
 *  CF(H,E)      = MB(H,E) - MD(H,E)          (lihat CfRule.cfPakar)
 *  CF evidence  = CF_User x CF_Rule
 *  Gabungan positif : CF1 + CF2 x (1 - CF1)
 *  Gabungan negatif : CF1 + CF2 x (1 + CF1)
 *  Konflik          : (CF1 + CF2) / (1 - min(|CF1|, |CF2|))
 */
object CfInferenceEngine {

    fun cfEvidence(cfUser: Double, rule: CfRule): Double = cfUser * rule.cfPakar

    fun combine(cf1: Double, cf2: Double): Double = when {
        cf1 == 0.0 -> cf2
        cf2 == 0.0 -> cf1
        cf1 > 0 && cf2 > 0 -> cf1 + cf2 * (1 - cf1)
        cf1 < 0 && cf2 < 0 -> cf1 + cf2 * (1 + cf1)
        else -> {
            val denominator = 1 - min(abs(cf1), abs(cf2))
            if (denominator == 0.0) 0.0 else (cf1 + cf2) / denominator
        }
    }

    fun combineAll(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        var accumulator = values.first()
        for (index in 1 until values.size) {
            accumulator = combine(accumulator, values[index])
        }
        return accumulator
    }

    fun aggregateHypothesis(module: ScreeningModule): Hypothesis = when (module) {
        ScreeningModule.HYPERTENSION -> Hypothesis.HYPERTENSION
        ScreeningModule.GBV -> Hypothesis.GBV
    }

    /**
     * Evaluasi evidence per sub-hipotesis (kategori). Hanya kategori yang
     * memiliki evidence terjawab yang muncul. Urutan deterministik.
     */
    fun evaluate(
        module: ScreeningModule,
        cfUser: Map<String, Double>,
        rules: List<CfRule>
    ): List<CategoryResult> {
        val aggregate = aggregateHypothesis(module)
        val rulesByHypothesis = rules
            .filter { it.hypothesis.module == module && it.hypothesis != aggregate }
            .groupBy { it.hypothesis }

        val orderedHypotheses = Hypothesis.values()
            .filter { it.module == module && it != aggregate }

        val results = mutableListOf<CategoryResult>()
        for (hypothesis in orderedHypotheses) {
            val hypothesisRules = rulesByHypothesis[hypothesis] ?: continue
            val contributions = hypothesisRules
                .mapNotNull { rule ->
                    val cfUserValue = cfUser[rule.questionId] ?: return@mapNotNull null
                    EvidenceContribution(
                        questionId = rule.questionId,
                        category = hypothesis.label,
                        cfUser = cfUserValue,
                        cfRule = rule.cfPakar,
                        cfEvidence = cfEvidence(cfUserValue, rule)
                    )
                }
                .sortedBy { it.questionId }

            if (contributions.isNotEmpty()) {
                results += CategoryResult(
                    hypothesis = hypothesis,
                    cf = combineAll(contributions.map { it.cfEvidence }),
                    contributions = contributions
                )
            }
        }
        return results
    }

    /** CF modul = gabungan CF seluruh kategori yang terindikasi. */
    fun moduleCf(categories: List<CategoryResult>): Double =
        combineAll(categories.map { it.cf })

    fun levelOf(cf: Double): CfLevel = when {
        cf < 0.25 -> CfLevel.RENDAH
        cf < 0.50 -> CfLevel.SEDANG
        cf < 0.75 -> CfLevel.TINGGI
        else -> CfLevel.SANGAT_TINGGI
    }
}
