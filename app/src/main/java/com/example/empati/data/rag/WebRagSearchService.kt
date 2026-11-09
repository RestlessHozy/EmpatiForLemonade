package com.example.empati.data.rag

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

data class WebRagSearchOutcome(
    val chunks: List<RagDocumentChunk>,
    val fetchedFromInternet: Boolean,
    val liveChunks: List<RagDocumentChunk>
)

/**
 * Layanan Pencarian Rujukan Medis Terfilter (Grounded Medical Reference Service).
 *
 * Berbeda dengan pendekatan RAG naif yang menempelkan paragraf Wikipedia/PubMed mentah ke layar,
 * layanan ini:
 * 1. Memilih pedoman klinis resmi yang paling relevan dengan topik pertanyaan pengguna.
 * 2. Menyaring ketat artikel Wikipedia Medis Bahasa Indonesia (menolak topik sempit yang
 *    menyesatkan seperti "Hipertensi paru" atau "Hipertensi portal" saat pengguna bertanya
 *    tentang hipertensi umum/pencegahan/usia).
 * 3. Tidak pernah menempelkan abstrak berbahasa Inggris mentah ke jawaban pengguna.
 */
object WebRagSearchService {

    private const val WIKIPEDIA_ID_API = "https://id.wikipedia.org/w/api.php"
    private const val EUROPE_PMC_API = "https://www.ebi.ac.uk/europepmc/webservices/rest/search"
    private const val USER_AGENT = "AI-EMPATI-Clinical-Assistant/3.1 (Android; Medical Education)"

    private val htmlTagRegex = Regex("<[^>]+>")
    private val multiSpaceRegex = Regex("\\s+")
    private val decimalNumberRegex = Regex("\\b(\\d+)\\.(\\d+)\\b")

    private val excludedIrrelevantWikiTitles = listOf(
        "hipertensi paru",
        "hipertensi pulmonal",
        "hipertensi portal",
        "hipertensi intrakranial",
        "hipertensi okular"
    )

    private val indonesianToPubMedTerms: Map<String, String> = mapOf(
        "hipertensi" to "hypertension blood pressure",
        "darah tinggi" to "hypertension blood pressure",
        "tensi" to "blood pressure monitoring",
        "umur" to "age risk factor hypertension prevalence",
        "usia" to "age risk factor hypertension adults",
        "garam" to "dietary sodium reduction hypertension",
        "asin" to "sodium intake blood pressure",
        "natrium" to "sodium DASH diet hypertension",
        "dash" to "DASH diet blood pressure",
        "diet" to "dietary modification hypertension",
        "makan" to "nutrition blood pressure",
        "kopi" to "caffeine intake blood pressure",
        "kafein" to "caffeine hypertension",
        "rokok" to "smoking cessation hypertension cardiovascular",
        "merokok" to "smoking blood pressure",
        "olahraga" to "aerobic exercise blood pressure reduction",
        "aktivitas" to "physical activity hypertension",
        "jalan" to "brisk walking blood pressure",
        "obesitas" to "obesity weight loss hypertension",
        "berat badan" to "body weight blood pressure",
        "stres" to "chronic psychosocial stress hypertension cortisol",
        "cemas" to "anxiety autonomic nervous system blood pressure",
        "tidur" to "sleep duration insomnia hypertension",
        "istirahat" to "sleep quality blood pressure",
        "hamil" to "gestational hypertension preeclampsia",
        "diabetes" to "diabetes mellitus hypertension comorbidity",
        "obat" to "antihypertensive medication adherence",
        "gejala" to "hypertension symptoms early detection",
        "cegah" to "hypertension primary prevention lifestyle",
        "pencegahan" to "hypertension prevention guidelines",
        "kekerasan" to "intimate partner violence gender-based violence chronic stress hypertension",
        "kbg" to "gender-based violence psychosocial stress cardiovascular",
        "kdrt" to "domestic violence women health stress"
    )

    /**
     * Mengambil rujukan pedoman yang tepat sasaran untuk mendampingi jawaban AI:
     * - Mengutamakan 1 pedoman resmi yang paling sesuai (dan `DOC-GBV-01` HANYA jika pertanyaan terkait KBG).
     * - Memperkaya dengan ringkasan ensiklopedia medis Bahasa Indonesia yang telah disaring dari topik
     *   sempit yang tidak relevan.
     */
    fun retrieveWithLiveInternet(
        rawQuery: String,
        contextSnapshot: ScreeningContextSnapshot? = null,
        localCorpus: List<RagDocumentChunk> = RagKnowledgeRepository.defaultCorpus,
        enableLiveWebSearch: Boolean = true,
        topK: Int = 2,
        timeoutMs: Int = 3_500
    ): Pair<RagRetrievalResult, WebRagSearchOutcome> {
        val sanitizedQuery = OllamaRagService.sanitizePrivacyInput(rawQuery)

        // Ambil 1 pedoman resmi paling relevan (sudah memfilter DOC-GBV-01 jika bukan pertanyaan KBG)
        val localRetrieval = RagKnowledgeRepository.retrieve(
            query = sanitizedQuery,
            contextSnapshot = contextSnapshot,
            topK = 1,
            corpus = localCorpus.filter { !it.chunkId.startsWith("WEB-PMC-") }
        )

        if (!RagKnowledgeRepository.isTopicAllowed(sanitizedQuery, localRetrieval.matchedChunks)) {
            val emptyOutcome = WebRagSearchOutcome(
                chunks = emptyList(),
                fetchedFromInternet = false,
                liveChunks = emptyList()
            )
            return RagRetrievalResult(sanitizedQuery, emptyList(), contextSnapshot) to emptyOutcome
        }

        val isGbv = RagKnowledgeRepository.isGbvRelated(sanitizedQuery)
        val liveWikiChunks = if (enableLiveWebSearch && !isGbv) {
            fetchIndonesianWikipediaChunks(
                sanitizedQuery = sanitizedQuery,
                limit = 1,
                timeoutMs = timeoutMs
            )
        } else {
            emptyList()
        }

        val gbvAnchor = if (isGbv) {
            localCorpus.firstOrNull { it.chunkId == "DOC-GBV-01" }
                ?: RagKnowledgeRepository.defaultCorpus.firstOrNull { it.chunkId == "DOC-GBV-01" }
        } else {
            null
        }

        val combinedChunks = buildList {
            if (gbvAnchor != null) {
                add(gbvAnchor)
            } else {
                addAll(localRetrieval.matchedChunks.take(1))
                for (wiki in liveWikiChunks) {
                    if (none { it.chunkId == wiki.chunkId }) {
                        add(wiki)
                    }
                }
            }
        }.take(topK)

        val finalRetrieval = RagRetrievalResult(
            query = sanitizedQuery,
            matchedChunks = combinedChunks,
            contextSnapshot = contextSnapshot
        )
        val webOutcome = WebRagSearchOutcome(
            chunks = combinedChunks,
            fetchedFromInternet = liveWikiChunks.isNotEmpty(),
            liveChunks = liveWikiChunks
        )
        return finalRetrieval to webOutcome
    }

    fun searchInternetCorpus(
        sanitizedQuery: String,
        maxResults: Int = 1,
        timeoutMs: Int = 3_500
    ): List<RagDocumentChunk> {
        if (!RagKnowledgeRepository.isTopicAllowed(sanitizedQuery)) return emptyList()
        return fetchIndonesianWikipediaChunks(
            sanitizedQuery = sanitizedQuery,
            limit = maxResults,
            timeoutMs = timeoutMs
        ).take(maxResults)
    }

    internal fun buildIndonesianMedicalSearchQuery(sanitizedQuery: String): String {
        val lower = sanitizedQuery.lowercase(Locale.forLanguageTag("id-ID"))
        return when {
            lower.contains("dash") || lower.contains("garam") || lower.contains("natrium") -> "Diet hipertensi garam natrium"
            lower.contains("preeklamsia") || lower.contains("hamil") -> "Preeklamsia hipertensi kehamilan"
            lower.contains("stroke") -> "Stroke hipertensi"
            lower.contains("diabetes") -> "Diabetes melitus hipertensi"
            lower.contains("kolesterol") -> "Kolesterol penyakit kardiovaskular"
            lower.contains("olahraga") || lower.contains("aktivitas fisik") -> "Latihan aerobik tekanan darah"
            lower.contains("stres") || lower.contains("tidur") -> "Stres psikologis tekanan darah"
            else -> "Tekanan darah tinggi"
        }
    }

    internal fun buildPubMedEnglishQuery(sanitizedQuery: String): String {
        val lower = sanitizedQuery.lowercase(Locale.forLanguageTag("id-ID"))
        val matchedEnglish = indonesianToPubMedTerms.entries
            .filter { (idTerm, _) -> lower.contains(idTerm) }
            .map { it.value }
            .distinct()
            .take(2)

        val baseQuery = if (matchedEnglish.isNotEmpty()) {
            matchedEnglish.joinToString(" AND ")
        } else {
            "hypertension cardiovascular prevention"
        }
        return "($baseQuery) AND HAS_ABSTRACT:y"
    }

    private fun fetchIndonesianWikipediaChunks(
        sanitizedQuery: String,
        limit: Int,
        timeoutMs: Int
    ): List<RagDocumentChunk> {
        return runCatching {
            val searchTerms = buildIndonesianMedicalSearchQuery(sanitizedQuery)
            val encoded = URLEncoder.encode(searchTerms, StandardCharsets.UTF_8.name())
            val urlStr =
                "$WIKIPEDIA_ID_API?action=query&generator=search&gsrsearch=$encoded&gsrlimit=3" +
                    "&prop=extracts&exintro=1&explaintext=1&exchars=460&utf8=1&format=json"

            val json = httpGet(urlStr, timeoutMs) ?: return emptyList()
            parseWikipediaJsonToChunks(json, sanitizedQuery).take(limit)
        }.getOrDefault(emptyList())
    }

    internal fun parseWikipediaJsonToChunks(
        json: String,
        originalQuery: String
    ): List<RagDocumentChunk> {
        val pagesIdx = json.indexOf("\"pages\"")
        if (pagesIdx < 0) return emptyList()
        val pagesSection = json.substring(pagesIdx)

        val objectBlocks = extractJsonObjectsContaining(pagesSection, "\"extract\"")
        val queryLower = originalQuery.lowercase(Locale.forLanguageTag("id-ID"))
        val scoredChunks = mutableListOf<Pair<Int, RagDocumentChunk>>()

        for (block in objectBlocks) {
            val pageId = extractJsonNumberOrString(block, "pageid") ?: continue
            val rawTitle = extractJsonStringValue(block, "title") ?: continue
            val titleLower = rawTitle.lowercase(Locale.forLanguageTag("id-ID"))

            // Jangan pernah mengambil "Hipertensi paru" / "Hipertensi portal" kecuali pengguna memang menanyakan istilah tersebut
            val isExcludedSpecialty = excludedIrrelevantWikiTitles.any { excluded ->
                titleLower.contains(excluded) && !queryLower.contains(excluded)
            }
            if (isExcludedSpecialty) continue

            val rawExtract = extractJsonStringValue(block, "extract") ?: continue
            val cleanExtract = sanitizeWebText(rawExtract)
            if (cleanExtract.length < 40) continue

            val searchIndex = extractJsonNumberOrString(block, "index")?.toIntOrNull() ?: 99
            val encodedTitle = rawTitle.replace(' ', '_')
            val sourceUrl = "https://id.wikipedia.org/wiki/$encodedTitle"
            val scope = inferTopicScope("$originalQuery $rawTitle $cleanExtract")
            val keywords = (extractSignificantTokens(originalQuery) + extractSignificantTokens(rawTitle))
                .distinct()
                .take(8)

            scoredChunks.add(
                searchIndex to RagDocumentChunk(
                    chunkId = "WEB-WIKI-$pageId",
                    docSource = "Ensiklopedia Medis Wikipedia Indonesia ($sourceUrl)",
                    topicScope = scope,
                    title = "$rawTitle (Rujukan Web)",
                    content = cleanExtract,
                    keywords = keywords.ifEmpty { listOf("hipertensi", "kesehatan") },
                    chunkVersion = "web-live-v3.1"
                )
            )
        }
        return scoredChunks.sortedBy { it.first }.map { it.second }
    }

    internal fun parseEuropePmcJsonToChunks(
        json: String,
        originalQuery: String
    ): List<RagDocumentChunk> {
        val resultIdx = json.indexOf("\"resultList\"")
        val searchSpace = if (resultIdx >= 0) json.substring(resultIdx) else json
        val objectBlocks = extractJsonObjectsContaining(searchSpace, "\"abstractText\"")
        val chunks = mutableListOf<RagDocumentChunk>()

        for (block in objectBlocks) {
            val pmid = extractJsonStringValue(block, "pmid")
                ?: extractJsonStringValue(block, "id")
                ?: continue
            val rawTitle = extractJsonStringValue(block, "title")?.trimEnd('.') ?: continue
            val journal = extractJsonStringValue(block, "journalTitle") ?: "Europe PMC"
            val pubYear = extractJsonStringValue(block, "pubYear") ?: ""
            val rawAbstract = extractJsonStringValue(block, "abstractText") ?: continue
            val cleanAbstract = sanitizeWebText(rawAbstract).take(460)
            if (cleanAbstract.length < 40) continue

            val yearSuffix = if (pubYear.isNotBlank()) " ($pubYear)" else ""
            val articleUrl = "https://europepmc.org/article/MED/$pmid"
            val scope = inferTopicScope("$originalQuery $rawTitle $cleanAbstract")
            val keywords = extractSignificantTokens(originalQuery).distinct().take(8)

            chunks.add(
                RagDocumentChunk(
                    chunkId = "WEB-PMC-$pmid",
                    docSource = "Jurnal Medis PubMed / $journal$yearSuffix ($articleUrl)",
                    topicScope = scope,
                    title = rawTitle,
                    content = "Temuan literatur medis ($journal$yearSuffix): $cleanAbstract",
                    keywords = keywords.ifEmpty { listOf("hipertensi", "jurnal", "medis") },
                    chunkVersion = "web-live-v3.1"
                )
            )
        }
        return chunks
    }

    internal fun sanitizeWebText(raw: String): String {
        return raw
            .replace(htmlTagRegex, " ")
            .replace("%", " persen")
            .replace(decimalNumberRegex, "$1,$2")
            .replace(Regex("\\b(CF|MB|MD)\\b"), "indikator")
            .replace(multiSpaceRegex, " ")
            .trim()
    }

    internal fun inferTopicScope(text: String): RagTopicScope {
        val lower = text.lowercase(Locale.forLanguageTag("id-ID"))
        return when {
            lower.contains("kekerasan") || lower.contains("kbg") || lower.contains("kdrt") ||
                lower.contains("violence") || lower.contains("sapa") -> RagTopicScope.GBV_PROTECTION
            lower.contains("stres") || lower.contains("tidur") || lower.contains("cemas") ||
                lower.contains("stress") || lower.contains("sleep") || lower.contains("kortisol") -> RagTopicScope.ASN_PSYCHOSOCIAL
            lower.contains("garam") || lower.contains("natrium") || lower.contains("diet") ||
                lower.contains("dash") || lower.contains("olahraga") || lower.contains("sodium") ||
                lower.contains("exercise") -> RagTopicScope.LIFESTYLE_DASH
            else -> RagTopicScope.HYPERTENSION_CLINICAL
        }
    }

    private fun extractSignificantTokens(text: String): List<String> {
        val stop = setOf(
            "apa", "apakah", "bagaimana", "berapa", "kapan", "dimana", "mengapa", "kenapa",
            "yang", "dan", "atau", "untuk", "dari", "pada", "dalam", "dengan", "tanpa",
            "saya", "anda", "kamu", "kita", "ini", "itu", "bisa", "dapat", "akan", "sudah",
            "tidak", "bukan", "jangan", "lebih", "kurang", "sangat", "cara", "agar", "tentang"
        )
        return text.lowercase(Locale.forLanguageTag("id-ID"))
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 && it !in stop }
    }

    private fun httpGet(urlStr: String, timeoutMs: Int): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return if (conn.responseCode == 200) {
            BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)).use {
                it.readText()
            }
        } else {
            null
        }
    }

    private fun extractJsonObjectsContaining(json: String, requiredKey: String): List<String> {
        val results = mutableListOf<String>()
        var searchFrom = 0
        while (searchFrom < json.length) {
            val keyPos = json.indexOf(requiredKey, searchFrom)
            if (keyPos < 0) break

            var openBrace = keyPos
            var depth = 0
            while (openBrace > 0) {
                val c = json[openBrace]
                if (c == '}') depth++
                else if (c == '{') {
                    if (depth == 0) break
                    depth--
                }
                openBrace--
            }

            var closeBrace = openBrace
            var braceBalance = 0
            var inQuotes = false
            var escaped = false
            while (closeBrace < json.length) {
                val c = json[closeBrace]
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inQuotes = !inQuotes
                } else if (!inQuotes) {
                    if (c == '{') braceBalance++
                    else if (c == '}') {
                        braceBalance--
                        if (braceBalance == 0) {
                            results.add(json.substring(openBrace, closeBrace + 1))
                            break
                        }
                    }
                }
                closeBrace++
            }
            searchFrom = if (closeBrace > keyPos) closeBrace + 1 else keyPos + requiredKey.length
        }
        return results
    }

    internal fun extractJsonStringValue(jsonObject: String, key: String): String? {
        val token = "\"$key\""
        val keyIdx = jsonObject.indexOf(token)
        if (keyIdx < 0) return null
        val colonIdx = jsonObject.indexOf(':', keyIdx + token.length)
        if (colonIdx < 0) return null

        var i = colonIdx + 1
        while (i < jsonObject.length && jsonObject[i].isWhitespace()) i++
        if (i >= jsonObject.length || jsonObject[i] != '"') return null
        i++

        val sb = StringBuilder()
        var escaped = false
        while (i < jsonObject.length) {
            val c = jsonObject[i]
            if (escaped) {
                when (c) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'u' -> {
                        if (i + 4 < jsonObject.length) {
                            val hex = jsonObject.substring(i + 1, i + 5)
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

    internal fun extractJsonNumberOrString(jsonObject: String, key: String): String? {
        extractJsonStringValue(jsonObject, key)?.let { return it }
        val token = "\"$key\""
        val keyIdx = jsonObject.indexOf(token)
        if (keyIdx < 0) return null
        val colonIdx = jsonObject.indexOf(':', keyIdx + token.length)
        if (colonIdx < 0) return null

        var i = colonIdx + 1
        while (i < jsonObject.length && jsonObject[i].isWhitespace()) i++
        val start = i
        while (i < jsonObject.length && (jsonObject[i].isDigit() || jsonObject[i] == '-')) i++
        return jsonObject.substring(start, i).takeIf { it.isNotEmpty() }
    }
}
