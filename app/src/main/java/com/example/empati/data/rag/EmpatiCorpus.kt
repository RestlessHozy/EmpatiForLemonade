package com.example.empati.data.rag

import com.example.empati.data.triage.ReferenceChunk

/**
 * Korpus rujukan Empati. Potongan di sini adalah satu-satunya sumber fakta
 * klinis yang boleh dipakai model. Setiap potongan wajib punya id stabil supaya
 * chip sitasi bisa membuka sumber yang sama dari riwayat.
 */
object EmpatiCorpus {

    val chunks: List<ReferenceChunk> = listOf(
        chunk(
            id = "who-hypertension-2023#definition",
            title = "Hypertension",
            publisher = "World Health Organization",
            year = 2023,
            section = "Key facts",
            text = "Hipertensi pada orang dewasa ditandai tekanan darah sistolik 140 mmHg atau lebih, " +
                "atau diastolik 90 mmHg atau lebih, pada dua hari pengukuran berbeda.",
            url = "https://www.who.int/news-room/fact-sheets/detail/hypertension"
        ),
        chunk(
            id = "who-hypertension-2023#risk",
            title = "Hypertension",
            publisher = "World Health Organization",
            year = 2023,
            section = "Risk factors",
            text = "Faktor risiko hipertensi yang dapat diubah meliputi konsumsi garam berlebih, " +
                "kurang aktivitas fisik, berat badan berlebih, konsumsi alkohol, dan merokok.",
            url = "https://www.who.int/news-room/fact-sheets/detail/hypertension"
        ),
        chunk(
            id = "who-salt-2023#intake",
            title = "Salt reduction",
            publisher = "World Health Organization",
            year = 2023,
            section = "Recommendation",
            text = "Organisasi Kesehatan Dunia menganjurkan konsumsi garam kurang dari 5 gram per hari " +
                "untuk orang dewasa, setara kurang dari 2 gram natrium.",
            url = "https://www.who.int/news-room/fact-sheets/detail/salt-reduction"
        ),
        chunk(
            id = "who-salt-2023#hidden",
            title = "Salt reduction",
            publisher = "World Health Organization",
            year = 2023,
            section = "Sources of salt",
            text = "Sebagian besar garam berasal dari makanan olahan dan makanan kemasan, " +
                "bukan dari garam yang ditambahkan saat memasak.",
            url = "https://www.who.int/news-room/fact-sheets/detail/salt-reduction"
        ),
        chunk(
            id = "who-activity-2024#adults",
            title = "Physical activity",
            publisher = "World Health Organization",
            year = 2024,
            section = "Adults",
            text = "Orang dewasa dianjurkan melakukan aktivitas fisik aerobik intensitas sedang " +
                "150 sampai 300 menit per minggu, atau intensitas tinggi 75 sampai 150 menit per minggu.",
            url = "https://www.who.int/news-room/fact-sheets/detail/physical-activity"
        ),
        chunk(
            id = "who-diet-2024#composition",
            title = "Healthy diet",
            publisher = "World Health Organization",
            year = 2024,
            section = "Practical advice",
            text = "Pola makan sehat mencakup sayur dan buah setiap hari, kacang-kacangan, " +
                "serta pembatasan lemak jenuh dan gula bebas.",
            url = "https://www.who.int/news-room/fact-sheets/detail/healthy-diet"
        ),
        chunk(
            id = "who-tobacco-2023#blood-pressure",
            title = "Tobacco",
            publisher = "World Health Organization",
            year = 2023,
            section = "Health effects",
            text = "Berhenti merokok menurunkan risiko penyakit jantung dan stroke, " +
                "dan manfaatnya mulai terasa dalam hitungan minggu setelah berhenti.",
            url = "https://www.who.int/news-room/fact-sheets/detail/tobacco"
        ),
        chunk(
            id = "who-alcohol-2024#blood-pressure",
            title = "Alcohol",
            publisher = "World Health Organization",
            year = 2024,
            section = "Health effects",
            text = "Konsumsi alkohol dalam jumlah banyak berhubungan dengan naiknya tekanan darah " +
                "dan risiko stroke.",
            url = "https://www.who.int/news-room/fact-sheets/detail/alcohol"
        ),
        chunk(
            id = "who-vaw-2024#prevalence",
            title = "Violence against women",
            publisher = "World Health Organization",
            year = 2024,
            section = "Intimate partner violence",
            text = "Kekerasan oleh pasangan intim dapat berdampak pada kesehatan fisik dan mental, " +
                "termasuk gangguan tidur, kecemasan, dan keluhan somatik.",
            url = "https://www.who.int/news-room/fact-sheets/detail/violence-against-women"
        ),
        chunk(
            id = "who-vaw-2024#response",
            title = "Violence against women",
            publisher = "World Health Organization",
            year = 2024,
            section = "Health sector response",
            text = "Layanan kesehatan dianjurkan menanyakan pengalaman kekerasan secara aman, " +
                "menjaga kerahasiaan, dan menghubungkan penyintas dengan layanan pendampingan.",
            url = "https://www.who.int/news-room/fact-sheets/detail/violence-against-women"
        ),
        chunk(
            id = "kemkes-htn-2023#screening",
            title = "Cek tekanan darah secara rutin",
            publisher = "Kementerian Kesehatan Republik Indonesia",
            year = 2023,
            section = "Pencegahan",
            text = "Pemeriksaan tekanan darah rutin di puskesmas atau posyandu membantu menemukan " +
                "hipertensi lebih awal, termasuk pada orang yang belum merasakan gejala.",
            url = "https://www.kemkes.go.id/"
        ),
        chunk(
            id = "kemkes-gizi-2023#gizi-seimbang",
            title = "Pedoman gizi seimbang",
            publisher = "Kementerian Kesehatan Republik Indonesia",
            year = 2023,
            section = "Batasan konsumsi",
            text = "Pedoman gizi seimbang menganjurkan membatasi gula, garam, dan lemak, " +
                "serta membaca label kemasan untuk mengetahui kandungan natrium.",
            url = "https://www.kemkes.go.id/"
        ),
        chunk(
            id = "kemkes-htn-2023#followup",
            title = "Hipertensi",
            publisher = "Kementerian Kesehatan Republik Indonesia",
            year = 2023,
            section = "Tindak lanjut",
            text = "Orang dengan tekanan darah tinggi perlu kontrol berkala dan minum obat sesuai anjuran " +
                "tenaga kesehatan, tanpa menghentikannya sendiri.",
            url = "https://www.kemkes.go.id/"
        ),
        chunk(
            id = "sapa129-2024#contact",
            title = "Layanan SAPA 129",
            publisher = "Kementerian Pemberdayaan Perempuan dan Perlindungan Anak",
            year = 2024,
            section = "Kontak",
            text = "SAPA 129 melayani pengaduan kekerasan terhadap perempuan dan anak " +
                "melalui telepon 129 atau WhatsApp 08111-129-129.",
            url = "https://sapa129.kemenpppa.go.id/"
        ),
        chunk(
            id = "empati-clinical-2023#stress-pathway",
            title = "Stres kronis dan tekanan darah",
            publisher = "Empati",
            year = 2023,
            section = "Kerangka penjelasan",
            text = "Beban stres yang berlangsung lama dapat memengaruhi tekanan darah melalui " +
                "peningkatan hormon stres dan ketegangan saraf yang terus menerus, " +
                "bukan melalui hubungan sebab akibat seketika.",
            url = "https://www.who.int/news-room/fact-sheets/detail/hypertension"
        )
    )

    fun byId(id: String): ReferenceChunk? = chunks.firstOrNull { it.id == id }

    private fun chunk(
        id: String,
        title: String,
        publisher: String,
        year: Int,
        section: String,
        text: String,
        url: String
    ) = ReferenceChunk(
        id = id,
        title = title,
        publisher = publisher,
        year = year,
        section = section,
        text = text,
        url = url
    )
}
