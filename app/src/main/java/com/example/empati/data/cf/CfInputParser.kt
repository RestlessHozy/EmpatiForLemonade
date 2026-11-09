package com.example.empati.data.cf

/**
 * Padanan Kotlin dari EMPATIDualInputParser (Python).
 *
 * Aturan:
 *  - Dikotomi: Ya = 1.0, Tidak = 0.0
 *  - Likert 5: 1=0.0, 2=0.2, 3=0.5, 4=0.8, 5=1.0
 *  - M08 reverse: 1=1.0, 2=0.8, 3=0.5, 4=0.2, 5=0.0
 *  - Jawaban kosong TIDAK dimasukkan sebagai 0.0 (tetap belum dijawab).
 *  - Input tidak valid menghasilkan ValidationIssue yang jelas.
 */
object CfInputParser {

    val likertMap: Map<Int, Double> = mapOf(
        1 to 0.0,
        2 to 0.2,
        3 to 0.5,
        4 to 0.8,
        5 to 1.0
    )

    val reverseLikertMap: Map<Int, Double> = mapOf(
        1 to 1.0,
        2 to 0.8,
        3 to 0.5,
        4 to 0.2,
        5 to 0.0
    )

    fun parse(
        questions: List<CfQuestion>,
        answers: Map<String, AnswerValue>
    ): ParseResult {
        val cfUser = LinkedHashMap<String, Double>()
        val issues = mutableListOf<ValidationIssue>()

        for (question in questions) {
            when (val answer = answers[question.id] ?: AnswerValue.Unanswered) {
                AnswerValue.Unanswered -> Unit // tidak dijawab: tidak masuk cfUser

                is AnswerValue.Dichotomy -> {
                    if (question.kind != QuestionKind.DICHOTOMY) {
                        issues += ValidationIssue(
                            question.id,
                            "Pertanyaan ${question.id} membutuhkan skala Likert 1-5, bukan jawaban Ya/Tidak."
                        )
                    } else {
                        cfUser[question.id] = if (answer.value) 1.0 else 0.0
                    }
                }

                is AnswerValue.Likert -> {
                    when {
                        question.kind != QuestionKind.LIKERT -> {
                            issues += ValidationIssue(
                                question.id,
                                "Pertanyaan ${question.id} membutuhkan jawaban Ya/Tidak, bukan skala Likert."
                            )
                        }
                        answer.value !in 1..5 -> {
                            issues += ValidationIssue(
                                question.id,
                                "Nilai skala untuk ${question.id} harus 1-5, ditemukan ${answer.value}."
                            )
                        }
                        else -> {
                            cfUser[question.id] = if (question.reverseScored) {
                                reverseLikertMap.getValue(answer.value)
                            } else {
                                likertMap.getValue(answer.value)
                            }
                        }
                    }
                }
            }
        }

        return ParseResult(cfUser = cfUser, issues = issues)
    }

    /** Nilai CF_User tunggal; null bila belum dijawab atau tidak valid. */
    fun parseSingle(question: CfQuestion, answer: AnswerValue): Double? =
        parse(listOf(question), mapOf(question.id to answer)).cfUser[question.id]
}
