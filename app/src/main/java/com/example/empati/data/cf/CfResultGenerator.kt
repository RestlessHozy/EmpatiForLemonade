package com.example.empati.data.cf

import com.example.empati.data.RiskLevel
import com.example.empati.data.ScreeningResult
import com.example.empati.data.formatDate
import java.util.Date
import java.util.Locale

object CfResultGenerator {

    fun formatCf(value: Double): String = String.format(Locale.US, "%.2f", value)

    fun buildModuleResult(
        module: ScreeningModule,
        questions: List<CfQuestion>,
        answers: Map<String, AnswerValue>
    ): ModuleResult {
        val parsed = CfInputParser.parse(questions, answers)
        val rules = rulesFor(module)
        val categories = CfInferenceEngine.evaluate(module, parsed.cfUser, rules)
        val rawCf = CfInferenceEngine.moduleCf(categories)
        val answeredCount = parsed.cfUser.size
        val complete = parsed.isValid && answeredCount == questions.size
        val hasDiagnosis = (answers["M01"] as? AnswerValue.Dichotomy)?.value == true
        val uncontrolledBp = (answers["M05"] as? AnswerValue.Dichotomy)?.value == true
        val hasDiabetes = (answers["M03"] as? AnswerValue.Dichotomy)?.value == true
        val nonMedicalCf = CfInferenceEngine.combineAll(
            categories.filter { it.hypothesis != Hypothesis.HT_MEDICAL }.map { it.cf }
        )
        // Jika memiliki riwayat diagnosis hipertensi (M01=Ya) tetapi tensi saat ini terkendali (M05=Tidak),
        // tanpa komorbid diabetes (M03=Tidak), dan faktor risiko gaya hidup/psikososial rendah (<0.50),
        // maka tingkat risikonya adalah SEDANG (Perlu Pemantauan), bukan Tinggi/Sangat Tinggi.
        val cf = if (module == ScreeningModule.HYPERTENSION &&
            hasDiagnosis && !uncontrolledBp && !hasDiabetes && nonMedicalCf < 0.50
        ) {
            0.40
        } else {
            rawCf
        }
        val level = CfInferenceEngine.levelOf(cf)

        return ModuleResult(
            module = module,
            cf = cf,
            level = level,
            interpretation = interpretationFor(module, cf, level, hasDiagnosis, complete),
            categories = categories,
            dominantFactors = buildDominantFactors(categories),
            protectiveFactors = if (module == ScreeningModule.HYPERTENSION)
                buildProtectiveFactors(answers) else emptyList(),
            hasDiagnosis = hasDiagnosis,
            answeredCount = answeredCount,
            totalCount = questions.size,
            complete = complete
        )
    }

    private fun interpretationFor(
        module: ScreeningModule,
        cf: Double,
        level: CfLevel,
        hasDiagnosis: Boolean,
        complete: Boolean
    ): String {
        if (!complete) {
            return "Sebagian pertanyaan belum dijawab, sehingga hasil ${module.shortTitle} belum lengkap."
        }
        return when (module) {
            ScreeningModule.HYPERTENSION -> if (hasDiagnosis) {
                "Terdapat riwayat diagnosis hipertensi. Tingkat keyakinan terhadap indikasi risiko saat ini: " +
                    "${level.label} (CF ${formatCf(cf)})."
            } else {
                "Berdasarkan jawaban Anda, tingkat keyakinan terhadap indikasi risiko hipertensi adalah " +
                    "${level.label} (CF ${formatCf(cf)})."
            }

            ScreeningModule.GBV -> if (cf < 0.25) {
                "Tidak terdapat indikasi kuat pengalaman kekerasan berbasis gender berdasarkan jawaban Anda " +
                    "(CF ${formatCf(cf)})."
            } else {
                "Terdapat indikasi pengalaman kekerasan berbasis gender dengan tingkat keyakinan " +
                    "${level.label} (CF ${formatCf(cf)})."
            }
        }
    }

    private fun buildDominantFactors(categories: List<CategoryResult>): List<String> =
        categories
            .filter { it.cf > 0.0 }
            .sortedByDescending { it.cf }
            .take(3)
            .map { "${it.hypothesis.label} (CF ${formatCf(it.cf)})" }

    private fun buildProtectiveFactors(answers: Map<String, AnswerValue>): List<String> {
        val out = mutableListOf<String>()
        fun no(id: String) = (answers[id] as? AnswerValue.Dichotomy)?.value == false
        fun likert(id: String) = (answers[id] as? AnswerValue.Likert)?.value

        if (no("M01")) out += "Tidak ada riwayat diagnosis hipertensi"
        if (no("M02")) out += "Tidak ada riwayat keluarga hipertensi"
        if (no("M03")) out += "Tidak ada riwayat diabetes melitus"
        if (no("M05")) out += "Tekanan darah dalam batas normal"
        if ((likert("M08") ?: 0) >= 4) out += "Aktivitas fisik teratur"
        if (no("M09")) out += "Tidak merokok / terpapar asap rokok"
        if (no("M11")) out += "Berat badan dalam kisaran sehat"
        if (no("M13")) out += "Durasi tidur cukup"
        if (no("M15")) out += "Rutin memeriksa tekanan darah"
        if (no("M16")) out += "Menghindari makanan cepat saji"

        return out.take(5)
    }

    private fun dominantCategory(result: ModuleResult): Hypothesis? =
        result.categories.maxByOrNull { it.cf }?.hypothesis

    fun integrate(ht: ModuleResult, gbv: ModuleResult): IntegratedAnalysis {
        val reasons = mutableListOf<String>()
        if (!ht.complete) reasons += "Modul hipertensi belum lengkap (${ht.answeredCount}/${ht.totalCount})."
        if (!gbv.complete) reasons += "Modul KBG belum lengkap (${gbv.answeredCount}/${gbv.totalCount})."

        if (reasons.isNotEmpty()) {
            return IntegratedAnalysis(
                category = IntegratedCategory.INCONCLUSIVE,
                hypertensionCf = ht.cf,
                gbvCf = gbv.cf,
                hypertensionLevel = ht.level,
                gbvLevel = gbv.level,
                relationship = "Data belum lengkap, sehingga pola hubungan antarindikator belum dapat dianalisis.",
                contributingFactors = emptyList(),
                recommendations = listOf("Lengkapi kedua modul skrining untuk memperoleh analisis terintegrasi."),
                inconclusiveReasons = reasons
            )
        }

        val htnCf = ht.cf
        val gbvCf = gbv.cf
        val htnIndicated = htnCf >= 0.25
        val gbvIndicated = gbvCf >= 0.25

        val category = when {
            !htnIndicated && !gbvIndicated -> IntegratedCategory.LOW_RISK
            !htnIndicated && gbvIndicated -> IntegratedCategory.GBV_ONLY
            htnIndicated && gbvIndicated -> IntegratedCategory.HYPERTENSION_WITH_GBV_INDICATION
            ht.hasDiagnosis -> IntegratedCategory.HYPERTENSION_MEDICAL_HISTORY
            dominantCategory(ht) == Hypothesis.HT_LIFESTYLE -> IntegratedCategory.HYPERTENSION_LIFESTYLE_ONLY
            else -> IntegratedCategory.HYPERTENSION_MEDICAL_HISTORY
        }

        val relationship = when (category) {
            IntegratedCategory.HYPERTENSION_WITH_GBV_INDICATION ->
                "Kedua modul menunjukkan indikasi yang meningkat. Terdapat pola keterkaitan antara indikasi KBG " +
                    "dan risiko hipertensi yang perlu diteliti lebih lanjut. Pola ini BUKAN bukti kausalitas."
            IntegratedCategory.HYPERTENSION_LIFESTYLE_ONLY ->
                "Indikasi hipertensi meningkat tanpa indikasi KBG yang berarti; faktor gaya hidup tampak dominan."
            IntegratedCategory.HYPERTENSION_MEDICAL_HISTORY ->
                "Indikasi hipertensi meningkat dan berkaitan dengan riwayat/kondisi medis; " +
                    "tidak ditemukan indikasi KBG yang berarti."
            IntegratedCategory.LOW_RISK ->
                "Kedua indikator berada pada tingkat rendah."
            IntegratedCategory.GBV_ONLY ->
                "Terdapat indikasi KBG tanpa indikasi hipertensi yang berarti. Hasil KBG tetap dilaporkan " +
                    "secara independen dan tidak otomatis dianggap sebagai penyebab hipertensi."
            IntegratedCategory.INCONCLUSIVE -> "Data belum lengkap."
        }

        val contributing = buildList {
            ht.dominantFactors.forEach { add("Hipertensi: $it") }
            gbv.dominantFactors.forEach { add("KBG: $it") }
        }.take(6)

        return IntegratedAnalysis(
            category = category,
            hypertensionCf = htnCf,
            gbvCf = gbvCf,
            hypertensionLevel = ht.level,
            gbvLevel = gbv.level,
            relationship = relationship,
            contributingFactors = contributing,
            recommendations = recommendationsFor(category),
            inconclusiveReasons = emptyList()
        )
    }

    private fun recommendationsFor(category: IntegratedCategory): List<String> {
        val recs = mutableListOf<String>()
        when (category) {
            IntegratedCategory.HYPERTENSION_WITH_GBV_INDICATION -> {
                recs += "Konsultasikan hasil ini kepada tenaga kesehatan dan layanan pendampingan."
                recs += "Pertimbangkan pendampingan psikologis untuk mengelola stres kronis."
                recs += "Hubungi SAPA 129 bila Anda membutuhkan dukungan terkait kekerasan."
            }
            IntegratedCategory.HYPERTENSION_LIFESTYLE_ONLY -> {
                recs += "Konsultasikan risiko hipertensi kepada dokter atau puskesmas."
                recs += "Perbaiki pola makan, aktivitas fisik, dan kualitas tidur."
            }
            IntegratedCategory.HYPERTENSION_MEDICAL_HISTORY -> {
                recs += "Lanjutkan pemantauan tekanan darah bersama tenaga kesehatan."
                recs += "Diskusikan riwayat medis Anda dengan dokter."
            }
            IntegratedCategory.LOW_RISK -> {
                recs += "Pertahankan gaya hidup sehat dan lakukan pemeriksaan tekanan darah berkala."
            }
            IntegratedCategory.GBV_ONLY -> {
                recs += "Hasil KBG perlu ditindaklanjuti secara independen."
                recs += "Hubungi SAPA 129 atau layanan pendampingan bila Anda merasa tidak aman."
                recs += "Pertimbangkan dukungan psikologis."
            }
            IntegratedCategory.INCONCLUSIVE -> {
                recs += "Lengkapi kedua modul skrining."
            }
        }
        recs += "Hasil ini adalah skrining, bukan diagnosis medis maupun psikologis, dan bukan pembuktian sebab-akibat."
        return recs
    }

    /** Ringkasan untuk Home/History/Chat (berbasis CF hipertensi). */
    fun toScreeningResult(analysis: IntegratedAnalysis): ScreeningResult {
        val score = (analysis.hypertensionCf.coerceIn(0.0, 1.0) * 100f).toInt()
        val level = when (analysis.hypertensionLevel) {
            CfLevel.RENDAH -> RiskLevel.LOW
            CfLevel.SEDANG -> RiskLevel.MEDIUM
            CfLevel.TINGGI -> RiskLevel.HIGH
            CfLevel.SANGAT_TINGGI -> RiskLevel.VERY_HIGH
        }
        return ScreeningResult(score = score, level = level, date = formatDate(Date()))
    }
}
