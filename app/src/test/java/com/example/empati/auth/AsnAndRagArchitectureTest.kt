package com.example.empati.auth

import com.example.empati.data.ChatEngine
import com.example.empati.data.RiskLevel
import com.example.empati.data.ScreeningResult
import com.example.empati.data.UserProfile
import com.example.empati.data.auth.AsnNipValidator
import com.example.empati.data.auth.AsnSecurityHasher
import com.example.empati.data.auth.NipValidationResult
import com.example.empati.data.cloud.SupabaseCloudSyncService
import com.example.empati.data.db.PersistedUserAccount
import com.example.empati.data.rag.OllamaRagService
import com.example.empati.data.rag.RagKnowledgeRepository
import com.example.empati.data.rag.RagTopicScope
import com.example.empati.data.rag.ScreeningContextSnapshot
import com.example.empati.data.rag.WebRagSearchService
import com.example.empati.data.research.ResearchModule
import com.example.empati.data.research.ResearchRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AsnAndRagArchitectureTest {

    @Test
    fun `valid female ASN NIP parses birth date, appointment TMT, and gender code 2`() {
        val result = AsnNipValidator.validate("19850312 201001 2 014", requireFemaleAsn = true)
        assertTrue(result is NipValidationResult.Valid)
        val meta = (result as NipValidationResult.Valid).metadata
        assertEquals("198503122010012014", meta.cleanNip)
        assertEquals("19850312 201001 2 014", meta.formattedNip)
        assertEquals(1985, meta.birthYear)
        assertEquals(3, meta.birthMonth)
        assertEquals(12, meta.birthDay)
        assertEquals(2010, meta.appointmentYear)
        assertEquals(1, meta.appointmentMonth)
        assertEquals(2, meta.genderCode)
        assertTrue(meta.isFemaleAsn)
    }

    @Test
    fun `general identity validator accepts email, phone, username, NIK, and both male and female NIP`() {
        val emailResult = AsnNipValidator.validateGeneralIdentity("Budi.Santoso@email.id")
        assertTrue(emailResult is NipValidationResult.Valid)
        assertEquals("budi.santoso@email.id", (emailResult as NipValidationResult.Valid).metadata.cleanNip)

        val phoneResult = AsnNipValidator.validateGeneralIdentity("0812-3456-7890")
        assertTrue(phoneResult is NipValidationResult.Valid)
        assertEquals("081234567890", (phoneResult as NipValidationResult.Valid).metadata.cleanNip)

        val usernameResult = AsnNipValidator.validateGeneralIdentity("pengguna_umum_01")
        assertTrue(usernameResult is NipValidationResult.Valid)

        val maleNipResult = AsnNipValidator.validateGeneralIdentity("19850312 201001 1 014")
        assertTrue("Male NIP must be accepted in universal mode", maleNipResult is NipValidationResult.Valid)

        val invalidShort = AsnNipValidator.validateGeneralIdentity("ab")
        assertTrue(invalidShort is NipValidationResult.Invalid)
    }

    @Test
    fun `pseudonym subject_hash is deterministic and hides raw identity for email and NIP`() {
        val rawNip = "198503122010012014"
        val hash1 = AsnSecurityHasher.deriveSubjectHash(rawNip)
        val hash2 = AsnSecurityHasher.deriveSubjectHash("19850312 201001 2 014")
        assertEquals(hash1, hash2)
        assertTrue(hash1.startsWith("sub_"))
        assertFalse(hash1.contains(rawNip))

        val emailHash1 = AsnSecurityHasher.deriveSubjectHash("Sari.Wulandari@Email.id")
        val emailHash2 = AsnSecurityHasher.deriveSubjectHash("sari.wulandari@email.id")
        assertEquals(emailHash1, emailHash2)
        assertFalse(emailHash1.contains("sari"))
    }

    @Test
    fun `PBKDF2 credential hash uses random salt and verifies correctly`() {
        val saltA = AsnSecurityHasher.newSaltHex()
        val saltB = AsnSecurityHasher.newSaltHex()
        assertFalse(saltA == saltB)
        val hashA = AsnSecurityHasher.hashCredential("123456", saltA)
        val hashB = AsnSecurityHasher.hashCredential("123456", saltB)
        assertFalse("Same PIN must not produce same hash across users", hashA == hashB)
        assertTrue(AsnSecurityHasher.verifyCredential("123456", saltA, hashA))
        assertFalse(AsnSecurityHasher.verifyCredential("654321", saltA, hashA))
    }

    @Test
    fun `RAG retriever matches relevant clinical and GBV chunks without colliding with CF rules`() {
        val snapshot = ScreeningContextSnapshot(
            riskLevelLabel = "Tinggi",
            integratedCategoryLabel = "Risiko Hipertensi dengan Dominasi Faktor Kesehatan dan Gaya Hidup",
            screeningDate = "05 Nov 2026"
        )
        val gbvRetrieval = RagKnowledgeRepository.retrieve(
            query = "Bagaimana cara menghubungi bantuan aman SAPA 129 jika mengalami kekerasan?",
            contextSnapshot = snapshot
        )
        assertTrue(gbvRetrieval.matchedChunks.isNotEmpty())
        assertEquals(RagTopicScope.GBV_PROTECTION, gbvRetrieval.matchedChunks.first().topicScope)

        val dashRetrieval = RagKnowledgeRepository.retrieve(
            query = "Berapa batas konsumsi garam dan bagaimana pola makan hipertensi?",
            contextSnapshot = snapshot
        )
        assertTrue(dashRetrieval.matchedChunks.isNotEmpty())
        assertEquals("DOC-HT-02", dashRetrieval.matchedChunks.first().chunkId)

        val chatReply = ChatEngine.replyWithRag(
            message = "Berapa batas garam harian?",
            lastResult = ScreeningResult(75, RiskLevel.HIGH, "05 Nov 2026")
        )
        assertTrue(chatReply.retrievedChunks.isEmpty())
        assertFalse("Pure LLM mode must not append Sumber rujukan", chatReply.text.contains("Sumber rujukan:"))
    }

    @Test
    fun `chatEngine screening result intent never leaks numeric score or percentage`() {
        val resultReply = ChatEngine.replyWithRag(
            message = "Lihat hasil skrining saya",
            lastResult = ScreeningResult(84, RiskLevel.HIGH, "05 Nov 2026"),
            integratedCategoryLabel = "Risiko Hipertensi dengan Dominasi Faktor Kesehatan dan Gaya Hidup"
        )
        assertEquals(
            "Dari hasil skrining Anda pada 05 Nov 2026, kondisi tekanan darah Anda saat ini berada pada kategori Tinggi (Risiko Hipertensi dengan Dominasi Faktor Kesehatan dan Gaya Hidup). Hasil ini merupakan gambaran awal untuk membantu Anda menjaga kesehatan sehari-hari, bukan diagnosis medis. Ada bagian dari hasil tersebut yang ingin kita bahas bersama?",
            resultReply.text
        )
        assertEquals("snapshot-zero-leak", resultReply.modelName)
        assertFalse(resultReply.text.contains("84"))
        assertFalse(resultReply.text.contains("100"))
        assertFalse(resultReply.text.contains("%"))
        assertFalse(resultReply.text.contains("CF"))
        assertTrue(resultReply.retrievedChunks.isEmpty())
    }

    @Test
    fun `screening analysis and conclusion queries invoke LLM with rich qualitative ScreeningContextSnapshot instead of static lookup`() {
        val queries = listOf(
            "analisis hasil screening saya",
            "apa yang bisa anda simpulkan dari hasil screening saya",
            "jelaskan hasil skrining saya",
            "apa saran untuk hasil skrining saya"
        )
        val lastResult = ScreeningResult(84, RiskLevel.HIGH, "05 Nov 2026")
        val catLabel = "Risiko Hipertensi dengan Dominasi Faktor Kesehatan dan Gaya Hidup"

        for (q in queries) {
            assertTrue("Query '$q' must be recognized as screening analysis query", RagKnowledgeRepository.isScreeningAnalysisQuery(q))
            assertFalse("Query '$q' must NOT be treated as simple result lookup", RagKnowledgeRepository.isSimpleResultLookupQuery(q))
            assertTrue("Query '$q' must be topic-allowed", RagKnowledgeRepository.isTopicAllowed(q))

            val reply = ChatEngine.replyWithRag(
                message = q,
                lastResult = lastResult,
                integratedCategoryLabel = catLabel,
                enableNetworkLlm = false
            )
            assertFalse(
                "Analysis query '$q' must NOT return static snapshot-zero-leak template",
                reply.modelName == "snapshot-zero-leak"
            )
            assertTrue(
                "Analysis query '$q' must synthesize conclusion from risk level and factors",
                reply.text.contains("Tinggi") && reply.text.contains("Faktor Kesehatan dan Gaya Hidup")
            )
            assertFalse("Must never leak raw numeric score 84", reply.text.contains("84"))
            assertFalse("Must never leak CF acronym", reply.text.contains("CF"))
        }

        // Verify buildAugmentedPrompt injects [KONTEKS HASIL SKRINING PENGGUNA SAAT INI] for LLM generation
        val snapshot = ScreeningContextSnapshot(
            riskLevelLabel = "Tinggi",
            integratedCategoryLabel = catLabel,
            screeningDate = "05 Nov 2026",
            summaryNarrative = "Kondisi tekanan darah dipengaruhi faktor gaya hidup.",
            hypertensionStatusLabel = "Tinggi · Perlu Perhatian",
            gbvStatusLabel = "Tidak terindikasi",
            keyFactors = listOf("Pola makan & aktivitas", "Stres & kualitas tidur"),
            protectiveFactors = listOf("Tidak merokok"),
            relationshipExplanation = "Faktor fisik dan istirahat saling berkaitan.",
            keyRecommendations = listOf("Kurangi asupan garam berlebih.")
        )
        val augmented = OllamaRagService.buildAugmentedPrompt(
            sanitizedQuery = "analisis hasil screening saya",
            retrieval = com.example.empati.data.rag.RagRetrievalResult(
                query = "analisis hasil screening saya",
                matchedChunks = emptyList(),
                contextSnapshot = snapshot
            )
        )
        assertTrue(augmented.contains("[KONTEKS HASIL SKRINING PENGGUNA SAAT INI]"))
        assertTrue(augmented.contains("Pola makan & aktivitas, Stres & kualitas tidur"))
        assertTrue(augmented.contains("Tidak merokok"))
        assertTrue(augmented.contains("Instruksi Analisis untuk SRYA Emora:"))
    }

    @Test
    fun `OllamaRagService strips 18-digit ASN NIP and enforces topic scoping and SAPA 129 guardrails`() {
        val rawWithNip = "NIP saya 19850312 201001 2 014 dan 199204152018012003, saya mengalami kekerasan psikologis dan stres kerja sehingga tensi naik"
        val sanitized = OllamaRagService.sanitizePrivacyInput(rawWithNip)
        assertFalse("18-digit NIP must never leave local boundary", sanitized.contains("19850312"))
        assertFalse("Contiguous NIP must never leave local boundary", sanitized.contains("199204152018012003"))
        assertTrue(sanitized.contains("[NIP-DIRAHASIAKAN]"))

        val snapshot = ScreeningContextSnapshot(
            riskLevelLabel = "Tinggi",
            integratedCategoryLabel = "Risiko Hipertensi dengan Indikasi Keterkaitan Pengalaman KBG",
            screeningDate = "05 Nov 2026"
        )
        val retrieval = RagKnowledgeRepository.retrieve(sanitized, snapshot)
        val augmentedPrompt = OllamaRagService.buildAugmentedPrompt(sanitized, retrieval)
        assertTrue(augmentedPrompt.contains("SAPA 129"))
        assertTrue(augmentedPrompt.contains("chronic stress pathway"))
        assertFalse(augmentedPrompt.contains("19850312"))

        // Simulasikan output mentah LLM yang bocor CF/desimal dan lupa mencantumkan SAPA 129
        val simulatedRawLlm = "Kondisi Anda memiliki CF 0.85 dan MB 0.90 (85%). Tekanan psikologis perlu ditangani."
        val guarded = OllamaRagService.enforceOutputGuardrails(simulatedRawLlm, retrieval, sanitized)
        assertFalse(guarded.contains("CF"))
        assertFalse(guarded.contains("MB"))
        assertFalse(guarded.contains("%"))
        assertFalse(Regex("\\d+\\.\\d+").containsMatchIn(guarded))
        assertTrue("Must include SAPA 129 hotline", guarded.contains("129") && guarded.contains("08111-129-129"))
        assertTrue("Must frame as chronic stress pathway", guarded.lowercase().contains("kronis"))

        // Pertanyaan di luar cakupan harus ditolak
        val outOfScopeRetrieval = RagKnowledgeRepository.retrieve("Bagaimana prediksi harga kripto hari ini?", snapshot)
        val outOfScopeOutcome = OllamaRagService.generateRagResponse(
            rawQuery = "Bagaimana prediksi harga kripto hari ini?",
            retrieval = outOfScopeRetrieval,
            enableNetworkLlm = false
        )
        assertEquals(OllamaRagService.OUT_OF_SCOPE_REFUSAL, outOfScopeOutcome.text)
    }

    @Test
    fun `WebRagSearchService parses Wikipedia Indonesia and EuropePMC JSON into zero-leak RagDocumentChunks`() {
        val sampleWikiJson = """
            {
              "batchcomplete": "",
              "query": {
                "pages": {
                  "45120": {
                    "pageid": 45120,
                    "ns": 0,
                    "title": "Hipertensi",
                    "index": 1,
                    "extract": "Hipertensi atau tekanan darah tinggi adalah kondisi medis kronis dengan peningkatan tekanan darah di arteri. Sekitar 90% kasus tergolong hipertensi primer dengan nilai 1.5 kali risiko kardiovaskular."
                  }
                }
              }
            }
        """.trimIndent()

        val wikiChunks = WebRagSearchService.parseWikipediaJsonToChunks(
            json = sampleWikiJson,
            originalQuery = "Apa komplikasi hipertensi pada pembuluh darah?"
        )
        assertEquals(1, wikiChunks.size)
        val wikiChunk = wikiChunks.first()
        assertEquals("WEB-WIKI-45120", wikiChunk.chunkId)
        assertTrue(wikiChunk.docSource.contains("https://id.wikipedia.org/wiki/Hipertensi"))
        assertFalse("Web text must not contain % symbol", wikiChunk.content.contains("%"))
        assertFalse("Web text must not contain dot decimals", Regex("\\d+\\.\\d+").containsMatchIn(wikiChunk.content))

        val samplePmcJson = """
            {
              "version": "6.9",
              "resultList": {
                "result": [
                  {
                    "id": "38192011",
                    "pmid": "38192011",
                    "title": "Dietary Sodium Reduction and Blood Pressure in Adults.",
                    "journalTitle": "Journal of Hypertension",
                    "pubYear": "2025",
                    "abstractText": "Reducing dietary sodium intake significantly lowers systolic blood pressure by 5.4 mmHg and reduces cardiovascular risk by 20%."
                  }
                ]
              }
            }
        """.trimIndent()

        val pmcChunks = WebRagSearchService.parseEuropePmcJsonToChunks(
            json = samplePmcJson,
            originalQuery = "Bagaimana pengaruh garam natrium terhadap hipertensi?"
        )
        assertEquals(1, pmcChunks.size)
        val pmcChunk = pmcChunks.first()
        assertEquals("WEB-PMC-38192011", pmcChunk.chunkId)
        assertEquals(RagTopicScope.LIFESTYLE_DASH, pmcChunk.topicScope)
        assertTrue(pmcChunk.docSource.contains("https://europepmc.org/article/MED/38192011"))
        assertFalse(pmcChunk.content.contains("%"))
        assertFalse(Regex("\\d+\\.\\d+").containsMatchIn(pmcChunk.content))

        val pubMedQuery = WebRagSearchService.buildPubMedEnglishQuery("Bagaimana pengaruh garam dan stres terhadap hipertensi?")
        assertTrue(pubMedQuery.contains("hypertension"))
        assertTrue(pubMedQuery.contains("HAS_ABSTRACT:y"))
    }

    @Test
    fun `age risk and prevention questions return direct Indonesian answers without pulmonary hypertension or SAPA 129 leaks`() {
        val preventionReply = ChatEngine.replyWithRag(
            message = "Cara mencegah hipertensi?",
            lastResult = null,
            enableNetworkLlm = false,
            enableLiveWebSearch = false
        )
        assertTrue(preventionReply.text.contains("garam"))
        assertTrue(preventionReply.text.contains("DASH") || preventionReply.text.contains("olahraga"))
        assertFalse("Must never dump pulmonary hypertension", preventionReply.text.lowercase().contains("hipertensi paru"))
        assertFalse("Must never dump 'Sebagai pelengkap'", preventionReply.text.contains("Sebagai pelengkap"))
        assertFalse("Prevention question must not trigger SAPA 129", preventionReply.text.contains("SAPA 129"))
        assertFalse(preventionReply.retrievedChunkIds.contains("DOC-GBV-01"))

        val ageReply = ChatEngine.replyWithRag(
            message = "berapa umur yang paling beresiko terkena hipertensi",
            lastResult = null,
            enableNetworkLlm = false,
            enableLiveWebSearch = false
        )
        assertTrue("Age question must directly discuss age groups", ageReply.text.contains("40") && ageReply.text.contains("55"))
        assertFalse("Must never dump English abstracts", ageReply.text.contains("Background Elevated"))
        assertFalse("Must never dump 'Sebagai pelengkap'", ageReply.text.contains("Sebagai pelengkap"))
        assertFalse("Age question must NEVER attach SAPA 129 / DOC-GBV-01", ageReply.text.contains("SAPA 129"))
        assertFalse(ageReply.retrievedChunkIds.contains("DOC-GBV-01"))
    }

    @Test
    fun `enforceOutputGuardrails fixes cut-off list items and unclosed markdown bold asterisks`() {
        val retrieval = RagKnowledgeRepository.retrieve("apa yang bisa saya lakukan untuk meningkatkan kualitas hidup dan kesehatan saya", null)
        val cutOffPoint4 = """
            Untuk meningkatkan kesehatan dan kualitas hidup Anda, ada beberapa langkah yang bisa Anda lakukan:

            1. Mengatur Pola Makan: Buatlah pola makan yang seimbang dengan mengonsumsi makanan sehat.

            2. Melakukan Olahraga: Lakukan aktivitas fisik minimal 150 menit per minggu.

            3. Membuat Pola Tidur yang Sehat: Coba untuk tidur 7-9 jam setiap malam.

            4. **Mengelola
        """.trimIndent()

        val cleaned1 = OllamaRagService.enforceOutputGuardrails(cutOffPoint4, retrieval)
        assertFalse("Must strip unclosed **", cleaned1.contains("**"))
        assertFalse("Must drop dangling unfinished point 4", cleaned1.contains("4. Mengelola"))
        assertTrue("Must end with complete sentence", cleaned1.endsWith("."))

        val cutOffMidSentence = """
            1. Pola Makan Sehat: Makan makanan bergizi dan kurangi makanan berlemak.
            2. Olahraga Rutin: Lakukan aktivitas fisik minimal 150 menit per minggu.
            3. Kualitas Tidur: Coba untuk tidur 7-9 jam setiap malam. Jaga pola tidur Anda, dan hindari penggunaan ponsel
        """.trimIndent()

        val cleaned2 = OllamaRagService.enforceOutputGuardrails(cutOffMidSentence, retrieval)
        assertTrue("Must end with complete sentence", cleaned2.endsWith("."))
        assertFalse("Must trim unfinished trailing clause", cleaned2.endsWith("ponsel"))
    }

    @Test
    fun `identity questions like siapa anda explain SRYA Emora instead of triggering out of scope refusal`() {
        val queries = listOf(
            "siapa anda",
            "Siapa Anda?",
            "siapa kamu",
            "apa itu srya emora",
            "kamu bisa bantu apa"
        )
        for (q in queries) {
            assertTrue("Identity query '$q' must be allowed", RagKnowledgeRepository.isTopicAllowed(q))
            val reply = ChatEngine.replyWithRag(
                message = q,
                lastResult = null,
                enableNetworkLlm = false
            )
            assertFalse("Identity query '$q' must never return OUT_OF_SCOPE_REFUSAL", reply.text == OllamaRagService.OUT_OF_SCOPE_REFUSAL)
            assertTrue("Identity query '$q' must introduce SRYA Emora", reply.text.contains("SRYA Emora"))
            assertTrue("Identity query '$q' must mention tekanan darah", reply.text.contains("tekanan darah"))
        }
    }

    @Test
    fun `SupabaseCloudSyncService builds pseudonymized research payload, persistent user account payload without screening, and OpenRouter payload`() {
        val profile = UserProfile(
            name = "Sari Wulandari",
            nip = "sari.wulandari@email.id",
            instansi = "Jakarta Selatan",
            jabatan = "Staf Administrasi",
            jenisAsn = "Karyawan / Pekerja",
            golongan = "Perempuan"
        )
        val subjectHash = AsnSecurityHasher.deriveSubjectHash(profile.nip)
        val record = ResearchRecord(
            screeningId = "scr-20261108-001",
            timestamp = 1794140000000L,
            hypertension = ResearchModule("HYPERTENSION", 0.42, "SEDANG", true, 16, 16, emptyList()),
            gbv = ResearchModule("GBV", 0.0, "RENDAH", true, 10, 10, emptyList()),
            integratedCategory = "HYPERTENSION_LIFESTYLE_ONLY",
            hypertensionCf = 0.42,
            gbvCf = 0.0
        )
        val summary = ScreeningResult(score = 42, level = RiskLevel.MEDIUM, date = "08 Nov 2026")
        val serialized = SupabaseCloudSyncService.buildResearchRecordJsonPayload(
            record = record,
            subjectHash = subjectHash,
            profile = profile,
            summary = summary
        )

        assertTrue(serialized.contains("\"screening_id\":\"scr-20261108-001\""))
        assertTrue(serialized.contains("\"subject_hash\":\"$subjectHash\""))
        assertTrue(serialized.contains("\"gender\":\"Perempuan\""))
        assertTrue(serialized.contains("\"occupation_category\":\"Karyawan / Pekerja\""))
        assertTrue(serialized.contains("\"risk_level\":\"MEDIUM\""))
        assertTrue(serialized.contains("\"score\":42"))
        assertFalse("Raw name must never be sent to Supabase research table", serialized.contains("Sari Wulandari"))
        assertFalse("Raw email/account ID must never be sent to Supabase research table", serialized.contains("sari.wulandari@email.id"))

        val salt = AsnSecurityHasher.newSaltHex()
        val persistedAccount = PersistedUserAccount(
            userId = "usr_1234567890abcdef",
            accountIdentifierClean = "sari.wulandari@email.id",
            accountIdentifierFormatted = "sari.wulandari@email.id",
            profile = profile,
            birthYear = 1992,
            passwordHash = AsnSecurityHasher.hashCredential("rahasia99", salt),
            passwordSalt = salt,
            subjectHash = subjectHash,
            createdAt = 1794140000000L,
            updatedAt = 1794140000000L,
            cloudSynced = false
        )
        val accountPayload = SupabaseCloudSyncService.buildUserAccountJsonPayload(persistedAccount)
        assertTrue(accountPayload.contains("\"user_id\":\"usr_1234567890abcdef\""))
        assertTrue(accountPayload.contains("\"account_identifier\":\"sari.wulandari@email.id\""))
        assertTrue(accountPayload.contains("\"full_name\":\"Sari Wulandari\""))
        assertFalse("Raw PIN must never be stored in user account payload", accountPayload.contains("rahasia99"))

        val baselinePayload = SupabaseCloudSyncService.buildAccountBaselineResearchPayload(persistedAccount)
        assertTrue(baselinePayload.contains("\"screening_id\":\"acct_$subjectHash\""))
        assertTrue(baselinePayload.contains("\"integrated_category\":\"ACCOUNT_REGISTERED_NO_SCREENING\""))
        assertFalse("Baseline research payload must remain pseudonymized", baselinePayload.contains("Sari Wulandari"))

        val openRouterPayload = OllamaRagService.buildOpenRouterJsonPayload(
            model = OllamaRagService.OPENROUTER_DEFAULT_MODEL,
            systemPrompt = "System test",
            userPrompt = "Bagaimana cara menjaga tekanan darah?"
        )
        assertTrue(openRouterPayload.contains("qwen/qwen-2.5-7b-instruct"))
        assertTrue(openRouterPayload.contains("\"max_tokens\":512"))
    }
}

