package com.example.empati.data.cf

/**
 * Bank pertanyaan dual-modul. ID M01–M16 dan K01–K10 tidak boleh diubah.
 * Tipe jawaban mengikuti referensi EMPATIDualInputParser:
 *  - Dikotomi: Ya / Tidak
 *  - Likert 5 tingkat: 1..5
 *  - M08 bersifat reverse-scored.
 */

val hypertensionQuestions: List<CfQuestion> = listOf(
    // Riwayat Kesehatan
    CfQuestion("M01", ScreeningModule.HYPERTENSION, "Riwayat Kesehatan",
        "Apakah Anda pernah didiagnosis hipertensi oleh tenaga kesehatan?", QuestionKind.DICHOTOMY),
    CfQuestion("M02", ScreeningModule.HYPERTENSION, "Riwayat Kesehatan",
        "Apakah orang tua atau saudara kandung Anda memiliki riwayat hipertensi?", QuestionKind.DICHOTOMY),
    CfQuestion("M03", ScreeningModule.HYPERTENSION, "Riwayat Kesehatan",
        "Apakah Anda memiliki riwayat diabetes melitus?", QuestionKind.DICHOTOMY),
    CfQuestion("M04", ScreeningModule.HYPERTENSION, "Riwayat Kesehatan",
        "Apakah Anda sedang mengonsumsi obat tekanan darah secara rutin?", QuestionKind.DICHOTOMY),
    CfQuestion("M05", ScreeningModule.HYPERTENSION, "Riwayat Kesehatan",
        "Pada pemeriksaan terakhir, apakah tekanan darah Anda di atas 130/85 mmHg?", QuestionKind.DICHOTOMY),
    CfQuestion("M06", ScreeningModule.HYPERTENSION, "Riwayat Kesehatan",
        "Apakah Anda sedang hamil atau baru melahirkan dalam 6 bulan terakhir? (Pilih Tidak bila tidak berlaku)", QuestionKind.DICHOTOMY),

    // Gaya Hidup
    CfQuestion("M07", ScreeningModule.HYPERTENSION, "Gaya Hidup",
        "Seberapa sering Anda mengonsumsi makanan asin atau tinggi garam?", QuestionKind.LIKERT),
    CfQuestion("M08", ScreeningModule.HYPERTENSION, "Gaya Hidup",
        "Seberapa sering Anda melakukan aktivitas fisik minimal 30 menit?", QuestionKind.LIKERT, reverseScored = true),
    CfQuestion("M09", ScreeningModule.HYPERTENSION, "Gaya Hidup",
        "Apakah Anda merokok atau terpapar asap rokok setiap hari?", QuestionKind.DICHOTOMY),
    CfQuestion("M10", ScreeningModule.HYPERTENSION, "Gaya Hidup",
        "Seberapa sering Anda mengonsumsi kopi atau minuman berkafein secara berlebihan?", QuestionKind.LIKERT),
    CfQuestion("M11", ScreeningModule.HYPERTENSION, "Gaya Hidup",
        "Apakah berat badan Anda cenderung berlebih (obesitas)?", QuestionKind.DICHOTOMY),

    // Faktor Risiko
    CfQuestion("M12", ScreeningModule.HYPERTENSION, "Faktor Risiko",
        "Seberapa sering Anda merasa stres berat akibat pekerjaan atau aktivitas harian dalam sebulan terakhir?", QuestionKind.LIKERT),
    CfQuestion("M13", ScreeningModule.HYPERTENSION, "Faktor Risiko",
        "Apakah Anda sering tidur kurang dari 7 jam setiap malam?", QuestionKind.DICHOTOMY),
    CfQuestion("M14", ScreeningModule.HYPERTENSION, "Faktor Risiko",
        "Seberapa sering Anda merasa cemas atau tertekan tanpa sebab yang jelas?", QuestionKind.LIKERT),
    CfQuestion("M15", ScreeningModule.HYPERTENSION, "Faktor Risiko",
        "Apakah Anda jarang memeriksakan tekanan darah (kurang dari sekali setahun)?", QuestionKind.DICHOTOMY),
    CfQuestion("M16", ScreeningModule.HYPERTENSION, "Faktor Risiko",
        "Apakah Anda mengonsumsi makanan cepat saji lebih dari 3 kali seminggu?", QuestionKind.DICHOTOMY)
)

val gbvQuestions: List<CfQuestion> = listOf(
    // Kekerasan Fisik
    CfQuestion("K01", ScreeningModule.GBV, "Kekerasan Fisik",
        "Apakah Anda pernah mengalami tindakan fisik seperti didorong, dipukul, ditampar, atau ditendang oleh seseorang di lingkungan kerja, sosial, atau rumah tangga?",
        QuestionKind.LIKERT, sensitive = true),
    CfQuestion("K02", ScreeningModule.GBV, "Kekerasan Fisik",
        "Apakah seseorang di lingkungan sekitar pernah melemparkan benda ke arah Anda atau melakukan tindakan fisik yang membuat Anda merasa terancam atau terluka?",
        QuestionKind.LIKERT, sensitive = true),

    // Kekerasan Psikologis
    CfQuestion("K03", ScreeningModule.GBV, "Kekerasan Psikologis",
        "Apakah Anda pernah dihina, dimaki, direndahkan, atau dipermalukan oleh seseorang di tempat kerja, lingkungan sosial, atau rumah tangga?",
        QuestionKind.LIKERT, sensitive = true),
    CfQuestion("K04", ScreeningModule.GBV, "Kekerasan Psikologis",
        "Apakah Anda pernah sengaja dikucilkan, diabaikan, atau dijauhkan dari interaksi sosial maupun kegiatan sehari-hari oleh orang di sekitar Anda?",
        QuestionKind.LIKERT, sensitive = true),

    // Kekerasan Seksual
    CfQuestion("K05", ScreeningModule.GBV, "Kekerasan Seksual",
        "Apakah Anda pernah menerima komentar, candaan, atau tindakan bernuansa seksual yang tidak diinginkan di lingkungan beraktivitas?",
        QuestionKind.LIKERT, sensitive = true),
    CfQuestion("K06", ScreeningModule.GBV, "Kekerasan Seksual",
        "Apakah Anda pernah mengalami sentuhan, ajakan, permintaan, atau tindakan seksual yang tidak diinginkan, termasuk tekanan atau pemaksaan dari seseorang?",
        QuestionKind.LIKERT, sensitive = true),

    // Kekerasan Ekonomi
    CfQuestion("K07", ScreeningModule.GBV, "Kekerasan Ekonomi",
        "Apakah gaji, upah, atau hak finansial Anda pernah diambil, ditahan, atau dikuasai orang lain tanpa persetujuan Anda?",
        QuestionKind.LIKERT, sensitive = true),
    CfQuestion("K08", ScreeningModule.GBV, "Kekerasan Ekonomi",
        "Apakah Anda pernah dihalangi untuk bekerja, memperoleh penghasilan, mengembangkan diri, atau mendapatkan kesempatan berkarya tanpa alasan yang wajar?",
        QuestionKind.LIKERT, sensitive = true),

    // Kekerasan Digital
    CfQuestion("K09", ScreeningModule.GBV, "Kekerasan Digital",
        "Apakah Anda pernah mengalami penguntitan, pemantauan aktivitas digital, atau pengiriman pesan yang mengganggu secara terus-menerus melalui media digital?",
        QuestionKind.LIKERT, sensitive = true),
    CfQuestion("K10", ScreeningModule.GBV, "Kekerasan Digital",
        "Apakah Anda pernah menerima ancaman, intimidasi, penghinaan, atau penyebaran informasi pribadi melalui media sosial maupun platform digital?",
        QuestionKind.LIKERT, sensitive = true)
)

val allCfQuestions: List<CfQuestion> = hypertensionQuestions + gbvQuestions

fun questionsFor(module: ScreeningModule): List<CfQuestion> = when (module) {
    ScreeningModule.HYPERTENSION -> hypertensionQuestions
    ScreeningModule.GBV -> gbvQuestions
}

/** Label skala Likert sesuai modul. */
fun likertLabels(module: ScreeningModule): List<String> = when (module) {
    ScreeningModule.HYPERTENSION -> listOf("Tidak pernah", "Jarang", "Kadang-kadang", "Sering", "Sangat sering")
    ScreeningModule.GBV -> listOf("Tidak pernah", "Jarang", "Cukup sering", "Sering", "Sangat sering")
}
