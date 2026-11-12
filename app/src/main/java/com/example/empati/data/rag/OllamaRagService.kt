package com.example.empati.data.rag

import com.example.empati.data.cloud.SupabaseCloudSyncService
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

data class OllamaGenerationOutcome(
    val text: String,
    val usedOllamaLlm: Boolean,
    val modelName: String,
    val retrievedChunks: List<RagDocumentChunk> = emptyList()
)

/**
 * Mesin Konsultasi AI-EMPATI berbasis **LLM Murni (Pure Scoped LLM)** tanpa injeksi korpus/rujukan kaku.
 *
 * Arsitektur:
 * 1. Menggunakan model LLM secara murni (`qwen2.5:3b-instruct` / `empati-qwen2.5:3b` via Ollama,
 *    serta fallback Cloud HTTPS LLM jika Ollama lokal tidak aktif) tanpa menempelkan teks korpus,
 *    tanpa chip sitasi dokumen, dan tanpa embel-embel `"Sumber rujukan: ..."`.
 * 2. Membatasi ruang lingkup percakapan (Topic Scoping) secara ketat hanya pada 3 ranah:
 *    - **Kesehatan** (hipertensi, tekanan darah, jantung, gejala, faktor usia/genetik, nutrisi, olahraga, gaya hidup).
 *    - **Psikologi** (stres, kecemasan, kualitas tidur, beban pikiran/pekerjaan, kesehatan mental, regulasi emosi).
 *    - **Kekerasan** (Kekerasan Berbasis Gender / KBG, KDRT, pelecehan, keamanan diri, & layanan SAPA 129).
 * 3. Tetap menjaga privasi pengguna (menyensor NIP/NIK/Email/No.HP) dan Zero-Leak (tanpa CF/MB/MD).
 */
object OllamaRagService {

    const val DEFAULT_MODEL = "qwen2.5:3b-instruct"
    const val CUSTOM_GUARDED_MODEL = "empati-qwen2.5:3b"

    // Urutan prioritas: Loopback/ADB reverse -> Emulator host -> Wi-Fi LAN
    val CANDIDATE_ENDPOINTS = listOf(
        "http://127.0.0.1:11434",   // ADB reverse (tcp:11434) / Localhost
        "http://10.0.2.2:11434",    // Android Emulator loopback ke host PC
        "http://192.168.1.7:11434"  // Wi-Fi LAN host PC
    )

    const val OUT_OF_SCOPE_REFUSAL =
        "Maaf ya, untuk topik itu saya belum bisa membantu. Sebagai teman bicara Anda di SRYA Emora, saya hadir khusus untuk menemani obrolan seputar tekanan darah dan gaya hidup sehat, pengelolaan stres atau kualitas tidur, serta dukungan ruang aman bila Anda menghadapi situasi kekerasan. Ada hal dari sekitar itu yang sedang ingin Anda ceritakan?"

    const val SRYA_EMORA_SELF_INTRO =
        "Halo, salam kenal! Saya SRYA Emora, asisten dan teman bicara pribadi Anda di aplikasi ini. Saya hadir sebagai ruang yang aman dan nyaman untuk menemani Anda memahami kondisi tekanan darah dan kesehatan harian, mengelola stres, kecemasan, atau pola tidur yang sedang terganggu, serta memberikan informasi dukungan dan perlindungan bila Anda menghadapi situasi hubungan yang tidak aman atau kekerasan. Anda bebas bercerita atau bertanya dengan bahasa sehari-hari. Ada yang sedang Anda rasakan atau ingin kita obrolkan hari ini?"

    const val SAPA_129_MANDATORY_NOTE =
        "Bila Anda atau seseorang di dekat Anda sedang membutuhkan ruang cerita dan perlindungan yang aman serta bebas penghakiman, Anda bisa menghubungi sahabat layanan SAPA 129 melalui telepon 129 atau WhatsApp 08111-129-129."

    const val CHRONIC_STRESS_PATHWAY_NOTE =
        "Sebagai catatan tambahan, kaitan antara beban stres yang berkepanjangan atau pengalaman kekerasan dengan naiknya tekanan darah bekerja melalui jalur stres kronis di dalam tubuh (peningkatan hormon stres dan ketegangan saraf secara terus-menerus), bukan hubungan sebab-akibat seketika."

    val SYSTEM_PROMPT: String = """
        Anda adalah SRYA Emora, teman bicara dan pendamping kesehatan pribadi pengguna di aplikasi ini.

        ATURAN UTAMA:
        1. IDENTITAS DIRI & PERAN:
           - Jika pengguna bertanya siapa Anda ("siapa anda", "siapa kamu", "apa itu SRYA Emora", "kamu bisa bantu apa"), perkenalkan diri Anda dengan hangat sebagai SRYA Emora, teman bicara dan pendamping ruang aman yang membantu pengguna memahami kesehatan tekanan darah dan gaya hidup, mengelola stres atau kualitas tidur, serta memberikan dukungan informasi perlindungan dari kekerasan.
        2. RUANG LINGKUP PERCAKAPAN:
           - KESEHATAN: Hipertensi, tekanan darah, kesehatan jantung & pembuluh darah, faktor risiko (usia, genetik, komorbid), gejala, pola makan/nutrisi, olahraga, dan gaya hidup sehat.
           - PSIKOLOGI: Manajemen stres, kecemasan, kualitas tidur/insomnia, kelelahan mental (burnout), emosi, dan dukungan kesehatan jiwa.
           - KEKERASAN: Kekerasan Berbasis Gender (KBG), KDRT, kekerasan fisik/psikologis/seksual/ekonomi/digital, keselamatan diri, serta informasi layanan SAPA 129 (Telepon 129 / WhatsApp 08111-129-129).
           Jika pengguna bertanya topik yang sama sekali di luar hal tersebut (misalnya politik, kripto, matematika, koding, hiburan), sampaikan dengan hangat dan manusiawi bahwa Anda fokus menemani obrolan seputar kesehatan, ketenangan pikiran, dan ruang aman.
        3. ANALISIS & KESIMPULAN HASIL SKRINING PENGGUNA:
           - Bila di dalam prompt terdapat [KONTEKS HASIL SKRINING PENGGUNA SAAT INI] dan pengguna meminta analisis, kesimpulan, penjelasan, atau saran atas hasil skriningnya, lakukan analisis mendalam dan relevan dengan memadukan temuan skrining tersebut bersama penalaran medis dan psikososial Anda.
           - Uraikan secara bernas: (a) kesimpulan utama dari kondisi tekanan darah dan kesejahteraan pengguna saat ini, (b) analisis bagaimana faktor-faktor yang teridentifikasi saling berkaitan secara fisiologis maupun psikologis (misalnya pengaruh pola makan/garam, aktivitas fisik, kualitas tidur, beban stres harian, riwayat keluarga, atau jalur stres kronis bila ada indikasi beban relasi/kekerasan), serta (c) langkah nyata yang paling prioritas untuk dilakukan pengguna.
           - Jangan hanya mengulang kalimat status singkat; berikan wawasan analitis yang relevan, empatik, dan mudah dipahami.
        4. GAYA BAHASA (MANUSIAWI, HANGAT & TUNTAS):
           - Gunakan Bahasa Indonesia yang luwes, hangat, empatik, dan mudah dipahami seperti berbicara dengan teman atau konselor yang peduli—hindari bahasa kaku seperti robot.
           - Sajikan maksimal 3 hingga 4 poin ringkas atau 2 hingga 3 paragraf padat, dan PASTIKAN seluruh kalimat selesai secara utuh hingga tanda titik terakhir.
           - DILARANG menggunakan simbol bintang markdown (** atau *).
           - Jangan mencantumkan teks "Sumber rujukan:" atau kode dokumen di akhir jawaban.
           - Jangan sebutkan layanan SAPA 129 kecuali pengguna memang bertanya tentang kekerasan/KBG atau hasil skrining pengguna memiliki indikasi KBG.
           - Jangan gunakan istilah teknis sistem pakar (CF, MB, MD) atau simbol %.
    """.trimIndent()

    private val nipContiguousRegex = Regex("\\b\\d{16,18}\\b")
    private val nipSpacedRegex = Regex("\\b\\d{8}\\s+\\d{6}\\s+[12]\\s+\\d{3}\\b")
    private val emailRegex = Regex("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")
    private val phoneRegex = Regex("(?:\\+62|62|0)8\\d{7,11}\\b")
    private val decimalLeakRegex = Regex("\\b(\\d+)\\.(\\d+)\\b")
    private val internalAcronymRegex = Regex("\\b(CF|MB|MD)\\b")
    private val markdownBoldRegex = Regex("\\*\\*(.*?)\\*\\*")

    /**
     * Firewall Privasi Input: Menyensor NIP, NIK, Email, atau Nomor HP sebelum dikirim ke LLM.
     */
    fun sanitizePrivacyInput(rawQuery: String): String {
        return rawQuery
            .replace(nipSpacedRegex, "[NIP-DIRAHASIAKAN]")
            .replace(nipContiguousRegex, "[NIP-DIRAHASIAKAN]")
            .replace(emailRegex, "[EMAIL-DIRAHASIAKAN]")
            .replace(phoneRegex, "[NO-HP-DIRAHASIAKAN]")
            .trim()
    }

    /**
     * Menyusun prompt LLM murni (tanpa korpus/dokumen rujukan), namun menyertakan jembatan konteks
     * kualitatif `ScreeningContextSnapshot` (100% Zero-Leak) ketika pengguna memiliki hasil skrining,
     * khususnya saat pengguna meminta analisis atau kesimpulan dari hasil skriningnya.
     */
    fun buildAugmentedPrompt(
        sanitizedQuery: String,
        retrieval: RagRetrievalResult
    ): String {
        val snapshot = retrieval.contextSnapshot
        val isAnalysisQuery = RagKnowledgeRepository.isScreeningAnalysisQuery(sanitizedQuery)
        val snapshotHasGbv = snapshot?.integratedCategoryLabel?.contains("KBG", ignoreCase = true) == true ||
            snapshot?.gbvStatusLabel?.contains("Terdapat indikasi", ignoreCase = true) == true
        val isGbv = RagKnowledgeRepository.isGbvRelated(sanitizedQuery) || (isAnalysisQuery && snapshotHasGbv)

        if (snapshot != null && isAnalysisQuery) {
            val catLabel = snapshot.integratedCategoryLabel ?: "Evaluasi kesehatan tekanan darah dan kesejahteraan"
            val htStatus = snapshot.hypertensionStatusLabel ?: snapshot.riskLevelLabel
            val gbvStatus = snapshot.gbvStatusLabel ?: if (snapshotHasGbv) "Terdapat indikasi" else "Tidak terindikasi"
            val factorsText = if (snapshot.keyFactors.isNotEmpty()) {
                snapshot.keyFactors.joinToString(", ")
            } else {
                "Tidak ada faktor pemicu dominan yang menonjol"
            }
            val protectiveText = if (snapshot.protectiveFactors.isNotEmpty()) {
                snapshot.protectiveFactors.joinToString(", ")
            } else {
                "Dapat terus ditingkatkan melalui rutinitas harian"
            }
            val summaryText = snapshot.summaryNarrative ?: "Kondisi risiko tekanan darah berada pada kategori ${snapshot.riskLevelLabel}."
            val relationText = snapshot.relationshipExplanation ?: "Faktor kesehatan fisik, gaya hidup, dan ketenangan pikiran saling memengaruhi secara bertahap."
            val recsText = if (snapshot.keyRecommendations.isNotEmpty()) {
                snapshot.keyRecommendations.joinToString("; ")
            } else {
                "Pantau tekanan darah secara berkala, jaga pola makan rendah garam, dan kelola waktu istirahat"
            }
            val gbvInstruction = if (isGbv) {
                "\n4. Karena terdapat konteks beban relasi/kekerasan, jelaskan secara empatik bagaimana tekanan batin berkepanjangan bekerja melalui jalur stres kronis (chronic stress pathway) terhadap tekanan darah, dan sertakan informasi ruang aman SAPA 129 (Telepon 129 atau WhatsApp 08111-129-129)."
            } else {
                ""
            }

            return buildString {
                append("Pertanyaan pengguna: ").append(sanitizedQuery).append("\n\n")
                append("[KONTEKS HASIL SKRINING PENGGUNA SAAT INI]\n")
                append("- Tanggal skrining: ").append(snapshot.screeningDate).append('\n')
                append("- Tingkat risiko tekanan darah: ").append(snapshot.riskLevelLabel).append('\n')
                append("- Kategori kesimpulan terintegrasi: ").append(catLabel).append('\n')
                append("- Status modul hipertensi: ").append(htStatus).append('\n')
                append("- Status modul kesejahteraan & ruang aman: ").append(gbvStatus).append('\n')
                append("- Gambaran ringkas: ").append(summaryText).append('\n')
                append("- Faktor pemicu yang teridentifikasi dari jawaban pengguna: ").append(factorsText).append('\n')
                append("- Faktor pelindung yang sudah dimiliki: ").append(protectiveText).append('\n')
                append("- Pola keterkaitan klinis & psikososial: ").append(relationText).append('\n')
                append("- Fokus saran awal: ").append(recsText).append("\n\n")
                append("Instruksi Analisis untuk SRYA Emora:\n")
                append("Berdasarkan data hasil skrining pengguna di atas, berikan analisis dan kesimpulan yang mendalam, relevan, dan bernas menggunakan intelegensi medis serta psikososial Anda:\n")
                append("1. Simpulkan arti dari tingkat risiko dan kategori hasil skrining pengguna saat ini secara hangat dan jelas.\n")
                append("2. Analisis bagaimana faktor-faktor yang teridentifikasi pada diri pengguna saling berkaitan memengaruhi tekanan darah maupun ketenangan batinnya secara fisiologis dan psikologis.\n")
                append("3. Berikan langkah tindak lanjut prioritas yang paling relevan bagi kondisi pengguna (tetap ingatkan secara singkat bahwa ini adalah panduan skrining awal, bukan diagnosis medis dokter, serta jangan gunakan simbol bintang ** atau istilah teknis CF/MB/MD).")
                append(gbvInstruction)
            }
        }

        val contextNote = if (snapshot != null) {
            val catInfo = snapshot.integratedCategoryLabel?.let { " ($it)" } ?: ""
            val factorsInfo = if (snapshot.keyFactors.isNotEmpty()) {
                ", dengan faktor perhatian pada ${snapshot.keyFactors.joinToString(", ")}"
            } else {
                ""
            }
            "\n\n(Konteks profil skrining pengguna saat ini: Kategori risiko ${snapshot.riskLevelLabel}$catInfo pada ${snapshot.screeningDate}$factorsInfo. Sesuaikan penjelasan agar relevan dengan kondisi pengguna bila berkaitan)."
        } else {
            ""
        }

        return if (isGbv) {
            "$sanitizedQuery$contextNote\n\n(Catatan: Jawab secara empatik dalam Bahasa Indonesia, jelaskan jalur stres kronis / chronic stress pathway bila membahas tensi, dan sertakan kontak layanan rahasia SAPA 129: Telepon 129 atau WhatsApp 08111-129-129)."
        } else {
            "$sanitizedQuery$contextNote"
        }
    }

    /**
     * Post-Generation Guardrail (Tanpa menambahkan "Sumber rujukan:" atau korpus!):
     * - Menghapus kebocoran CF, MB, MD, %, atau desimal bertitik.
     * - Memastikan kontak SAPA 129 hadir jika topik adalah kekerasan/KBG.
     */
    fun enforceOutputGuardrails(
        rawOutput: String,
        retrieval: RagRetrievalResult,
        query: String = retrieval.query
    ): String {
        var cleaned = rawOutput.trim()
            .replace(markdownBoldRegex, "$1")
            .replace("**", "")
            .replace(internalAcronymRegex, "indikator")
            .replace("%", " persen")
            .replace(decimalLeakRegex) { match ->
                val left = match.groupValues[1]
                val right = match.groupValues[2]
                if (right.length == 3 && right == "000") "$left$right" else "$left,$right"
            }

        cleaned = ensureCompleteResponse(cleaned)

        val isAnalysis = RagKnowledgeRepository.isScreeningAnalysisQuery(query)
        val snapshotHasGbv = isAnalysis && (
            retrieval.contextSnapshot?.integratedCategoryLabel?.contains("KBG", ignoreCase = true) == true ||
                retrieval.contextSnapshot?.gbvStatusLabel?.contains("Terdapat indikasi", ignoreCase = true) == true
            )
        val isGbv = RagKnowledgeRepository.isGbvRelated(query) || snapshotHasGbv
        val mentionsStressAndBp = (query.lowercase().contains("kbg") || query.lowercase().contains("kekerasan") || query.lowercase().contains("stres")) &&
            (query.lowercase().contains("hipertensi") || query.lowercase().contains("tensi") || query.lowercase().contains("darah"))

        if ((isGbv || mentionsStressAndBp) &&
            !cleaned.lowercase().contains("kronis") &&
            !cleaned.lowercase().contains("sebab-akibat")
        ) {
            cleaned = "$cleaned\n\n$CHRONIC_STRESS_PATHWAY_NOTE"
        }

        if (isGbv && (!cleaned.contains("129") || !cleaned.contains("08111-129-129"))) {
            cleaned = "$cleaned\n\n$SAPA_129_MANDATORY_NOTE"
        }

        return cleaned.trim()
    }

    /**
     * Memastikan teks hasil generasi tidak terpotong di tengah kata/kalimat atau menyisakan
     * nomor poin yang menggantung (seperti "4. Mengelola" atau "...dan hindari penggunaan ponsel").
     */
    internal fun ensureCompleteResponse(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return trimmed
        val terminalChars = charArrayOf('.', '!', '?', ')', '"', '\'')
        if (trimmed.last() in terminalChars) return trimmed

        // Jika baris terakhir adalah poin baru yang terpotong sebelum sempat membentuk kalimat utuh
        val lastNewline = trimmed.lastIndexOf('\n')
        if (lastNewline > 0) {
            val beforeLastLine = trimmed.substring(0, lastNewline).trim()
            val lastLine = trimmed.substring(lastNewline + 1).trim()
            val lastSentenceInLastLine = maxOf(
                lastLine.lastIndexOf('.'),
                lastLine.lastIndexOf('!'),
                lastLine.lastIndexOf('?')
            )
            // Misal: "4. Mengelola" (hanya nomor poin <= 3 indeks titik dari awal baris)
            if (lastSentenceInLastLine <= 3 && beforeLastLine.isNotEmpty() && beforeLastLine.last() in terminalChars) {
                return beforeLastLine
            }
        }

        // Jika terpotong di tengah kalimat setelah kalimat utuh sebelumnya, potong di titik kalimat utuh terakhir
        val lastDot = maxOf(
            trimmed.lastIndexOf(". "),
            trimmed.lastIndexOf(".\n"),
            trimmed.lastIndexOf("! "),
            trimmed.lastIndexOf("!\n"),
            trimmed.lastIndexOf("? "),
            trimmed.lastIndexOf("?\n")
        )
        if (lastDot >= (trimmed.length * 0.55).toInt()) {
            return trimmed.substring(0, lastDot + 1).trim()
        }

        return "$trimmed."
    }

    /**
     * Menghasilkan jawaban murni dari LLM (tanpa korpus dan tanpa sitasi dokumen):
     * 1. Supabase Edge Function `empati-chat` sebagai jalur cloud.
     * 2. Local/LAN Ollama (`qwen2.5:3b-instruct` / `empati-qwen2.5:3b`) untuk pengembangan lokal.
     * 3. Hanya jika perangkat benar-benar tanpa Ollama dan tanpa internet (atau saat unit test offline),
     *    menggunakan synthesizer klinis.
     */
    fun generateRagResponse(
        rawQuery: String,
        retrieval: RagRetrievalResult = RagRetrievalResult(rawQuery, emptyList(), null),
        enableNetworkLlm: Boolean = true,
        preferredModel: String = DEFAULT_MODEL,
        timeoutMs: Int = 60_000
    ): OllamaGenerationOutcome {
        val sanitizedQuery = sanitizePrivacyInput(rawQuery)

        // 1. Pre-Generation Topic Scoping (Kesehatan, Psikologi, Kekerasan)
        if (!RagKnowledgeRepository.isTopicAllowed(sanitizedQuery)) {
            return OllamaGenerationOutcome(
                text = OUT_OF_SCOPE_REFUSAL,
                usedOllamaLlm = false,
                modelName = "guardrail-topic-filter",
                retrievedChunks = emptyList()
            )
        }

        val cleanRetrieval = RagRetrievalResult(
            query = sanitizedQuery,
            matchedChunks = emptyList(),
            contextSnapshot = retrieval.contextSnapshot
        )

        if (enableNetworkLlm) {
            val userPrompt = buildAugmentedPrompt(sanitizedQuery, cleanRetrieval)

            // 2a. Panggil Supabase Edge Function (`empati-chat`) sebagai jalur cloud
            val edgeOutcome = SupabaseCloudSyncService.callEdgeChatFunction(
                systemPrompt = SYSTEM_PROMPT,
                userPrompt = userPrompt,
                timeoutMs = 25_000
            )
            if (edgeOutcome != null) {
                val (edgeText, edgeModel) = edgeOutcome
                val guarded = enforceOutputGuardrails(edgeText, cleanRetrieval, sanitizedQuery)
                return OllamaGenerationOutcome(
                    text = guarded,
                    usedOllamaLlm = true,
                    modelName = edgeModel,
                    retrievedChunks = emptyList()
                )
            }

            // 2b. Panggil Ollama Lokal (`qwen2.5:3b-instruct` / `empati-qwen2.5:3b`) untuk development lokal
            for (baseUrl in CANDIDATE_ENDPOINTS) {
                val llmText = callOllamaChatApi(
                    baseUrl = baseUrl,
                    model = preferredModel,
                    systemPrompt = SYSTEM_PROMPT,
                    userPrompt = userPrompt,
                    timeoutMs = timeoutMs
                )
                if (!llmText.isNullOrBlank()) {
                    val guarded = enforceOutputGuardrails(llmText, cleanRetrieval, sanitizedQuery)
                    return OllamaGenerationOutcome(
                        text = guarded,
                        usedOllamaLlm = true,
                        modelName = preferredModel,
                        retrievedChunks = emptyList()
                    )
                }
            }
        }

        // 3. Offline Fallback (Hanya bila perangkat 100% tanpa Ollama & tanpa internet, atau saat unit test JVM)
        val offlineRaw = synthesizeOfflineClinicalAnswer(sanitizedQuery, cleanRetrieval.contextSnapshot)
        val guardedOffline = enforceOutputGuardrails(offlineRaw, cleanRetrieval, sanitizedQuery)
        return OllamaGenerationOutcome(
            text = guardedOffline,
            usedOllamaLlm = false,
            modelName = "offline-clinical-assistant",
            retrievedChunks = emptyList()
        )
    }

    internal fun callOllamaChatApi(
        baseUrl: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        timeoutMs: Int
    ): String? {
        return runCatching {
            val url = URL("$baseUrl/api/chat")
            val payload = buildChatJsonPayload(
                model = model,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 2_000
                readTimeout = timeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(payload)
                writer.flush()
            }
            if (conn.responseCode != 200) return null
            val responseBody = BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8))
                .use { it.readText() }
            extractAssistantContentFromJson(responseBody)
        }.getOrNull()
    }

    internal fun buildChatJsonPayload(
        model: String,
        systemPrompt: String,
        userPrompt: String
    ): String {
        return """{"model":"${escapeJson(model)}","stream":false,"keep_alive":"30m","options":{"temperature":0.3,"top_p":0.85,"num_ctx":2048,"num_predict":512},"messages":[{"role":"system","content":"${escapeJson(systemPrompt)}"},{"role":"user","content":"${escapeJson(userPrompt)}"}]}"""
    }

    internal fun extractAssistantContentFromJson(jsonResponse: String): String? {
        val messageIdx = jsonResponse.indexOf("\"message\"")
        val searchSpace = if (messageIdx >= 0) jsonResponse.substring(messageIdx) else jsonResponse
        val contentKey = "\"content\":"
        val keyIdx = searchSpace.indexOf(contentKey)
        if (keyIdx < 0) return null

        var i = keyIdx + contentKey.length
        while (i < searchSpace.length && searchSpace[i].isWhitespace()) i++
        if (i >= searchSpace.length || searchSpace[i] != '"') return null
        i++

        val sb = StringBuilder()
        var escaped = false
        while (i < searchSpace.length) {
            val c = searchSpace[i]
            if (escaped) {
                when (c) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'u' -> {
                        if (i + 4 < searchSpace.length) {
                            val hex = searchSpace.substring(i + 1, i + 5)
                            hex.toIntOrNull(16)?.let { sb.append(it.toChar()) }
                            i += 4
                        }
                    }
                    else -> sb.append(c)
                }
                escaped = false
            } else {
                if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    break
                } else {
                    sb.append(c)
                }
            }
            i++
        }
        return sb.toString().trim().takeIf { it.isNotEmpty() }
    }

    private fun synthesizeOfflineClinicalAnswer(
        sanitizedQuery: String,
        contextSnapshot: ScreeningContextSnapshot? = null
    ): String {
        val q = sanitizedQuery.lowercase(Locale.forLanguageTag("id-ID"))
        if (RagKnowledgeRepository.isScreeningAnalysisQuery(q) && contextSnapshot != null) {
            return synthesizeOfflineScreeningAnalysis(contextSnapshot)
        }
        return when {
            RagKnowledgeRepository.isIdentityQuery(q) ->
                SRYA_EMORA_SELF_INTRO

            RagKnowledgeRepository.isGbvRelated(q) ->
                "Setiap bentuk kekerasan—baik fisik, psikologis, seksual, ekonomi, maupun digital—sama sekali bukan kesalahan korban, dan Anda berhak merasa aman. Tekanan batin dari pengalaman kekerasan dapat memicu jalur stres kronis yang perlahan ikut memengaruhi kondisi tubuh dan tekanan darah Anda, meskipun bukan hubungan sebab-akibat seketika. Bila membutuhkan teman bicara atau perlindungan rahasia, Anda dapat menghubungi layanan SAPA 129 di telepon 129 atau WhatsApp 08111-129-129."

            q.contains("umur") || q.contains("usia") || q.contains("lansia") || q.contains("muda") || q.contains("tua") ->
                "Risiko tekanan darah tinggi memang mulai meningkat cukup terasa saat memasuki usia 40 hingga 45 tahun ke atas, dan paling sering dialami pada usia di atas 55 hingga 65 tahun karena pembuluh darah alami menjadi kurang lentur seiring bertambahnya usia. Namun, usia muda (20–35 tahun) juga tetap bisa mengalaminya bila ada faktor riwayat keluarga, kebiasaan makan tinggi garam, kurang gerak, atau beban stres kronis."

            q.contains("cegah") || q.contains("pencegahan") || q.contains("mencegah") ->
                "Untuk membantu mencegah hipertensi dalam keseharian, Anda bisa mulai dari langkah sederhana: batasi konsumsi garam maksimal 1 sendok teh sehari (pola makan sehat ala DASH dengan perbanyak sayur dan buah), sempatkan olahraga ringan seperti jalan cepat minimal 30 menit sehari, jaga waktu tidur malam 7–8 jam, hindari rokok, dan beri waktu istirahat saat pikiran sedang penat."

            q.contains("gejala") || q.contains("tanda") || q.contains("ciri") ->
                "Sering kali tekanan darah tinggi hadir tanpa keluhan yang jelas, sehingga banyak orang baru menyadarinya saat mengecek tensi. Namun bila angkanya sedang cukup tinggi, tubuh biasanya memberi sinyal seperti rasa berat atau nyeri di tengkuk belakang kepala, pusing, jantung berdebar lebih kencang, atau mudah lelah. Cara paling pasti untuk mengetahuinya adalah dengan rutin memeriksa tekanan darah menggunakan tensimeter."

            q.contains("stres") || q.contains("cemas") || q.contains("psikolog") || q.contains("mental") || q.contains("tidur") || q.contains("begadang") ->
                "Saat pikiran terus-menerus tegang, cemas, atau waktu tidur kurang dari 7 jam per malam, tubuh mengaktifkan jalur stres kronis yang memicu naiknya hormon stres dan membuat detak jantung serta tekanan darah ikut meningkat. Cobalah memberi jeda istirahat sejenak, menarik napas dalam secara perlahan, serta menjaga jam tidur yang lebih teratur."

            else ->
                "Menjaga tekanan darah dan ketenangan pikiran dapat dimulai dari kebiasaan sehari-hari: kurangi makanan yang terlalu asin (garam kurang dari 1 sendok teh sehari), rutin bergerak aktif atau olahraga ringan 30 menit sehari, cukupkan tidur malam 7–8 jam, kelola beban stres dengan baik, serta periksa tensi secara berkala."
        }
    }

    private fun synthesizeOfflineScreeningAnalysis(snapshot: ScreeningContextSnapshot): String {
        val catLabel = snapshot.integratedCategoryLabel ?: "Evaluasi kesehatan tekanan darah dan kesejahteraan"
        val summary = snapshot.summaryNarrative
            ?: "Berdasarkan skrining pada ${snapshot.screeningDate}, risiko tekanan darah Anda berada pada tingkat ${snapshot.riskLevelLabel}."
        val factorsSection = if (snapshot.keyFactors.isNotEmpty()) {
            "Dari analisis jawaban Anda, faktor utama yang paling berperan memengaruhi kondisi ini meliputi ${snapshot.keyFactors.joinToString(", ").lowercase()}. Secara fisiologis dan psikologis, kombinasi kebiasaan harian seperti asupan garam, kurangnya waktu istirahat, maupun beban pikiran yang menumpuk dapat meningkatkan ketegangan saraf simpatis dan membuat pembuluh darah bekerja lebih keras."
        } else {
            "Dari analisis jawaban Anda, belum tampak faktor pemicu dominan yang membebani pembuluh darah maupun ketenangan batin Anda secara berlebih."
        }
        val relationshipSection = snapshot.relationshipExplanation
            ?: "Kondisi kesehatan fisik dan ketenangan batin saling berkaitan erat dalam menjaga kestabilan tekanan darah sehari-hari."
        val protectiveSection = if (snapshot.protectiveFactors.isNotEmpty()) {
            " Hal positif yang patut Anda pertahankan adalah ${snapshot.protectiveFactors.joinToString(", ").lowercase()}."
        } else {
            ""
        }
        val recsSection = if (snapshot.keyRecommendations.isNotEmpty()) {
            "Sebagai langkah prioritas ke depan: ${snapshot.keyRecommendations.joinToString(" ")}"
        } else {
            "Sebagai langkah prioritas ke depan, pantau tekanan darah secara berkala, batasi konsumsi garam maksimal 1 sendok teh sehari, sempatkan gerak aktif 30 menit sehari, dan jaga waktu tidur malam 7–8 jam."
        }

        return "Berdasarkan analisis hasil skrining Anda pada ${snapshot.screeningDate}, dapat disimpulkan bahwa kondisi Anda saat ini tergolong ${snapshot.riskLevelLabel} dengan gambaran utama: $catLabel. $summary\n\n$factorsSection $relationshipSection$protectiveSection\n\n$recsSection Ingat bahwa kesimpulan ini adalah panduan skrining awal untuk mendukung langkah sehat Anda, bukan diagnosis medis dokter."
    }

    private fun escapeJson(raw: String): String {
        val sb = StringBuilder(raw.length + 32)
        for (c in raw) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
