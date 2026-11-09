package com.example.empati.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Kategori latar belakang / aktivitas pengguna secara umum (Inklusif Semua Kalangan).
 * Tetap mempertahankan enum lama sebagai alias agar kompatibel dengan seluruh modul.
 */
enum class UserOccupationCategory(val label: String) {
    GENERAL("Masyarakat Umum"),
    PRIVATE_EMPLOYEE("Karyawan / Pekerja"),
    ENTREPRENEUR("Wiraswasta / UMKM"),
    STUDENT("Mahasiswa / Pelajar"),
    HOMEMAKER("Ibu Rumah Tangga"),
    ASN_PUBLIC("ASN / Instansi Publik"),
    HEALTH_EDUCATION("Tenaga Medis / Pendidik")
}

enum class UserGenderOption(val label: String) {
    FEMALE("Perempuan"),
    MALE("Laki-laki")
}

enum class AsnEmploymentType(val label: String) {
    PNS("PNS"),
    PPPK("PPPK")
}

data class AsnNipMetadata(
    val cleanNip: String,
    val formattedNip: String,
    val birthYear: Int,
    val birthMonth: Int,
    val birthDay: Int,
    val appointmentYear: Int,
    val appointmentMonth: Int,
    val genderCode: Int,
    val sequenceNumber: String,
    val identityKind: String = "ID Pengguna Umum"
) {
    val isFemaleAsn: Boolean get() = genderCode == 2
    val genderLabel: String
        get() = when (genderCode) {
            1 -> "Laki-laki"
            2 -> "Perempuan"
            else -> "Umum"
        }
    val birthDateFormatted: String
        get() = "%02d/%02d/%04d".format(birthDay, birthMonth, birthYear)
    val appointmentFormatted: String
        get() = "%02d/%04d".format(appointmentMonth, appointmentYear)
}

sealed interface NipValidationResult {
    data class Valid(val metadata: AsnNipMetadata) : NipValidationResult
    data class Invalid(val reason: String) : NipValidationResult
}

/**
 * Validator Identitas Inklusif (Semua Kalangan):
 * - Mendukung ID Akun Umum: Email, Nomor HP, Username, NIK (16 digit), maupun NIP (18 digit).
 * - Menyediakan `validateGeneralIdentity(rawInput)` untuk login/registrasi semua kalangan.
 * - Tetap mempertahankan `validate(rawNip, requireFemaleAsn)` untuk kompatibilitas pengujian struktur NIP.
 */
object AsnNipValidator {

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val phoneRegex = Regex("^(\\+62|62|0)8[0-9]{7,12}$")
    private val usernameRegex = Regex("^[A-Za-z0-9._-]{4,40}$")

    /**
     * Menormalisasi ID akun pengguna (Email, Nomor HP, Username, NIK, atau NIP):
     * - Jika hanya berisi angka dan spasi/tanda hubung (mis. No. HP, NIK, NIP), ambil digitnya saja.
     * - Jika berupa Email atau Username, trim dan ubah menjadi huruf kecil (lowercase).
     */
    fun normalizeAccountId(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ""
        val digitsOnly = trimmed.filter { it.isDigit() }
        val nonDigitChars = trimmed.filter { !it.isDigit() && !it.isWhitespace() && it != '-' && it != '+' }
        return if (nonDigitChars.isEmpty() && digitsOnly.length >= 8) {
            digitsOnly
        } else {
            trimmed.lowercase(Locale.ROOT)
        }
    }

    /**
     * Validasi identitas untuk SEMUA KALANGAN (Email, Nomor HP, Username, NIK 16 digit, atau NIP 18 digit).
     * Tidak membatasi jenis kelamin maupun profesi.
     */
    fun validateGeneralIdentity(rawInput: String): NipValidationResult {
        val trimmed = rawInput.trim()
        if (trimmed.length < 4) {
            return NipValidationResult.Invalid(
                "ID Akun minimal 4 karakter (dapat berupa Email, Nomor HP, Username, NIK, atau NIP)."
            )
        }

        val digitsOnly = trimmed.filter { it.isDigit() }
        val hasLetters = trimmed.any { it.isLetter() }

        // 1. Jika murni 18 digit angka, coba urai sebagai NIP (menerima Laki-laki maupun Perempuan)
        if (!hasLetters && digitsOnly.length == 18) {
            val nipResult = validate(digitsOnly, requireFemaleAsn = false)
            if (nipResult is NipValidationResult.Valid) {
                return nipResult
            }
        }

        // 2. Jika murni 16 digit angka (NIK KTP)
        if (!hasLetters && digitsOnly.length == 16) {
            return NipValidationResult.Valid(
                AsnNipMetadata(
                    cleanNip = digitsOnly,
                    formattedNip = digitsOnly,
                    birthYear = 1990,
                    birthMonth = 1,
                    birthDay = 1,
                    appointmentYear = 2020,
                    appointmentMonth = 1,
                    genderCode = 0,
                    sequenceNumber = digitsOnly.takeLast(4),
                    identityKind = "NIK 16 Digit"
                )
            )
        }

        // 3. Jika Email
        if (emailRegex.matches(trimmed)) {
            val clean = trimmed.lowercase(Locale.ROOT)
            return NipValidationResult.Valid(
                AsnNipMetadata(
                    cleanNip = clean,
                    formattedNip = clean,
                    birthYear = 1990,
                    birthMonth = 1,
                    birthDay = 1,
                    appointmentYear = 2020,
                    appointmentMonth = 1,
                    genderCode = 0,
                    sequenceNumber = "000",
                    identityKind = "Alamat Email"
                )
            )
        }

        // 4. Jika Nomor HP / WhatsApp Indonesia
        val normalizedPhone = trimmed.replace(" ", "").replace("-", "")
        if (phoneRegex.matches(normalizedPhone)) {
            return NipValidationResult.Valid(
                AsnNipMetadata(
                    cleanNip = digitsOnly,
                    formattedNip = normalizedPhone,
                    birthYear = 1990,
                    birthMonth = 1,
                    birthDay = 1,
                    appointmentYear = 2020,
                    appointmentMonth = 1,
                    genderCode = 0,
                    sequenceNumber = digitsOnly.takeLast(3),
                    identityKind = "Nomor Telepon / HP"
                )
            )
        }

        // 5. Username / ID Umum (4-40 karakter alfanumerik)
        if (usernameRegex.matches(trimmed) || (!hasLetters && digitsOnly.length in 6..20)) {
            val clean = normalizeAccountId(trimmed)
            return NipValidationResult.Valid(
                AsnNipMetadata(
                    cleanNip = clean,
                    formattedNip = trimmed,
                    birthYear = 1990,
                    birthMonth = 1,
                    birthDay = 1,
                    appointmentYear = 2020,
                    appointmentMonth = 1,
                    genderCode = 0,
                    sequenceNumber = "001",
                    identityKind = "Username / ID Akun"
                )
            )
        }

        return NipValidationResult.Invalid(
            "Format ID Akun tidak valid. Gunakan Email, Nomor HP, Username (tanpa spasi), NIK, atau NIP."
        )
    }

    /**
     * Memvalidasi struktur 18 digit NIP (tetap dipertahankan untuk kompatibilitas modul riset & unit test).
     * Secara default `requireFemaleAsn = false` agar mendukung semua kalangan.
     */
    fun validate(rawNip: String, requireFemaleAsn: Boolean = false): NipValidationResult {
        val digits = rawNip.filter { it.isDigit() }
        if (digits.length != 18) {
            return NipValidationResult.Invalid(
                "NIP harus terdiri dari 18 digit angka (atau gunakan Email / Nomor HP / Username)."
            )
        }

        val birthYear = digits.substring(0, 4).toIntOrNull() ?: 0
        val birthMonth = digits.substring(4, 6).toIntOrNull() ?: 0
        val birthDay = digits.substring(6, 8).toIntOrNull() ?: 0
        val appYear = digits.substring(8, 12).toIntOrNull() ?: 0
        val appMonth = digits.substring(12, 14).toIntOrNull() ?: 0
        val genderCode = digits.substring(14, 15).toIntOrNull() ?: 0
        val seq = digits.substring(15, 18)

        if (birthYear !in 1945..2015 || birthMonth !in 1..12 || birthDay !in 1..31) {
            return NipValidationResult.Invalid(
                "8 digit pertama NIP (tanggal lahir YYYYMMDD) tidak valid."
            )
        }

        if (appYear !in 1965..2035 || appMonth !in 1..12 || appYear < birthYear + 17) {
            return NipValidationResult.Invalid(
                "6 digit tengah NIP (TMT pengangkatan YYYYMM) tidak valid."
            )
        }

        if (genderCode != 1 && genderCode != 2) {
            return NipValidationResult.Invalid(
                "Digit ke-15 NIP (kode jenis kelamin) harus bernilai 1 (Laki-laki) atau 2 (Perempuan)."
            )
        }

        if (requireFemaleAsn && genderCode != 2) {
            return NipValidationResult.Invalid(
                "Untuk filter kohort khusus Perempuan ASN, digit ke-15 NIP harus bernilai 2."
            )
        }

        val formatted = "${digits.substring(0, 8)} ${digits.substring(8, 14)} $genderCode $seq"
        return NipValidationResult.Valid(
            AsnNipMetadata(
                cleanNip = digits,
                formattedNip = formatted,
                birthYear = birthYear,
                birthMonth = birthMonth,
                birthDay = birthDay,
                appointmentYear = appYear,
                appointmentMonth = appMonth,
                genderCode = genderCode,
                sequenceNumber = seq,
                identityKind = "NIP 18 Digit"
            )
        )
    }

    fun formatNipInput(rawNip: String): String {
        val trimmed = rawNip.trim()
        if (trimmed.any { it.isLetter() || it == '@' }) return trimmed
        val d = trimmed.filter { it.isDigit() }
        if (d.length != 18) return trimmed
        return buildString {
            for (i in d.indices) {
                if (i == 8 || i == 14 || i == 15) append(' ')
                append(d[i])
            }
        }
    }
}

object AsnSecurityHasher {
    private const val PSEUDONYM_NAMESPACE = "empati-asn-research-v2:"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun newSaltHex(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    /** PBKDF2-HMAC-SHA256 dengan salt acak per pengguna (tahan brute-force PIN/kata sandi). */
    fun hashCredential(pinOrPassword: String, saltHex: String): String {
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(pinOrPassword.trim().toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return key.joinToString("") { "%02x".format(it) }
    }

    /** Perbandingan waktu-konstan untuk mencegah timing attack. */
    fun verifyCredential(pinOrPassword: String, saltHex: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(
            hashCredential(pinOrPassword, saltHex).toByteArray(),
            expectedHash.toByteArray()
        )

    /**
     * Menghasilkan pseudonim satu arah (subject_hash) dari ID akun apa pun (Email, No. HP, Username, NIK, atau NIP)
     * agar tabel penelitian CF dan KBG tidak pernah menyimpan identitas pribadi pengguna secara mentah.
     */
    fun deriveSubjectHash(rawIdentifier: String): String {
        val normalized = AsnNipValidator.normalizeAccountId(rawIdentifier)
        return "sub_" + sha256Hex(PSEUDONYM_NAMESPACE + normalized).take(20)
    }
}
