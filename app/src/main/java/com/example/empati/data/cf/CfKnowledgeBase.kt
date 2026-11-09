package com.example.empati.data.cf

/**
 * Satu aturan pakar: evidence (pertanyaan) mendukung hipotesis dengan MB/MD.
 * CF pakar = MB - MD.
 *
 * PENTING: seluruh bobot di bawah ini adalah PLACEHOLDER dan BELUM
 * divalidasi pakar. Setiap aturan ditandai expertValidated = false.
 */
data class CfRule(
    val id: String,
    val questionId: String,
    val hypothesis: Hypothesis,
    val mb: Double,
    val md: Double,
    val source: String,
    val expertValidated: Boolean = false,
    val note: String = ""
) {
    val cfPakar: Double get() = mb - md
}

private const val PLACEHOLDER_SOURCE = "Rujukan klinis umum (placeholder)"
private const val NEEDS_VALIDATION = "NEEDS_EXPERT_VALIDATION"

private fun rule(
    questionId: String,
    hypothesis: Hypothesis,
    mb: Double,
    md: Double
) = CfRule(
    id = "R-$questionId",
    questionId = questionId,
    hypothesis = hypothesis,
    mb = mb,
    md = md,
    source = PLACEHOLDER_SOURCE,
    expertValidated = false,
    note = NEEDS_VALIDATION
)

val hypertensionRules: List<CfRule> = listOf(
    // Riwayat Kesehatan
    rule("M01", Hypothesis.HT_MEDICAL, 0.95, 0.05),
    rule("M02", Hypothesis.HT_GENETIC, 0.85, 0.10),
    rule("M03", Hypothesis.HT_MEDICAL, 0.85, 0.10),
    rule("M04", Hypothesis.HT_MEDICAL, 0.80, 0.10),
    rule("M05", Hypothesis.HT_MEDICAL, 0.90, 0.05),
    rule("M06", Hypothesis.HT_MEDICAL, 0.70, 0.15),

    // Gaya Hidup
    rule("M07", Hypothesis.HT_LIFESTYLE, 0.75, 0.15),
    rule("M08", Hypothesis.HT_LIFESTYLE, 0.70, 0.15),
    rule("M09", Hypothesis.HT_LIFESTYLE, 0.75, 0.15),
    rule("M10", Hypothesis.HT_LIFESTYLE, 0.65, 0.20),
    rule("M11", Hypothesis.HT_LIFESTYLE, 0.80, 0.10),

    // Faktor Risiko
    rule("M12", Hypothesis.HT_PSYCHOSOCIAL, 0.75, 0.15),
    rule("M13", Hypothesis.HT_PSYCHOSOCIAL, 0.70, 0.15),
    rule("M14", Hypothesis.HT_PSYCHOSOCIAL, 0.75, 0.15),
    rule("M15", Hypothesis.HT_MEDICAL, 0.65, 0.20),
    rule("M16", Hypothesis.HT_LIFESTYLE, 0.65, 0.20)
)

val gbvRules: List<CfRule> = listOf(
    rule("K01", Hypothesis.GBV_PHYSICAL, 0.90, 0.05),
    rule("K02", Hypothesis.GBV_PHYSICAL, 0.85, 0.10),
    rule("K03", Hypothesis.GBV_PSYCHOLOGICAL, 0.85, 0.10),
    rule("K04", Hypothesis.GBV_PSYCHOLOGICAL, 0.80, 0.10),
    rule("K05", Hypothesis.GBV_SEXUAL, 0.90, 0.05),
    rule("K06", Hypothesis.GBV_SEXUAL, 0.95, 0.03),
    rule("K07", Hypothesis.GBV_ECONOMIC, 0.80, 0.10),
    rule("K08", Hypothesis.GBV_ECONOMIC, 0.80, 0.10),
    rule("K09", Hypothesis.GBV_DIGITAL, 0.75, 0.10),
    rule("K10", Hypothesis.GBV_DIGITAL, 0.80, 0.10)
)

val allCfRules: List<CfRule> = hypertensionRules + gbvRules

fun rulesFor(module: ScreeningModule): List<CfRule> = when (module) {
    ScreeningModule.HYPERTENSION -> hypertensionRules
    ScreeningModule.GBV -> gbvRules
}
