package com.example.empati.data

import android.content.Context
import com.example.empati.data.auth.AsnNipValidator
import com.example.empati.data.auth.AsnSecurityHasher
import com.example.empati.data.cloud.SupabaseCloudSyncService
import com.example.empati.data.db.EmpatiDatabase
import com.example.empati.data.presentation.UserIntegratedPresentation
import com.example.empati.data.presentation.UserResultPresenter
import com.example.empati.data.rag.RagDocumentChunk
import com.example.empati.data.rag.ScreeningContextSnapshot
import com.example.empati.data.research.ResearchCategory
import com.example.empati.data.research.ResearchEvidence
import com.example.empati.data.research.ResearchModule
import com.example.empati.data.research.ResearchRecord
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repositori tunggal antara ViewModel dan penyimpanan lokal/cloud.
 *
 * - SQLite (`EmpatiDatabase`) = sumber kebenaran utama untuk akun & profil pengguna (`auth_asn_users`),
 *   sesi aktif (`is_active_session`), riwayat skrining, data riset CF, dan korpus RAG.
 * - Akun dan profil pengguna tersimpan secara persisten di SQLite maupun Cloud Supabase (`user_accounts`)
 *   sejak saat pendaftaran atau pembaruan profil, tanpa mengharuskan pengguna melakukan skrining terlebih dahulu.
 */
class ScreeningStore(
    context: Context,
    private val database: EmpatiDatabase = EmpatiDatabase(context),
    prefsName: String = "empati"
) {

    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    init {
        migrateLegacyPrefsIfNeeded()
    }

    fun hasCompletedOnboarding(): Boolean = prefs.getBoolean("onboarded", false)

    fun setOnboardingComplete() {
        prefs.edit().putBoolean("onboarded", true).commit()
    }

    // ---- Sesi & Otentikasi Pengguna (Domain 1 — Persisten walau belum skrining) ----

    fun activeUserId(): String {
        val prefId = prefs.getString("active_user_id", null)
            ?.takeIf { it.isNotBlank() && it != EmpatiDatabase.LEGACY_DEMO_USER_ID }
        if (prefId != null && database.loadUserProfile(prefId) != null) {
            return prefId
        }
        val dbActiveId = database.loadActiveSessionUserId()
            ?.takeIf { it.isNotBlank() && it != EmpatiDatabase.LEGACY_DEMO_USER_ID }
        if (dbActiveId != null) {
            prefs.edit()
                .putBoolean("asn_authenticated", true)
                .putString("active_user_id", dbActiveId)
                .commit()
            return dbActiveId
        }
        return ""
    }

    fun isLoggedIn(): Boolean {
        val uid = activeUserId()
        if (uid.isBlank()) return false
        return database.loadUserProfile(uid) != null
    }

    private fun startSession(userId: String) {
        database.setActiveSessionUser(userId)
        prefs.edit()
            .putBoolean("asn_authenticated", true)
            .putString("active_user_id", userId)
            .commit()
    }

    fun loginAsn(
        rawNip: String,
        pinOrPassword: String,
        allowCloudRestore: Boolean = false
    ): Result<UserProfile> {
        val localAuth = database.authenticateAsnUser(rawNip, pinOrPassword)
        if (localAuth.isSuccess) {
            val (userId, profile) = localAuth.getOrThrow()
            startSession(userId)
            return Result.success(profile)
        }

        if (allowCloudRestore) {
            val cleanId = AsnNipValidator.normalizeAccountId(rawNip)
            val cloudAccount = SupabaseCloudSyncService.fetchUserAccountByIdentifier(cleanId)
            if (cloudAccount != null &&
                cloudAccount.passwordSalt.isNotBlank() &&
                AsnSecurityHasher.verifyCredential(pinOrPassword, cloudAccount.passwordSalt, cloudAccount.passwordHash)
            ) {
                database.upsertRestoredAccount(cloudAccount, makeActiveSession = true)
                startSession(cloudAccount.userId)
                return Result.success(cloudAccount.profile)
            }
        }

        return localAuth.map { (_, profile) -> profile }
    }

    fun registerAsn(profile: UserProfile, pinOrPassword: String): Result<UserProfile> =
        database.registerAsnUser(profile, pinOrPassword).map { userId ->
            startSession(userId)
            database.loadUserProfile(userId) ?: profile
        }

    fun logoutAsn() {
        database.setActiveSessionUser(null)
        prefs.edit()
            .putBoolean("asn_authenticated", false)
            .remove("active_user_id")
            .commit()
    }

    // ---- Riwayat Skrining Kualitatif (Domain 2 — UI Safe, per pengguna) ----

    fun loadHistory(): List<ScreeningResult> {
        val uid = activeUserId()
        if (uid.isBlank()) return emptyList()
        return database.loadUserHistory(uid)
    }

    fun addHistory(result: ScreeningResult) {
        val uid = activeUserId()
        if (uid.isBlank()) return
        database.insertUserSummary(uid, result)
    }

    /**
     * Menyimpan ringkasan kualitatif (`ScreeningContextSnapshot`) per pengguna ke `SharedPreferences`
     * agar saat aplikasi dibuka kembali di sesi berikutnya, chatbot Empati tetap memiliki konteks
     * kualitatif lengkap untuk dianalisis tanpa membaca tabel riset CF.
     */
    fun saveQualitativeSnapshot(snapshot: ScreeningContextSnapshot) {
        val uid = activeUserId()
        if (uid.isBlank()) return
        val json = JSONObject().apply {
            put("riskLevelLabel", snapshot.riskLevelLabel)
            put("integratedCategoryLabel", snapshot.integratedCategoryLabel ?: "")
            put("screeningDate", snapshot.screeningDate)
            put("summaryNarrative", snapshot.summaryNarrative ?: "")
            put("hypertensionStatusLabel", snapshot.hypertensionStatusLabel ?: "")
            put("gbvStatusLabel", snapshot.gbvStatusLabel ?: "")
            put("keyFactors", JSONArray(snapshot.keyFactors))
            put("protectiveFactors", JSONArray(snapshot.protectiveFactors))
            put("relationshipExplanation", snapshot.relationshipExplanation ?: "")
            put("keyRecommendations", JSONArray(snapshot.keyRecommendations))
        }
        prefs.edit().putString("qualitative_snapshot_$uid", json.toString()).apply()
    }

    /**
     * Memuat `ScreeningContextSnapshot` kualitatif milik pengguna aktif untuk dianalisis oleh LLM.
     */
    fun loadQualitativeSnapshot(
        lastResult: ScreeningResult?,
        currentIntegrated: UserIntegratedPresentation? = null
    ): ScreeningContextSnapshot? {
        if (lastResult == null) return null
        if (currentIntegrated != null) {
            return UserResultPresenter.buildContextSnapshot(
                lastResult = lastResult,
                integrated = currentIntegrated
            )
        }
        val uid = activeUserId()
        if (uid.isNotBlank()) {
            val raw = prefs.getString("qualitative_snapshot_$uid", null)
            if (!raw.isNullOrBlank()) {
                val parsed = runCatching {
                    val o = JSONObject(raw)
                    val factorsArr = o.optJSONArray("keyFactors") ?: JSONArray()
                    val protArr = o.optJSONArray("protectiveFactors") ?: JSONArray()
                    val recsArr = o.optJSONArray("keyRecommendations") ?: JSONArray()
                    ScreeningContextSnapshot(
                        riskLevelLabel = lastResult.level.label,
                        integratedCategoryLabel = o.optString("integratedCategoryLabel").takeIf { it.isNotBlank() },
                        screeningDate = lastResult.date,
                        summaryNarrative = o.optString("summaryNarrative").takeIf { it.isNotBlank() },
                        hypertensionStatusLabel = o.optString("hypertensionStatusLabel").takeIf { it.isNotBlank() },
                        gbvStatusLabel = o.optString("gbvStatusLabel").takeIf { it.isNotBlank() },
                        keyFactors = (0 until factorsArr.length()).map { factorsArr.getString(it) },
                        protectiveFactors = (0 until protArr.length()).map { protArr.getString(it) },
                        relationshipExplanation = o.optString("relationshipExplanation").takeIf { it.isNotBlank() },
                        keyRecommendations = (0 until recsArr.length()).map { recsArr.getString(it) }
                    )
                }.getOrNull()
                if (parsed != null) return parsed
            }
        }
        return UserResultPresenter.buildContextSnapshot(lastResult = lastResult)
    }

    // ---- Internal research storage (Domain 3 — Pseudonymized via subject_hash) ----

    fun activeSubjectHash(): String =
        database.subjectHashFor(activeUserId())
            ?: AsnSecurityHasher.deriveSubjectHash(loadProfile().nip)

    fun saveResearchRecord(record: ResearchRecord) {
        val subjectHash = activeSubjectHash()
        database.insertResearchRecord(
            screeningId = record.screeningId,
            subjectHash = subjectHash,
            kbVersion = EmpatiDatabase.CURRENT_KB_VERSION,
            timestamp = record.timestamp,
            hypertensionCf = record.hypertensionCf,
            gbvCf = record.gbvCf,
            integratedCategory = record.integratedCategory,
            payloadJson = recordToJson(record).toString()
        )
    }

    /**
     * Menyinkronkan data akun & profil pengguna ke database Cloud Supabase (`user_accounts`
     * dan baseline demografi) sehingga data akun tidak pernah hilang meskipun pengguna belum
     * melakukan skrining. Akun yang sedang aktif (`activeAccount`) selalu di-upsert secara
     * idempoten untuk menjamin sinkronisasi penuh ke server.
     */
    fun syncUserAccountsToSupabase(): Int {
        val accountsToSync = database.loadUnsyncedUserAccounts().toMutableList()
        val activeAccount = database.loadUserAccount(activeUserId())
        if (activeAccount != null && accountsToSync.none { it.userId == activeAccount.userId }) {
            accountsToSync.add(activeAccount)
        }
        var syncedCount = 0
        for (account in accountsToSync) {
            if (SupabaseCloudSyncService.syncUserAccount(account)) {
                database.markAccountCloudSynced(account.userId)
                syncedCount++
            }
        }
        return syncedCount
    }

    /**
     * Menyinkronkan akun pengguna serta seluruh `ResearchRecord` lokal yang belum terunggah ke
     * Cloud Supabase (`empati-research`). Aman dipanggil berulang kali (idempoten & offline-safe),
     * termasuk saat pengguna belum memiliki riwayat skrining.
     */
    fun syncPendingResearchToSupabase(
        latestSummary: ScreeningResult? = loadHistory().firstOrNull()
    ): Int {
        // 1. Pastikan akun & profil pengguna selalu tersinkronisasi walau belum pernah skrining
        val syncedAccounts = syncUserAccountsToSupabase()

        // 2. Sinkronisasi hasil skrining CF milik subjek aktif bila ada
        val syncedIds = prefs.getStringSet("synced_supabase_ids", emptySet())?.toMutableSet()
            ?: mutableSetOf()
        val subjectHash = activeSubjectHash()
        val currentProfile = loadProfile()
        var newlySynced = 0

        val subjectRecords = database.loadResearchPayloads(subjectHash).mapNotNull { raw ->
            runCatching { recordFromJson(JSONObject(raw)) }.getOrNull()
        }
        for (record in subjectRecords) {
            if (record.screeningId in syncedIds) continue
            val summaryForRecord = if (subjectRecords.size == 1) latestSummary else null
            val rowJsonPayload = SupabaseCloudSyncService.buildResearchRecordJsonPayload(
                record = record,
                subjectHash = subjectHash,
                profile = currentProfile,
                summary = summaryForRecord,
                rawPayloadJson = recordToJson(record).toString()
            )
            if (SupabaseCloudSyncService.syncResearchRecord(rowJsonPayload)) {
                syncedIds.add(record.screeningId)
                newlySynced++
            }
        }

        if (newlySynced > 0) {
            prefs.edit().putStringSet("synced_supabase_ids", syncedIds).apply()
        }
        return syncedAccounts + newlySynced
    }

    /**
     * Menyinkronkan percakapan chatbot secara anonim (`subject_hash` + sanitasi PII)
     * ke tabel `public.research_chat_logs` di Cloud Supabase.
     */
    fun syncChatExchangeToSupabase(userText: String, botReply: ChatReplyResult) {
        val subjectHash = activeSubjectHash()
        val now = System.currentTimeMillis()
        SupabaseCloudSyncService.syncChatLog(
            subjectHash = subjectHash,
            isUser = true,
            rawMessage = userText,
            modelName = "user-query",
            timestampMs = now
        )
        SupabaseCloudSyncService.syncChatLog(
            subjectHash = subjectHash,
            isUser = false,
            rawMessage = botReply.text,
            modelName = botReply.modelName,
            timestampMs = now + 1
        )
    }

    /** Seluruh data riset di perangkat (untuk ekspor peneliti). */
    fun loadResearchRecords(): List<ResearchRecord> =
        database.loadResearchPayloads().mapNotNull { raw ->
            runCatching { recordFromJson(JSONObject(raw)) }.getOrNull()
        }

    // ---- Korpus Pengetahuan RAG & Log Chat (Domain 4) ----

    fun loadRagCorpus(): List<RagDocumentChunk> = database.loadRagCorpus()

    fun cacheWebChunks(chunks: List<RagDocumentChunk>) {
        database.upsertRagChunks(chunks)
    }

    fun loadChatLogs(): List<ChatMessage> {
        val uid = activeUserId()
        if (uid.isBlank()) return emptyList()
        return database.loadChatLogs(uid)
    }

    fun appendChatExchange(userText: String, botReply: ChatReplyResult) {
        val uid = activeUserId()
        if (uid.isBlank()) return
        if (botReply.retrievedChunks.isNotEmpty()) {
            database.upsertRagChunks(botReply.retrievedChunks)
        }
        database.insertChatLog(uid, isUser = true, messageText = userText)
        database.insertChatLog(
            uid,
            isUser = false,
            messageText = botReply.text,
            retrievedChunkIds = botReply.retrievedChunkIds
        )
    }

    // ---- Profile ----

    fun loadProfile(): UserProfile = database.loadUserProfile(activeUserId()) ?: UserProfile()

    fun saveProfile(p: UserProfile) {
        database.updateEditableProfile(activeUserId(), p)
    }

    /**
     * Menghapus seluruh data milik pengguna yang sedang login dari penyimpanan lokal
     * (dan opsional dari Cloud Supabase `user_accounts`), lalu mengakhiri sesi.
     */
    fun clearCurrentUserData(deleteFromCloud: Boolean = false): Pair<String, String> {
        val uid = activeUserId()
        val subjectHash = activeSubjectHash()
        database.deleteUserData(uid)
        logoutAsn()
        if (deleteFromCloud && uid.isNotBlank()) {
            SupabaseCloudSyncService.deleteUserAccount(uid, subjectHash)
        }
        return uid to subjectHash
    }

    fun debugTableCounts(): Map<String, Long> = database.tableRowCounts()

    // ---- Pembersihan preferensi warisan (≤ v2.3) & pembersihan sesi demo lama ----

    private fun migrateLegacyPrefsIfNeeded() {
        val storedActive = prefs.getString("active_user_id", null)
        if (storedActive == EmpatiDatabase.LEGACY_DEMO_USER_ID) {
            prefs.edit()
                .putBoolean("asn_authenticated", false)
                .remove("active_user_id")
                .commit()
        }
        if (prefs.getBoolean("legacy_migrated_v24", false)) return

        prefs.edit()
            .remove("history").remove("research_records")
            .remove("name").remove("nip").remove("instansi").remove("jabatan")
            .remove("jenisAsn").remove("golongan")
            .putBoolean("legacy_migrated_v24", true)
            .commit()
    }

    // ---- Serialisasi payload riset ----

    private fun recordToJson(record: ResearchRecord): JSONObject = JSONObject().apply {
        put("screeningId", record.screeningId)
        put("timestamp", record.timestamp)
        put("hypertension", moduleToJson(record.hypertension))
        put("gbv", moduleToJson(record.gbv))
        put("integratedCategory", record.integratedCategory)
        put("hypertensionCf", record.hypertensionCf)
        put("gbvCf", record.gbvCf)
    }

    private fun moduleToJson(module: ResearchModule): JSONObject = JSONObject().apply {
        put("module", module.module)
        put("cf", module.cf)
        put("level", module.level)
        put("complete", module.complete)
        put("answered", module.answered)
        put("total", module.total)
        val categories = JSONArray()
        module.categories.forEach { category ->
            val categoryJson = JSONObject().apply {
                put("hypothesis", category.hypothesis)
                put("cf", category.cf)
                val evidence = JSONArray()
                category.evidence.forEach { e ->
                    evidence.put(JSONObject().apply {
                        put("questionId", e.questionId)
                        put("cfUser", e.cfUser)
                        put("cfRule", e.cfRule)
                        put("cfEvidence", e.cfEvidence)
                    })
                }
                put("evidence", evidence)
            }
            categories.put(categoryJson)
        }
        put("categories", categories)
    }

    private fun recordFromJson(o: JSONObject): ResearchRecord = ResearchRecord(
        screeningId = o.getString("screeningId"),
        timestamp = o.getLong("timestamp"),
        hypertension = moduleFromJson(o.getJSONObject("hypertension")),
        gbv = moduleFromJson(o.getJSONObject("gbv")),
        integratedCategory = o.getString("integratedCategory"),
        hypertensionCf = o.optDouble("hypertensionCf", 0.0),
        gbvCf = o.optDouble("gbvCf", 0.0)
    )

    private fun moduleFromJson(o: JSONObject): ResearchModule {
        val categoriesArray = o.optJSONArray("categories") ?: JSONArray()
        val categories = (0 until categoriesArray.length()).map { i ->
            val c = categoriesArray.getJSONObject(i)
            val evidenceArray = c.optJSONArray("evidence") ?: JSONArray()
            ResearchCategory(
                hypothesis = c.getString("hypothesis"),
                cf = c.optDouble("cf", 0.0),
                evidence = (0 until evidenceArray.length()).map { j ->
                    val e = evidenceArray.getJSONObject(j)
                    ResearchEvidence(
                        questionId = e.getString("questionId"),
                        cfUser = e.optDouble("cfUser", 0.0),
                        cfRule = e.optDouble("cfRule", 0.0),
                        cfEvidence = e.optDouble("cfEvidence", 0.0)
                    )
                }
            )
        }
        return ResearchModule(
            module = o.getString("module"),
            cf = o.optDouble("cf", 0.0),
            level = o.getString("level"),
            complete = o.optBoolean("complete", false),
            answered = o.optInt("answered", 0),
            total = o.optInt("total", 0),
            categories = categories
        )
    }
}
