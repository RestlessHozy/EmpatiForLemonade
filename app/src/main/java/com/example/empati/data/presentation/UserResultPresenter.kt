package com.example.empati.data.presentation

import com.example.empati.data.RiskLevel
import com.example.empati.data.ScreeningResult
import com.example.empati.data.label
import com.example.empati.data.cf.CfLevel
import com.example.empati.data.cf.Hypothesis
import com.example.empati.data.cf.IntegratedAnalysis
import com.example.empati.data.cf.IntegratedCategory
import com.example.empati.data.cf.ModuleResult
import com.example.empati.data.cf.ScreeningModule
import com.example.empati.data.rag.ScreeningContextSnapshot

/**
 * Menerjemahkan model perhitungan internal (Certainty Factor) menjadi ringkasan klinis
 * berbahasa awam yang ringkas, padat, dan mudah dipindai (scannable).
 * Tidak ada nilai CF, MB/MD, bobot, ID evidence/rule, atau istilah inference yang
 * diteruskan ke lapisan presentasi pengguna.
 */
object UserResultPresenter {

    private const val USER_DISCLAIMER =
        "Hasil skrining ini adalah panduan awal untuk mengenali kondisi diri Anda, bukan diagnosis medis maupun penentu sebab-akibat langsung."

    private const val INDICATION_THRESHOLD = 0.25

    fun presentRiskSummary(result: ScreeningResult): UserRiskSummary =
        UserRiskSummary(level = result.level, date = result.date)

    /**
     * Membangun jembatan konteks satu arah (`ScreeningContextSnapshot`) yang kaya secara kualitatif
     * (100% Zero-Leak: tanpa angka CF, MB, MD, maupun persentase) untuk digunakan oleh LLM Empati
     * saat pengguna meminta analisis atau kesimpulan dari hasil skriningnya.
     */
    fun buildContextSnapshot(
        lastResult: ScreeningResult?,
        integrated: UserIntegratedPresentation? = null,
        fallbackCategoryLabel: String? = null
    ): ScreeningContextSnapshot? {
        if (lastResult == null) return null
        if (integrated != null) {
            return ScreeningContextSnapshot(
                riskLevelLabel = lastResult.level.label,
                integratedCategoryLabel = integrated.categoryLabel,
                screeningDate = lastResult.date,
                summaryNarrative = integrated.summary,
                hypertensionStatusLabel = integrated.hypertension.statusLabel,
                gbvStatusLabel = integrated.gbv.statusLabel,
                keyFactors = integrated.factors,
                protectiveFactors = integrated.protectiveFactors,
                relationshipExplanation = integrated.relationshipExplanation,
                keyRecommendations = integrated.recommendations
            )
        }

        val hasGbvInCategory = fallbackCategoryLabel?.contains("KBG", ignoreCase = true) == true
        val htStatus = when (lastResult.level) {
            RiskLevel.LOW -> "Rendah · Batas Wajar"
            RiskLevel.MEDIUM -> "Sedang · Perlu Pemantauan"
            RiskLevel.HIGH -> "Tinggi · Perlu Perhatian"
            RiskLevel.VERY_HIGH -> "Sangat Tinggi · Perlu Perhatian"
        }
        val gbvStatus = if (hasGbvInCategory) "Terdapat indikasi" else "Tidak terindikasi"
        val inferredSummary = when {
            hasGbvInCategory && lastResult.level != RiskLevel.LOW ->
                "Terdapat tanda risiko hipertensi yang muncul bersamaan dengan beban pengalaman kekerasan atau relasi yang tidak aman, sehingga baik kesehatan fisik maupun ketenangan batin Anda sama-sama perlu diperhatikan."
            hasGbvInCategory ->
                "Kondisi tekanan darah Anda saat ini masih terjaga, namun ada tanda pengalaman kekerasan atau ketidakteramanan relasi yang patut mendapat perhatian dan dukungan."
            lastResult.level == RiskLevel.LOW ->
                "Saat ini kondisi tekanan darah maupun ruang keseharian Anda berada dalam batas yang aman dan terjaga."
            else ->
                "Kondisi tekanan darah Anda saat ini lebih banyak dipengaruhi oleh faktor kesehatan fisik, pola makan, waktu istirahat, dan kebiasaan sehari-hari."
        }
        val inferredFactors = when {
            hasGbvInCategory && lastResult.level != RiskLevel.LOW ->
                listOf("Pola makan & aktivitas", "Stres & kualitas tidur", "Kondisi medis & tensi", "Kekerasan psikologis")
            hasGbvInCategory ->
                listOf("Stres & kualitas tidur", "Kekerasan psikologis")
            lastResult.level == RiskLevel.LOW ->
                emptyList()
            else ->
                listOf("Pola makan & aktivitas", "Stres & kualitas tidur", "Kondisi medis & tensi")
        }
        val inferredRelationship = if (hasGbvInCategory) {
            "Tekanan batin yang berlangsung lama dari situasi yang tidak aman dapat membuat tubuh terus menegang dan ikut memengaruhi tekanan darah Anda dari waktu ke waktu melalui jalur stres kronis (bukan hubungan sebab-akibat seketika)."
        } else {
            "Tanda risiko hipertensi yang muncul berkaitan dengan faktor fisik, asupan harian, kualitas istirahat, dan beban stres keseharian."
        }
        val inferredRecs = when {
            hasGbvInCategory -> conciseIntegratedRecommendations(IntegratedCategory.HYPERTENSION_WITH_GBV_INDICATION)
            lastResult.level == RiskLevel.LOW -> conciseIntegratedRecommendations(IntegratedCategory.LOW_RISK)
            else -> conciseIntegratedRecommendations(IntegratedCategory.HYPERTENSION_LIFESTYLE_ONLY)
        }

        return ScreeningContextSnapshot(
            riskLevelLabel = lastResult.level.label,
            integratedCategoryLabel = fallbackCategoryLabel,
            screeningDate = lastResult.date,
            summaryNarrative = inferredSummary,
            hypertensionStatusLabel = htStatus,
            gbvStatusLabel = gbvStatus,
            keyFactors = inferredFactors,
            protectiveFactors = emptyList(),
            relationshipExplanation = inferredRelationship,
            keyRecommendations = inferredRecs
        )
    }

    fun presentModule(result: ModuleResult): UserModulePresentation =
        if (result.module == ScreeningModule.HYPERTENSION) {
            presentHypertension(result)
        } else {
            presentGbv(result)
        }

    fun presentIntegrated(
        hypertension: ModuleResult,
        gbv: ModuleResult,
        analysis: IntegratedAnalysis
    ): UserIntegratedPresentation {
        val ht = presentHypertension(hypertension)
        val kbg = presentGbv(gbv)

        val categoryLabel = when (analysis.category) {
            IntegratedCategory.HYPERTENSION_LIFESTYLE_ONLY,
            IntegratedCategory.HYPERTENSION_MEDICAL_HISTORY ->
                "Risiko Hipertensi dengan Dominasi Faktor Kesehatan dan Gaya Hidup"
            IntegratedCategory.HYPERTENSION_WITH_GBV_INDICATION ->
                "Risiko Hipertensi dengan Indikasi Keterkaitan Pengalaman KBG"
            IntegratedCategory.LOW_RISK -> "Risiko Hipertensi Rendah"
            IntegratedCategory.GBV_ONLY -> "Indikasi KBG Tanpa Indikasi Risiko Hipertensi"
            IntegratedCategory.INCONCLUSIVE -> "Hasil Belum Dapat Disimpulkan"
        }

        val summary = when (analysis.category) {
            IntegratedCategory.HYPERTENSION_LIFESTYLE_ONLY,
            IntegratedCategory.HYPERTENSION_MEDICAL_HISTORY ->
                "Kondisi tekanan darah Anda saat ini lebih banyak dipengaruhi oleh faktor kesehatan fisik dan kebiasaan sehari-hari, tanpa adanya indikasi pengalaman kekerasan."
            IntegratedCategory.HYPERTENSION_WITH_GBV_INDICATION ->
                "Terdapat tanda risiko hipertensi yang muncul bersamaan dengan beban pengalaman kekerasan atau relasi yang tidak aman, sehingga baik kesehatan fisik maupun ketenangan batin Anda sama-sama perlu diperhatikan."
            IntegratedCategory.LOW_RISK ->
                "Kabar baik, saat ini kondisi tekanan darah maupun ruang keseharian Anda berada dalam batas yang aman dan terjaga."
            IntegratedCategory.GBV_ONLY ->
                "Kondisi tekanan darah Anda saat ini masih terjaga, namun ada tanda pengalaman kekerasan atau ketidakteramanan relasi yang patut mendapat perhatian dan dukungan."
            IntegratedCategory.INCONCLUSIVE ->
                "Masih ada beberapa pertanyaan yang belum terjawab, sehingga gambaran utuhnya belum bisa kami simpulkan."
        }

        val relationship = when (analysis.category) {
            IntegratedCategory.HYPERTENSION_WITH_GBV_INDICATION ->
                "Tekanan batin yang berlangsung lama dari situasi yang tidak aman dapat membuat tubuh terus menegang dan ikut memengaruhi tekanan darah Anda dari waktu ke waktu (bukan hubungan sebab-akibat seketika)."
            IntegratedCategory.HYPERTENSION_LIFESTYLE_ONLY,
            IntegratedCategory.HYPERTENSION_MEDICAL_HISTORY ->
                "Tanda risiko hipertensi yang muncul murni berasal dari faktor fisik dan pola hidup sehari-hari, tidak berkaitan dengan isu kekerasan."
            IntegratedCategory.LOW_RISK ->
                "Baik kondisi fisik maupun kesejahteraan relasi Anda sama-sama berada dalam kondisi yang baik."
            IntegratedCategory.GBV_ONLY ->
                "Pengalaman tidak menyenangkan yang Anda alami belum tampak memengaruhi tekanan darah saat ini, namun tetap penting untuk mendapatkan dukungan agar Anda merasa aman."
            IntegratedCategory.INCONCLUSIVE ->
                "Keterkaitan antara kondisi kesehatan dan kesejahteraan Anda belum dapat dinilai sebelum seluruh pertanyaan terjawab."
        }

        val factors = (ht.factorsToNote + kbg.identifiedCategories).distinct().take(6)
        val recommendations = conciseIntegratedRecommendations(analysis.category)

        return UserIntegratedPresentation(
            categoryLabel = categoryLabel,
            summary = summary,
            hypertension = ht,
            gbv = kbg,
            relationshipExplanation = relationship,
            factors = factors,
            recommendations = recommendations,
            disclaimer = USER_DISCLAIMER,
            isInconclusive = analysis.category == IntegratedCategory.INCONCLUSIVE,
            protectiveFactors = ht.protectiveFactors
        )
    }

    private fun presentHypertension(result: ModuleResult): UserModulePresentation {
        val hasIndication = result.cf >= INDICATION_THRESHOLD
        val status = when {
            !result.complete -> "Belum lengkap"
            result.level == CfLevel.RENDAH -> "Rendah · Batas Wajar"
            result.level == CfLevel.SEDANG -> "Sedang · Perlu Pemantauan"
            result.level == CfLevel.TINGGI -> "Tinggi · Perlu Perhatian"
            else -> "Sangat Tinggi · Perlu Perhatian"
        }
        val tone = when {
            !result.complete -> PresentationTone.INCOMPLETE
            result.hasDiagnosis || hasIndication -> PresentationTone.ATTENTION
            else -> PresentationTone.POSITIVE
        }
        val conclusion = when {
            !result.complete ->
                "Masih ada pertanyaan yang terlewat, sehingga gambaran risiko hipertensi Anda belum bisa disimpulkan."
            result.level == CfLevel.RENDAH ->
                "Saat ini tidak terlihat tanda risiko hipertensi yang menonjol. Terus pertahankan kebiasaan sehat Anda sehari-hari."
            result.level == CfLevel.SEDANG && result.hasDiagnosis ->
                "Anda memiliki riwayat diagnosis hipertensi yang saat ini cukup terkendali, namun tetap perlu dipantau secara rutin agar tetap stabil."
            result.level == CfLevel.SEDANG ->
                "Ada beberapa kebiasaan atau kondisi harian yang mulai meningkatkan risiko hipertensi dan sebaiknya mulai Anda pantau dari sekarang."
            result.hasDiagnosis ->
                "Riwayat hipertensi Anda disertai beberapa faktor pemicu yang cukup kuat, sehingga sangat disarankan untuk memeriksakan diri ke tenaga kesehatan."
            else ->
                "Terlihat beberapa faktor risiko hipertensi yang cukup kuat dan memerlukan perhatian lebih lanjut."
        }

        val factors = buildList {
            if (result.hasDiagnosis) add("Riwayat diagnosis hipertensi")
            result.categories
                .filter { it.cf > 0.0 }
                .sortedByDescending { it.cf }
                .forEach { add(userLabel(it.hypothesis)) }
        }.distinct().take(4)

        return UserModulePresentation(
            title = "Skrining Hipertensi",
            isHypertension = true,
            statusLabel = status,
            tone = tone,
            conclusion = conclusion,
            factorsToNote = factors,
            identifiedCategories = emptyList(),
            recommendations = hypertensionRecommendations(result),
            supportNote = null,
            isComplete = result.complete,
            disclaimer = USER_DISCLAIMER,
            protectiveFactors = result.protectiveFactors
        )
    }

    private fun presentGbv(result: ModuleResult): UserModulePresentation {
        val hasIndication = result.cf >= INDICATION_THRESHOLD
        val status = when {
            !result.complete -> "Belum lengkap"
            hasIndication -> "Terdapat indikasi"
            else -> "Tidak terindikasi"
        }
        val tone = when {
            !result.complete -> PresentationTone.INCOMPLETE
            hasIndication -> PresentationTone.ATTENTION
            else -> PresentationTone.POSITIVE
        }

        val indicated = result.categories
            .filter { it.cf > 0.0 }
            .sortedByDescending { it.cf }
            .map { userLabel(it.hypothesis) }

        val conclusion = when {
            !result.complete ->
                "Masih ada pertanyaan pada bagian kesejahteraan yang belum terjawab."
            hasIndication ->
                "Dari cerita dan jawaban Anda, tampak adanya tanda pengalaman ${joinCategories(indicated)} di lingkungan keseharian atau relasi Anda. Ingatlah bahwa Anda berhak merasa aman dan dihargai."
            else ->
                "Tidak terlihat adanya tanda pengalaman kekerasan berbasis gender dalam keseharian maupun relasi Anda saat ini."
        }

        val recommendations = if (hasIndication) {
            listOf(
                "Bila Anda merasa nyaman, ceritakan kondisi ini kepada orang tepercaya atau konselor pendamping.",
                "Hubungi layanan sahabat perempuan dan anak SAPA 129 kapan pun Anda membutuhkan ruang aman."
            )
        } else {
            listOf(
                "Terus jaga hubungan dan lingkungan keseharian yang sehat serta saling menghargai.",
                "Simpan nomor SAPA 129 untuk berjaga-jaga bila suatu saat Anda atau orang terdekat membutuhkannya."
            )
        }

        return UserModulePresentation(
            title = "Skrining Kesejahteraan & KBG",
            isHypertension = false,
            statusLabel = status,
            tone = tone,
            conclusion = conclusion,
            factorsToNote = emptyList(),
            identifiedCategories = indicated,
            recommendations = recommendations,
            supportNote = if (hasIndication) "Layanan SAPA 129 sepenuhnya rahasia, ramah, dan bebas penghakiman." else null,
            isComplete = result.complete,
            disclaimer = USER_DISCLAIMER
        )
    }

    private fun hypertensionRecommendations(result: ModuleResult): List<String> {
        val categories = result.categories.map { it.hypothesis }.toSet()
        val recs = mutableListOf<String>()
        recs += "Cek tekanan darah Anda secara berkala di rumah, posbindu, atau klinik."
        if (Hypothesis.HT_LIFESTYLE in categories) {
            recs += "Kurangi makanan yang terlalu asin dan sempatkan bergerak aktif sekitar 30 menit sehari."
        }
        if (Hypothesis.HT_PSYCHOSOCIAL in categories) {
            recs += "Beri jeda istirahat saat pikiran penat dan usahakan tidur malam yang cukup."
        }
        if (Hypothesis.HT_MEDICAL in categories || Hypothesis.HT_GENETIC in categories) {
            recs += "Diskusikan riwayat kesehatan Anda atau keluarga bersama dokter saat pemeriksaan."
        }
        return recs.distinct().take(3)
    }

    private fun conciseIntegratedRecommendations(category: IntegratedCategory): List<String> =
        when (category) {
            IntegratedCategory.HYPERTENSION_WITH_GBV_INDICATION -> listOf(
                "Periksakan tekanan darah Anda ke puskesmas atau fasilitas kesehatan terdekat.",
                "Pertimbangkan berbicara dengan konselor untuk membantu meredakan beban pikiran yang berkepanjangan.",
                "Hubungi layanan rahasia SAPA 129 bila Anda membutuhkan pendampingan dan perlindungan."
            )
            IntegratedCategory.HYPERTENSION_LIFESTYLE_ONLY -> listOf(
                "Sempatkan mengecek tekanan darah di puskesmas, klinik, atau fasilitas kesehatan terdekat.",
                "Mulai kurangi asupan garam berlebih, rutin bergerak aktif, dan jaga waktu tidur malam."
            )
            IntegratedCategory.HYPERTENSION_MEDICAL_HISTORY -> listOf(
                "Lanjutkan pemantauan tekanan darah secara rutin bersama dokter Anda.",
                "Ikuti anjuran pengobatan dan perawatan harian agar tekanan darah tetap terjaga."
            )
            IntegratedCategory.LOW_RISK -> listOf(
                "Pertahankan kebiasaan hidup sehat Anda dan tetap cek tekanan darah secara berkala."
            )
            IntegratedCategory.GBV_ONLY -> listOf(
                "Pertimbangkan berdiskusi dengan pendamping atau konselor mengenai situasi yang Anda hadapi.",
                "Hubungi layanan SAPA 129 kapan saja bila Anda memerlukan dukungan keamanan."
            )
            IntegratedCategory.INCONCLUSIVE -> listOf(
                "Lengkapi seluruh pertanyaan pada kedua modul agar hasilnya dapat kami rangkum secara utuh."
            )
        }

    private const val CHRONIC_LINKAGE_NOTE =
        "Perlu diingat: kaitan antara beban stres atau pengalaman tidak aman dengan tekanan darah bekerja secara perlahan dalam jangka panjang, bukan sebab-akibat seketika."

    fun allKnownClinicalTags(): List<String> = listOf(
        "Riwayat diagnosis hipertensi",
        "Riwayat keluarga",
        "Kondisi medis & tensi",
        "Pola makan & aktivitas",
        "Stres & kualitas tidur",
        "Kekerasan fisik",
        "Kekerasan psikologis",
        "Kekerasan seksual",
        "Kekerasan ekonomi",
        "Kekerasan digital"
    )

    fun explainClinicalTag(tag: String): ClinicalTagDetail = when (tag) {
        "Riwayat diagnosis hipertensi" -> ClinicalTagDetail(
            tag = tag,
            categoryBadge = "KESEHATAN JANTUNG · RIWAYAT MEDIS",
            explanation = "Jika Anda pernah didiagnosis memiliki tekanan darah tinggi sebelumnya, memantau tensi secara rutin dan mengikuti saran dokter akan sangat membantu menjaga jantung serta pembuluh darah tetap sehat.",
            linkageReminder = CHRONIC_LINKAGE_NOTE
        )
        "Riwayat keluarga" -> ClinicalTagDetail(
            tag = tag,
            categoryBadge = "KESEHATAN JANTUNG · FAKTOR KELUARGA",
            explanation = "Memiliki orang tua atau keluarga dekat dengan riwayat darah tinggi memang membuat tubuh sedikit lebih peka terhadap kenaikan tensi. Namun, pola makan seimbang dan kebiasaan gerak aktif sangat ampuh membantu menjaganya tetap stabil.",
            linkageReminder = CHRONIC_LINKAGE_NOTE
        )
        "Kondisi medis & tensi" -> ClinicalTagDetail(
            tag = tag,
            categoryBadge = "KESEHATAN JANTUNG · KONDISI FISIK",
            explanation = "Perubahan angka tekanan darah, berat badan, atau kondisi gula darah dan kolesterol berkaitan erat dengan seberapa keras jantung bekerja memompa darah setiap hari.",
            linkageReminder = CHRONIC_LINKAGE_NOTE
        )
        "Pola makan & aktivitas" -> ClinicalTagDetail(
            tag = tag,
            categoryBadge = "KESEHATAN JANTUNG · KEBIASAAN HARIAN",
            explanation = "Makanan yang tinggi garam membuat tubuh menahan lebih banyak cairan, sementara jarang bergerak membuat pembuluh darah kurang lentur. Mengurangi asin dan rutin berjalan kaki ringan saja sudah membawa perubahan baik.",
            linkageReminder = CHRONIC_LINKAGE_NOTE
        )
        "Stres & kualitas tidur" -> ClinicalTagDetail(
            tag = tag,
            categoryBadge = "KESEJAHTERAAN · PIKIRAN & ISTIRAHAT",
            explanation = "Saat pikiran terus-menerus tegang dan tubuh kurang tidur di malam hari, hormon stres akan tetap tinggi sehingga detak jantung dan tekanan darah ikut terdorong naik.",
            linkageReminder = CHRONIC_LINKAGE_NOTE
        )
        "Kekerasan fisik" -> ClinicalTagDetail(
            tag = tag,
            categoryBadge = "RUANG AMAN · KESELAMATAN FISIK",
            explanation = "Tindakan atau ancaman yang menyakiti tubuh membuat Anda berada dalam kondisi waspada dan tertekan terus-menerus. Keselamatan Anda sangat berharga dan Anda berhak mendapatkan perlindungan yang aman.",
            linkageReminder = CHRONIC_LINKAGE_NOTE
        )
        "Kekerasan psikologis" -> ClinicalTagDetail(
            tag = tag,
            categoryBadge = "RUANG AMAN · KETENANGAN BATIN",
            explanation = "Bentakan, sikap meremehkan, ancaman, atau perlakuan yang memojokkan secara berulang—baik di rumah, tempat kerja, maupun pergaulan—dapat menguras energi mental dan mengikis rasa aman Anda.",
            linkageReminder = CHRONIC_LINKAGE_NOTE
        )
        "Kekerasan seksual" -> ClinicalTagDetail(
            tag = tag,
            categoryBadge = "RUANG AMAN · KEHORMATAN DIRI",
            explanation = "Segala bentuk pelecehan, baik berupa ucapan maupun tindakan yang melanggar batas kenyamanan tubuh Anda, bukanlah kesalahan Anda. Anda berhak memperoleh pendampingan yang aman dan bebas penghakiman.",
            linkageReminder = CHRONIC_LINKAGE_NOTE
        )
        "Kekerasan ekonomi" -> ClinicalTagDetail(
            tag = tag,
            categoryBadge = "RUANG AMAN · KEMANDIRIAN FINANSIAL",
            explanation = "Pembatasan sepihak atas uang, larangan bekerja, atau pemaksaan finansial sering kali menimbulkan rasa cemas yang mendalam dan berkepanjangan dalam kehidupan sehari-hari.",
            linkageReminder = CHRONIC_LINKAGE_NOTE
        )
        "Kekerasan digital" -> ClinicalTagDetail(
            tag = tag,
            categoryBadge = "RUANG AMAN · PRIVASI DIGITAL",
            explanation = "Penguntitan lewat ponsel, pemaksaan membuka kata sandi pribadi, atau ancaman di media sosial dapat merusak rasa tenang Anda dan patut mendapatkan dukungan perlindungan.",
            linkageReminder = CHRONIC_LINKAGE_NOTE
        )
        else -> ClinicalTagDetail(
            tag = tag,
            categoryBadge = "INFORMASI KESEHATAN & KESEJAHTERAAN",
            explanation = "Hal ini tercatat dari jawaban Anda sebagai bagian untuk memahami kesehatan tubuh dan ketenangan pikiran Anda secara menyeluruh.",
            linkageReminder = CHRONIC_LINKAGE_NOTE
        )
    }

    private fun userLabel(hypothesis: Hypothesis): String = when (hypothesis) {
        Hypothesis.HT_GENETIC -> "Riwayat keluarga"
        Hypothesis.HT_MEDICAL -> "Kondisi medis & tensi"
        Hypothesis.HT_LIFESTYLE -> "Pola makan & aktivitas"
        Hypothesis.HT_PSYCHOSOCIAL -> "Stres & kualitas tidur"
        Hypothesis.HYPERTENSION -> "Risiko hipertensi"
        Hypothesis.GBV_PHYSICAL -> "Kekerasan fisik"
        Hypothesis.GBV_PSYCHOLOGICAL -> "Kekerasan psikologis"
        Hypothesis.GBV_SEXUAL -> "Kekerasan seksual"
        Hypothesis.GBV_ECONOMIC -> "Kekerasan ekonomi"
        Hypothesis.GBV_DIGITAL -> "Kekerasan digital"
        Hypothesis.GBV -> "Pengalaman KBG"
    }

    private fun joinCategories(categories: List<String>): String {
        if (categories.isEmpty()) return "pengalaman tertentu"
        val lower = categories.map { it.lowercase() }
        return when (lower.size) {
            1 -> lower[0]
            2 -> "${lower[0]} dan ${lower[1]}"
            else -> lower.dropLast(1).joinToString(", ") + ", dan " + lower.last()
        }
    }
}

