package com.example.empati.data.triage

import com.example.empati.data.cf.CfLevel
import com.example.empati.data.cf.Hypothesis
import com.example.empati.data.cf.IntegratedAnalysis
import com.example.empati.data.cf.IntegratedCategory
import com.example.empati.data.cf.ModuleResult
import com.example.empati.data.cf.ScreeningModule
import com.example.empati.data.lemonade.MiniJson
import java.util.Locale

data class TriageEvidence(
    val questionId: String,
    val cfUser: Double,
    val cfExpert: Double,
    val cfEvidence: Double
)

data class TriageCategory(
    val hypothesis: Hypothesis,
    val cf: Double,
    val evidence: List<TriageEvidence>
)

data class TriageModule(
    val module: ScreeningModule,
    val level: CfLevel,
    val cf: Double,
    val answered: Int,
    val unanswered: Int,
    val complete: Boolean,
    val dominantCategories: List<TriageCategory>,
    val protectiveFactors: List<String>
)

data class TriageIntegrated(
    val category: IntegratedCategory,
    val indication: Boolean,
    val hypertensionCf: Double,
    val gbvCf: Double,
    val relationshipNote: String?
)

data class TriageContext(
    val schema: String,
    val kbVersion: String,
    val capturedAt: String,
    val modules: List<TriageModule>,
    val integrated: TriageIntegrated?
)

data class ReferenceChunk(
    val id: String,
    val title: String,
    val publisher: String,
    val year: Int,
    val section: String,
    val text: String,
    val url: String,
    val score: Double = 0.0
)

/**
 * Jembatan antara hasil deterministik Certainty Factor dan lapisan retrieval.
 * Modul ini murni: tidak menghitung apa pun, hanya memetakan dan menyerialkan
 * supaya angka yang sampai ke model sama persis dengan angka dari mesin.
 */
object HybridTriageBridge {

    const val SCHEMA = "empati.screening_context/v1"

    private const val MAX_CATEGORIES = 3
    private const val MAX_EVIDENCE = 3

    private const val DEFAULT_QUERY =
        "informasi kesehatan hipertensi dan kekerasan berbasis gender"

    fun build(
        hypertension: ModuleResult?,
        gbv: ModuleResult?,
        integrated: IntegratedAnalysis?,
        kbVersion: String,
        capturedAt: String
    ): TriageContext = TriageContext(
        schema = SCHEMA,
        kbVersion = kbVersion,
        capturedAt = capturedAt,
        modules = listOfNotNull(hypertension?.toTriageModule(), gbv?.toTriageModule()),
        integrated = integrated?.toTriageIntegrated()
    )

    fun toJson(context: TriageContext): String = MiniJson.write(
        mapOf(
            "schema" to context.schema,
            "captured_at" to context.capturedAt,
            "kb_version" to context.kbVersion,
            "modules" to context.modules.map { module ->
                mapOf(
                    "module" to module.module.name,
                    "level" to module.level.name,
                    "cf" to module.cf,
                    "answered" to module.answered,
                    "unanswered" to module.unanswered,
                    "complete" to module.complete,
                    "dominant_categories" to module.dominantCategories.map { category ->
                        mapOf(
                            "hypothesis" to category.hypothesis.name,
                            "cf" to category.cf,
                            "evidence" to category.evidence.map { evidence ->
                                mapOf(
                                    "question_id" to evidence.questionId,
                                    "cf_user" to evidence.cfUser,
                                    "cf_expert" to evidence.cfExpert,
                                    "cf_evidence" to evidence.cfEvidence
                                )
                            }
                        )
                    },
                    "protective_factors" to module.protectiveFactors
                )
            },
            "integrated" to context.integrated?.let { integrated ->
                mapOf(
                    "category" to integrated.category.name,
                    "indication" to integrated.indication,
                    "hypertension_cf" to integrated.hypertensionCf,
                    "gbv_cf" to integrated.gbvCf,
                    "relationship_note" to integrated.relationshipNote
                )
            }
        )
    )

    /**
     * Query untuk embedding. Memakai kosakata klinis dari label enum, bukan angka,
     * supaya hasil pencarian dekat dengan istilah yang dipakai literatur.
     */
    fun toRetrievalQuery(context: TriageContext): String {
        val terms = mutableListOf<String>()
        context.modules.forEach { module ->
            terms += module.module.title.lowercase()
            terms += module.level.label.lowercase()
            module.dominantCategories.take(2).forEach { terms += it.hypothesis.label.lowercase() }
        }
        context.integrated?.let { terms += it.category.label.lowercase() }

        val joined = terms.filter { it.isNotBlank() }.distinct().joinToString(" ")
        val query = if (joined.isBlank()) DEFAULT_QUERY else joined
        return query.replace(Regex("\\s+"), " ").trim()
    }

    fun toPromptBlock(context: TriageContext): String = buildString {
        appendLine("Hasil skrining pengguna:")
        if (context.modules.isEmpty()) {
            appendLine("- Belum ada hasil skrining pada sesi ini.")
        }
        context.modules.forEach { module ->
            val total = module.answered + module.unanswered
            append("- ${module.module.title}, tingkat risiko: ${module.level.label}")
            append(" (${module.answered} dari $total pertanyaan terjawab")
            if (!module.complete && module.unanswered > 0) {
                append(", ${module.unanswered} pertanyaan belum dijawab")
            }
            appendLine(").")
            appendLine(
                "- Kekuatan indikasi internal: ${formatConfidence(module.cf)} dari 1.00. " +
                    "Pakai hanya untuk memilih nada bicara, jangan menyebut angka ini ke pengguna."
            )
            if (module.dominantCategories.isNotEmpty()) {
                appendLine(
                    "- Faktor utama pada ${module.module.shortTitle}: " +
                        module.dominantCategories.joinToString(", ") { it.hypothesis.label }
                )
            }
            if (module.protectiveFactors.isNotEmpty()) {
                appendLine("- Faktor pelindung: ${module.protectiveFactors.joinToString(", ")}")
            }
        }
        context.integrated?.let { integrated ->
            appendLine("- Kategori kesimpulan: ${integrated.category.label}")
        }
    }.trimEnd()

    private fun formatConfidence(value: Double): String =
        String.format(Locale.US, "%.2f", value)

    private fun ModuleResult.toTriageModule(): TriageModule = TriageModule(
        module = module,
        level = level,
        cf = cf,
        answered = answeredCount,
        unanswered = (totalCount - answeredCount).coerceAtLeast(0),
        complete = complete,
        dominantCategories = categories
            .filter { it.cf > 0.0 }
            .sortedByDescending { it.cf }
            .take(MAX_CATEGORIES)
            .map { category ->
                TriageCategory(
                    hypothesis = category.hypothesis,
                    cf = category.cf,
                    evidence = category.contributions
                        .sortedByDescending { it.cfEvidence }
                        .take(MAX_EVIDENCE)
                        .map { contribution ->
                            TriageEvidence(
                                questionId = contribution.questionId,
                                cfUser = contribution.cfUser,
                                cfExpert = contribution.cfRule,
                                cfEvidence = contribution.cfEvidence
                            )
                        }
                )
            },
        protectiveFactors = protectiveFactors
    )

    private fun IntegratedAnalysis.toTriageIntegrated(): TriageIntegrated = TriageIntegrated(
        category = category,
        indication = category != IntegratedCategory.LOW_RISK &&
            category != IntegratedCategory.INCONCLUSIVE,
        hypertensionCf = hypertensionCf,
        gbvCf = gbvCf,
        relationshipNote = if (category == IntegratedCategory.HYPERTENSION_WITH_GBV_INDICATION) {
            "chronic_stress_pathway"
        } else {
            null
        }
    )
}
