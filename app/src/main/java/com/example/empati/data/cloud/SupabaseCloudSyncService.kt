package com.example.empati.data.cloud

import com.example.empati.data.RiskLevel
import com.example.empati.data.ScreeningResult
import com.example.empati.data.UserProfile
import com.example.empati.data.auth.AsnNipValidator
import com.example.empati.data.auth.NipValidationResult
import com.example.empati.data.db.EmpatiDatabase
import com.example.empati.data.db.PersistedUserAccount
import com.example.empati.data.rag.OllamaRagService
import com.example.empati.data.research.ResearchRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Layanan sinkronisasi Cloud Supabase (`empati-research`) untuk persistensi akun pengguna,
 * keperluan riset disertasi, dan proxy Edge Function OpenRouter (`empati-chat`).
 *
 * Prinsip Keamanan & Privasi:
 * - Tabel `public.user_accounts` menyimpan akun & profil pengguna dengan kredensial ter-hash
 *   (`PBKDF2-HMAC-SHA256` + `salt`) agar akun dan profil tetap tersimpan di database walaupun
 *   pengguna belum melakukan skrining.
 * - Tabel `public.research_cf_records` (Zero-PII Leak) hanya menerima `subject_hash` satu arah
 *   (`sub_<20-hex>`), demografi umum, dan hasil matematis Certainty Factor (`hypertension_cf`, `gbv_cf`, `payload_json`)
 *   tanpa pernah mengirimkan nama lengkap, email, nomor HP, NIK, maupun NIP mentah ke tabel riset.
 */
object SupabaseCloudSyncService {

    const val PROJECT_ID = "qnvcuhmvrrffzdxxdloc"
    const val SUPABASE_URL = "https://qnvcuhmvrrffzdxxdloc.supabase.co"
    const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_eF1IqgzAvjXf_NVZUPTFZg_fiOrLgn7"
    const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFudmN1aG12cnJmZnpkeHhkbG9jIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQxMzg1MTYsImV4cCI6MjEwOTcxNDUxNn0.4FS2HCFdpK-0bQoNhqMPXjgdXVVxBK-5abSEjpl_NeU"

    const val REST_USER_ACCOUNTS_ENDPOINT = "$SUPABASE_URL/rest/v1/user_accounts"
    const val REST_RESEARCH_CF_ENDPOINT = "$SUPABASE_URL/rest/v1/research_cf_records"
    const val REST_RESEARCH_CHAT_ENDPOINT = "$SUPABASE_URL/rest/v1/research_chat_logs"
    const val EDGE_CHAT_ENDPOINT = "$SUPABASE_URL/functions/v1/empati-chat"

    /**
     * Menyusun payload JSON untuk tabel `public.user_accounts` agar akun, kredensial ter-hash,
     * dan profil demografi pengguna tersimpan di database walaupun belum melakukan skrining.
     */
    fun buildUserAccountJsonPayload(account: PersistedUserAccount): String {
        val gender = account.profile.golongan.ifBlank { "Perempuan" }
        val occupationCategory = account.profile.jenisAsn.ifBlank { "Masyarakat Umum" }
        val domicile = account.profile.instansi.ifBlank { "-" }
        val occupation = account.profile.jabatan.ifBlank { "-" }

        return buildString {
            append('{')
            append("\"user_id\":\"").append(escapeJson(account.userId)).append("\",")
            append("\"account_identifier\":\"").append(escapeJson(account.accountIdentifierClean)).append("\",")
            append("\"account_display\":\"").append(escapeJson(account.accountIdentifierFormatted)).append("\",")
            append("\"subject_hash\":\"").append(escapeJson(account.subjectHash)).append("\",")
            append("\"full_name\":\"").append(escapeJson(account.profile.name.trim())).append("\",")
            append("\"gender\":\"").append(escapeJson(gender)).append("\",")
            append("\"occupation_category\":\"").append(escapeJson(occupationCategory)).append("\",")
            append("\"domicile\":\"").append(escapeJson(domicile)).append("\",")
            append("\"occupation\":\"").append(escapeJson(occupation)).append("\",")
            append("\"birth_year\":").append(account.birthYear).append(',')
            append("\"password_hash\":\"").append(escapeJson(account.passwordHash)).append("\",")
            append("\"password_salt\":\"").append(escapeJson(account.passwordSalt)).append("\",")
            append("\"created_at_ms\":").append(account.createdAt).append(',')
            append("\"updated_at_ms\":").append(account.updatedAt)
            append('}')
        }
    }

    /**
     * Menyusun baseline demografi terpseudonimisasi (Zero-PII) pada `public.research_cf_records`
     * untuk pengguna yang baru mendaftar atau memperbarui profil namun belum melakukan skrining,
     * sehingga profil subjek tetap tercatat di database riset.
     */
    fun buildAccountBaselineResearchPayload(account: PersistedUserAccount): String {
        val gender = account.profile.golongan.ifBlank { "Perempuan" }
        val occupationCategory = account.profile.jenisAsn.ifBlank { "Masyarakat Umum" }
        val domicile = account.profile.instansi.ifBlank { "-" }
        val occupation = account.profile.jabatan.ifBlank { "-" }
        val baselineScreeningId = "acct_${account.subjectHash}"
        val dateLabel = runCatching {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.forLanguageTag("id-ID"))
                .format(java.util.Date(account.updatedAt.takeIf { it > 0L } ?: account.createdAt))
        }.getOrDefault("-")
        val nestedPayload = buildString {
            append('{')
            append("\"screeningId\":\"").append(escapeJson(baselineScreeningId)).append("\",")
            append("\"accountRegistered\":true,")
            append("\"hasCompletedScreening\":false,")
            append("\"updatedAt\":").append(account.updatedAt)
            append('}')
        }

        return buildString {
            append('{')
            append("\"screening_id\":\"").append(escapeJson(baselineScreeningId)).append("\",")
            append("\"subject_hash\":\"").append(escapeJson(account.subjectHash)).append("\",")
            append("\"kb_version\":\"").append(escapeJson(EmpatiDatabase.CURRENT_KB_VERSION)).append("\",")
            append("\"gender\":\"").append(escapeJson(gender)).append("\",")
            append("\"occupation_category\":\"").append(escapeJson(occupationCategory)).append("\",")
            append("\"domicile\":\"").append(escapeJson(domicile)).append("\",")
            append("\"occupation\":\"").append(escapeJson(occupation)).append("\",")
            append("\"birth_year\":").append(account.birthYear).append(',')
            append("\"score\":0,")
            append("\"risk_level\":\"LOW\",")
            append("\"hypertension_cf\":0.0,")
            append("\"gbv_cf\":0.0,")
            append("\"integrated_category\":\"ACCOUNT_REGISTERED_NO_SCREENING\",")
            append("\"screening_date\":\"").append(escapeJson(dateLabel)).append("\",")
            append("\"payload_json\":").append(nestedPayload).append(',')
            append("\"client_timestamp\":").append(account.updatedAt.takeIf { it > 0L } ?: account.createdAt)
            append('}')
        }
    }

    /**
     * Menyinkronkan akun & profil pengguna ke database cloud Supabase walaupun pengguna belum melakukan skrining.
     * Mengembalikan `true` hanya apabila sinkronisasi ke tabel utama `public.user_accounts` berhasil,
     * sehingga flag `cloud_synced` di SQLite lokal tidak pernah ditandai selesai jika `user_accounts` gagal.
     */
    fun syncUserAccount(
        account: PersistedUserAccount,
        timeoutMs: Int = 8_000
    ): Boolean {
        val accountPayload = buildUserAccountJsonPayload(account)
        val userTableSynced = postToSupabaseRest(
            endpointUrl = "$REST_USER_ACCOUNTS_ENDPOINT?on_conflict=account_identifier",
            jsonBody = accountPayload,
            preferHeader = "resolution=merge-duplicates,return=minimal",
            timeoutMs = timeoutMs
        )
        val baselinePayload = buildAccountBaselineResearchPayload(account)
        val baselineUpserted = postToSupabaseRest(
            endpointUrl = "$REST_RESEARCH_CF_ENDPOINT?on_conflict=screening_id",
            jsonBody = baselinePayload,
            preferHeader = "resolution=merge-duplicates,return=minimal",
            timeoutMs = timeoutMs
        )
        if (!baselineUpserted) {
            postToSupabaseRest(
                endpointUrl = REST_RESEARCH_CF_ENDPOINT,
                jsonBody = baselinePayload,
                preferHeader = "return=minimal",
                timeoutMs = timeoutMs
            )
        }
        return userTableSynced
    }

    /**
     * Mengambil akun pengguna dari tabel `public.user_accounts` di Supabase berdasarkan `account_identifier`
     * (berguna untuk memulihkan akun saat login di perangkat baru atau setelah instal ulang aplikasi).
     * Dijalankan pada background worker thread agar aman dipanggil dari UI thread Android tanpa
     * memicu `NetworkOnMainThreadException`.
     */
    fun fetchUserAccountByIdentifier(
        cleanIdentifier: String,
        timeoutMs: Int = 8_000
    ): PersistedUserAccount? {
        if (cleanIdentifier.length < 4) return null
        var fetched: PersistedUserAccount? = null
        val worker = Thread {
            fetched = runCatching {
                val encoded = URLEncoder.encode(cleanIdentifier, StandardCharsets.UTF_8.name())
                val url = URL("$REST_USER_ACCOUNTS_ENDPOINT?account_identifier=eq.$encoded&limit=1")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5_000
                    readTimeout = timeoutMs
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "SRYA-Emora-Android/4.1.3")
                }
                if (conn.responseCode !in 200..299) return@runCatching null
                val body = BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8))
                    .use { it.readText() }
                val arr = JSONArray(body)
                if (arr.length() == 0) return@runCatching null
                val obj = arr.getJSONObject(0)
                PersistedUserAccount(
                    userId = obj.getString("user_id"),
                    accountIdentifierClean = obj.getString("account_identifier"),
                    accountIdentifierFormatted = obj.optString("account_display", obj.getString("account_identifier")),
                    profile = UserProfile(
                        name = obj.optString("full_name", ""),
                        nip = obj.optString("account_display", obj.getString("account_identifier")),
                        instansi = obj.optString("domicile", ""),
                        jabatan = obj.optString("occupation", ""),
                        jenisAsn = obj.optString("occupation_category", "Masyarakat Umum"),
                        golongan = obj.optString("gender", "Perempuan")
                    ),
                    birthYear = obj.optInt("birth_year", 0),
                    passwordHash = obj.getString("password_hash"),
                    passwordSalt = obj.optString("password_salt", ""),
                    subjectHash = obj.getString("subject_hash"),
                    createdAt = obj.optLong("created_at_ms", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updated_at_ms", System.currentTimeMillis()),
                    cloudSynced = true
                )
            }.getOrNull()
        }.apply {
            isDaemon = true
            start()
        }
        runCatching { worker.join(timeoutMs.toLong() + 2_000L) }
        return fetched
    }

    /**
     * Menghapus data akun pengguna dari tabel `public.user_accounts` di Cloud Supabase
     * ketika pengguna memilih "Hapus seluruh data akun & riwayat saya".
     */
    fun deleteUserAccount(
        userId: String,
        subjectHash: String? = null,
        timeoutMs: Int = 8_000
    ): Boolean {
        if (userId.isBlank() && subjectHash.isNullOrBlank()) return false
        return runCatching {
            val filterQuery = if (userId.isNotBlank()) {
                "user_id=eq.${URLEncoder.encode(userId, StandardCharsets.UTF_8.name())}"
            } else {
                "subject_hash=eq.${URLEncoder.encode(subjectHash.orEmpty(), StandardCharsets.UTF_8.name())}"
            }
            val url = URL("$REST_USER_ACCOUNTS_ENDPOINT?$filterQuery")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 5_000
                readTimeout = timeoutMs
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Prefer", "return=minimal")
                setRequestProperty("User-Agent", "SRYA-Emora-Android/4.1.3")
            }
            conn.responseCode in 200..299
        }.getOrDefault(false)
    }

    /**
     * Menyusun payload JSON string untuk tabel `public.research_cf_records` di Supabase Postgres.
     * Memastikan seluruh atribut identitas pribadi (nama, email, HP, NIP/NIK mentah) TIDAK disertakan.
     */
    fun buildResearchRecordJsonPayload(
        record: ResearchRecord,
        subjectHash: String,
        profile: UserProfile,
        summary: ScreeningResult?,
        rawPayloadJson: String = "{\"screeningId\":\"${escapeJson(record.screeningId)}\"}"
    ): String {
        val derivedScore = (summary?.score
            ?: (record.hypertensionCf.coerceIn(0.0, 1.0) * 100.0).toInt()).coerceIn(0, 100)
        val derivedRiskLevel = summary?.level?.name ?: when (record.hypertension.level.uppercase()) {
            "SANGAT_TINGGI" -> RiskLevel.VERY_HIGH.name
            "TINGGI" -> RiskLevel.HIGH.name
            "SEDANG" -> RiskLevel.MEDIUM.name
            "RENDAH" -> RiskLevel.LOW.name
            else -> when {
                record.hypertensionCf >= 0.75 -> RiskLevel.VERY_HIGH.name
                record.hypertensionCf >= 0.50 -> RiskLevel.HIGH.name
                record.hypertensionCf >= 0.25 -> RiskLevel.MEDIUM.name
                else -> RiskLevel.LOW.name
            }
        }
        val screeningDate = summary?.date ?: runCatching {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.forLanguageTag("id-ID"))
                .format(java.util.Date(record.timestamp))
        }.getOrDefault("-")
        val birthYear = (AsnNipValidator.validateGeneralIdentity(profile.nip) as? NipValidationResult.Valid)
            ?.metadata?.birthYear ?: 0
        val gender = profile.golongan.ifBlank { "Perempuan" }
        val occupationCategory = profile.jenisAsn.ifBlank { "Masyarakat Umum" }
        val domicile = profile.instansi.ifBlank { "-" }
        val occupation = profile.jabatan.ifBlank { "-" }
        val hCf = record.hypertensionCf.coerceIn(-1.0, 1.0)
        val gCf = record.gbvCf.coerceIn(-1.0, 1.0)
        val safeNestedPayload = rawPayloadJson.trim().let {
            if (it.startsWith("{") && it.endsWith("}")) it else "{\"screeningId\":\"${escapeJson(record.screeningId)}\"}"
        }

        return buildString {
            append('{')
            append("\"screening_id\":\"").append(escapeJson(record.screeningId)).append("\",")
            append("\"subject_hash\":\"").append(escapeJson(subjectHash)).append("\",")
            append("\"kb_version\":\"").append(escapeJson(EmpatiDatabase.CURRENT_KB_VERSION)).append("\",")
            append("\"gender\":\"").append(escapeJson(gender)).append("\",")
            append("\"occupation_category\":\"").append(escapeJson(occupationCategory)).append("\",")
            append("\"domicile\":\"").append(escapeJson(domicile)).append("\",")
            append("\"occupation\":\"").append(escapeJson(occupation)).append("\",")
            append("\"birth_year\":").append(birthYear).append(',')
            append("\"score\":").append(derivedScore).append(',')
            append("\"risk_level\":\"").append(escapeJson(derivedRiskLevel)).append("\",")
            append("\"hypertension_cf\":").append(hCf).append(',')
            append("\"gbv_cf\":").append(gCf).append(',')
            append("\"integrated_category\":\"").append(escapeJson(record.integratedCategory)).append("\",")
            append("\"screening_date\":\"").append(escapeJson(screeningDate)).append("\",")
            append("\"payload_json\":").append(safeNestedPayload).append(',')
            append("\"client_timestamp\":").append(record.timestamp)
            append('}')
        }
    }

    /**
     * Mengunggah satu rekam skrining CF ke tabel `public.research_cf_records` di Supabase.
     * Mendukung upsert idempoten (`?on_conflict=screening_id`) dengan fallback ke `INSERT` murni.
     */
    fun syncResearchRecord(
        rowJsonPayload: String,
        timeoutMs: Int = 10_000
    ): Boolean {
        val upserted = postToSupabaseRest(
            endpointUrl = "$REST_RESEARCH_CF_ENDPOINT?on_conflict=screening_id",
            jsonBody = rowJsonPayload,
            preferHeader = "resolution=merge-duplicates,return=minimal",
            timeoutMs = timeoutMs
        )
        if (upserted) return true
        return postToSupabaseRest(
            endpointUrl = REST_RESEARCH_CF_ENDPOINT,
            jsonBody = rowJsonPayload,
            preferHeader = "return=minimal",
            timeoutMs = timeoutMs
        )
    }

    /**
     * Mengunggah log percakapan terpseudonimisasi (sudah disanitasi dari NIP/NIK/Email/No.HP)
     * ke tabel `public.research_chat_logs` di Supabase.
     */
    fun syncChatLog(
        subjectHash: String,
        isUser: Boolean,
        rawMessage: String,
        modelName: String,
        timestampMs: Long = System.currentTimeMillis(),
        timeoutMs: Int = 8_000
    ): Boolean {
        val sanitizedMessage = OllamaRagService.sanitizePrivacyInput(rawMessage).take(7500)
        if (sanitizedMessage.isBlank() || !subjectHash.startsWith("sub_")) return false

        val safeModel = modelName.ifBlank { "openrouter-llm" }
        val jsonBody = buildString {
            append('{')
            append("\"subject_hash\":\"").append(escapeJson(subjectHash)).append("\",")
            append("\"is_user\":").append(isUser).append(',')
            append("\"message_text\":\"").append(escapeJson(sanitizedMessage)).append("\",")
            append("\"model_name\":\"").append(escapeJson(safeModel)).append("\",")
            append("\"client_timestamp\":").append(timestampMs)
            append('}')
        }

        return postToSupabaseRest(
            endpointUrl = REST_RESEARCH_CHAT_ENDPOINT,
            jsonBody = jsonBody,
            preferHeader = "return=minimal",
            timeoutMs = timeoutMs
        )
    }

    internal fun postToSupabaseRest(
        endpointUrl: String,
        jsonBody: String,
        preferHeader: String,
        timeoutMs: Int
    ): Boolean {
        return runCatching {
            val url = URL(endpointUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = timeoutMs
                doOutput = true
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Prefer", preferHeader)
                setRequestProperty("User-Agent", "SRYA-Emora-Android/4.1.3")
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }
            val code = conn.responseCode
            // 201 Created = berhasil masuk; 409 Conflict = screening_id tersebut sudah tercatat sebelumnya di cloud
            code in 200..299 || code == 409
        }.getOrDefault(false)
    }

    /**
     * Memanggil Supabase Edge Function `empati-chat` yang bertindak sebagai proxy aman ke OpenRouter.
     */
    fun callEdgeChatFunction(
        systemPrompt: String,
        userPrompt: String,
        preferredModel: String,
        openRouterApiKeyOverride: String = "",
        timeoutMs: Int = 25_000
    ): Pair<String, String>? {
        return runCatching {
            val url = URL(EDGE_CHAT_ENDPOINT)
            val payload = buildString {
                append('{')
                append("\"model\":\"").append(escapeJson(preferredModel)).append("\",")
                append("\"systemPrompt\":\"").append(escapeJson(systemPrompt)).append("\",")
                append("\"userPrompt\":\"").append(escapeJson(userPrompt)).append('"')
                append('}')
            }

            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = timeoutMs
                doOutput = true
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (openRouterApiKeyOverride.isNotBlank()) {
                    setRequestProperty("x-openrouter-key", openRouterApiKeyOverride.trim())
                }
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(payload)
                writer.flush()
            }
            if (conn.responseCode !in 200..299) return null
            val responseBody = BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8))
                .use { it.readText() }
            val content = OllamaRagService.extractAssistantContentFromJson(responseBody)
            if (content.isNullOrBlank()) return null
            val modelUsed = runCatching {
                JSONObject(responseBody).optString("model", preferredModel)
            }.getOrDefault(preferredModel)
            content to modelUsed
        }.getOrNull()
    }

    internal fun escapeJson(text: String): String = buildString(text.length + 16) {
        for (ch in text) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
}
