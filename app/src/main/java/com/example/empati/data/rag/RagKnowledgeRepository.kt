package com.example.empati.data.rag

import java.util.Locale

enum class RagTopicScope {
    HYPERTENSION_CLINICAL,
    LIFESTYLE_DASH,
    ASN_PSYCHOSOCIAL,
    GBV_PROTECTION
}

/**
 * Representasi rujukan pedoman klinis pada domain database RAG (`rag_knowledge_chunks`).
 * Terpisah sepenuhnya dari aturan inferensi `CfRule` (`CfKnowledgeBase`) agar sistem konsultasi AI
 * tidak pernah mengintervensi perhitungan Certainty Factor.
 */
data class RagDocumentChunk(
    val chunkId: String,
    val docSource: String,
    val topicScope: RagTopicScope,
    val title: String,
    val content: String,
    val keywords: List<String>,
    val chunkVersion: String = "rag-corpus-v3.1"
)

/**
 * Jembatan konteks satu arah (Read-Only Context Bridge) dari hasil skrining ke modul RAG/Chatbot.
 * Hanya mengekspos label kualitatif awam; TIDAK PERNAH membawa angka CF, MB, MD, atau bobot.
 */
data class ScreeningContextSnapshot(
    val riskLevelLabel: String,
    val integratedCategoryLabel: String?,
    val screeningDate: String,
    val summaryNarrative: String? = null,
    val hypertensionStatusLabel: String? = null,
    val gbvStatusLabel: String? = null,
    val keyFactors: List<String> = emptyList(),
    val protectiveFactors: List<String> = emptyList(),
    val relationshipExplanation: String? = null,
    val keyRecommendations: List<String> = emptyList()
)

data class RagRetrievalResult(
    val query: String,
    val matchedChunks: List<RagDocumentChunk>,
    val contextSnapshot: ScreeningContextSnapshot?
)

object RagKnowledgeRepository {

    val defaultCorpus: List<RagDocumentChunk> = listOf(
        RagDocumentChunk(
            chunkId = "DOC-HT-01",
            docSource = "Pedoman Tatalaksana Hipertensi Kemenkes RI & PERKI",
            topicScope = RagTopicScope.HYPERTENSION_CLINICAL,
            title = "Pedoman Klinis Hipertensi, Faktor Risiko Usia & Deteksi Dini",
            content = "Tekanan darah optimal berada di bawah 120/80 mmHg, normal tinggi (pra-hipertensi) pada kisaran 130–139/85–89 mmHg, dan hipertensi ditegakkan bila pengukuran berulang mencapai >=140/90 mmHg. Risiko hipertensi meningkat seiring bertambahnya usia (terutama mulai usia 40 tahun ke atas dan semakin tinggi di atas usia 55–65 tahun akibat kekakuan pembuluh darah arteri), namun kelompok usia muda juga berisiko bila memiliki riwayat keluarga, obesitas, atau gaya hidup tinggi natrium.",
            keywords = listOf(
                "hipertensi", "darah tinggi", "tensi", "mmhg", "normal", "gejala", "tanda",
                "umur", "usia", "lansia", "muda", "tua", "faktor", "penyebab", "keturunan",
                "genetik", "komplikasi", "stroke", "jantung", "ginjal", "obat", "hamil", "diabetes"
            )
        ),
        RagDocumentChunk(
            chunkId = "DOC-HT-02",
            docSource = "Panduan Diet DASH & Pencegahan Hipertensi WHO/Kemenkes",
            topicScope = RagTopicScope.LIFESTYLE_DASH,
            title = "Pencegahan Hipertensi, Pembatasan Garam & Pola Makan DASH",
            content = "Langkah utama mencegah dan mengendalikan hipertensi meliputi pembatasan konsumsi garam maksimal 1 sendok teh per hari (sekitar 2000 mg natrium), penerapan pola makan DASH (memperbanyak sayur, buah kaya kalium seperti pisang dan alpukat, serta biji-bijian utuh), membatasi makanan cepat saji dan kafein berlebih, berhenti merokok, menjaga berat badan sehat, serta rutin memeriksa tekanan darah.",
            keywords = listOf(
                "cegah", "pencegahan", "mencegah", "garam", "asin", "diet", "makan", "makanan",
                "dash", "natrium", "kopi", "kafein", "rokok", "merokok", "buah", "sayur", "kalium", "pantangan"
            )
        ),
        RagDocumentChunk(
            chunkId = "DOC-HT-03",
            docSource = "Rekomendasi Aktivitas Fisik Kardiovaskular WHO & Kemenkes",
            topicScope = RagTopicScope.LIFESTYLE_DASH,
            title = "Aktivitas Fisik Aerobik & Pengendalian Berat Badan",
            content = "Aktivitas fisik aerobik intensitas sedang seperti jalan cepat, bersepeda, senam, atau berenang minimal 150 menit per minggu (30 menit per hari selama 5 hari) terbukti menjaga elastisitas pembuluh darah dan menurunkan tekanan darah sistolik secara alami bagi seluruh kelompok usia dewasa.",
            keywords = listOf(
                "olahraga", "aktivitas", "gerak", "jalan", "senam", "sepeda", "renang", "berat badan", "obesitas", "fisik"
            )
        ),
        RagDocumentChunk(
            chunkId = "DOC-ASN-01",
            docSource = "Pedoman Kesehatan Mental, Kualitas Tidur & Manajemen Stres",
            topicScope = RagTopicScope.ASN_PSYCHOSOCIAL,
            title = "Stres Kronis, Kualitas Tidur, dan Tekanan Darah",
            content = "Beban aktivitas harian, tekanan pekerjaan atau rumah tangga, kecemasan, dan kurang tidur (<7 jam per malam) mengaktifkan jalur stres kronis (chronic stress pathway) melalui peningkatan saraf simpatis serta hormon kortisol yang memicu lonjakan tekanan darah. Hubungan faktor psikososial dengan hipertensi bersifat pola keterkaitan kronis jangka panjang, bukan sebab-akibat langsung.",
            keywords = listOf(
                "stres", "cemas", "tidur", "begadang", "insomnia", "istirahat", "kerja", "kantor",
                "beban", "kronis", "kortisol", "lelah", "burnout", "pikiran"
            )
        ),
        RagDocumentChunk(
            chunkId = "DOC-GBV-01",
            docSource = "UU TPKS & Layanan Perlindungan KemenPPPA (SAPA 129)",
            topicScope = RagTopicScope.GBV_PROTECTION,
            title = "Ruang Aman & Layanan Sahabat Perempuan dan Anak (SAPA 129)",
            content = "Setiap bentuk kekerasan fisik, psikologis, seksual, ekonomi, maupun digital di lingkungan kerja, sosial, maupun rumah tangga bukanlah kesalahan korban. Tekanan akibat pengalaman KBG dapat memicu respons stres kronis (chronic stress pathway) yang berdampak pada kesehatan, meskipun bukan penyebab langsung hipertensi. Layanan pengaduan dan pendampingan rahasia bebas stigma tersedia melalui hotline SAPA 129 (Telepon 129 atau WhatsApp 08111-129-129).",
            keywords = listOf(
                "kekerasan", "kbg", "bantuan", "sapa", "129", "kdrt", "pelecehan", "ancaman",
                "psikologis", "terancam", "intimidasi", "seksual", "ekonomi", "digital", "korban"
            )
        ),
        RagDocumentChunk(
            chunkId = "DOC-HT-04",
            docSource = "Pedoman Pemantauan Tekanan Darah Mandiri PERKI & Kemenkes RI",
            topicScope = RagTopicScope.HYPERTENSION_CLINICAL,
            title = "Prosedur Pengukuran Tekanan Darah di Rumah & Gejala Hipertensi",
            content = "Pengukuran tekanan darah yang akurat dilakukan setelah duduk tenang 5 menit dengan punggung tersandar, kaki menapak lantai, dan manset sejajar jantung, serta tidak merokok atau minum kopi 30 menit sebelumnya. Sebagian besar hipertensi tidak bergejala, namun bila melonjak dapat muncul keluhan tengkuk terasa berat, nyeri kepala belakang, pusing, atau jantung berdebar.",
            keywords = listOf(
                "ukur", "pengukuran", "tensimeter", "manset", "rumah", "gejala", "tengkuk",
                "pusing", "kepala", "berdebar", "akurat", "posisi"
            )
        ),
        RagDocumentChunk(
            chunkId = "DOC-HT-05",
            docSource = "Pedoman Kegawatdaruratan Kardiovaskular PERKI & WHO",
            topicScope = RagTopicScope.HYPERTENSION_CLINICAL,
            title = "Krisis Hipertensi, Tanda Bahaya Darurat, dan Kepatuhan Obat",
            content = "Tekanan darah yang mencapai >=180/120 mmHg disertai nyeri dada, sesak napas, pandangan kabur, bicara pelo, atau kelemahan satu sisi tubuh merupakan kondisi darurat medis yang memerlukan penanganan segera di IGD atau fasilitas kesehatan. Penyandang hipertensi yang mendapat terapi obat dari dokter wajib meminumnya secara teratur tanpa menghentikan sendiri.",
            keywords = listOf(
                "krisis", "darurat", "bahaya", "180", "120", "nyeri dada", "sesak napas",
                "stroke", "lumpuh", "pelo", "obat", "berhenti obat", "igd"
            )
        ),
        RagDocumentChunk(
            chunkId = "DOC-HT-06",
            docSource = "Pedoman Kesehatan Kardiovaskular Perempuan Kemenkes RI",
            topicScope = RagTopicScope.HYPERTENSION_CLINICAL,
            title = "Hipertensi pada Perempuan, Kehamilan, Preeklamsia, dan Menopause",
            content = "Perempuan dengan riwayat tekanan darah tinggi saat kehamilan (preeklamsia) atau yang telah memasuki masa menopause mengalami perubahan profil hormon estrogen yang meningkatkan risiko kekakuan pembuluh darah dan hipertensi, sehingga dianjurkan rutin memantau tensi.",
            keywords = listOf(
                "perempuan", "wanita", "hamil", "kehamilan", "preeklamsia", "menopause",
                "hormon", "estrogen", "ibu", "melahirkan", "nifas"
            )
        ),
        RagDocumentChunk(
            chunkId = "DOC-DASH-04",
            docSource = "Pedoman Gizi Seimbang & Pangan Lokal Rendah Natrium Kemenkes RI",
            topicScope = RagTopicScope.LIFESTYLE_DASH,
            title = "Sumber Garam Tersembunyi & Pangan Lokal Tinggi Kalium di Indonesia",
            content = "Selain garam dapur, sumber natrium tinggi yang sering tersembunyi pada hidangan harian meliputi mi instan, ikan asin, telur asin, kerupuk, kecap, saus kemasan, dan penyedap rasa. Sebaliknya, pangan lokal tinggi kalium dan serat seperti pisang, alpukat, pepaya, bayam, daun kelor, tempe, dan tahu membantu menyeimbangkan natrium dan menjaga kelenturan pembuluh darah.",
            keywords = listOf(
                "mie instan", "mi instan", "ikan asin", "telur asin", "kerupuk", "kecap", "saus",
                "penyedap", "micin", "msg", "pisang", "alpukat", "bayam", "kelor", "tempe", "tahu", "kalium"
            )
        ),
        RagDocumentChunk(
            chunkId = "DOC-ASN-02",
            docSource = "Pedoman Manajemen Stres Kerja, Burnout & Kebersihan Tidur WHO",
            topicScope = RagTopicScope.ASN_PSYCHOSOCIAL,
            title = "Kebersihan Tidur (Sleep Hygiene), Relaksasi Pernapasan & Regulasi Emosi",
            content = "Menjaga waktu tidur 7–9 jam setiap malam membantu penurunan alami tekanan darah saat tidur (nocturnal dipping). Saat menghadapi tekanan pekerjaan atau kecemasan, latihan pernapasan lambat dan dalam selama 5–10 menit, membatasi layar gawai sebelum tidur, dan mengurangi kafein sore hari membantu menenangkan sistem saraf simpatis.",
            keywords = listOf(
                "relaksasi", "napas", "pernapasan", "meditasi", "tenang", "burnout", "lelah mental",
                "gawai", "hp", "malam", "jam tidur", "kualitas tidur", "emosi", "overthinking"
            )
        ),
        RagDocumentChunk(
            chunkId = "DOC-GBV-02",
            docSource = "Panduan Keselamatan Diri Penyintas Komnas Perempuan & KemenPPPA",
            topicScope = RagTopicScope.GBV_PROTECTION,
            title = "Rencana Keselamatan Diri (Safety Planning) & Pendampingan Terpadu",
            content = "Penyintas kekerasan di rumah tangga, tempat kerja, maupun ranah digital dapat menyiapkan rencana keselamatan diri dengan menyimpan kontak darurat SAPA 129 (Telepon 129 atau WhatsApp 08111-129-129), mengamankan dokumen identitas penting, mengenali jalur aman atau kerabat tepercaya, serta mengakses konseling psikologis dan bantuan hukum gratis melalui UPTD PPA.",
            keywords = listOf(
                "rencana keselamatan", "aman", "keamanan", "uptd ppa", "komnas perempuan",
                "rumah aman", "hukum", "konseling", "lapor", "pengaduan", "melindungi diri"
            )
        )
    )

    private val allowedDomainKeywords = setOf(
        // Kesehatan, Medis, Hipertensi, Tubuh & Gaya Hidup
        "sehat", "kesehatan", "medis", "penyakit", "sakit", "dokter", "puskesmas", "rumah sakit", "klinik",
        "hipertensi", "darah", "tensi", "mmhg", "sistolik", "diastolik", "jantung", "vaskular",
        "kardiovaskular", "stroke", "pembuluh", "arteri", "kolesterol", "ginjal", "preeklamsia",
        "umur", "usia", "lansia", "tua", "muda", "dewasa", "anak", "remaja", "menopause", "wanita", "perempuan", "pria", "laki",
        "keturunan", "genetik", "faktor", "penyebab", "beresiko", "berisiko", "risiko", "bahaya", "komplikasi", "normal",
        "garam", "asin", "natrium", "diet", "dash", "makan", "makanan", "minuman", "pantangan", "gizi", "nutrisi", "vitamin",
        "kopi", "kafein", "rokok", "merokok", "olahraga", "aktivitas", "gerak", "jalan", "senam", "lari", "fisik",
        "berat", "obesitas", "kurus", "kalium", "sayur", "buah", "pisang", "alpukat", "gula", "alkohol", "lemak",
        "cegah", "pencegahan", "mencegah", "gejala", "tanda", "ciri", "obat", "terapi", "sembuh", "pengobatan",
        "skrining", "screening", "hasil", "skor", "evaluasi", "analisis", "analisa", "simpul", "simpulkan", "kesimpulan",
        "interpretasi", "rekomendasi", "saran", "solusi", "kondisi", "ulasan", "bahas",
        "hamil", "kehamilan", "nifas", "menyusui", "diabetes", "pusing", "kepala", "tengkuk",
        "nyeri", "dada", "sesak", "napas", "lemas", "mual", "demam", "batuk", "asam urat", "lambung", "maag", "anemia",
        // Psikologi, Kesehatan Mental, Emosi & Tidur
        "psikologi", "psikologis", "mental", "jiwa", "emosi", "perasaan", "stres", "stress", "cemas", "kecemasan", "anxiety",
        "depresi", "sedih", "takut", "panik", "trauma", "burnout", "lelah", "capek", "beban", "pikiran", "overthinking",
        "tekan", "tekanan", "tidur", "istirahat", "begadang", "insomnia", "kerja", "kantor", "konseling", "psikolog", "psikiater",
        "relaksasi", "meditasi", "tenang", "bahagia", "motivasi", "percaya diri", "kesepian", "hubungan", "keluarga",
        // Kekerasan, KBG, KDRT & Perlindungan
        "kekerasan", "kbg", "aman", "keamanan", "bantuan", "sapa", "129", "kdrt", "pelecehan", "ancaman", "terancam",
        "intimidasi", "seksual", "ekonomi", "digital", "verbal", "korban", "rujukan", "lapor", "pengaduan", "perlindungan",
        "dipukul", "dihina", "dilecehkan", "dikucilkan", "dimaki", "didorong", "paksa", "pemaksaan", "bulli", "bullying", "toxic",
        // Identitas Diri & Peran Asisten (Empati)
        "empati", "siapa anda", "siapa kamu", "anda siapa", "kamu siapa",
        "siapakah anda", "siapakah kamu", "nama anda", "nama kamu", "siapa namamu", "perkenalkan",
        "kenalan", "tentang anda", "tentang kamu", "kamu bisa apa", "anda bisa apa", "bisa bantu apa",
        "fungsi anda", "fungsi kamu", "tugas anda", "tugas kamu", "kegunaan anda", "kegunaan kamu"
    )

    private val identityPatterns = listOf(
        "siapa anda", "siapa kamu", "anda siapa", "kamu siapa",
        "siapakah anda", "siapakah kamu", "nama anda", "nama kamu",
        "siapa namamu", "siapa nama anda", "siapa nama kamu",
        "apa itu empati", "siapa empati", "tentang empati",
        "perkenalkan diri", "kenalan dong", "boleh kenalan",
        "tentang anda", "tentang kamu", "profil anda", "profil kamu",
        "kamu bisa apa", "anda bisa apa", "bisa bantu apa",
        "apa yang bisa kamu lakukan", "apa yang bisa anda lakukan",
        "apa yang bisa kamu bantu", "apa yang bisa anda bantu",
        "fungsi anda", "fungsi kamu", "tugas anda", "tugas kamu",
        "kegunaan anda", "kegunaan kamu", "chatbot ini apa"
    )

    fun isIdentityQuery(query: String): Boolean {
        val normalized = query.lowercase(Locale.forLanguageTag("id-ID"))
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (isScreeningAnalysisQuery(normalized) || isSimpleResultLookupQuery(normalized)) return false
        if (normalized == "siapa" || normalized == "empati") return true
        return identityPatterns.any { normalized.contains(it) }
    }

    private val screeningTargetKeywords = listOf(
        "skrining", "screening", "hasil", "skor", "risiko saya", "kondisi saya",
        "tes saya", "pemeriksaan saya", "evaluasi saya"
    )

    private val screeningAnalysisIndicators = listOf(
        "analisis", "analisa", "menganalisis", "menganalisa",
        "simpul", "simpulkan", "kesimpulan", "menyimpulkan", "rangkum", "rangkuman",
        "jelaskan", "penjelasan", "terangkan", "uraikan", "jabarkan",
        "arti", "artinya", "makna", "maksud", "interpretasi",
        "evaluasi", "bahas", "membahas", "diskusi", "ulasan", "bedah",
        "kenapa", "mengapa", "faktor", "penyebab", "pemicu", "alasan", "pengaruh", "kaitan", "hubungan",
        "saran", "rekomendasi", "solusi", "langkah", "tips", "harus apa", "apa yang harus",
        "apa yang perlu", "apa yang sebaiknya", "tindak lanjut", "solusinya",
        "menurut anda", "menurut kamu", "pendapat", "pandangan", "bagaimana kondisi",
        "gimana hasil", "bagaimana hasil", "apa kesimpulan"
    )

    private val simpleLookupPhrases = listOf(
        "lihat hasil", "cek hasil", "tampilkan hasil", "buka hasil",
        "apa hasil skrining", "apa hasil screening",
        "hasil skrining saya", "hasil screening saya",
        "berapa skor", "skor skrining", "skor screening", "lihat skor"
    )

    /**
     * Mendeteksi apakah pengguna meminta **analisis, kesimpulan, penjelasan mendalam, faktor penyebab,
     * atau saran tindak lanjut** atas hasil skriningnya (mis. "analisis hasil screening saya",
     * "apa yang bisa anda simpulkan dari hasil screening saya", "jelaskan hasil skrining saya").
     * Pertanyaan jenis ini HARUS diteruskan ke LLM beserta `ScreeningContextSnapshot` agar dianalisis secara cerdas.
     */
    fun isScreeningAnalysisQuery(query: String): Boolean {
        val normalized = query.lowercase(Locale.forLanguageTag("id-ID"))
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val mentionsScreeningOrResult = screeningTargetKeywords.any { normalized.contains(it) }
        if (!mentionsScreeningOrResult) return false
        return screeningAnalysisIndicators.any { normalized.contains(it) }
    }

    /**
     * Mendeteksi apakah pengguna hanya meminta **melihat status/ringkasan singkat hasil skrining**
     * (mis. tombol cepat "Lihat hasil skrining saya", "Berapa skor skrining saya?", "Cek hasil skrining saya")
     * tanpa meminta analisis atau kesimpulan mendalam.
     */
    fun isSimpleResultLookupQuery(query: String): Boolean {
        val normalized = query.lowercase(Locale.forLanguageTag("id-ID"))
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (isScreeningAnalysisQuery(normalized)) return false
        if (simpleLookupPhrases.any { normalized.contains(it) }) return true
        return normalized == "hasil" || normalized == "skrining" || normalized == "screening" || normalized == "skor"
    }

    private val gbvTriggerKeywords = setOf(
        "kekerasan", "kbg", "sapa", "129", "kdrt", "pelecehan", "ancaman",
        "terancam", "intimidasi", "seksual", "dipukul", "dihina", "dilecehkan", "dikucilkan", "korban"
    )

    private val indonesianStopwords = setOf(
        "apa", "apakah", "bagaimana", "berapa", "kapan", "dimana", "mengapa", "kenapa", "siapa",
        "yang", "dan", "atau", "untuk", "dari", "pada", "dalam", "dengan", "tanpa", "oleh",
        "saya", "aku", "anda", "kamu", "kita", "kami", "mereka", "ini", "itu", "tersebut",
        "hari", "harian", "minggu", "bulan", "tahun", "saat", "ketika", "jika", "bila", "kalau",
        "bisa", "dapat", "akan", "sudah", "telah", "belum", "sedang", "masih", "harus", "perlu",
        "tidak", "bukan", "jangan", "lebih", "kurang", "sangat", "sekali", "paling", "cukup",
        "cara", "buat", "agar", "supaya", "tentang", "seperti", "sebagai", "karena", "sebab", "bagi",
        "terkena", "kena", "orang", "usia", "umur"
    )

    fun isGbvRelated(query: String, matchedChunks: List<RagDocumentChunk> = emptyList()): Boolean {
        val normalized = query.lowercase(Locale.forLanguageTag("id-ID"))
        val words = normalized.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.toSet()
        if (gbvTriggerKeywords.any { kw -> words.contains(kw) || normalized.contains(kw) }) {
            return true
        }
        return matchedChunks.any { it.topicScope == RagTopicScope.GBV_PROTECTION || it.chunkId == "DOC-GBV-01" }
    }

    fun isTopicAllowed(query: String, matchedChunks: List<RagDocumentChunk> = emptyList()): Boolean {
        val normalized = query.lowercase(Locale.forLanguageTag("id-ID")).trim()
        if (isIdentityQuery(normalized)) return true
        if (isScreeningAnalysisQuery(normalized) || isSimpleResultLookupQuery(normalized)) return true
        if (allowedDomainKeywords.any { normalized.contains(it) }) return true
        return matchedChunks.any { chunk -> chunk.keywords.any { kw -> normalized.contains(kw) } }
    }

    fun retrieve(
        query: String,
        contextSnapshot: ScreeningContextSnapshot? = null,
        topK: Int = 1,
        corpus: List<RagDocumentChunk> = defaultCorpus
    ): RagRetrievalResult {
        val normalized = query.lowercase(Locale.forLanguageTag("id-ID")).trim()
        val queryWords = normalized.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.toSet()
        val tokens = queryWords.filter { it.length >= 3 && it !in indonesianStopwords }
        val queryIsGbv = isGbvRelated(normalized)

        val scored = corpus
            .filter { chunk ->
                // JANGAN PERNAH mengambil chunk KBG/SAPA 129 jika pertanyaan bukan tentang KBG/kekerasan!
                if (chunk.topicScope == RagTopicScope.GBV_PROTECTION || chunk.chunkId == "DOC-GBV-01") {
                    queryIsGbv
                } else {
                    true
                }
            }
            .map { chunk ->
                var score = 0
                for (kw in chunk.keywords) {
                    val kwLower = kw.lowercase(Locale.forLanguageTag("id-ID"))
                    if (kwLower.contains(" ")) {
                        if (normalized.contains(kwLower)) score += 5
                    } else if (queryWords.contains(kwLower)) {
                        score += 4
                    } else if (kwLower.length >= 5 && normalized.contains(kwLower)) {
                        score += 3
                    }
                }
                val titleWords = chunk.title.lowercase(Locale.forLanguageTag("id-ID"))
                    .split(Regex("[^a-z0-9]+")).toSet()
                for (token in tokens) {
                    if (titleWords.contains(token)) score += 2
                }
                chunk to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(topK.coerceAtLeast(1))
            .map { it.first }

        return RagRetrievalResult(
            query = query,
            matchedChunks = scored,
            contextSnapshot = contextSnapshot
        )
    }
}
