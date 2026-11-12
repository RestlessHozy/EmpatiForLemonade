package com.example.empati.data.chat

/**
 * Pemeriksa keluaran akhir. Angka dari mesin Certainty Factor boleh dipakai
 * model untuk memilih nada, tetapi tidak boleh sampai ke pembaca. Pemeriksa ini
 * bekerja pada teks yang sudah jadi, bukan pada prompt.
 */
object AnswerGuardrail {

    data class Result(
        val text: String,
        val violations: List<String>
    ) {
        val safe: Boolean get() = violations.isEmpty()
    }

    private val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
    private val internalAcronymRegex = Regex("\\b(CF|MB|MD)\\b")
    private val percentRegex = Regex("\\d+(?:[.,]\\d+)?\\s*%")
    private val percentWordRegex = Regex("(?i)\\bpersen\\b")
    private val scoreAfterMarkerRegex = Regex("(?i)(indikator|nilai|skor)\\s+\\d+(?:[.,]\\d+)?")
    private val diagnosisWords = listOf("menderita", "diagnosis", "positif terkena", "terdiagnosis")

    private const val SAPA_NOTE =
        "Bila Anda sedang menghadapi situasi yang tidak aman, Anda bisa menghubungi " +
            "SAPA 129 melalui telepon 129 atau WhatsApp 08111-129-129."

    fun enforce(raw: String, gbvIndicated: Boolean = false): Result {
        val violations = mutableListOf<String>()
        var text = raw.trim()

        text = boldRegex.replace(text, "$1")
        text = text.replace("*", "")

        if (internalAcronymRegex.containsMatchIn(text)) {
            violations += "internal-acronym"
            text = internalAcronymRegex.replace(text, "indikator")
        }

        if (percentRegex.containsMatchIn(text) || percentWordRegex.containsMatchIn(text)) {
            violations += "percentage"
            text = percentRegex.replace(text, "indikator")
            text = percentWordRegex.replace(text, "indikator")
        }

        if (scoreAfterMarkerRegex.containsMatchIn(text)) {
            violations += "internal-score"
            text = scoreAfterMarkerRegex.replace(text) { match ->
                match.groupValues[1] + " yang sudah dihitung aplikasi"
            }
        }

        val lower = text.lowercase()
        if (diagnosisWords.any { lower.contains(it) }) {
            violations += "diagnosis"
        }

        text = text.replace(Regex("\\s+"), " ").trim()

        if (gbvIndicated && !text.contains("08111-129-129")) {
            text = if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?")) {
                "$text $SAPA_NOTE"
            } else {
                "$text. $SAPA_NOTE"
            }
        }

        if (text.isNotEmpty() && text.last() !in ".!?") {
            text += "."
        }

        return Result(text = text, violations = violations)
    }
}
