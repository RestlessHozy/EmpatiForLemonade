package com.example.empati.data.presentation

import com.example.empati.data.RiskLevel

/**
 * Model hasil yang AMAN untuk ditampilkan ke pengguna.
 * Tidak mengandung CF, MB/MD, bobot, ID evidence/rule, atau istilah teknis.
 */

enum class PresentationTone { POSITIVE, ATTENTION, INCOMPLETE, NEUTRAL }

data class UserRiskSummary(
    val level: RiskLevel,
    val date: String
)

data class ClinicalTagDetail(
    val tag: String,
    val categoryBadge: String,
    val explanation: String,
    val linkageReminder: String
)

data class UserModulePresentation(
    val title: String,
    val isHypertension: Boolean,
    val statusLabel: String,
    val tone: PresentationTone,
    val conclusion: String,
    val factorsToNote: List<String>,
    val identifiedCategories: List<String>,
    val recommendations: List<String>,
    val supportNote: String?,
    val isComplete: Boolean,
    val disclaimer: String,
    val protectiveFactors: List<String> = emptyList()
)

data class UserIntegratedPresentation(
    val categoryLabel: String,
    val summary: String,
    val hypertension: UserModulePresentation,
    val gbv: UserModulePresentation,
    val relationshipExplanation: String,
    val factors: List<String>,
    val recommendations: List<String>,
    val disclaimer: String,
    val isInconclusive: Boolean,
    val protectiveFactors: List<String> = emptyList()
)
