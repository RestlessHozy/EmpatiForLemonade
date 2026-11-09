package com.example.empati

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.empati.data.RiskLevel
import com.example.empati.data.ScreeningResult
import com.example.empati.data.ScreeningStore
import com.example.empati.data.UserProfile
import com.example.empati.data.db.EmpatiDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Menguji SQLite SUNGGUHAN (in-memory) di perangkat/emulator:
 * registrasi → tersimpan → login → riwayat per-pengguna → isolasi → hapus data.
 *
 * Jalankan: ./gradlew connectedDebugAndroidTest (butuh emulator / HP via USB debugging).
 */
@RunWith(AndroidJUnit4::class)
class EmpatiDatabaseInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: EmpatiDatabase
    private lateinit var store: ScreeningStore

    private val newAsn = UserProfile(
        name = "Sari Wulandari, S.Kep",
        nip = "199204152018012003",
        instansi = "Dinkes Kota Bandung",
        jabatan = "Perawat Ahli Pertama",
        jenisAsn = "PNS",
        golongan = "III/a"
    )

    private val secondUser = UserProfile(
        name = "Budi Santoso",
        nip = "budi.santoso@email.id",
        instansi = "Jakarta",
        jabatan = "Wiraswasta",
        jenisAsn = "Wiraswasta / UMKM",
        golongan = "Laki-laki"
    )

    @Before
    fun setUp() {
        context.getSharedPreferences("empati_test", 0).edit().clear().commit()
        db = EmpatiDatabase(context, dbName = null) // in-memory, tidak menyentuh data asli
        store = ScreeningStore(context, db, prefsName = "empati_test")
    }

    @After
    fun tearDown() {
        db.close()
        context.getSharedPreferences("empati_test", 0).edit().clear().commit()
    }

    @Test
    fun databaseStartsWithoutDemoAccountAndWithRagCorpus() {
        val counts = db.tableRowCounts()
        assertEquals(0L, counts[EmpatiDatabase.TABLE_ASN_USERS])
        assertEquals(5L, counts[EmpatiDatabase.TABLE_RAG_CHUNKS])
        assertEquals(0L, counts[EmpatiDatabase.TABLE_USER_SUMMARIES])
    }

    @Test
    fun registerPersistsUserAccountWithoutScreeningAndLoginSucceedsWithCorrectPin() {
        val registered = store.registerAsn(newAsn, "rahasia99")
        assertTrue(registered.isSuccess)
        assertEquals(1L, db.tableRowCounts()[EmpatiDatabase.TABLE_ASN_USERS])
        assertTrue("Akun tetap tersimpan walau belum melakukan skrining", store.loadHistory().isEmpty())

        // Perbarui profil tanpa melakukan skrining
        store.saveProfile(newAsn.copy(instansi = "RSUD Kota Bandung", jabatan = "Perawat Madya"))
        assertEquals("RSUD Kota Bandung", store.loadProfile().instansi)

        store.logoutAsn()
        assertFalse(store.isLoggedIn())

        val login = store.loginAsn("19920415 201801 2 003", "rahasia99")
        assertTrue(login.isSuccess)
        assertEquals("Sari Wulandari, S.Kep", login.getOrThrow().name)
        assertEquals("RSUD Kota Bandung", login.getOrThrow().instansi)
        assertEquals("III/a", login.getOrThrow().golongan)
        assertTrue(store.isLoggedIn())
    }

    @Test
    fun loginFailsWithWrongPinOrUnknownNip() {
        store.registerAsn(newAsn, "rahasia99")
        assertTrue(store.loginAsn(newAsn.nip, "salah123").isFailure)
        assertTrue(store.loginAsn("199001012015012001", "rahasia99").isFailure)
    }

    @Test
    fun duplicateAccountIsRejectedWhileGeneralAndMaleUsersAreAccepted() {
        assertTrue(store.registerAsn(newAsn, "rahasia99").isSuccess)
        assertTrue(store.registerAsn(newAsn, "lainnya99").isFailure)
        assertTrue(store.registerAsn(secondUser, "rahasia99").isSuccess)
    }

    @Test
    fun historyIsIsolatedPerUser() {
        store.registerAsn(secondUser, "rahasia99")
        store.addHistory(ScreeningResult(72, RiskLevel.HIGH, "01 Nov 2026"))
        assertEquals(1, store.loadHistory().size)

        store.registerAsn(newAsn, "rahasia99")
        assertTrue("Akun baru tidak boleh melihat riwayat akun lain", store.loadHistory().isEmpty())

        store.addHistory(ScreeningResult(15, RiskLevel.LOW, "05 Nov 2026"))
        assertEquals(RiskLevel.LOW, store.loadHistory().single().level)

        store.loginAsn(secondUser.nip, "rahasia99")
        assertEquals(RiskLevel.HIGH, store.loadHistory().single().level)
    }

    @Test
    fun passwordIsStoredHashedWithUniqueSalt() {
        store.registerAsn(secondUser, "123456")
        store.registerAsn(newAsn, "123456")
        val rows = db.readableDatabase.rawQuery(
            "SELECT password_hash, password_salt FROM ${EmpatiDatabase.TABLE_ASN_USERS}", null
        ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0) to c.getString(1)) } }
        assertEquals(2, rows.size)
        rows.forEach { (hash, _) -> assertFalse(hash.contains("123456")) }
        // Dua akun memakai PIN sama (123456) tetapi hash harus berbeda karena salt berbeda.
        assertNotEquals(rows[0].first, rows[1].first)
    }

    @Test
    fun deleteUserDataRemovesOnlyThatUser() {
        store.registerAsn(secondUser, "rahasia99")
        store.addHistory(ScreeningResult(72, RiskLevel.HIGH, "01 Nov 2026"))

        store.registerAsn(newAsn, "rahasia99")
        store.addHistory(ScreeningResult(15, RiskLevel.LOW, "05 Nov 2026"))
        store.clearCurrentUserData()

        assertFalse(store.isLoggedIn())
        assertTrue(store.loginAsn(newAsn.nip, "rahasia99").isFailure)

        store.loginAsn(secondUser.nip, "rahasia99")
        assertEquals("Data akun lain harus tetap utuh", 1, store.loadHistory().size)
    }

    @Test
    fun chatLogsArePersistedPerUser() {
        store.registerAsn(newAsn, "rahasia99")
        val reply = com.example.empati.data.ChatEngine.replyWithRag(
            message = "Bagaimana batas konsumsi garam harian?",
            lastResult = null,
            corpus = store.loadRagCorpus()
        )
        store.appendChatExchange("Bagaimana batas konsumsi garam harian?", reply)
        val logs = store.loadChatLogs()
        assertEquals(2, logs.size)
        assertTrue(logs[0].isUser)
        assertFalse(logs[1].isUser)
    }
}

