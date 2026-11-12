package com.example.empati

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.empati.data.UserProfile
import com.example.empati.data.RiskLevel
import com.example.empati.data.ChatMessage
import com.example.empati.data.cf.CfQuestion
import com.example.empati.data.cf.QuestionKind
import com.example.empati.data.cf.ScreeningModule
import com.example.empati.data.presentation.PresentationTone
import com.example.empati.data.presentation.UserIntegratedPresentation
import com.example.empati.data.presentation.UserModulePresentation
import com.example.empati.data.presentation.UserRiskSummary
import com.example.empati.data.rag.EmpatiCorpus
import com.example.empati.data.rag.RagDocumentChunk
import com.example.empati.data.rag.RagTopicScope
import com.example.empati.ui.theme.EmpatiTheme

/*
 * Visual baseline of the "Soft Daylight" design system.
 *
 * Every screenshot test renders BOTH themes (Light / Dark). Motion is forced to its end state
 * (reducedMotion = true) so reference images are deterministic.
 *
 * Update references:  gradlew updateScreenshotTestDefaultDebugTestSuite
 * Verify:             gradlew testScreenshotTestDefaultDebugTestSuite
 */

/** Phone, one screen tall. */
@Preview(name = "Light", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class PhoneLightDark

/** Phone, tall canvas so long scrolling pages are captured in full. */
@Preview(name = "Light", showBackground = true, widthDp = 390, heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class PhoneTallLightDark

/** Accessibility: largest practical text scale on a small phone. */
@Preview(name = "Light x1.5 text", showBackground = true, widthDp = 360, heightDp = 1900, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark x1.5 text", showBackground = true, widthDp = 360, heightDp = 1900, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class LargeTextLightDark

/** Compact component sheets. */
@Preview(name = "Light", showBackground = true, widthDp = 390, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ComponentSheetLightDark

@Composable
fun PreviewTheme(content: @Composable () -> Unit) {
    EmpatiTheme(darkTheme = isSystemInDarkTheme(), reducedMotion = true) {
        Surface(color = MaterialTheme.colorScheme.background) { content() }
    }
}

// ── Fixtures (copy mirrors what UserResultPresenter produces) ────────────────────────────────

object Fixtures {
    const val DISCLAIMER = "Skrining bersifat edukatif, bukan diagnosis medis maupun penetapan sebab-akibat."

    val profile = UserProfile(
        name = "Ratna Andari",
        nip = "ratna.andari@empati.id",
        instansi = "Kota Bandung",
        jabatan = "Pekerja Kantoran",
        jenisAsn = "Karyawan / Pekerja",
        golongan = "Perempuan"
    )

    val history = listOf(
        UserRiskSummary(RiskLevel.MEDIUM, "6 Nov 2026"),
        UserRiskSummary(RiskLevel.LOW, "2 Okt 2026"),
        UserRiskSummary(RiskLevel.HIGH, "28 Agu 2026"),
        UserRiskSummary(RiskLevel.VERY_HIGH, "11 Jul 2026")
    )

    val hypertensionAttention = UserModulePresentation(
        title = "Skrining Hipertensi",
        isHypertension = true,
        statusLabel = "Perlu perhatian",
        tone = PresentationTone.ATTENTION,
        conclusion = "Ditemukan beberapa faktor risiko hipertensi yang perlu mulai dikendalikan.",
        factorsToNote = listOf("Riwayat keluarga", "Pola makan & aktivitas", "Stres & kualitas tidur"),
        identifiedCategories = emptyList(),
        recommendations = listOf(
            "Periksa tekanan darah secara berkala di fasilitas kesehatan.",
            "Kurangi asupan garam dan perbanyak aktivitas fisik ringan.",
            "Sisihkan waktu istirahat yang cukup setiap hari."
        ),
        supportNote = null,
        isComplete = true,
        disclaimer = DISCLAIMER,
        protectiveFactors = listOf("Tidak merokok", "Tidur cukup")
    )

    val hypertensionPositive = hypertensionAttention.copy(
        statusLabel = "Dalam batas wajar",
        tone = PresentationTone.POSITIVE,
        conclusion = "Faktor risiko hipertensi Anda tampak terkendali. Pertahankan kebiasaan sehat ini.",
        factorsToNote = emptyList(),
        recommendations = listOf(
            "Pertahankan pola makan seimbang dan aktivitas fisik rutin.",
            "Periksa tekanan darah minimal setahun sekali."
        )
    )

    val wellbeingWithSupport = UserModulePresentation(
        title = "Skrining Kesejahteraan & KBG",
        isHypertension = false,
        statusLabel = "Perlu perhatian",
        tone = PresentationTone.ATTENTION,
        conclusion = "Jawaban Anda menunjukkan adanya pengalaman yang berhak mendapat pendampingan.",
        factorsToNote = emptyList(),
        identifiedCategories = listOf("Kekerasan psikologis", "Kekerasan digital"),
        recommendations = listOf(
            "Hubungi layanan SAPA 129 secara rahasia bila Anda membutuhkan pendampingan.",
            "Ceritakan pada orang yang Anda percayai agar tidak menanggungnya sendiri."
        ),
        supportNote = "Layanan SAPA 129 bersifat rahasia dan bebas stigma.",
        isComplete = true,
        disclaimer = DISCLAIMER
    )

    val wellbeingIncomplete = UserModulePresentation(
        title = "Skrining Kesejahteraan & KBG",
        isHypertension = false,
        statusLabel = "Belum lengkap",
        tone = PresentationTone.INCOMPLETE,
        conclusion = "Belum cukup jawaban untuk menyimpulkan modul ini.",
        factorsToNote = emptyList(),
        identifiedCategories = emptyList(),
        recommendations = listOf("Lengkapi pertanyaan yang tertinggal agar hasilnya lebih tepat."),
        supportNote = null,
        isComplete = false,
        disclaimer = DISCLAIMER
    )

    val integratedAttention = UserIntegratedPresentation(
        categoryLabel = "Perlu perhatian pada tekanan darah dan kesejahteraan",
        summary = "Faktor tekanan darah dan pengalaman psikososial Anda saling memengaruhi, sehingga perlu ditangani bersama.",
        hypertension = hypertensionAttention,
        gbv = wellbeingWithSupport,
        relationshipExplanation = "Tekanan yang berkepanjangan dapat mengaktifkan respons stres tubuh dan memengaruhi tekanan darah. Ini bukan penetapan sebab-akibat.",
        factors = listOf("Riwayat keluarga", "Stres & kualitas tidur", "Kekerasan psikologis"),
        recommendations = listOf(
            "Periksa tekanan darah secara berkala di fasilitas kesehatan.",
            "Hubungi SAPA 129 bila membutuhkan pendampingan.",
            "Tanyakan hal yang membingungkan kepada AI-EMPATI."
        ),
        disclaimer = DISCLAIMER,
        isInconclusive = false,
        protectiveFactors = listOf("Tidak merokok", "Tidur cukup")
    )

    val integratedPositive = integratedAttention.copy(
        categoryLabel = "Kondisi Anda tampak baik",
        summary = "Tidak ditemukan faktor yang memerlukan perhatian khusus saat ini.",
        hypertension = hypertensionPositive,
        gbv = wellbeingWithSupport.copy(
            statusLabel = "Tidak ada indikasi",
            tone = PresentationTone.POSITIVE,
            identifiedCategories = emptyList(),
            supportNote = null
        ),
        relationshipExplanation = "Kebiasaan sehat dan rasa aman yang terjaga saling menguatkan kesehatan Anda.",
        factors = emptyList(),
        recommendations = listOf("Pertahankan kebiasaan sehat Anda.", "Skrining ulang secara berkala.")
    )

    val dichotomyQuestion = CfQuestion(
        id = "HT-07",
        module = ScreeningModule.HYPERTENSION,
        category = "Riwayat keluarga",
        text = "Apakah ada anggota keluarga inti Anda yang pernah didiagnosis tekanan darah tinggi?",
        kind = QuestionKind.DICHOTOMY
    )

    val sensitiveLikertQuestion = CfQuestion(
        id = "GBV-03",
        module = ScreeningModule.GBV,
        category = "Pengalaman relasi",
        text = "Dalam tiga bulan terakhir, seberapa sering Anda merasa direndahkan atau diintimidasi oleh orang di sekitar Anda?",
        kind = QuestionKind.LIKERT,
        sensitive = true
    )

    private val dashChunk = RagDocumentChunk(
        chunkId = "DOC-HT-02",
        docSource = "WHO · Diet DASH",
        topicScope = RagTopicScope.LIFESTYLE_DASH,
        title = "Pola makan DASH untuk tekanan darah",
        content = "Pola makan DASH menekankan sayur, buah, biji-bijian utuh, dan pembatasan natrium untuk membantu menjaga tekanan darah.",
        keywords = listOf("dash", "garam")
    )

    private val sapaChunk = RagDocumentChunk(
        chunkId = "DOC-GBV-01",
        docSource = "KemenPPPA · SAPA 129",
        topicScope = RagTopicScope.GBV_PROTECTION,
        title = "Layanan pengaduan SAPA 129",
        content = "SAPA 129 menyediakan layanan pengaduan dan pendampingan yang rahasia.",
        keywords = listOf("sapa")
    )

    private val dashSource = requireNotNull(EmpatiCorpus.byId("who-diet-2024#composition"))
    private val sapaSource = requireNotNull(EmpatiCorpus.byId("sapa129-2024#contact"))

    val chat = listOf(
        ChatMessage(
            isUser = false,
            text = "Halo, saya AI-EMPATI. Saya siap membantu Anda memahami hipertensi dan faktor risikonya. Apa yang ingin Anda tanyakan?"
        ),
        ChatMessage(isUser = true, text = "Bagaimana cara mengurangi garam tanpa membuat makanan hambar?"),
        ChatMessage(
            isUser = false,
            text = "Coba tambahkan rasa dari bawang, jahe, jeruk nipis, dan rempah segar. Perlahan lidah akan terbiasa dengan rasa asin yang lebih ringan.",
            retrievedChunks = listOf(dashChunk),
            citations = listOf(dashSource)
        ),
        ChatMessage(isUser = true, text = "Saya juga sering merasa tertekan di rumah."),
        ChatMessage(
            isUser = false,
            text = "Terima kasih sudah mau bercerita. Anda tidak sendirian, dan perasaan Anda penting.",
            retrievedChunks = listOf(sapaChunk),
            citations = listOf(sapaSource)
        )
    )
}
