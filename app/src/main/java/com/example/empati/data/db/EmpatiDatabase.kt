package com.example.empati.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.empati.data.ChatMessage
import com.example.empati.data.RiskLevel
import com.example.empati.data.ScreeningResult
import com.example.empati.data.UserProfile
import com.example.empati.data.auth.AsnNipValidator
import com.example.empati.data.auth.AsnSecurityHasher
import com.example.empati.data.auth.NipValidationResult
import com.example.empati.data.rag.RagDocumentChunk
import com.example.empati.data.rag.RagKnowledgeRepository
import com.example.empati.data.rag.RagTopicScope
import java.util.UUID

/**
 * Representasi rekaman akun & profil pengguna yang disimpan secara persisten di database
 * (`auth_asn_users` lokal maupun sinkronisasi `user_accounts` di Cloud), tetap utuh meskipun
 * pengguna belum pernah melakukan skrining.
 */
data class PersistedUserAccount(
    val userId: String,
    val accountIdentifierClean: String,
    val accountIdentifierFormatted: String,
    val profile: UserProfile,
    val birthYear: Int,
    val passwordHash: String,
    val passwordSalt: String,
    val subjectHash: String,
    val createdAt: Long,
    val updatedAt: Long,
    val cloudSynced: Boolean = false
)

/**
 * Arsitektur Database Multi-Domain Terisolasi (Zero-Collision SQLite Database).
 *
 * Memisahkan penyimpanan lokal menjadi 4 domain tabel yang tidak saling menimpa:
 * 1. Domain Auth & Profil Pengguna (`auth_asn_users`) — persisten mandiri walau belum skrining
 * 2. Domain Riwayat Kualitatif Pengguna (`user_screening_summaries`) — aman untuk UI
 * 3. Domain Audit Sistem Pakar CF (`research_cf_records`) — terpseudonimisasi (`subject_hash`) & berversi (`kb_version`)
 * 4. Domain Korpus RAG & Log Konsultasi (`rag_knowledge_chunks`, `rag_chat_logs`)
 *
 * @param dbName nama file database; `null` = database in-memory (dipakai oleh instrumented test).
 */
class EmpatiDatabase(context: Context, dbName: String? = DB_NAME) :
    SQLiteOpenHelper(context.applicationContext, dbName, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Domain Identitas, Profil & Otentikasi Pengguna (Persisten tanpa bergantung skrining)
        db.execSQL(
            """
            CREATE TABLE $TABLE_ASN_USERS (
                user_id TEXT PRIMARY KEY,
                nip_clean TEXT UNIQUE NOT NULL,
                nip_formatted TEXT NOT NULL,
                full_name TEXT NOT NULL,
                jenis_asn TEXT NOT NULL,
                golongan TEXT NOT NULL,
                instansi TEXT NOT NULL,
                jabatan TEXT NOT NULL,
                birth_year INTEGER NOT NULL,
                password_hash TEXT NOT NULL,
                password_salt TEXT,
                subject_hash TEXT UNIQUE NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL DEFAULT 0,
                is_active_session INTEGER NOT NULL DEFAULT 0,
                cloud_synced INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        // 2. Domain Ringkasan Skrining Kualitatif (UI Layer)
        db.execSQL(
            """
            CREATE TABLE $TABLE_USER_SUMMARIES (
                summary_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                score INTEGER NOT NULL,
                risk_level TEXT NOT NULL,
                screening_date TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 3. Domain Sistem Pakar CF / Riset (Dipisahkan dengan Pseudonym Firewall subject_hash)
        db.execSQL(
            """
            CREATE TABLE $TABLE_RESEARCH_CF (
                screening_id TEXT PRIMARY KEY,
                subject_hash TEXT NOT NULL,
                kb_version TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                hypertension_cf REAL NOT NULL,
                gbv_cf REAL NOT NULL,
                integrated_category TEXT NOT NULL,
                payload_json TEXT NOT NULL
            )
            """.trimIndent()
        )

        // 4a. Domain Korpus Pengetahuan RAG (Read-Only Reference bagi Chatbot)
        db.execSQL(
            """
            CREATE TABLE $TABLE_RAG_CHUNKS (
                chunk_id TEXT PRIMARY KEY,
                doc_source TEXT NOT NULL,
                topic_scope TEXT NOT NULL,
                title TEXT NOT NULL,
                content_text TEXT NOT NULL,
                keywords_csv TEXT NOT NULL,
                chunk_version TEXT NOT NULL
            )
            """.trimIndent()
        )

        // 4b. Domain Log Konsultasi Chatbot
        db.execSQL(
            """
            CREATE TABLE $TABLE_RAG_CHAT_LOGS (
                log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                is_user INTEGER NOT NULL,
                message_text TEXT NOT NULL,
                retrieved_chunk_ids TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        seedDefaultRagCorpus(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            runCatching {
                db.execSQL("ALTER TABLE $TABLE_ASN_USERS ADD COLUMN password_salt TEXT")
            }
        }
        if (oldVersion < 4) {
            runCatching {
                db.execSQL("ALTER TABLE $TABLE_ASN_USERS ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
            }
            runCatching {
                db.execSQL("ALTER TABLE $TABLE_ASN_USERS ADD COLUMN is_active_session INTEGER NOT NULL DEFAULT 0")
            }
            runCatching {
                db.execSQL("ALTER TABLE $TABLE_ASN_USERS ADD COLUMN cloud_synced INTEGER NOT NULL DEFAULT 0")
            }
            // Bersihkan akun demo pengembangan lama jika ada di perangkat
            db.delete(TABLE_USER_SUMMARIES, "user_id = ?", arrayOf(LEGACY_DEMO_USER_ID))
            db.delete(TABLE_RAG_CHAT_LOGS, "user_id = ?", arrayOf(LEGACY_DEMO_USER_ID))
            db.delete(
                TABLE_ASN_USERS,
                "user_id = ? OR nip_clean = ?",
                arrayOf(LEGACY_DEMO_USER_ID, LEGACY_DEMO_ACCOUNT_ID)
            )
        }
        seedDefaultRagCorpus(db)
    }

    private fun seedDefaultRagCorpus(db: SQLiteDatabase) {
        RagKnowledgeRepository.defaultCorpus.forEach { chunk ->
            val values = ContentValues().apply {
                put("chunk_id", chunk.chunkId)
                put("doc_source", chunk.docSource)
                put("topic_scope", chunk.topicScope.name)
                put("title", chunk.title)
                put("content_text", chunk.content)
                put("keywords_csv", chunk.keywords.joinToString(","))
                put("chunk_version", chunk.chunkVersion)
            }
            db.insertWithOnConflict(TABLE_RAG_CHUNKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    // ---- Operasi Domain 1: Auth & Profil Pengguna (Inklusif Semua Kalangan) ----

    fun registerAsnUser(profile: UserProfile, pinOrPassword: String): Result<String> {
        val validation = AsnNipValidator.validateGeneralIdentity(profile.nip)
        if (validation is NipValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(validation.reason))
        }
        val meta = (validation as NipValidationResult.Valid).metadata
        val cleanPin = pinOrPassword.trim()
        if (cleanPin.length < 6) {
            return Result.failure(IllegalArgumentException("PIN / Kata sandi minimal 6 karakter."))
        }
        if (profile.name.isBlank() || profile.instansi.isBlank() || profile.jabatan.isBlank()) {
            return Result.failure(IllegalArgumentException("Lengkapi Nama, Domisili/Tempat Aktivitas, dan Pekerjaan/Peran Anda."))
        }

        val db = writableDatabase
        val alreadyExists = db.query(
            TABLE_ASN_USERS, arrayOf("user_id"), "nip_clean = ?", arrayOf(meta.cleanNip),
            null, null, null
        ).use { it.moveToFirst() }
        if (alreadyExists) {
            return Result.failure(IllegalStateException("ID Akun sudah terdaftar. Silakan masuk melalui tab Masuk Akun."))
        }

        val now = System.currentTimeMillis()
        val userId = "usr_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val salt = AsnSecurityHasher.newSaltHex()
        db.beginTransaction()
        try {
            db.update(TABLE_ASN_USERS, ContentValues().apply { put("is_active_session", 0) }, null, null)
            val values = ContentValues().apply {
                put("user_id", userId)
                put("nip_clean", meta.cleanNip)
                put("nip_formatted", meta.formattedNip)
                put("full_name", profile.name.trim())
                put("jenis_asn", profile.jenisAsn)
                put("golongan", profile.golongan)
                put("instansi", profile.instansi.trim())
                put("jabatan", profile.jabatan.trim())
                put("birth_year", meta.birthYear)
                put("password_hash", AsnSecurityHasher.hashCredential(cleanPin, salt))
                put("password_salt", salt)
                put("subject_hash", AsnSecurityHasher.deriveSubjectHash(meta.cleanNip))
                put("created_at", now)
                put("updated_at", now)
                put("is_active_session", 1)
                put("cloud_synced", 0)
            }
            db.insertOrThrow(TABLE_ASN_USERS, null, values)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return Result.success(userId)
    }

    fun authenticateAsnUser(rawIdentifier: String, pinOrPassword: String): Result<Pair<String, UserProfile>> {
        val cleanId = AsnNipValidator.normalizeAccountId(rawIdentifier)
        if (cleanId.length < 4) {
            return Result.failure(IllegalArgumentException("Masukkan ID Akun (Email / No. HP / Username / NIK / NIP) yang valid."))
        }
        val db = writableDatabase
        db.query(TABLE_ASN_USERS, null, "nip_clean = ?", arrayOf(cleanId), null, null, null).use {
            val invalid = Result.failure<Pair<String, UserProfile>>(
                IllegalArgumentException("ID Akun atau PIN/Kata Sandi tidak sesuai.")
            )
            if (!it.moveToFirst()) return invalid

            val userId = it.getString(it.getColumnIndexOrThrow("user_id"))
            val storedHash = it.getString(it.getColumnIndexOrThrow("password_hash"))
            val saltIdx = it.getColumnIndexOrThrow("password_salt")
            val salt = if (it.isNull(saltIdx)) null else it.getString(saltIdx)

            val ok = if (salt != null) {
                AsnSecurityHasher.verifyCredential(pinOrPassword, salt, storedHash)
            } else {
                // Akun dari skema v1: verifikasi hash lama lalu migrasikan ke PBKDF2.
                val legacy = AsnSecurityHasher.sha256Hex("empati-auth:$cleanId:${pinOrPassword.trim()}")
                val match = legacy == storedHash
                if (match) {
                    val newSalt = AsnSecurityHasher.newSaltHex()
                    db.update(
                        TABLE_ASN_USERS,
                        ContentValues().apply {
                            put("password_salt", newSalt)
                            put("password_hash", AsnSecurityHasher.hashCredential(pinOrPassword, newSalt))
                            put("updated_at", System.currentTimeMillis())
                            put("cloud_synced", 0)
                        },
                        "user_id = ?", arrayOf(userId)
                    )
                }
                match
            }
            if (!ok) return invalid
            val profile = readProfile(it)
            setActiveSessionUser(userId)
            return Result.success(userId to profile)
        }
    }

    /**
     * Menyimpan status sesi aktif langsung di tabel SQLite `auth_asn_users` agar akun pengguna
     * yang sedang login tetap persisten walaupun aplikasi ditutup sebelum skrining.
     */
    fun setActiveSessionUser(userId: String?) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.update(TABLE_ASN_USERS, ContentValues().apply { put("is_active_session", 0) }, null, null)
            if (!userId.isNullOrBlank()) {
                db.update(
                    TABLE_ASN_USERS,
                    ContentValues().apply { put("is_active_session", 1) },
                    "user_id = ?",
                    arrayOf(userId)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Mengambil `user_id` sesi aktif yang tersimpan di SQLite (fallback bila SharedPreferences kosong). */
    fun loadActiveSessionUserId(): String? =
        readableDatabase.query(
            TABLE_ASN_USERS,
            arrayOf("user_id"),
            "is_active_session = 1",
            null,
            null,
            null,
            "updated_at DESC",
            "1"
        ).use { if (it.moveToFirst()) it.getString(0) else null }

    private fun readProfile(c: android.database.Cursor) = UserProfile(
        name = c.getString(c.getColumnIndexOrThrow("full_name")),
        nip = c.getString(c.getColumnIndexOrThrow("nip_formatted")),
        instansi = c.getString(c.getColumnIndexOrThrow("instansi")),
        jabatan = c.getString(c.getColumnIndexOrThrow("jabatan")),
        jenisAsn = c.getString(c.getColumnIndexOrThrow("jenis_asn")),
        golongan = c.getString(c.getColumnIndexOrThrow("golongan"))
    )

    private fun readPersistedAccount(c: android.database.Cursor): PersistedUserAccount {
        val createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"))
        val updatedIdx = c.getColumnIndex("updated_at")
        val updatedAt = if (updatedIdx >= 0 && !c.isNull(updatedIdx)) c.getLong(updatedIdx).takeIf { it > 0L } ?: createdAt else createdAt
        val syncedIdx = c.getColumnIndex("cloud_synced")
        val cloudSynced = syncedIdx >= 0 && !c.isNull(syncedIdx) && c.getInt(syncedIdx) == 1
        val saltIdx = c.getColumnIndexOrThrow("password_salt")
        return PersistedUserAccount(
            userId = c.getString(c.getColumnIndexOrThrow("user_id")),
            accountIdentifierClean = c.getString(c.getColumnIndexOrThrow("nip_clean")),
            accountIdentifierFormatted = c.getString(c.getColumnIndexOrThrow("nip_formatted")),
            profile = readProfile(c),
            birthYear = c.getInt(c.getColumnIndexOrThrow("birth_year")),
            passwordHash = c.getString(c.getColumnIndexOrThrow("password_hash")),
            passwordSalt = if (c.isNull(saltIdx)) "" else c.getString(saltIdx),
            subjectHash = c.getString(c.getColumnIndexOrThrow("subject_hash")),
            createdAt = createdAt,
            updatedAt = updatedAt,
            cloudSynced = cloudSynced
        )
    }

    fun loadUserProfile(userId: String): UserProfile? {
        if (userId.isBlank()) return null
        return readableDatabase.query(TABLE_ASN_USERS, null, "user_id = ?", arrayOf(userId), null, null, null)
            .use { if (it.moveToFirst()) readProfile(it) else null }
    }

    fun loadUserAccount(userId: String): PersistedUserAccount? {
        if (userId.isBlank()) return null
        return readableDatabase.query(TABLE_ASN_USERS, null, "user_id = ?", arrayOf(userId), null, null, null)
            .use { if (it.moveToFirst()) readPersistedAccount(it) else null }
    }

    fun loadUnsyncedUserAccounts(): List<PersistedUserAccount> {
        val list = mutableListOf<PersistedUserAccount>()
        readableDatabase.query(
            TABLE_ASN_USERS,
            null,
            "cloud_synced = 0",
            null,
            null,
            null,
            "updated_at ASC"
        ).use {
            while (it.moveToNext()) {
                list.add(readPersistedAccount(it))
            }
        }
        return list
    }

    fun markAccountCloudSynced(userId: String) {
        if (userId.isBlank()) return
        writableDatabase.update(
            TABLE_ASN_USERS,
            ContentValues().apply { put("cloud_synced", 1) },
            "user_id = ?",
            arrayOf(userId)
        )
    }

    /**
     * Memulihkan atau memperbarui data akun pengguna dari Cloud Database ke SQLite lokal
     * (misalnya saat pengguna login kembali setelah instal ulang aplikasi walaupun belum pernah skrining).
     */
    fun upsertRestoredAccount(account: PersistedUserAccount, makeActiveSession: Boolean = true) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (makeActiveSession) {
                db.update(TABLE_ASN_USERS, ContentValues().apply { put("is_active_session", 0) }, null, null)
            }
            val values = ContentValues().apply {
                put("user_id", account.userId)
                put("nip_clean", account.accountIdentifierClean)
                put("nip_formatted", account.accountIdentifierFormatted)
                put("full_name", account.profile.name.trim())
                put("jenis_asn", account.profile.jenisAsn)
                put("golongan", account.profile.golongan)
                put("instansi", account.profile.instansi.trim())
                put("jabatan", account.profile.jabatan.trim())
                put("birth_year", account.birthYear)
                put("password_hash", account.passwordHash)
                put("password_salt", account.passwordSalt)
                put("subject_hash", account.subjectHash)
                put("created_at", account.createdAt)
                put("updated_at", account.updatedAt)
                put("is_active_session", if (makeActiveSession) 1 else 0)
                put("cloud_synced", if (account.cloudSynced) 1 else 0)
            }
            db.insertWithOnConflict(TABLE_ASN_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun subjectHashFor(userId: String): String? {
        if (userId.isBlank()) return null
        return readableDatabase.query(
            TABLE_ASN_USERS, arrayOf("subject_hash"), "user_id = ?", arrayOf(userId), null, null, null
        ).use { if (it.moveToFirst()) it.getString(0) else null }
    }

    /**
     * Memperbarui data profil pengguna dan menandainya untuk disinkronkan ke database cloud (`cloud_synced = 0`).
     * ID Akun (`nip_clean`) tetap dipertahankan sebagai identitas login.
     */
    fun updateEditableProfile(userId: String, profile: UserProfile): Boolean {
        if (userId.isBlank()) return false
        val values = ContentValues().apply {
            put("full_name", profile.name.trim())
            put("instansi", profile.instansi.trim())
            put("jabatan", profile.jabatan.trim())
            put("jenis_asn", profile.jenisAsn)
            put("golongan", profile.golongan)
            put("updated_at", System.currentTimeMillis())
            put("cloud_synced", 0)
        }
        return writableDatabase.update(TABLE_ASN_USERS, values, "user_id = ?", arrayOf(userId)) > 0
    }

    // ---- Operasi Domain 2: Ringkasan Skrining Kualitatif (UI Layer) ----

    fun insertUserSummary(userId: String, result: ScreeningResult) {
        val values = ContentValues().apply {
            put("user_id", userId)
            put("score", result.score)
            put("risk_level", result.level.name)
            put("screening_date", result.date)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insertOrThrow(TABLE_USER_SUMMARIES, null, values)
    }

    fun loadUserHistory(userId: String): List<ScreeningResult> {
        val results = mutableListOf<ScreeningResult>()
        readableDatabase.query(
            TABLE_USER_SUMMARIES,
            arrayOf("score", "risk_level", "screening_date"),
            "user_id = ?", arrayOf(userId), null, null, "summary_id DESC"
        ).use {
            while (it.moveToNext()) {
                val level = runCatching { RiskLevel.valueOf(it.getString(1)) }.getOrDefault(RiskLevel.LOW)
                results.add(ScreeningResult(score = it.getInt(0), level = level, date = it.getString(2)))
            }
        }
        return results
    }

    // ---- Operasi Domain 3: Log Audit Sistem Pakar CF (Pseudonymized) ----

    fun insertResearchRecord(
        screeningId: String,
        subjectHash: String,
        kbVersion: String,
        timestamp: Long,
        hypertensionCf: Double,
        gbvCf: Double,
        integratedCategory: String,
        payloadJson: String
    ) {
        val values = ContentValues().apply {
            put("screening_id", screeningId)
            put("subject_hash", subjectHash)
            put("kb_version", kbVersion)
            put("timestamp", timestamp)
            put("hypertension_cf", hypertensionCf)
            put("gbv_cf", gbvCf)
            put("integrated_category", integratedCategory)
            put("payload_json", payloadJson)
        }
        writableDatabase.insertWithOnConflict(TABLE_RESEARCH_CF, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** Seluruh payload riset (untuk ekspor peneliti). Tidak memuat NIP — hanya subject_hash. */
    fun loadResearchPayloads(subjectHash: String? = null): List<String> {
        val payloads = mutableListOf<String>()
        readableDatabase.query(
            TABLE_RESEARCH_CF, arrayOf("payload_json"),
            if (subjectHash != null) "subject_hash = ?" else null,
            if (subjectHash != null) arrayOf(subjectHash) else null,
            null, null, "timestamp ASC"
        ).use { while (it.moveToNext()) payloads.add(it.getString(0)) }
        return payloads
    }

    // ---- Operasi Domain 4: Korpus Pengetahuan RAG ----

    /**
     * Menyimpan atau memperbarui chunk rujukan (termasuk hasil Live Internet RAG `WEB-WIKI-*` / `WEB-PMC-*`)
     * ke tabel `rag_knowledge_chunks` agar tetap tersedia secara offline dan saat riwayat chat dibuka kembali.
     */
    fun upsertRagChunks(chunks: List<RagDocumentChunk>) {
        if (chunks.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            chunks.forEach { chunk ->
                val values = ContentValues().apply {
                    put("chunk_id", chunk.chunkId)
                    put("doc_source", chunk.docSource)
                    put("topic_scope", chunk.topicScope.name)
                    put("title", chunk.title)
                    put("content_text", chunk.content)
                    put("keywords_csv", chunk.keywords.joinToString(","))
                    put("chunk_version", chunk.chunkVersion)
                }
                db.insertWithOnConflict(TABLE_RAG_CHUNKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadRagCorpus(): List<RagDocumentChunk> {
        val list = mutableListOf<RagDocumentChunk>()
        readableDatabase.query(TABLE_RAG_CHUNKS, null, null, null, null, null, "chunk_id ASC").use {
            while (it.moveToNext()) {
                val scope = runCatching {
                    RagTopicScope.valueOf(it.getString(it.getColumnIndexOrThrow("topic_scope")))
                }.getOrDefault(RagTopicScope.HYPERTENSION_CLINICAL)
                list.add(
                    RagDocumentChunk(
                        chunkId = it.getString(it.getColumnIndexOrThrow("chunk_id")),
                        docSource = it.getString(it.getColumnIndexOrThrow("doc_source")),
                        topicScope = scope,
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        content = it.getString(it.getColumnIndexOrThrow("content_text")),
                        keywords = it.getString(it.getColumnIndexOrThrow("keywords_csv"))
                            .split(",").map { kw -> kw.trim() }.filter { kw -> kw.isNotEmpty() },
                        chunkVersion = it.getString(it.getColumnIndexOrThrow("chunk_version"))
                    )
                )
            }
        }
        return list.ifEmpty { RagKnowledgeRepository.defaultCorpus }
    }

    fun insertChatLog(
        userId: String,
        isUser: Boolean,
        messageText: String,
        retrievedChunkIds: List<String> = emptyList()
    ) {
        val values = ContentValues().apply {
            put("user_id", userId)
            put("is_user", if (isUser) 1 else 0)
            put("message_text", messageText)
            put("retrieved_chunk_ids", retrievedChunkIds.joinToString(","))
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insertOrThrow(TABLE_RAG_CHAT_LOGS, null, values)
    }

    fun loadChatLogs(userId: String): List<ChatMessage> {
        val corpusById = loadRagCorpus().associateBy { it.chunkId }
        val list = mutableListOf<ChatMessage>()
        readableDatabase.query(
            TABLE_RAG_CHAT_LOGS,
            arrayOf("is_user", "message_text", "retrieved_chunk_ids"),
            "user_id = ?",
            arrayOf(userId),
            null, null,
            "log_id ASC"
        ).use {
            while (it.moveToNext()) {
                val isUser = it.getInt(0) == 1
                val text = it.getString(1)
                val rawChunkIds = if (it.isNull(2)) "" else it.getString(2)
                val chunks = rawChunkIds
                    .split(",")
                    .mapNotNull { id -> corpusById[id.trim()] }
                list.add(
                    ChatMessage(
                        isUser = isUser,
                        text = text,
                        retrievedChunks = chunks
                    )
                )
            }
        }
        return list
    }

    // ---- Penghapusan Data ----

    /** Menghapus data milik SATU pengguna saja (bukan seluruh pengguna di perangkat). */
    fun deleteUserData(userId: String) {
        if (userId.isBlank()) return
        val db = writableDatabase
        val subjectHash = subjectHashFor(userId)
        db.beginTransaction()
        try {
            db.delete(TABLE_USER_SUMMARIES, "user_id = ?", arrayOf(userId))
            db.delete(TABLE_RAG_CHAT_LOGS, "user_id = ?", arrayOf(userId))
            if (subjectHash != null) {
                db.delete(TABLE_RESEARCH_CF, "subject_hash = ?", arrayOf(subjectHash))
            }
            db.delete(TABLE_ASN_USERS, "user_id = ?", arrayOf(userId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Jumlah baris per tabel — dipakai untuk verifikasi/debug. */
    fun tableRowCounts(): Map<String, Long> {
        val db = readableDatabase
        return listOf(
            TABLE_ASN_USERS, TABLE_USER_SUMMARIES, TABLE_RESEARCH_CF, TABLE_RAG_CHUNKS, TABLE_RAG_CHAT_LOGS
        ).associateWith { android.database.DatabaseUtils.queryNumEntries(db, it) }
    }

    companion object {
        const val DB_NAME = "empati_multi_domain.db"
        const val DB_VERSION = 4
        const val CURRENT_KB_VERSION = "cf-kb-v2.0"

        internal const val LEGACY_DEMO_USER_ID = "asn_demo_ratna"
        internal const val LEGACY_DEMO_ACCOUNT_ID = "ratna.andari@empati.id"

        const val TABLE_ASN_USERS = "auth_asn_users"
        const val TABLE_USER_SUMMARIES = "user_screening_summaries"
        const val TABLE_RESEARCH_CF = "research_cf_records"
        const val TABLE_RAG_CHUNKS = "rag_knowledge_chunks"
        const val TABLE_RAG_CHAT_LOGS = "rag_chat_logs"
    }
}
