package com.example.empati.data

import com.example.empati.data.rag.RagDocumentChunk
import com.example.empati.data.triage.ReferenceChunk

enum class RiskLevel { LOW, MEDIUM, HIGH, VERY_HIGH }

val RiskLevel.label: String
    get() = when (this) {
        RiskLevel.LOW -> "Rendah"
        RiskLevel.MEDIUM -> "Sedang"
        RiskLevel.HIGH -> "Tinggi"
        RiskLevel.VERY_HIGH -> "Sangat Tinggi"
    }

val RiskLevel.statusLabel: String
    get() = when (this) {
        RiskLevel.LOW -> "Rendah · Batas Wajar"
        RiskLevel.MEDIUM -> "Sedang · Perlu Pemantauan"
        RiskLevel.HIGH -> "Tinggi · Perlu Perhatian"
        RiskLevel.VERY_HIGH -> "Sangat Tinggi · Perlu Perhatian"
    }

data class AnswerOption(val label: String, val points: Int)

data class Question(
    val id: Int,
    val section: String,
    val text: String,
    val sensitive: Boolean = false,
    val options: List<AnswerOption>
)

data class ScreeningResult(
    val score: Int,
    val level: RiskLevel,
    val date: String
)

enum class InsightIcon { BP, GBV, FAMILY, LIFESTYLE }

data class Insight(
    val icon: InsightIcon,
    val title: String,
    val body: String
)

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val retrievedChunks: List<RagDocumentChunk> = emptyList(),
    val citations: List<ReferenceChunk> = emptyList(),
    val modelBadge: String? = null
)

data class UserProfile(
    val name: String = "",
    val nip: String = "",
    val instansi: String = "",
    val jabatan: String = "",
    val jenisAsn: String = "Masyarakat Umum",
    val golongan: String = "Perempuan"
)

fun UserProfile.initials(): String =
    name.trim().split(" ").filter { it.isNotBlank() }
        .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "SE" }
