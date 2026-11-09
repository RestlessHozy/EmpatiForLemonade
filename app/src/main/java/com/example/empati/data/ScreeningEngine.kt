package com.example.empati.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(date: Date): String =
    SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(date)

fun computeResult(questions: List<Question>, answers: List<Int>): ScreeningResult {
    val maxPoints = questions.sumOf { q -> q.options.maxOf { it.points } }
    val totalPoints = questions.indices.sumOf { i ->
        val idx = answers.getOrNull(i) ?: -1
        questions[i].options.getOrNull(idx)?.points ?: 0
    }
    val score = if (maxPoints == 0) 0 else ((totalPoints.toFloat() / maxPoints) * 100f).toInt().coerceIn(0, 100)
    val level = when {
        score < 25 -> RiskLevel.LOW
        score < 50 -> RiskLevel.MEDIUM
        score < 75 -> RiskLevel.HIGH
        else -> RiskLevel.VERY_HIGH
    }
    return ScreeningResult(score = score, level = level, date = formatDate(Date()))
}

fun buildInsights(questions: List<Question>, answers: List<Int>): List<Insight> {
    fun idx(id: Int) = questions.indexOfFirst { it.id == id }
    fun points(id: Int) = answers.getOrNull(idx(id))?.let { i -> questions[idx(id)].options.getOrNull(i)?.points ?: 0 } ?: 0
    fun pointsSum(ids: List<Int>) = ids.sumOf { points(it) }

    val candidates = mutableListOf<Insight>()

    if (points(1) + points(5) > 0) {
        candidates += Insight(
            InsightIcon.BP,
            "Tekanan darah",
            "Tekanan darah Anda cenderung tinggi. Pantau secara berkala bersama tenaga kesehatan."
        )
    }

    if (pointsSum((17..24).toList()) > 0) {
        candidates += Insight(
            InsightIcon.GBV,
            "Pengalaman stres & kekerasan",
            "Pengalaman kekerasan dapat meningkatkan risiko hipertensi. Anda tidak sendirian, dan ada bantuan yang tersedia."
        )
    }

    if (points(2) > 0) {
        candidates += Insight(
            InsightIcon.FAMILY,
            "Riwayat keluarga",
            "Ada riwayat hipertensi dalam keluarga, sehingga pemantauan dini lebih disarankan."
        )
    }

    if (pointsSum(listOf(7, 8, 9, 10, 11, 12, 13, 16)) > 0) {
        candidates += Insight(
            InsightIcon.LIFESTYLE,
            "Gaya hidup & stres",
            "Pola makan, aktivitas fisik, dan kualitas tidur Anda dapat disesuaikan untuk menurunkan risiko."
        )
    }

    val defaults = listOf(
        Insight(InsightIcon.BP, "Tekanan darah", "Pantau tekanan darah secara rutin untuk mendeteksi perubahan sejak dini."),
        Insight(InsightIcon.LIFESTYLE, "Gaya hidup & stres", "Pola makan seimbang, aktivitas fisik, dan tidur cukup membantu menurunkan risiko."),
        Insight(InsightIcon.FAMILY, "Riwayat keluarga", "Riwayat keluarga adalah faktor yang tidak dapat diubah, namun tetap dapat dipantau.")
    )
    for (d in defaults) {
        if (candidates.size >= 3) break
        if (candidates.none { it.icon == d.icon }) candidates += d
    }
    return candidates.take(3)
}

fun buildRecommendations(level: RiskLevel, hasGbvRisk: Boolean): List<String> {
    val recs = mutableListOf<String>()
    if (level == RiskLevel.HIGH || level == RiskLevel.VERY_HIGH) {
        recs += "Segera konsultasikan hasil ini kepada dokter atau puskesmas terdekat."
    } else {
        recs += "Konsultasikan hasil ini kepada dokter bila Anda merasa khawatir."
    }
    recs += "Kurangi asupan garam dan perbanyak buah serta sayur."
    recs += "Lakukan aktivitas fisik ringan minimal 30 menit sehari."
    recs += "Kelola stres dengan istirahat cukup dan dukungan sosial yang sehat."
    if (hasGbvRisk) {
        recs += "Jika Anda merasa tidak aman, hubungi layanan SAPA 129 atau tenaga kesehatan terdekat."
    }
    return recs
}

fun hasGbvRisk(questions: List<Question>, answers: List<Int>): Boolean {
    return questions.filter { it.sensitive }.any { q ->
        val idx = questions.indexOfFirst { it.id == q.id }
        val points = answers.getOrNull(idx)?.let { q.options.getOrNull(it)?.points ?: 0 } ?: 0
        points > 0
    }
}
