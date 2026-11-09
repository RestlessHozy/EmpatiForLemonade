package com.example.empati.data

private fun yesNo(yesPoints: Int) = listOf(
    AnswerOption("Ya", yesPoints),
    AnswerOption("Tidak", 0)
)

private fun freq(vararg points: Int) = listOf(
    AnswerOption("Tidak pernah", points[0]),
    AnswerOption("Jarang", points[1]),
    AnswerOption("Kadang-kadang", points[2]),
    AnswerOption("Sering", points[3]),
    AnswerOption("Sangat sering", points[4])
)

val screeningQuestions = listOf(
    // ---- Riwayat Kesehatan ----
    Question(1, "Riwayat Kesehatan", "Apakah Anda pernah didiagnosis hipertensi oleh tenaga kesehatan?", options = yesNo(3)),
    Question(2, "Riwayat Kesehatan", "Apakah orang tua atau saudara kandung Anda memiliki riwayat hipertensi?", options = yesNo(2)),
    Question(3, "Riwayat Kesehatan", "Apakah Anda memiliki riwayat diabetes melitus?", options = yesNo(3)),
    Question(4, "Riwayat Kesehatan", "Apakah Anda sedang mengonsumsi obat tekanan darah secara rutin?", options = yesNo(2)),
    Question(5, "Riwayat Kesehatan", "Pada pemeriksaan terakhir, apakah tekanan darah Anda di atas 130/85 mmHg?", options = yesNo(3)),
    Question(6, "Riwayat Kesehatan", "Apakah Anda sedang hamil atau baru melahirkan dalam 6 bulan terakhir?", options = yesNo(2)),

    // ---- Gaya Hidup ----
    Question(7, "Gaya Hidup", "Seberapa sering Anda mengonsumsi makanan asin atau tinggi garam?", options = freq(0, 1, 2, 3, 4)),
    Question(8, "Gaya Hidup", "Seberapa sering Anda melakukan aktivitas fisik minimal 30 menit?", options = freq(4, 3, 2, 1, 0)),
    Question(9, "Gaya Hidup", "Apakah Anda merokok atau terpapar asap rokok setiap hari?", options = yesNo(3)),
    Question(10, "Gaya Hidup", "Seberapa sering Anda mengonsumsi kopi atau minuman berkafein secara berlebihan?", options = freq(0, 1, 2, 3, 4)),
    Question(11, "Gaya Hidup", "Apakah berat badan Anda cenderung berlebih (obesitas)?", options = yesNo(3)),

    // ---- Faktor Risiko ----
    Question(12, "Faktor Risiko", "Seberapa sering Anda merasa stres berat akibat pekerjaan dalam sebulan terakhir?", options = freq(0, 1, 2, 3, 4)),
    Question(13, "Faktor Risiko", "Apakah Anda sering tidur kurang dari 7 jam setiap malam?", options = yesNo(3)),
    Question(14, "Faktor Risiko", "Seberapa sering Anda merasa cemas atau tertekan tanpa sebab yang jelas?", options = freq(0, 1, 2, 3, 4)),
    Question(15, "Faktor Risiko", "Apakah Anda jarang memeriksakan tekanan darah (kurang dari sekali setahun)?", options = yesNo(2)),
    Question(16, "Faktor Risiko", "Apakah Anda mengonsumsi makanan cepat saji lebih dari 3 kali seminggu?", options = yesNo(2)),

    // ---- Kekerasan & Kesejahteraan (sensitive) ----
    Question(17, "Kekerasan & Kesejahteraan", "Apakah Anda pernah mengalami kekerasan fisik, verbal, atau emosional dari pasangan atau orang terdekat?", sensitive = true, options = yesNo(4)),
    Question(18, "Kekerasan & Kesejahteraan", "Seberapa sering Anda merasa tidak aman atau terancam di lingkungan tempat tinggal?", sensitive = true, options = freq(0, 1, 2, 3, 4)),
    Question(19, "Kekerasan & Kesejahteraan", "Apakah Anda merasa tidak memiliki dukungan emosional dari keluarga atau teman?", sensitive = true, options = yesNo(3)),
    Question(20, "Kekerasan & Kesejahteraan", "Seberapa sering Anda mengalami kelelahan emosional akibat tanggung jawab ganda (pekerjaan dan rumah tangga)?", sensitive = true, options = freq(0, 1, 2, 3, 4)),
    Question(21, "Kekerasan & Kesejahteraan", "Apakah Anda pernah merasa dipaksa melakukan sesuatu yang tidak Anda inginkan oleh pasangan?", sensitive = true, options = yesNo(4)),
    Question(22, "Kekerasan & Kesejahteraan", "Seberapa sering Anda merasa diabaikan atau direndahkan oleh orang terdekat?", sensitive = true, options = freq(0, 1, 2, 3, 4)),
    Question(23, "Kekerasan & Kesejahteraan", "Apakah Anda memiliki akses aman untuk mencari bantuan bila mengalami kekerasan?", sensitive = true, options = listOf(AnswerOption("Ya", 0), AnswerOption("Tidak", 3))),
    Question(24, "Kekerasan & Kesejahteraan", "Seberapa besar pengalaman kekerasan memengaruhi kesehatan Anda saat ini?", sensitive = true, options = freq(0, 1, 2, 3, 4))
)
