package com.example.empati.data.cf

/**
 * Domain model untuk sistem pakar Certainty Factor dual-modul AI-EMPATI.
 */

enum class ScreeningModule(val title: String, val shortTitle: String) {
    HYPERTENSION("Skrining Hipertensi", "Hipertensi"),
    GBV("Skrining Kekerasan Berbasis Gender", "KBG")
}

enum class QuestionKind { DICHOTOMY, LIKERT }

data class CfQuestion(
    val id: String,
    val module: ScreeningModule,
    val category: String,
    val text: String,
    val kind: QuestionKind,
    val reverseScored: Boolean = false,
    val sensitive: Boolean = false
)

/** Jawaban mentah dari UI. Unanswered berarti belum dijawab (bukan CF 0.0). */
sealed interface AnswerValue {
    data object Unanswered : AnswerValue
    data class Dichotomy(val value: Boolean) : AnswerValue
    data class Likert(val value: Int) : AnswerValue
}

data class ValidationIssue(val questionId: String, val message: String)

data class ParseResult(
    val cfUser: Map<String, Double>,
    val issues: List<ValidationIssue>
) {
    val isValid: Boolean get() = issues.isEmpty()
}

/**
 * Hipotesis. Modul hipertensi memiliki sub-hipotesis per kategori dan satu
 * hipotesis agregat; modul KBG serupa. Pemisahan ini mencegah seluruh evidence
 * dicampur ke satu nilai CF tanpa penjelasan.
 */
enum class Hypothesis(val module: ScreeningModule, val label: String) {
    HT_GENETIC(ScreeningModule.HYPERTENSION, "Riwayat & Genetik"),
    HT_MEDICAL(ScreeningModule.HYPERTENSION, "Kondisi Medis"),
    HT_LIFESTYLE(ScreeningModule.HYPERTENSION, "Gaya Hidup"),
    HT_PSYCHOSOCIAL(ScreeningModule.HYPERTENSION, "Faktor Psikososial"),
    HYPERTENSION(ScreeningModule.HYPERTENSION, "Indikasi Hipertensi"),

    GBV_PHYSICAL(ScreeningModule.GBV, "Kekerasan Fisik"),
    GBV_PSYCHOLOGICAL(ScreeningModule.GBV, "Kekerasan Psikologis"),
    GBV_SEXUAL(ScreeningModule.GBV, "Kekerasan Seksual"),
    GBV_ECONOMIC(ScreeningModule.GBV, "Kekerasan Ekonomi"),
    GBV_DIGITAL(ScreeningModule.GBV, "Kekerasan Digital"),
    GBV(ScreeningModule.GBV, "Indikasi KBG")
}

enum class CfLevel(val label: String) {
    RENDAH("Rendah"),
    SEDANG("Sedang"),
    TINGGI("Tinggi"),
    SANGAT_TINGGI("Sangat Tinggi")
}

data class EvidenceContribution(
    val questionId: String,
    val category: String,
    val cfUser: Double,
    val cfRule: Double,
    val cfEvidence: Double
)

data class CategoryResult(
    val hypothesis: Hypothesis,
    val cf: Double,
    val contributions: List<EvidenceContribution>
)

data class ModuleResult(
    val module: ScreeningModule,
    val cf: Double,
    val level: CfLevel,
    val interpretation: String,
    val categories: List<CategoryResult>,
    val dominantFactors: List<String>,
    val protectiveFactors: List<String>,
    val hasDiagnosis: Boolean,
    val answeredCount: Int,
    val totalCount: Int,
    val complete: Boolean
)

enum class IntegratedCategory(val label: String) {
    HYPERTENSION_LIFESTYLE_ONLY("Hipertensi Murni Faktor Gaya Hidup"),
    HYPERTENSION_WITH_GBV_INDICATION("Hipertensi dengan Indikasi Keterkaitan KBG"),
    LOW_RISK("Risiko Hipertensi Rendah / Normal"),
    GBV_ONLY("Indikasi KBG Tanpa Indikasi Hipertensi"),
    HYPERTENSION_MEDICAL_HISTORY("Hipertensi dengan Riwayat / Kondisi Medis"),
    INCONCLUSIVE("Hasil Belum Dapat Disimpulkan")
}

data class IntegratedAnalysis(
    val category: IntegratedCategory,
    val hypertensionCf: Double,
    val gbvCf: Double,
    val hypertensionLevel: CfLevel,
    val gbvLevel: CfLevel,
    val relationship: String,
    val contributingFactors: List<String>,
    val recommendations: List<String>,
    val inconclusiveReasons: List<String>
)
