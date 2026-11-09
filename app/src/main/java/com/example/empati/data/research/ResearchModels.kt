package com.example.empati.data.research

import com.example.empati.data.cf.IntegratedAnalysis
import com.example.empati.data.cf.ModuleResult
import java.util.UUID

/**
 * Model INTERNAL untuk penelitian disertasi. Tidak boleh dirender di UI pengguna.
 * Disimpan pada penyimpanan privat aplikasi bersama persetujuan responden.
 * Tidak ada nilai ini yang dicatat ke log aplikasi.
 */

data class ResearchEvidence(
    val questionId: String,
    val cfUser: Double,
    val cfRule: Double,
    val cfEvidence: Double
)

data class ResearchCategory(
    val hypothesis: String,
    val cf: Double,
    val evidence: List<ResearchEvidence>
)

data class ResearchModule(
    val module: String,
    val cf: Double,
    val level: String,
    val complete: Boolean,
    val answered: Int,
    val total: Int,
    val categories: List<ResearchCategory>
)

data class ResearchRecord(
    val screeningId: String,
    val timestamp: Long,
    val hypertension: ResearchModule,
    val gbv: ResearchModule,
    val integratedCategory: String,
    val hypertensionCf: Double,
    val gbvCf: Double
)

object ResearchRecordFactory {

    fun create(
        hypertension: ModuleResult,
        gbv: ModuleResult,
        analysis: IntegratedAnalysis
    ): ResearchRecord = ResearchRecord(
        screeningId = UUID.randomUUID().toString(),
        timestamp = System.currentTimeMillis(),
        hypertension = toResearchModule(hypertension),
        gbv = toResearchModule(gbv),
        integratedCategory = analysis.category.name,
        hypertensionCf = analysis.hypertensionCf,
        gbvCf = analysis.gbvCf
    )

    private fun toResearchModule(module: ModuleResult): ResearchModule = ResearchModule(
        module = module.module.name,
        cf = module.cf,
        level = module.level.name,
        complete = module.complete,
        answered = module.answeredCount,
        total = module.totalCount,
        categories = module.categories.map { category ->
            ResearchCategory(
                hypothesis = category.hypothesis.name,
                cf = category.cf,
                evidence = category.contributions.map { contribution ->
                    ResearchEvidence(
                        questionId = contribution.questionId,
                        cfUser = contribution.cfUser,
                        cfRule = contribution.cfRule,
                        cfEvidence = contribution.cfEvidence
                    )
                }
            )
        }
    )
}
