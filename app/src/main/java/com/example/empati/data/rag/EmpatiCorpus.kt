package com.example.empati.data.rag

import com.example.empati.data.triage.ReferenceChunk
import java.util.Locale

/**
 * Korpus rujukan Empati. Potongan di sini adalah satu-satunya sumber fakta
 * klinis dan psikososial yang boleh dipakai model. Setiap potongan wajib punya
 * id stabil supaya chip sitasi bisa membuka sumber yang sama dari riwayat.
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
                "kurang aktivitas fisik, berat badan berlebih, konsumsi alkohol, dan merokok, " +
                "sedangkan faktor yang tidak dapat diubah mencakup usia dan riwayat keluarga.",
            url = "https://www.who.int/news-room/fact-sheets/detail/hypertension"
        ),
        chunk(
            id = "perki-htn-2024#classification",
            title = "Pedoman Tata Laksana Hipertensi pada Penyakit Kardiovaskular",
            publisher = "Perhimpunan Dokter Spesialis Kardiovaskular Indonesia",
            year = 2024,
            section = "Klasifikasi tekanan darah",
            text = "Tekanan darah optimal berada di bawah 120/80 mmHg, normal pada 120 sampai 129 " +
                "dan atau 80 sampai 84 mmHg, serta normal tinggi atau pra-hipertensi pada 130 sampai 139 " +
                "dan atau 85 sampai 89 mmHg. Hipertensi derajat satu berada pada 140 sampai 159 dan atau " +
                "90 sampai 99 mmHg, sedangkan derajat dua mencapai 160/100 mmHg atau lebih.",
            url = "https://www.inaheart.org/"
        ),
        chunk(
            id = "perki-htn-2024#home-measurement",
            title = "Pedoman Pemantauan Tekanan Darah di Rumah",
            publisher = "Perhimpunan Dokter Spesialis Kardiovaskular Indonesia",
            year = 2024,
            section = "Prosedur pengukuran",
            text = "Pengukuran tekanan darah yang akurat dilakukan setelah duduk tenang selama lima menit " +
                "dengan punggung tersandar, kaki menapak lantai dan tidak menyilang, serta manset dipasang " +
                "sejajar jantung. Hindari merokok, kopi, atau olahraga tiga puluh menit sebelum pengukuran, " +
                "dan lakukan minimal dua kali pengukuran dengan jeda satu sampai dua menit.",
            url = "https://www.inaheart.org/"
        ),
        chunk(
            id = "perki-htn-2024#crisis-redflags",
            title = "Kewaspadaan Krisis Hipertensi dan Tanda Bahaya Kardiovaskular",
            publisher = "Perhimpunan Dokter Spesialis Kardiovaskular Indonesia",
            year = 2024,
            section = "Tanda bahaya darurat",
            text = "Tekanan darah yang melonjak hingga 180/120 mmHg atau lebih, terutama bila disertai " +
                "nyeri dada, sesak napas, sakit kepala hebat mendadak, pandangan kabur, mati rasa atau " +
                "kelemahan pada wajah dan anggota gerak, maupun kesulitan berbicara, merupakan kondisi " +
                "darurat yang memerlukan pemeriksaan medis segera di fasilitas kesehatan.",
            url = "https://www.inaheart.org/"
        ),
        chunk(
            id = "kemkes-htn-2023#symptoms",
            title = "Kenali Gejala dan Bahaya Hipertensi",
            publisher = "Kementerian Kesehatan Republik Indonesia",
            year = 2023,
            section = "Gejala klinis",
            text = "Hipertensi sering dijuluki pembunuh senyap karena sebagian besar penyandangnya tidak " +
                "merasakan keluhan apa pun. Bila muncul gejala, keluhan yang sering dirasakan meliputi " +
                "sakit kepala bagian belakang, tengkuk terasa berat atau kaku, pusing, jantung berdebar, " +
                "mudah lelah, dan penglihatan terasa buram.",
            url = "https://www.kemkes.go.id/"
        ),
        chunk(
            id = "kemkes-htn-2023#age-genetics",
            title = "Faktor Risiko Usia, Genetik, dan Penyakit Penyerta Hipertensi",
            publisher = "Kementerian Kesehatan Republik Indonesia",
            year = 2023,
            section = "Faktor risiko",
            text = "Risiko hipertensi meningkat seiring bertambahnya usia, terutama mulai usia 40 tahun " +
                "ke atas dan semakin sering pada usia di atas 55 sampai 65 tahun akibat berkurangnya " +
                "kelenturan pembuluh darah arteri. Usia dewasa muda tetap berisiko tinggi bila memiliki " +
                "riwayat hipertensi dalam keluarga, diabetes melitus, kolesterol tinggi, atau obesitas.",
            url = "https://www.kemkes.go.id/"
        ),
        chunk(
            id = "kemkes-htn-2023#womens-health",
            title = "Kesehatan Kardiovaskular Perempuan, Kehamilan, dan Menopause",
            publisher = "Kementerian Kesehatan Republik Indonesia",
            year = 2023,
            section = "Kesehatan perempuan",
            text = "Pada perempuan, perubahan hormon estrogen setelah memasuki masa menopause serta " +
                "riwayat tekanan darah tinggi saat kehamilan atau preeklamsia meningkatkan kerentanan " +
                "terhadap hipertensi di kemudian hari, sehingga pemantauan tekanan darah berkala sangat dianjurkan.",
            url = "https://www.kemkes.go.id/"
        ),
        chunk(
            id = "who-salt-2023#intake",
            title = "Salt reduction",
            publisher = "World Health Organization",
            year = 2023,
            section = "Recommendation",
            text = "Organisasi Kesehatan Dunia menganjurkan konsumsi garam kurang dari 5 gram per hari " +
                "atau setara satu sendok teh peres untuk orang dewasa, yang mengandung kurang dari 2 gram natrium.",
            url = "https://www.who.int/news-room/fact-sheets/detail/salt-reduction"
        ),
        chunk(
            id = "who-salt-2023#hidden",
            title = "Salt reduction",
            publisher = "World Health Organization",
            year = 2023,
            section = "Sources of salt",
            text = "Sebagian besar asupan garam harian berasal dari makanan olahan, makanan siap saji, " +
                "dan bumbu kemasan, bukan semata dari garam meja yang ditambahkan saat memasak.",
            url = "https://www.who.int/news-room/fact-sheets/detail/salt-reduction"
        ),
        chunk(
            id = "kemkes-gizi-2023#hidden-sodium-local",
            title = "Waspada Garam Tersembunyi pada Makanan Harian",
            publisher = "Kementerian Kesehatan Republik Indonesia",
            year = 2023,
            section = "Sumber natrium harian",
            text = "Dalam pola makan sehari-hari di Indonesia, sumber natrium tinggi yang perlu dibatasi " +
                "meliputi mi instan beserta bumbunya, ikan asin, telur asin, kerupuk, kecap asin maupun " +
                "kecap manis berlebih, saus botol, bumbu penyedap kemasan, serta daging olahan dan gorengan bersaus.",
            url = "https://www.kemkes.go.id/"
        ),
        chunk(
            id = "kemkes-gizi-2023#dash-indonesia",
            title = "Pola Makan DASH dengan Pangan Lokal untuk Mengendalikan Tekanan Darah",
            publisher = "Kementerian Kesehatan Republik Indonesia",
            year = 2023,
            section = "Pola makan DASH",
            text = "Pola makan Dietary Approaches to Stop Hypertension atau DASH mengutamakan sayur dan " +
                "buah kaya kalium seperti pisang, alpukat, pepaya, jeruk, bayam, daun kelor, dan brokoli, " +
                "dipadukan sumber protein nabati dan rendah lemak seperti tempe, tahu, kacang-kacangan, " +
                "dan ikan segar untuk membantu merelaksasi pembuluh darah.",
            url = "https://www.kemkes.go.id/"
        ),
        chunk(
            id = "who-activity-2024#adults",
            title = "Physical activity",
            publisher = "World Health Organization",
            year = 2024,
            section = "Adults",
            text = "Orang dewasa dianjurkan melakukan aktivitas fisik aerobik intensitas sedang seperti " +
                "jalan cepat, bersepeda santai, senam, atau berenang selama 150 sampai 300 menit per minggu, " +
                "atau sekitar 30 menit sehari selama lima hari.",
            url = "https://www.who.int/news-room/fact-sheets/detail/physical-activity"
        ),
        chunk(
            id = "who-diet-2024#composition",
            title = "Healthy diet",
            publisher = "World Health Organization",
            year = 2024,
            section = "Practical advice",
            text = "Pola makan sehat mencakup sayur dan buah setiap hari, kacang-kacangan dan biji-bijian utuh, " +
                "serta pembatasan lemak jenuh, lemak trans dari gorengan berulang, dan gula bebas.",
            url = "https://www.who.int/news-room/fact-sheets/detail/healthy-diet"
        ),
        chunk(
            id = "who-diet-2024#caffeine-hydration",
            title = "Healthy diet and beverage habits",
            publisher = "World Health Organization",
            year = 2024,
            section = "Beverages and caffeine",
            text = "Konsumsi kopi atau minuman berkafein tinggi secara berlebihan dapat memicu kenaikan " +
                "tekanan darah sementara, jantung berdebar, dan gangguan tidur. Mencukupi kebutuhan air " +
                "putih serta membatasi minuman manis kemasan membantu menjaga kesehatan pembuluh darah.",
            url = "https://www.who.int/news-room/fact-sheets/detail/healthy-diet"
        ),
        chunk(
            id = "kemkes-htn-2023#weight-waist",
            title = "Pengendalian Berat Badan dan Lingkar Perut untuk Pencegahan Penyakit Tidak Menular",
            publisher = "Kementerian Kesehatan Republik Indonesia",
            year = 2023,
            section = "Pengendalian berat badan",
            text = "Menjaga berat badan ideal dan mengendalikan lingkar perut di bawah 90 sentimeter pada " +
                "laki-laki serta di bawah 80 sentimeter pada perempuan membantu menurunkan beban kerja " +
                "jantung dan menjaga kestabilan tekanan darah.",
            url = "https://www.kemkes.go.id/"
        ),
        chunk(
            id = "who-tobacco-2023#blood-pressure",
            title = "Tobacco",
            publisher = "World Health Organization",
            year = 2023,
            section = "Health effects",
            text = "Paparan nikotin dan asap rokok, baik rokok tembakau maupun rokok elektrik dan perokok " +
                "pasif, menyempitkan pembuluh darah dan merusak dinding arteri. Berhenti merokok menurunkan " +
                "risiko serangan jantung dan stroke dengan manfaat yang mulai terasa dalam hitungan minggu.",
            url = "https://www.who.int/news-room/fact-sheets/detail/tobacco"
        ),
        chunk(
            id = "who-alcohol-2024#blood-pressure",
            title = "Alcohol",
            publisher = "World Health Organization",
            year = 2024,
            section = "Health effects",
            text = "Konsumsi alkohol berhubungan dengan naiknya tekanan darah, gangguan irama jantung, " +
                "dan peningkatan risiko stroke.",
            url = "https://www.who.int/news-room/fact-sheets/detail/alcohol"
        ),
        chunk(
            id = "kemkes-htn-2023#screening",
            title = "Cek tekanan darah secara rutin",
            publisher = "Kementerian Kesehatan Republik Indonesia",
            year = 2023,
            section = "Pencegahan",
            text = "Pemeriksaan tekanan darah rutin melalui posbindu, posyandu, puskesmas, atau mandiri " +
                "di rumah membantu menemukan hipertensi lebih awal sebelum menimbulkan komplikasi pada " +
                "jantung, otak, dan ginjal.",
            url = "https://www.kemkes.go.id/"
        ),
        chunk(
            id = "kemkes-gizi-2023#gizi-seimbang",
            title = "Pedoman gizi seimbang",
            publisher = "Kementerian Kesehatan Republik Indonesia",
            year = 2023,
            section = "Batasan konsumsi",
            text = "Pedoman gizi seimbang menganjurkan pembatasan harian maksimal empat sendok makan gula, " +
                "satu sendok teh garam, dan lima sendok makan minyak atau lemak, disertai kebiasaan " +
                "membaca label informasi nilai gizi pada kemasan makanan.",
            url = "https://www.kemkes.go.id/"
        ),
        chunk(
            id = "kemkes-htn-2023#followup",
            title = "Hipertensi",
            publisher = "Kementerian Kesehatan Republik Indonesia",
            year = 2023,
            section = "Tindak lanjut",
            text = "Orang dengan tekanan darah tinggi perlu kontrol berkala dan mengonsumsi obat " +
                "antihipertensi sesuai anjuran dokter secara teratur, tanpa menghentikan atau mengubah " +
                "dosis sendiri meskipun tubuh sudah terasa nyaman.",
            url = "https://www.kemkes.go.id/"
        ),
        chunk(
            id = "who-mental-2024#sleep-hygiene",
            title = "Sleep and cardiovascular health",
            publisher = "World Health Organization",
            year = 2024,
            section = "Sleep quality",
            text = "Tidur malam yang cukup dan berkualitas selama 7 sampai 9 jam bagi orang dewasa " +
                "memberi kesempatan pembuluh darah beristirahat dan menurunkan tekanan darah secara alami " +
                "saat malam hari. Kebiasaan begadang, sering terbangun, atau insomnia kronis meningkatkan " +
                "hormon kortisol dan ketegangan saraf yang memicu naiknya tekanan darah.",
            url = "https://www.who.int/news-room/fact-sheets/detail/mental-health-strengthening-our-response"
        ),
        chunk(
            id = "who-mental-2024#stress-breathing",
            title = "Stress management and mental wellbeing",
            publisher = "World Health Organization",
            year = 2024,
            section = "Self-care and coping",
            text = "Mengelola beban stres kerja, kecemasan, dan kelelahan mental dapat dibantu dengan " +
                "latihan pernapasan lambat dan dalam selama lima sampai sepuluh menit, mengambil jeda " +
                "istirahat dari layar, membatasi beban pekerjaan di luar jam istirahat, serta berbagi " +
                "cerita dengan orang tepercaya atau tenaga konseling.",
            url = "https://www.who.int/news-room/fact-sheets/detail/mental-health-strengthening-our-response"
        ),
        chunk(
            id = "empati-clinical-2023#stress-pathway",
            title = "Stres kronis dan tekanan darah",
            publisher = "Empati",
            year = 2023,
            section = "Kerangka penjelasan",
            text = "Beban stres yang berlangsung lama maupun tekanan batin akibat hubungan yang tidak " +
                "aman dapat memengaruhi tekanan darah melalui jalur stres kronis, yaitu peningkatan hormon " +
                "stres dan ketegangan saraf simpatis secara terus-menerus, bukan melalui hubungan sebab " +
                "akibat langsung seketika.",
            url = "https://www.who.int/news-room/fact-sheets/detail/hypertension"
        ),
        chunk(
            id = "who-vaw-2024#prevalence",
            title = "Violence against women",
            publisher = "World Health Organization",
            year = 2024,
            section = "Intimate partner violence",
            text = "Kekerasan oleh pasangan intim maupun di lingkungan terdekat dapat berdampak serius " +
                "pada kesehatan fisik dan mental, termasuk gangguan tidur, kecemasan berkepanjangan, " +
                "depresi, keluhan fisik berulang, dan peningkatan risiko kardiovaskular melalui beban stres kronis.",
            url = "https://www.who.int/news-room/fact-sheets/detail/violence-against-women"
        ),
        chunk(
            id = "who-vaw-2024#forms-of-violence",
            title = "Violence against women",
            publisher = "World Health Organization",
            year = 2024,
            section = "Forms of violence",
            text = "Kekerasan berbasis gender mencakup kekerasan fisik, psikologis atau emosional seperti " +
                "penghinaan, ancaman, dan perendahan martabat, kekerasan seksual, penelantaran atau " +
                "pengendalian ekonomi secara paksa, serta pembatasan hubungan sosial dan kekerasan berbasis digital.",
            url = "https://www.who.int/news-room/fact-sheets/detail/violence-against-women"
        ),
        chunk(
            id = "who-vaw-2024#response",
            title = "Violence against women",
            publisher = "World Health Organization",
            year = 2024,
            section = "Health sector response",
            text = "Pendampingan bagi penyintas kekerasan mengutamakan ruang yang aman, mendengarkan " +
                "tanpa menyalahkan korban, menjaga kerahasiaan penuh, dan menghubungkan penyintas dengan " +
                "layanan perlindungan serta pemulihan sesuai kesiapan penyintas.",
            url = "https://www.who.int/news-room/fact-sheets/detail/violence-against-women"
        ),
        chunk(
            id = "komnas-perempuan-2024#safety-planning",
            title = "Panduan Keselamatan Diri dan Pemulihan Penyintas Kekerasan",
            publisher = "Komisi Nasional Anti Kekerasan terhadap Perempuan",
            year = 2024,
            section = "Rencana keselamatan diri",
            text = "Setiap bentuk kekerasan tidak pernah menjadi kesalahan korban. Langkah persiapan " +
                "keselamatan diri meliputi mengenali situasi darurat, menyimpan kontak bantuan tepercaya, " +
                "mengamankan dokumen identitas penting, serta mencari dukungan lembaga layanan pengada " +
                "layanan yang menjamin kerahasiaan penyintas.",
            url = "https://komnasperempuan.go.id/"
        ),
        chunk(
            id = "sapa129-2024#contact",
            title = "Layanan SAPA 129",
            publisher = "Kementerian Pemberdayaan Perempuan dan Perlindungan Anak",
            year = 2024,
            section = "Kontak",
            text = "SAPA 129 melayani pengaduan dan pendampingan kekerasan terhadap perempuan dan anak " +
                "secara rahasia melalui telepon 129 atau WhatsApp 08111-129-129.",
            url = "https://sapa129.kemenpppa.go.id/"
        ),
        chunk(
            id = "sapa129-2024#services",
            title = "Layanan Terpadu Sahabat Perempuan dan Anak (SAPA 129)",
            publisher = "Kementerian Pemberdayaan Perempuan dan Perlindungan Anak",
            year = 2024,
            section = "Layanan pendampingan",
            text = "Melalui jejaring SAPA 129 dan UPTD Perlindungan Perempuan dan Anak di daerah, " +
                "penyintas dapat memperoleh layanan pengaduan, penjangkauan, konseling psikologis, " +
                "pendampingan hukum, hingga rujukan rumah aman tanpa dipungut biaya melalui telepon 129 " +
                "atau WhatsApp 08111-129-129.",
            url = "https://sapa129.kemenpppa.go.id/"
        )
    )

    fun byId(id: String): ReferenceChunk? = chunks.firstOrNull { it.id == id }

    /**
     * Pencarian leksikal atas [chunks] sebagai cadangan ketika server embedding lokal
     * (`:13305`) sedang tidak aktif (misalnya saat aplikasi dijalankan mandiri di ponsel
     * Android menggunakan jalur cloud). Menjaga agar model cloud tetap mendapat rujukan
     * resmi dari korpus tanpa mencampur potongan KBG pada pertanyaan non-kekerasan.
     */
    fun searchLexical(
        query: String,
        limit: Int = 4,
        corpus: List<ReferenceChunk> = chunks
    ): List<ReferenceChunk> {
        if (query.isBlank() || corpus.isEmpty() || limit <= 0) return emptyList()
        val normalized = query.lowercase(Locale.forLanguageTag("id-ID")).trim()
        val queryWords = normalized.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.toSet()
        val tokens = queryWords.filter { it.length >= 3 && it !in STOPWORDS }
        if (tokens.isEmpty()) return emptyList()

        val queryIsGbv = RagKnowledgeRepository.isGbvRelated(normalized)

        return corpus
            .filter { chunk ->
                val isGbvChunk = chunk.id.startsWith("who-vaw-") ||
                    chunk.id.startsWith("sapa129-") ||
                    chunk.id.startsWith("komnas-perempuan-")
                if (isGbvChunk) queryIsGbv else true
            }
            .map { chunk ->
                val titleLower = chunk.title.lowercase(Locale.forLanguageTag("id-ID"))
                val sectionLower = chunk.section.lowercase(Locale.forLanguageTag("id-ID"))
                val textLower = chunk.text.lowercase(Locale.forLanguageTag("id-ID"))
                val chunkWords = (titleLower + " " + sectionLower + " " + textLower)
                    .split(Regex("[^a-z0-9]+"))
                    .filter { it.isNotEmpty() }
                    .toSet()

                var score = 0
                for (token in tokens) {
                    val expanded = SYNONYMS[token].orEmpty() + token
                    for (term in expanded) {
                        when {
                            term.contains(" ") && (textLower.contains(term) || titleLower.contains(term)) -> score += 6
                            chunkWords.contains(term) -> {
                                score += 4
                                if (titleLower.contains(term) || sectionLower.contains(term)) score += 3
                            }
                            term.length >= 5 && (textLower.contains(term) || titleLower.contains(term)) -> score += 2
                        }
                    }
                }
                chunk to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

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

    private val STOPWORDS = setOf(
        "apa", "apakah", "bagaimana", "berapa", "kapan", "dimana", "mengapa", "kenapa", "siapa",
        "yang", "dan", "atau", "untuk", "dari", "pada", "dalam", "dengan", "tanpa", "oleh",
        "saya", "aku", "anda", "kamu", "kita", "kami", "mereka", "ini", "itu", "tersebut",
        "hari", "harian", "minggu", "bulan", "tahun", "saat", "ketika", "jika", "bila", "kalau",
        "bisa", "dapat", "akan", "sudah", "telah", "belum", "sedang", "masih", "harus", "perlu",
        "tidak", "bukan", "jangan", "lebih", "kurang", "sangat", "sekali", "paling", "cukup",
        "cara", "buat", "agar", "supaya", "tentang", "seperti", "sebagai", "karena", "sebab", "bagi"
    )

    private val SYNONYMS: Map<String, List<String>> = mapOf(
        "tensi" to listOf("tekanan darah", "hipertensi", "mmhg"),
        "darah" to listOf("tekanan darah", "hipertensi"),
        "tinggi" to listOf("hipertensi", "tekanan darah"),
        "asin" to listOf("garam", "natrium"),
        "makanan" to listOf("pola makan", "diet", "gizi", "sayur", "buah", "garam"),
        "makan" to listOf("pola makan", "diet", "dash", "garam"),
        "olahraga" to listOf("aktivitas fisik", "jalan cepat", "senam"),
        "gerak" to listOf("aktivitas fisik"),
        "umur" to listOf("usia", "40", "55", "65"),
        "lansia" to listOf("usia", "65"),
        "muda" to listOf("usia", "dewasa muda"),
        "keturunan" to listOf("riwayat keluarga", "genetik"),
        "begadang" to listOf("tidur", "insomnia", "kortisol"),
        "cemas" to listOf("stres", "kecemasan", "pernapasan"),
        "burnout" to listOf("stres", "kelelahan mental"),
        "kdrt" to listOf("kekerasan", "pasangan intim", "sapa 129"),
        "kbg" to listOf("kekerasan berbasis gender", "kekerasan", "sapa 129"),
        "pelecehan" to listOf("kekerasan", "seksual", "sapa 129"),
        "ancaman" to listOf("kekerasan", "psikologis", "keselamatan", "sapa 129"),
        "bantuan" to listOf("sapa 129", "pengaduan", "pendampingan"),
        "lapor" to listOf("sapa 129", "pengaduan", "129"),
        "ukur" to listOf("pengukuran", "manset", "duduk tenang"),
        "tensimeter" to listOf("pengukuran", "manset"),
        "darurat" to listOf("krisis", "180", "nyeri dada", "sesak napas"),
        "bahaya" to listOf("krisis", "komplikasi", "stroke", "jantung")
    )
}
