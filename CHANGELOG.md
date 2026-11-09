# Changelog — SRYA Emora (sebelumnya AI-EMPATI)

## v4.1.5 — Ergonomi Penempatan Chatbox di Atas Keyboard (`imePadding` & Sembunyikan Navbar) & Opsi Salin Teks Chatbox/Pesan (`SelectionContainer` & `ContentCopy`) (2026-11-09)

Rilis ini menyempurnakan kenyamanan pengalaman pengetikan pada ruang konsultasi SRYA Emora dengan memastikan bilah chatbox dan placeholder selalu terangkat tepat di atas keyboard virtual, serta menyediakan opsi salin teks yang komprehensif baik pada gelembung obrolan maupun pada bilah input pesan.

### 1. Penempatan Chatbox & Placeholder Selalu di Atas Keyboard (`ChatScreen.kt` & `MainScaffold.kt`)
- **Adaptasi Insets Dinamis (`Modifier.imePadding()`)**:
  - Menerapkan `Modifier.imePadding()` pada kanvas utama `ChatContent` di [`ChatScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ChatScreen.kt), sehingga `ChatComposer` beserta placeholder *"Ceritakan yang Anda rasakan atau tanyakan sesuatu…"* secara dinamis terangkat dan duduk manis tepat di atas keyboard virtual saat pengguna mulai mengetik.
- **Sembunyikan Bottom Navigation Bar Cerdas (`MainScaffold.kt`)**:
  - Pada [`MainScaffold.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/MainScaffold.kt), saat tab aktif adalah Konsultasi (`Screen.Chat`) dan keyboard terdeteksi muncul (`WindowInsets.isImeVisible == true`), `BottomNavBar` secara otomatis disembunyikan sehingga tidak memakan ruang layar vertikal dan tidak bertumpuk dengan keyboard virtual.
- **Auto-Scroll ke Pesan Terakhir saat Keyboard Aktif**:
  - Menambahkan efek reaktif `LaunchedEffect(isImeVisible)` yang secara otomatis menggulir `LazyColumn` obrolan ke pesan paling akhir begitu keyboard terangkat, menjaga konteks percakapan tetap terbaca tanpa terhalang.

### 2. Opsi Salin Teks Menyeluruh pada Gelembung Pesan & Bilah Chatbox (`ChatScreen.kt`)
- **Seleksi Teks Presisi (`SelectionContainer`)**:
  - Membungkus seluruh teks gelembung pesan (baik respons asisten SRYA Emora maupun pesan pengguna) dalam `SelectionContainer` dari Jetpack Compose, memungkinkan pengguna menyeleksi sebagian kata atau kalimat (misal nomor rujukan atau instruksi tertentu) dengan mudah.
- **Tombol Salin Cepat pada Gelembung Pesan (`Icons.Rounded.ContentCopy`)**:
  - Setiap pesan kini memiliki tombol salin elegan berikon `ContentCopy` dengan target sentuh yang nyaman. Mengetuk tombol ini akan menyalin seluruh isi pesan ke clipboard sistem melalui `LocalClipboardManager` dan menampilkan notifikasi toast yang menenangkan (*"Pesan disalin ke clipboard"*).
- **Tombol Salin Draf Teks pada Bilah Chatbox**:
  - Pada bilah komposer input (`ChatComposer`), ketika terdapat teks yang sedang diketik (`value.isNotEmpty()`), muncul tombol salin cepat berikon `Icons.Rounded.ContentCopy` di sebelah tombol Kirim. Pengguna dapat menyalin draf pesan yang sedang disusun kapan saja dengan umpan balik toast (*"Draf pesan disalin ke clipboard"*).

---

## v4.1.4 — Analisis & Kesimpulan Kontekstual Hasil Skrining oleh LLM (`"analisis hasil screening saya"` / `"apa yang bisa anda simpulkan dari hasil screening saya"`) (2026-11-09)

Rilis ini memisahkan intent cek status singkat (`"Lihat hasil skrining saya"`) dari intent analisis/kesimpulan mendalam, serta memperkaya jembatan konteks kualitatif `ScreeningContextSnapshot` agar LLM benar-benar menghasilkan analisis medis & psikososial yang relevan berdasarkan temuan skrining pengguna.

### 1. Pemisahan Intent Cek Ringkas vs Analisis/Kesimpulan Mendalam (`RagKnowledgeRepository.kt` & `ChatEngine.kt`)
- **`isSimpleResultLookupQuery(query)` vs `isScreeningAnalysisQuery(query)`**:
  - Ketika pengguna menekan tombol cepat `"Lihat hasil skrining saya"` (atau bertanya `"Berapa skor skrining saya?"` / `"Cek hasil skrining saya"`), chatbot tetap menampilkan ringkasan deterministik singkat (`snapshot-zero-leak`).
  - Ketika pengguna bertanya `"analisis hasil screening saya"`, `"apa yang bisa anda simpulkan dari hasil screening saya"`, `"jelaskan hasil skrining saya"`, `"apa saran dari hasil skrining saya"`, atau pertanyaan analitis sejenis (baik dengan ejaan `"skrining"` maupun `"screening"`), `ChatEngine` meneruskan pertanyaan tersebut ke LLM (`OllamaRagService.generateRagResponse`) beserta konteks hasil skrining pengguna.

### 2. Pengayaan `ScreeningContextSnapshot` & Injeksi Konteks Analisis ke LLM (`UserResultPresenter.kt`, `ScreeningStore.kt`, `OllamaRagService.kt`, `EmpatiViewModel.kt`, `ChatScreen.kt`)
- **Konteks Kualitatif Lengkap (100% Zero-Leak)**:
  - `ScreeningContextSnapshot` kini memuat `riskLevelLabel`, `integratedCategoryLabel`, `screeningDate`, `summaryNarrative`, `hypertensionStatusLabel`, `gbvStatusLabel`, `keyFactors` (faktor pemicu dominan dari jawaban pengguna), `protectiveFactors`, `relationshipExplanation`, dan `keyRecommendations`.
  - Konteks ini disimpan secara persisten per akun di `ScreeningStore` (`saveQualitativeSnapshot` / `loadQualitativeSnapshot`) sehingga tetap tersedia walaupun aplikasi ditutup dan dibuka kembali di sesi berikutnya.
- **Prompt Analitis LLM (`OllamaRagService.buildAugmentedPrompt` & `SYSTEM_PROMPT`)**:
  - Menyertakan blok `[KONTEKS HASIL SKRINING PENGGUNA SAAT INI]` ke dalam prompt LLM beserta instruksi untuk menyimpulkan arti kategori risiko, menganalisis keterkaitan fisiologis & psikososial antar faktor yang ditemukan, serta menyusun rekomendasi langkah prioritas yang dipersonalisasi.
  - Menambahkan chip pintasan `"Analisis hasil skrining saya"` pada `ChatScreen.kt`.

---

## v4.1.3 — Migrasi & Sinkronisasi Live Database Cloud Supabase (`public.user_accounts`, `public.research_cf_records`, & Pemulihan Akun Lintas Perangkat) (2026-11-09)

Rilis ini memperbaiki kendala sinkronisasi data registrasi/akun pengguna ke database Cloud Supabase (`empati-research` / `qnvcuhmvrrffzdxxdloc`) serta menutup potensi isu hak akses (`RLS` & `GRANT`) dan pemulihan akun lintas perangkat.

### 1. Pembuatan & Migrasi Live Tabel `public.user_accounts` di Server Supabase (`qnvcuhmvrrffzdxxdloc`)
- **Perbaikan `HTTP 404 (PGRST205)` pada `/rest/v1/user_accounts`**:
  - Menerapkan migrasi `create_user_accounts_and_fix_research_sync_grants` langsung ke database produksi Supabase (`qnvcuhmvrrffzdxxdloc`) menggunakan skema kunci utama `id bigint generated always as identity primary key` dan `account_identifier text not null unique` (sebagai satu-satunya *unique business constraint* agar `ON CONFLICT (account_identifier) DO UPDATE` tidak pernah gagal akibat konflik unik sekunder pada `user_id` atau `subject_hash`).
  - Menambahkan indeks (`idx_user_accounts_identifier`, `idx_user_accounts_subject_hash`, `idx_user_accounts_user_id`), trigger otomatis `trg_user_accounts_updated_at`, izin sequence (`user_accounts_id_seq`), serta 4 kebijakan Row-Level Security (`allow_validated_user_accounts_select`, `allow_validated_user_accounts_insert`, `allow_validated_user_accounts_update`, `allow_validated_user_accounts_delete`).

### 2. Perbaikan Hak Akses Upsert (`HTTP 401 / 42501 permission denied`) pada `public.research_cf_records`
- **Perbaikan Izin `ON CONFLICT (screening_id) DO UPDATE`**:
  - Di PostgreSQL, operasi `POST /rest/v1/research_cf_records?on_conflict=screening_id` dengan header `Prefer: resolution=merge-duplicates,return=minimal` membutuhkan hak akses `SELECT` dan `UPDATE` beserta kebijakan RLS pada tabel target.
  - Menambahkan `grant select, insert, update` dan kebijakan RLS tervalidasi (`allow_validated_research_cf_select` & `allow_validated_research_cf_update` yang memeriksa `subject_hash like 'sub_%'` dan `char_length(screening_id) >= 8`) sehingga pencatatan baseline demografi akun (`ACCOUNT_REGISTERED_NO_SCREENING`) maupun pembaruan profil berhasil dengan `HTTP 201 Created` / `200 OK` dan lulus audit `get_advisors(type="security")` dengan **0 peringatan keamanan**.

### 3. Ketahanan Sinkronisasi Akun & Pemulihan Login Cloud di Aplikasi Android (`SupabaseCloudSyncService.kt`, `ScreeningStore.kt`, `EmpatiViewModel.kt`)
- **Validasi Status `cloud_synced` (`SupabaseCloudSyncService.syncUserAccount`)**:
  - `syncUserAccount()` kini mengembalikan `userTableSynced` (bukan `userTableSynced || baselineSynced`) disertai *fallback* `INSERT` murni pada `research_cf_records`, sehingga flag `cloud_synced = 1` di SQLite lokal hanya ditandai apabila data akun benar-benar sudah masuk ke `public.user_accounts`.
- **Sinkronisasi Idempoten Akun Aktif (`ScreeningStore.syncUserAccountsToSupabase`)**:
  - Memastikan akun yang sedang aktif (`activeAccount`) selalu di-upsert secara idempoten ke `public.user_accounts` setiap kali aplikasi dibuka (`finishSplash`), selesai onboarding (`completeOnboarding`), login/registrasi (`enterSession`), maupun saat profil diperbarui (`updateProfile`), serta memfilter sinkronisasi `research_cf_records` berdasarkan `subjectHash` pengguna aktif.
- **Pemulihan Akun dari Cloud Saat Login (`EmpatiViewModel.loginAsn` & `fetchUserAccountByIdentifier`)**:
  - Mengaktifkan `allowCloudRestore = true` pada `EmpatiViewModel.loginAsn()` dan menjalankan `fetchUserAccountByIdentifier()` di *background daemon thread* (bebas `NetworkOnMainThreadException`), sehingga pengguna yang sudah terdaftar di `public.user_accounts` tetap dapat login meskipun aplikasi diinstal ulang atau digunakan di perangkat baru.

---

## v4.1.2 — Penyelesaian Audit Copywriting ("Anti AI-Slop"), Kesiapan Deploy Produksi, Penghapusan Akun Cloud, & Dukungan Screenshot/Screen Record (Kode `FLAG_SECURE` Dipertahankan) (2026-11-09)

Rilis ini menuntaskan seluruh daftar temuan **Audit A (Humanisasi Copywriting & Bahasa Anti "AI-Slop")** dan **Audit B (Teknis & Kesiapan Deploy Produksi)**, serta mengizinkan pengguna melakukan tangkapan layar (*screenshot*) dan perekaman layar (*screen record*) tanpa menghapus kode `FLAG_SECURE` agar tetap dapat diaktifkan kembali kapan saja di masa depan.

### 1. Dukungan Screenshot & Screen Record Tanpa Menghapus Kode `FLAG_SECURE` (`MainActivity.kt`, `ProfileScreen.kt`)
- **Konstanta Sakelar `MainActivity.ENABLE_SCREENSHOT_PROTECTION = false`**:
  - Menonaktifkan pemblokiran layar secara default sehingga aplikasi dapat melakukan *screenshot* dan *screen recording* dengan lancar.
  - Kode `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)` tetap dipertahankan utuh di dalam blok `if (ENABLE_SCREENSHOT_PROTECTION)` pada `MainActivity.kt` agar pengembang dapat mengaktifkannya kembali kapan saja cukup dengan mengubah nilai konstanta menjadi `true`.
- **Sinkronisasi Indikator di `ProfileScreen.kt`**:
  - Baris `"Perlindungan layar"` otomatis mengikuti status `MainActivity.ENABLE_SCREENSHOT_PROTECTION` dan menampilkan `"Tangkapan & perekaman layar diizinkan"`, serta label versi diperbarui ke `"Versi 4.1.2 · Inklusif semua kalangan"`.

### 2. Penyelesaian Audit A — Humanisasi Copywriting & Penghapusan Bahasa Kaku / "AI-Slop" (Poin 1–5)
- **A.1 — Layar Chat (`ChatScreen.kt`)**:
  - Mengubah banner ruang lingkup menjadi `"Teman cerita seputar kesehatan, ketenangan pikiran, dan ruang aman"`.
  - Mengubah indikator mengetik menjadi `"SRYA Emora sedang mengetik…"`.
  - Mengubah placeholder kotak pesan menjadi `"Ceritakan yang Anda rasakan atau tanyakan sesuatu…"`.
- **A.2 — Respons Hasil Skrining di Chatbot (`ChatEngine.kt` & `AsnAndRagArchitectureTest.kt`)**:
  - Memanusiakan jawaban saat pengguna menanyakan hasil skrining menjadi sapaan hangat yang mengajak berdiskusi (*"...Ada bagian dari hasil tersebut yang ingin kita bahas bersama?"*) maupun ajakan ramah bila pengguna belum pernah melakukan skrining, dengan tetap 100% mematuhi aturan *Zero-Leak*.
- **A.3 — Narasi Hasil Skrining & Penjelasan Faktor (`UserResultPresenter.kt`)**:
  - Merombak seluruh narasi `summary`, `relationship`, `conclusion`, `recommendations`, `CHRONIC_LINKAGE_NOTE`, dan 10 penjelasan `explainClinicalTag` dari bahasa laporan klinis kaku menjadi bahasa Indonesia sehari-hari yang hangat, suportif, dan mudah dipahami tanpa kehilangan kata kunci pengujian (`UserPresentationTest`).
- **A.4 — Layar Splash, Onboarding, Persetujuan, Beranda, & Riwayat (`SplashScreen.kt`, `OnboardingScreen.kt`, `ConsentScreen.kt`, `HomeScreen.kt`, `HistoryScreen.kt`)**:
  - Mengganti kalimat kaku/birokratis menjadi sapaan percakapan yang tenang dan akrab (*"Teman pendamping kesehatan tekanan darah dan ketenangan pikiran Anda"*, *"Ngobrol bersama SRYA Emora"*, *"Sebelum kita mulai"*, *"Cek kesehatan & batin"*, *"Teman cerita & tanya"*).
- **A.5 — Catatan Stres Kronis, SAPA 129, & Jawaban Offline (`OllamaRagService.kt`)**:
  - Menghaluskan redaksi `CHRONIC_STRESS_PATHWAY_NOTE`, `SAPA_129_MANDATORY_NOTE`, dan seluruh cabang jawaban `synthesizeOfflineClinicalAnswer` agar tetap terasa hangat dan natural saat perangkat sedang tanpa koneksi internet.

### 3. Penyelesaian Audit B — Teknis & Kesiapan Deploy Produksi (Poin 6–9)
- **B.6 — Penyelarasan Teks Privasi dengan Arsitektur Hybrid Cloud (`AuthScreen.kt`, `ConsentScreen.kt`, `OnboardingScreen.kt`, `HomeScreen.kt`, `ProfileScreen.kt`)**:
  - Menyelaraskan seluruh pemberitahuan privasi agar secara jujur, transparan, dan menenangkan menjelaskan bahwa kata sandi dienkripsi (`PBKDF2-HMAC-SHA256`) dan jawaban skrining dipisahkan dari identitas asli menggunakan kode samaran satu arah (`subject_hash`).
- **B.7 — Penghapusan Data Akun hingga ke Cloud Supabase (`SupabaseCloudSyncService.kt`, `ScreeningStore.kt`, `EmpatiViewModel.kt`, `20261108000001_create_empati_research_schema.sql`)**:
  - Menambahkan fungsi `SupabaseCloudSyncService.deleteUserAccount(userId, subjectHash)` serta kebijakan RLS `allow_validated_user_accounts_delete` pada tabel `public.user_accounts`.
  - Ketika pengguna menekan `"Hapus seluruh data akun & riwayat saya"` di `ProfileScreen`, aplikasi menghapus data lokal SQLite sekaligus menghapus baris akun pengguna di Cloud Supabase.
- **B.8 & B.9 — Penyatuan Header `"SRYA Emora"` & Metadata Versi Rilis (`OllamaRagService.kt`, `SupabaseCloudSyncService.kt`, `supabase/functions/empati-chat/index.ts`, `app/build.gradle.kts`)**:
  - Mengganti seluruh sisa header HTTP (`User-Agent`, `X-Title`) dari `"AI-EMPATI"` menjadi `"SRYA-Emora-Android/4.1.2"` dan `"SRYA Emora Health & Psychosocial Companion"`.
  - Menaikkan `versionCode = 5` dan `versionName = "4.1.2"` di `app/build.gradle.kts`.

---

## v4.1.1 — Penghapusan Akun Demo (Production-Ready Auth), Perkenalan Diri Chatbot "SRYA Emora" & Humanisasi Bahasa, serta Persistensi Akun Pengguna Tanpa Skrining (2026-11-09)

Rilis ini menyiapkan aplikasi **SRYA Emora** agar benar-benar siap pakai (*production-ready*) dengan menghapus seluruh akun demo pengembangan, menyempurnakan kecerdasan percakapan chatbot saat ditanya identitas dirinya serta memanusiakan (*humanize*) pesan batasan ruang lingkup, dan memastikan data akun serta profil pengguna tersimpan permanen di database lokal maupun cloud meskipun pengguna belum melakukan skrining.

### 1. Penghapusan Akun Demo Pengembangan (`EmpatiDatabase.kt`, `AuthScreen.kt`, `ScreeningStore.kt`, `EmpatiViewModel.kt`, `Models.kt`)
- **Pembersihan Skema SQLite (`DB_VERSION = 4` di `EmpatiDatabase.kt`)**:
  - Menghapus fungsi `seedDefaultAsnAccount()` serta konstanta `DEFAULT_DEMO_USER_ID`, `DEMO_ACCOUNT_ID`, `DEMO_NIP`, dan `DEMO_PIN`.
  - Menambahkan migrasi otomatis pada `onUpgrade` (`oldVersion < 4`) yang menghapus baris akun demo lama (`asn_demo_ratna` / `ratna.andari@empati.id`) beserta ringkasan dan log chat demonya.
- **Layar Login & Registrasi Bersih (`AuthScreen.kt` & `Models.kt`)**:
  - Mengosongkan nilai awal `initialAccountId` dan `initialPin` pada `AuthFormState` (tidak ada lagi kredensial demo yang terisi otomatis).
  - Menghapus tombol `"Coba dengan akun demo (Ratna Andari)"` (`GhostButton`) serta fungsi `loginDemoAsn()` di `EmpatiViewModel` dan `ScreeningStore`.
  - Mengubah nilai default `UserProfile` di `Models.kt` menjadi kosong agar tidak pernah menampilkan identitas fiktif sebelum pengguna login.

### 2. Perkenalan Diri Chatbot "SRYA Emora" (`"siapa anda"`) & Humanisasi Teks Penolakan (`ChatEngine.kt`, `OllamaRagService.kt`, `RagKnowledgeRepository.kt`, `Modelfile.empati-qwen`)
- **Deteksi Intent Identitas & Peran Asisten (`RagKnowledgeRepository.isIdentityQuery` & `ChatEngine.replyWithRag`)**:
  - Saat pengguna bertanya pertanyaan seperti `"siapa anda"`, `"siapa kamu"`, `"apa itu SRYA Emora"`, `"kamu bisa bantu apa"`, atau `"kenalan"`, chatbot tidak lagi memicu penolakan topik, melainkan memperkenalkan dirinya secara hangat melalui `SRYA_EMORA_SELF_INTRO` sebagai **SRYA Emora**, teman bicara dan pendamping kesehatan pribadi pengguna.
- **Humanisasi Pesan Penolakan Ruang Lingkup (`OllamaRagService.OUT_OF_SCOPE_REFUSAL`)**:
  - Mengganti teks penolakan kaku (*AI-slop*) menjadi sapaan yang hangat, luwes, dan empatik:
    *"Maaf ya, untuk topik itu saya belum bisa membantu. Sebagai teman bicara Anda di SRYA Emora, saya hadir khusus untuk menemani obrolan seputar tekanan darah dan gaya hidup sehat, pengelolaan stres atau kualitas tidur, serta dukungan ruang aman bila Anda menghadapi situasi kekerasan. Ada hal dari sekitar itu yang sedang ingin Anda ceritakan?"*
- **Humanisasi Sapaan & System Prompt**:
  - Memperbarui pesan sambutan awal di `EmpatiViewModel.welcomeMessage()`, respons sapaan di `ChatEngine.kt`, serta `SYSTEM_PROMPT` di `OllamaRagService.kt` dan `ollama/Modelfile.empati-qwen`.

### 3. Persistensi Data Akun & Profil Pengguna di Database Walaupun Belum Skrining (`EmpatiDatabase.kt`, `ScreeningStore.kt`, `SupabaseCloudSyncService.kt`, `20261108000001_create_empati_research_schema.sql`)
- **Persistensi Sesi & Profil di SQLite Lokal (`EmpatiDatabase.kt` v4)**:
  - Menambahkan kolom `updated_at`, `is_active_session`, dan `cloud_synced` pada tabel `auth_asn_users` serta model `PersistedUserAccount`.
  - Menyimpan status sesi aktif langsung di SQLite (`setActiveSessionUser` / `loadActiveSessionUserId`) dan `SharedPreferences` secara sinkron (`.commit()`), sehingga data akun dan sesi pengguna tidak pernah hilang walaupun pengguna menutup aplikasi tanpa melakukan skrining.
- **Persistensi Akun & Profil ke Database Cloud Supabase (`SupabaseCloudSyncService.kt` & `user_accounts`)**:
  - Menambahkan tabel `public.user_accounts` pada skema migrasi Supabase serta fungsi `syncUserAccount()`, `buildUserAccountJsonPayload()`, `buildAccountBaselineResearchPayload()`, dan `fetchUserAccountByIdentifier()`.
  - Memastikan `ScreeningStore.syncUserAccountsToSupabase()` dipanggil otomatis saat registrasi, login, pembaruan profil (`updateProfile`), maupun saat aplikasi dibuka (`finishSplash`), sehingga akun dan profil pengguna tetap tersimpan di database walaupun pengguna belum pernah melakukan skrining.

---

## v4.1.0 — Rebranding Aplikasi & Chatbot menjadi "SRYA Emora" + Implementasi Logo Multi-Resolusi (Dengan & Tanpa Background) (2026-11-08)

Rilis ini memperbarui identitas aplikasi dan asisten AI/chatbot menjadi **"SRYA Emora"** beserta implementasi aset logo resmi bergradasi biru-toska (monogram **"SA"** dengan ruang negatif berbentuk hati dan ekor balon percakapan) sesuai standar kepadatan layar Android (*multi-density*).

### 1. Rebranding Nama Aplikasi & Chatbot menjadi "SRYA Emora"
- **`res/values/strings.xml`**: Mengubah `app_name` dari `AI-EMPATI` menjadi **`SRYA Emora`** (nama aplikasi di *launcher* Android).
- **Seluruh Layar UI (`SplashScreen.kt`, `AuthScreen.kt`, `OnboardingScreen.kt`, `HomeScreen.kt`, `ResultScreen.kt`, `ChatScreen.kt`, `ProfileScreen.kt`)**:
  - Judul utama di layar *Splash*, *Auth*, dan header *Chat* diperbarui menjadi **`SRYA Emora`**.
  - Pintasan dan tombol konsultasi diperbarui menjadi **`"Tanya SRYA Emora"`**, **`"Konsultasi dengan SRYA Emora"`**, **`"Tanyakan ke SRYA Emora"`**, **`"Diskusikan dengan SRYA Emora"`**, dan **`"Tentang SRYA Emora"`**.
  - Indikator mengetik dan banner ruang lingkup di `ChatScreen.kt` diperbarui menjadi **`"SRYA Emora sedang menyusun jawaban…"`** dan **`"SRYA Emora · Ruang lingkup: Kesehatan, Psikologi & Kekerasan"`**.
- **Prompt & Sapaan Asisten (`OllamaRagService.kt`, `ChatEngine.kt`, `EmpatiViewModel.kt`)**:
  - Memperbarui *System Prompt* LLM (`"Anda adalah SRYA Emora, pendamping pintar di bidang Kesehatan, Psikologi, dan Perlindungan Kekerasan."`) serta pesan sapaan awal di ruang konsultasi.

### 2. Implementasi Logo Multi-Resolusi (Tanpa Background & Dengan Background)
- **Pembersihan & Pemotongan Presisi (*Cropping & Bicubic Resampling*)**:
  - Memotong area lambang monogram `"SA"` secara presisi (`472×366 px` dari kanvas sumber `1024×682 px`), membersihkan residu piksel di bagian bawah, dan menempatkannya secara proporsional di tengah kanvas persegi `1:1` dengan *high-quality bicubic interpolation*.
- **Aset Drawable Dalam Aplikasi (`res/drawable-mdpi` s/d `res/drawable-xxxhdpi`)**:
  - **`srya_emora_logo_transparent.png`** (Tanpa Background — digunakan pada `SplashScreen` dan `AuthScreen` melalui `EmpatiLogo(..., withBackground = false)`):
    - `mdpi`: `128×128 px` | `hdpi`: `192×192 px` | `xhdpi`: `256×256 px` | `xxhdpi`: `384×384 px` | `xxxhdpi`: `512×512 px`.
  - **`srya_emora_logo_bg.png`** (Dengan Background Putih — digunakan pada `CompanionAvatar` 48dp dan `CompanionMark` 32dp di `ChatScreen` melalui `EmpatiLogo(..., withBackground = true)`):
    - `mdpi`: `128×128 px` | `hdpi`: `192×192 px` | `xhdpi`: `256×256 px` | `xxhdpi`: `384×384 px` | `xxxhdpi`: `512×512 px`.
- **Aset Ikon Peluncur / Launcher Icon (`res/mipmap-mdpi` s/d `res/mipmap-xxxhdpi` & `mipmap-anydpi-v26`)**:
  - **Adaptive Icon Foreground (`ic_launcher_foreground.png`, kanvas 108dp dengan *safe zone* 66dp)**:
    - `mdpi`: `108×108 px` | `hdpi`: `162×162 px` | `xhdpi`: `216×216 px` | `xxhdpi`: `324×324 px` | `xxxhdpi`: `432×432 px`, dipadukan dengan `@drawable/ic_launcher_background` berwarna putih bersih (`#FFFFFF`).
  - **Legacy Launcher Icons (`ic_launcher.png` squircle & `ic_launcher_round.png` lingkaran)**:
    - `mdpi`: `48×48 px` | `hdpi`: `72×72 px` | `xhdpi`: `96×96 px` | `xxhdpi`: `144×144 px` | `xxxhdpi`: `192×192 px`.

---

## v4.0.0 — Sistem Desain EMPATI "Soft Daylight", Tipografi 3-Suara (Fraunces + Inter + JetBrains Mono), Pustaka Komponen Baru & Aksesibilitas WCAG 2.2 (2026-11-06)

Rilis ini merombak total lapisan antarmuka pengguna (*front-end* & UI/UX) ke **EMPATI "Soft Daylight" Design System (v4.0)** sebagaimana terdokumentasi di `project_analysis.md`, menghadirkan suasana ruang kesehatan yang tenang, manusiawi, dan beraksesibilitas tinggi.

### 1. Arsitektur Warna 3-Lapis "Soft Daylight" (`ui/theme/Color.kt` & `ui/theme/Theme.kt`)
- **Lapis 1 — Palet Tonal Mentah**:
  - Mengganti palet Mauve/Teal lama dengan **Sage** (primary — ketenangan & kepercayaan, `Sage40` `#2E6A57`), **Clay** (secondary — kehangatan manusiawi, `Clay40` `#93482E`), **Mist** (tertiary — kehadiran pendamping AI-EMPATI, `Mist40` `#5A5896`), **Honey** (`#D29A2E`), **Terracotta** (`#A8402B`), **Berry** (`#8E2F55`), dan permukaan netral **Stone** berbias hijau alami (`StoneLightBackground` `#F7F5F0` di mode terang & `StoneDarkBackground` `#121512` di mode gelap).
- **Lapis 2 — Peran Material 3**: Pemetaan lengkap `LightColorScheme` dan `DarkColorScheme` (`surfaceContainerLowest` hingga `surfaceContainerHighest`).
- **Lapis 3 — Semantik Afektif (`EmpatiColors` via `MaterialTheme.empati`)**:
  - Menyediakan nada emosional terstruktur `calm` (Sage), `attentive` (Honey), `concerned` (Terracotta), `urgent` (Berry), `neutral` (Stone), `presence` (Mist), `aura`, dan `shadowTint`.

### 2. Sistem Tipografi 3-Suara (`ui/theme/Type.kt` & `res/font/`)
- **`DisplayFont` — Fraunces (`res/font/fraunces.ttf`)**: Variable soft-serif dengan sumbu `opsz=72, SOFT=100, WONK=0` untuk `displayLarge` s/d `headlineSmall` (sapaan waktu, pertanyaan kuesioner, judul halaman, pernyataan hasil di `ResultHero`, dan inisial `EmpatiAvatar`).
- **`BodyFont` — Inter (`res/font/inter.ttf`) + `EmpatiNumeralStyle` (`tnum`)**: Sans-serif fungsional untuk `titleLarge` s/d `labelSmall` dengan *sentence-case* percakapan (`toSentenceCase()`) serta angka tabular pada penghitung langkah kuesioner (`3 / 16`) agar digit tidak bergeser.
- **`MetricFont` — JetBrains Mono (`res/font/jetbrains_mono.ttf`) via `EmpatiMetricStyle`**: Dibatasi secara disiplin hanya untuk pengenal mentah (ID akun di Profil & anatomi NIP 18 digit)—tidak lagi digunakan pada lencana atau label UI.

### 3. Token Desain, Gerak & Pustaka Komponen (`ui/theme/Tokens.kt` & `ui/components/Components.kt`)
- **`Tokens.kt`**: Menambahkan `EmpatiSpace` (grid 4dp, `gutter = 24.dp`, `readableWidth = 560.dp`, `touchTarget = 48.dp`), `EmpatiIcon`, `EmpatiElevation`, `Modifier.softShadow`, `EmpatiMotion` (`EaseOutSoft`, `EaseInOutSoft`, `press()`, `settle()`), serta `LocalReducedMotion` (`rememberSystemReducedMotion()`).
- **`Components.kt`**:
  - Menambahkan `ScreenFrame`, `ScreenTopBar`, `ScreenHeader`, `StateOrb` (orb afektif bernapas dengan cincin progres melingkar), `ToneBadge`, `SegmentedSwitch` (*gliding pill thumb*), `ChoiceChip`, `EmpatiTextField` (dukungan autofill `ContentType`, indikator fokus tumbuh dari tengah, & `LiveRegionMode.Polite`), `SoftNotice`, `BreathingDots`, `Reveal`, `Modifier.pressScale`, dan `Modifier.focusHalo` (WCAG 2.4.7).
- **Pemisahan Stateless Content + Stateful Wrapper di Seluruh Layar (`ui/screens/`)**:
  - Memisahkan composable stateless (`ModuleResultContent`, `IntegratedResultContent`, `QuestionnaireContent`, `ConsentScreenContent`, `ProfileScreenContent`, `AuthFormState`, dll.) dari wrapper `EmpatiViewModel` untuk kemudahan *Preview* dan pengujian UI.

### 4. Kesiapan Deploy Produksi: Integrasi Supabase Cloud (`empati-research`) & OpenRouter LLM (`data/cloud/SupabaseCloudSyncService.kt`, `data/rag/OllamaRagService.kt`, `supabase/`)
- **Database Riset Cloud Terpusat di Supabase (`https://qnvcuhmvrrffzdxxdloc.supabase.co`)**:
  - Menyediakan project Supabase **`empati-research`** (Region `ap-southeast-1` Singapore) dengan tabel `public.research_cf_records` dan `public.research_chat_logs`.
  - Menerapkan **Row-Level Security (RLS)** berprinsip *Least-Privilege Write-Only Telemetry*: aplikasi Android (`anon`) hanya diizinkan melakukan `INSERT` baris terpseudonimisasi (`subject_hash` tanpa izin `SELECT`), menggunakan `POST` murni dengan header `Prefer: return=minimal` serta penanganan idempoten `201 Created` / `409 Conflict`. Peneliti memiliki akses baca penuh dan **Export to CSV** langsung dari Supabase Dashboard Table Editor untuk analisis disertasi.
  - Menambahkan antrean sinkronisasi *offline-first* (`ScreeningStore.syncPendingResearchToSupabase`) yang otomatis mengunggah hasil skrining saat kuesioner selesai (`persistCurrentScreeningIfNeeded`), saat login (`enterSession`), maupun saat aplikasi dibuka kembali (`finishSplash`).
- **Integrasi OpenRouter LLM (Free-Tier Smart Routing) & Supabase Edge Function (`empati-chat`)**:
  - Memperbarui `OllamaRagService.kt` dan Supabase Edge Function `empati-chat` (`supabase/functions/empati-chat/index.ts`) agar mendukung **OpenRouter API** (`https://openrouter.ai/api/v1/chat/completions`) dengan prioritas **Free-Tier Smart Router (`openrouter/free`)** diikuti kandidat gratis (`meta-llama/llama-3.3-70b-instruct:free`, `google/gemma-3-12b-it:free`, `qwen/qwen3-8b:free`), kandidat berbayar opsional (`qwen/qwen-2.5-7b-instruct`, `openai/gpt-4o-mini`), serta *keyless cloud fallback* otomatis bila kuota harian gratis tercapai.
  - **Penyembunyian Badge Model Teknis di UI (`ChatScreen.kt`)**: Menghapus `StatusPill` (`"Qwen 2.5 LLM"`) dari `ChatHeader` agar pengguna tidak terdistraksi oleh pergantian model dinamis OpenRouter, sembari tetap mencatat nama model aktual yang menjawab setiap pesan pada kolom `model_name` di tabel `public.research_chat_logs` untuk audit peneliti.
- **Konfigurasi Build Release (`app/build.gradle.kts`, `app/proguard-rules.pro`)**:
  - Menambahkan aturan ProGuard/R8 (`proguard-rules.pro`), dukungan `BuildConfig`, dan konfigurasi penandatanganan rilis agar `./gradlew assembleRelease` menghasilkan APK rilis yang siap didistribusikan.

---

## v3.1.0 — Pure Scoped LLM (`AI-EMPATI`), Penyelarasan Status Risiko ("Perlu Pemantauan") & Anti-Truncated Generation (2026-11-06)

Rilis ini menyempurnakan kualitas percakapan chatbot menjadi **LLM Murni Terbatasi Topik (*Pure Scoped LLM*)**, menyelaraskan status risiko skrining antara layar **Hasil (`ResultScreen`)**, **Beranda (`HomeScreen`)**, dan **Riwayat (`HistoryScreen`)**, serta memperbaiki pemotongan kalimat (*truncated generation*) pada respons LLM.

### 1. Penyelarasan Status Risiko Skrining di Hasil, Beranda, & Riwayat (`CfResultGenerator.kt`, `UserResultPresenter.kt`, `Models.kt`, `HomeScreen.kt`, `HistoryScreen.kt`, `EmpatiViewModel.kt`)
- **Sinkronisasi Penuh 4 Tingkat Risiko**:
  - Sebelumnya, `UserResultPresenter.presentHypertension` mengunci label `"Perlu pemantauan"` setiap kali `M01 = Ya` (riwayat diagnosis hipertensi) tanpa melihat tingkat `CfLevel`, sedangkan `CfResultGenerator.toScreeningResult` memetakannya ke `RiskLevel.HIGH` / `VERY_HIGH` sehingga di layar Hasil tertulis `"Perlu pemantauan"` namun di Beranda dan Riwayat tertulis `"Risiko Tinggi"`.
  - Kini keempat tingkat risiko diselaraskan secara konsisten di seluruh layar:
    - `CfLevel.RENDAH` ↔ `RiskLevel.LOW`: **`Rendah · Batas Wajar`**
    - `CfLevel.SEDANG` ↔ `RiskLevel.MEDIUM`: **`Sedang · Perlu Pemantauan`**
    - `CfLevel.TINGGI` ↔ `RiskLevel.HIGH`: **`Tinggi · Perlu Perhatian`**
    - `CfLevel.SANGAT_TINGGI` ↔ `RiskLevel.VERY_HIGH`: **`Sangat Tinggi · Perlu Perhatian`**
- **Kalibrasi Hipertensi Terkendali (`CfResultGenerator.buildModuleResult`)**:
  - Apabila responden memiliki riwayat diagnosis (`M01 = Ya`), namun tekanan darah saat ini terkendali (`M05 = Tidak`), tidak memiliki komorbid diabetes (`M03 = Tidak`), dan faktor risiko gaya hidup/psikososial rendah (`< 0.50`), maka status klinisnya diklasifikasikan secara konsisten sebagai **`Sedang · Perlu Pemantauan`** (`RiskLevel.MEDIUM`) baik di layar Hasil, Beranda, maupun Riwayat.
- **Penyimpanan Otomatis Saat Kuesioner Selesai (`EmpatiViewModel.persistCurrentScreeningIfNeeded`)**:
  - Hasil skrining langsung tersimpan ke riwayat pengguna begitu kedua modul selesai dijawab, sehingga Beranda dan Riwayat selalu menampilkan hasil terbaru meskipun pengguna berpindah tab melalui *Bottom Navigation Bar*.

### 2. Chatbot `AI-EMPATI` Berbasis LLM Murni & Perbaikan Kalimat Terpotong (`OllamaRagService.kt`, `ChatEngine.kt`, `ChatScreen.kt`, `ollama/Modelfile.empati-qwen`)
- **Mode LLM Murni Tanpa Korpus Kaku**:
  - Menghapus penyuntikan korpus teks mentah dan chip sitasi dokumen dari respons chatbot; menggunakan `qwen2.5:3b-instruct` secara murni dengan pembatasan ruang lingkup ketat pada **Kesehatan**, **Psikologi**, dan **Perlindungan Kekerasan**.
- **Perbaikan Generasi Jawaban Terpotong (*Anti-Truncated Generation*)**:
  - Menaikkan batas token generasi `num_predict` dari `220` menjadi **`512`** dan konteks `num_ctx` menjadi **`2048`** (dengan `timeoutMs = 60_000`) agar jawaban berpoin tidak pernah berhenti di tengah kalimat.
  - Menambahkan pasca-pemrosesan `ensureCompleteResponse()` di `enforceOutputGuardrails()` yang menghapus simbol bintang markdown (`**`) yang tidak tertutup serta membersihkan poin atau klausa menggantung di akhir teks.
- **Penyederhanaan Header `ChatScreen.kt`**:
  - Mengubah judul header dari `"Asisten AI-EMPATI"` menjadi **`"AI-EMPATI"`** dan menghapus subjudul `"Edukasi Hipertensi & Kesejahteraan Psikososial"` sesuai arahan desain minimalis.

---

## v3.0.0 — Transformasi Inklusif untuk Semua Kalangan (Universal Public & Multi-Occupation Edition) (2026-11-06)

Rilis ini merombak aplikasi **AI-EMPATI** agar **cocok untuk semua kalangan masyarakat secara umum** (pekerja swasta/kantoran, wiraswasta/UMKM, mahasiswa/pelajar, ibu rumah tangga, tenaga medis/pendidik, maupun ASN dari semua gender), sembari tetap menyediakan dukungan atribut demografi dan inspektor NIP 18 digit opsional untuk kebutuhan riset disertasi (*"Skrining Risiko Hipertensi Berbasis Gender untuk Perempuan ASN di Indonesia"*).

### 1. Login & Registrasi Multi-Format Inklusif (`data/auth/AsnNipValidator.kt`, `ui/screens/AuthScreen.kt`, `data/Models.kt`)
- **Identitas Akun Fleksibel (`validateGeneralIdentity` & `normalizeAccountId`)**:
  - Pengguna kini dapat mendaftar dan masuk menggunakan **Email** (mis. `nama@email.com`), **Nomor HP** (`08xx` / `+628xx`), **Username**, **NIK 16 digit**, ataupun **NIP 18 digit** (menerima kode gender `1` Laki-laki maupun `2` Perempuan tanpa pemblokiran).
  - Menyertakan *Live Account Format Badge* (`FORMAT EMAIL TERDETEKSI`, `FORMAT NOMOR HP TERDETEKSI`, `FORMAT USERNAME TERDETEKSI`, `FORMAT NIK 16 DIGIT`, atau *Live NIP Anatomy Inspector* bila pengguna memasukkan 18 digit angka).
- **Profil Demografi & Latar Belakang Universal (`UserOccupationCategory` & `UserGenderOption`)**:
  - Pilihan kategori latar belakang/aktivitas: `Masyarakat Umum`, `Karyawan / Pekerja`, `Wiraswasta / UMKM`, `Mahasiswa / Pelajar`, `Ibu Rumah Tangga`, `ASN / Instansi Publik`, dan `Tenaga Medis / Pendidik`.
  - Pilihan jenis kelamin inklusif (`Perempuan`, `Laki-laki`) untuk analisis risiko berbasis gender pada riset disertasi.
  - Akun demo universal default: **`ratna.andari@empati.id`** (PIN `123456`), tetap mendukung kompatibilitas mundur dengan NIP demo `198503122010012014`.

### 2. Adaptasi Database SQLite v3 (`data/db/EmpatiDatabase.kt`)
- Menaikkan versi skema menjadi **`DB_VERSION = 3`** dengan migrasi mulus yang menyemai profil demo universal (`ratna.andari@empati.id`) dan memperbarui korpus RAG.
- `AsnSecurityHasher.deriveSubjectHash(accountId)` menghasilkan pseudonim satu arah `subject_hash` (`sub_<20-hex>`) untuk format identitas apa pun sehingga tabel `research_cf_records` tetap 100% terisolasi dari identitas asli pengguna.

### 3. Kuesioner, Presentasi Klinis, Korpus RAG & Prompt Ollama untuk Semua Kalangan
- **Kuesioner (`data/cf/CfQuestions.kt`)**:
  - Memperjelas `M06` (*"Apakah Anda sedang hamil atau baru melahirkan dalam 6 bulan terakhir? (Pilih Tidak bila tidak berlaku)"*) agar relevan bagi seluruh responden.
  - Menginklusifkan redaksi pertanyaan stres (`M12`) dan KBG (`K01`–`K10`) agar mencakup **lingkungan rumah tangga, tempat kerja/usaha, kampus/sekolah, maupun lingkungan sosial**.
- **Presentasi Klinis (`data/presentation/UserResultPresenter.kt`)**:
  - Mengganti narasi spesifik birokrasi/ASN pada `explainClinicalTag` dengan penjelasan kesehatan universal yang tetap mematuhi aturan **Zero-Leak** (tanpa angka desimal, `%`, `CF`, `MB`, `MD`).
- **Korpus RAG, Live Internet RAG & Ollama (`data/rag/WebRagSearchService.kt`, `data/rag/RagKnowledgeRepository.kt`, `data/rag/OllamaRagService.kt`, `data/ChatEngine.kt`, `ollama/Modelfile.empati-qwen`)**:
  - Menambahkan **`WebRagSearchService.kt`** untuk pencarian rujukan medis secara *live* di internet (**Wikipedia Medis Bahasa Indonesia API** `id.wikipedia.org/w/api.php` & **Europe PMC / PubMed Clinical Literature API** `ebi.ac.uk/europepmc/webservices/rest/search`) tanpa memerlukan API Key berbayar.
  - Menyimpan otomatis (*auto-caching*) setiap dokumen rujukan internet (`WEB-WIKI-*` & `WEB-PMC-*`) ke tabel SQLite `rag_knowledge_chunks` melalui `EmpatiDatabase.upsertRagChunks()` sehingga sitasi interaktif tetap berfungsi saat offline.
  - Memperbarui dokumen inti `DOC-HT-03`, `DOC-ASN-01`, dan `DOC-GBV-01` (*UU TPKS & Layanan SAPA 129*) serta System Prompt `empati-qwen2.5:3b` agar melayani masyarakat umum dari segala latar belakang.
  - Memperluas **Firewall Privasi Input (`sanitizePrivacyInput`)** untuk menyensor otomatis pola **18 digit NIP**, **16 digit NIK**, **alamat Email**, dan **Nomor Telepon/HP** sebelum dikirim ke pencarian internet maupun generator LLM.

### 4. Penyesuaian UI/UX di Seluruh Layar (`ui/screens/`)
- **`SplashScreen.kt` & `OnboardingScreen.kt`**: Lencana `SKRINING KESEHATAN & KESEJAHTERAAN` dan pengenalan inklusif bagi semua kalangan.
- **`HomeScreen.kt`**: Sapaan hangat universal tanpa awalan eksklusif `"Ibu ..."`, menampilkan latar belakang & domisili/instansi pengguna.
- **`ConsentScreen.kt`, `ChatScreen.kt`, & `ProfileScreen.kt`**: Menggunakan istilah universal (*Data Profil & Demografi*, *Keluar Sesi / Ganti Akun*, serta saran pertanyaan chatbot yang relevan untuk semua aktivitas harian).

---

## v2.5.0 — Integrasi Ollama (`qwen2.5:3b-instruct` / `empati-qwen2.5:3b`) pada Chatbot RAG On-Device (2026-11-05)

Rilis ini mengintegrasikan generator LLM lokal **Ollama (`qwen2.5:3b-instruct`)** ke dalam pipeline **Retrieval-Augmented Generation (RAG)** AI-EMPATI melalui lapisan layanan terisolasi (`OllamaRagService.kt`) dengan *guardrails* etika klinis berlapis dan ketahanan *offline-first*.

### Integrasi Model & Service Layer (`ollama/Modelfile.empati-qwen`, `data/rag/OllamaRagService.kt`)
- **Modelfile Kustom (`ollama/Modelfile.empati-qwen`)**:
  - Membangun profil model `empati-qwen2.5:3b` berbasis `qwen2.5:3b-instruct` (`temperature 0.25`, `top_p 0.85`, `num_ctx 4096`) yang ditanamkan System Prompt klinis khusus Perempuan ASN.
- **Layanan RAG Terisolasi (`OllamaRagService.kt`)**:
  - Menghubungkan aplikasi ke endpoint lokal Ollama (`http://127.0.0.1:11434` untuk ADB reverse/on-device dan `http://10.0.2.2:11434` untuk Android Emulator) melalui konfigurasi keamanan jaringan ketat (`res/xml/network_security_config.xml`).
  - **Firewall Privasi Input (`sanitizePrivacyInput`)**: Menyensor otomatis pola 18 digit NIP ASN (`[NIP-DIRAHASIAKAN]`) sebelum prompt meninggalkan batas database lokal SQLite.
  - **Guardrail Berlapis (Pre-Generation Topic Filter + System Prompt + Post-Generation Validator)**:
    1. *Topic Scoping*: Membatasi diskusi hanya pada hipertensi, pola makan DASH & aktivitas fisik, stres kerja birokrasi ASN & kualitas tidur, serta perlindungan KBG.
    2. *Zero-Leak & Non-Kausalitas*: Menghapus kebocoran istilah `CF`, `MB`, `MD`, angka desimal, atau simbol `%`, serta memastikan hubungan KBG/stres dengan hipertensi disajikan sebagai *chronic stress pathway* (jalur stres kronis), bukan sebab-akibat langsung.
    3. *Rujukan Aman Wajib*: Menyertakan otomatis informasi hotline rahasia **SAPA 129 (Telepon 129 atau WhatsApp 08111-129-129)** setiap kali topik atau konteks menyangkut KBG.
  - **Offline-First Graceful Fallback**: Beralih otomatis ke synthesizer RAG deterministik on-device apabila daemon Ollama sedang tidak aktif tanpa memutus pengalaman pengguna.

---

## v2.4.0 — Database SQLite Multi-Domain (Zero-Collision), Login/Registrasi Khusus ASN & Fondasi RAG (2026-11-05)

Rilis ini menghadirkan **Arsitektur Database SQLite Multi-Domain Terisolasi**, sistem **Login &
Registrasi Khusus Perempuan ASN** dengan validasi anatomi 18 digit NIP standar BKN, serta
fondasi **Retrieval-Augmented Generation (RAG)** yang dijamin tidak akan bertabrakan dengan
mesin inferensi **Certainty Factor (CF)**.

### Otentikasi & Identitas Khusus Perempuan ASN (`data/auth/AsnNipValidator.kt`, `ui/screens/AuthScreen.kt`)

- **Validasi Anatomi 18 Digit NIP / NI PPPK (`AsnNipValidator.kt`)**:
  - Mengurai dan memvalidasi otomatis 8 digit pertama (`YYYYMMDD` tanggal lahir), 6 digit tengah
    (`YYYYMM` TMT pengangkatan CPNS/PPPK), digit ke-15 (`2` = Perempuan ASN), dan 3 digit nomor urut.
  - Menampilkan **Live NIP Anatomy Inspector** berfont `JetBrains Mono` di `AuthScreen.kt` saat
    pengguna mengetik 18 digit NIP.
- **Atribut Kepegawaian Birokrasi (`UserProfile` & `AuthScreen.kt`)**:
  - Pilihan status kepegawaian **`PNS`** dan **`PPPK`** beserta pilihan **Golongan/Ruang**
    (`II/c`–`IV/b` untuk PNS, `VII`–`XI` untuk PPPK), Instansi/OPD, Jabatan, dan PIN/Kata Sandi.
  - Dilengkapi tombol **Masuk Cepat Akun Demo ASN** dan tombol **Keluar Sesi ASN / Ganti Akun**
    pada `ProfileScreen.kt`.
- **Pseudonymization Firewall (`AsnSecurityHasher.deriveSubjectHash`)**:
  - Menghasilkan hash satu arah `subject_hash` (`sub_<20-hex>`) dari NIP sehingga tabel riset CF
    dan KBG tidak pernah menyimpan NIP atau nama ASN secara mentah.

### Database SQLite 4-Domain Terisolasi (`data/db/EmpatiDatabase.kt`)

- Memisahkan penyimpanan ke dalam 4 domain tabel di `empati_multi_domain.db` agar tidak saling
  menimpa (*Zero-Collision Architecture*):
  1. `auth_asn_users` — Kredensial ter-hash (**PBKDF2-HMAC-SHA256** 120.000 iterasi + salt acak 16 byte per pengguna di `password_salt`), profil kepegawaian ASN, dan `subject_hash`.
  2. `user_screening_summaries` — Ringkasan risiko kualitatif yang diisolasi ketat per `user_id` untuk Beranda & Riwayat.
  3. `research_cf_records` — Log audit CF yang diikat ke `subject_hash` dan dikunci dengan versi basis pengetahuan `kb_version = 'cf-kb-v2.0'`.
  4. `rag_knowledge_chunks` & `rag_chat_logs` — Korpus dokumen pedoman resmi dan riwayat konsultasi per pengguna beserta ID dokumen rujukan (`retrieved_chunk_ids`).

### Isolasi RAG, Zero-Leak Chat & Sitasi Interaktif (`data/rag/RagKnowledgeRepository.kt`, `data/ChatEngine.kt`, `ui/screens/ChatScreen.kt`)

- Menyediakan korpus awal terverifikasi (`DOC-HT-01` s/d `DOC-GBV-01`: Kemenkes RI, Diet DASH, Kesehatan Kerja ASN, UU TPKS & SAPA 129).
- **Strict Zero-Leak pada Intent Hasil Skrining (`ChatEngine.kt`)**: menghapus penyebutan skor numerik (`0–100`) dan hanya menyajikan `riskLevelLabel`, `integratedCategoryLabel`, dan `screeningDate` dari `ScreeningContextSnapshot`.
- **Interactive RAG Citations (`ChatScreen.kt`, `Models.kt`, `EmpatiDatabase.kt`)**:
  - `ChatMessage` kini membawa metadata `retrievedChunks: List<RagDocumentChunk>` yang juga dipulihkan dari `rag_chat_logs` saat sesi dibuka kembali.
  - Menambahkan `RagSourceCitationChip` (`MonoFont` / `JetBrains Mono`) di bawah gelembung pesan bot yang membuka `RagSourceBottomSheet` (`ModalBottomSheet` Material 3) berisi kode dokumen, judul (`Plus Jakarta Sans`), sumber resmi (`Teal600`), dan isi pedoman (`Inter`).
  - Menambahkan `GbvContextualActionStrip` berwarna `secondaryContainer` (`Teal50`) yang otomatis tampil ketika dokumen `DOC-GBV-01` / `GBV_PROTECTION` dirujuk.

### Penguatan Positif Klinis & Chip Faktor Interaktif (`data/presentation/`, `ui/screens/ResultScreen.kt`)

- **Penyaluran Faktor Protektif (`UserPresentation.kt`, `UserResultPresenter.kt`)**:
  - Menambahkan properti `protectiveFactors: List<String>` pada `UserModulePresentation` dan `UserIntegratedPresentation` yang menyalurkan maksimal 5 kebiasaan pelindung dari `ModuleResult.protectiveFactors`.
- **`ProtectiveStrengthSection` (`ResultScreen.kt`)**:
  - Menampilkan bagian **`FAKTOR PELINDUNG YANG SUDAH BAIK`** (`MonoFont` / `JetBrains Mono`) dengan deretan chip `FlowRow` berwarna `secondaryContainer` (`Teal50`) dan ikon/teks `secondary` (`Teal600`, `BodyFont` / `Inter`) pada `ModuleResultScreen` dan `IntegratedResultScreen` untuk menyeimbangkan kecemasan kesehatan (*health anxiety*) tanpa melanggar prinsip ringkasan 3 detik.
- **`ClinicalTagChip` Interaktif & `ClinicalDetailBottomSheet` (`ResultScreen.kt`, `UserResultPresenter.kt`)**:
  - Mengubah `ClinicalTagChip` menjadi komponen interaktif dengan indikator `Icons.Outlined.Info` (`14.dp`).
  - Menambahkan fungsi `UserResultPresenter.explainClinicalTag(tag)` (bebas bocoran angka desimal/`%`/`CF`/`MB`/`MD`) yang memetakan setiap faktor risiko hipertensi dan indikasi KBG ke penjelasan edukatif 1–2 kalimat awam di dalam `ModalBottomSheet` (`ClinicalDetailBottomSheet`), lengkap dengan pengingat pola keterkaitan kronis non-kausal dan pintasan `"Diskusikan di Tanya AI-EMPATI"`.

### Dokumentasi & Pengujian

- Ditambahkan dokumen master context [`PROJECT_CONTEXT_SUMMARY.md`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/PROJECT_CONTEXT_SUMMARY.md) (siap diunggah sebagai sumber pengetahuan di Gemini Notebook / NotebookLM).
- Ditambahkan dokumen baru [`DATABASE_AUTH_AND_RAG_ARCHITECTURE.md`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/DATABASE_AUTH_AND_RAG_ARCHITECTURE.md) (lengkap dengan panduan penggunaan & verifikasi Database Inspector).
- Diperbarui `EXPERT_SYSTEM.md`, `FRONTEND_DESIGN.md`, `README.md`, dan `CHANGELOG.md`.
- Ditambahkan `AsnAndRagArchitectureTest.kt` (JVM) dan `EmpatiDatabaseInstrumentedTest.kt` (Android SQLite) — **41 unit test JVM lulus** dan **6 instrumented SQLite test siap dijalankan di perangkat/emulator**.

---

## v2.3.0 — Minimalisme Klinis Hasil Skrining & Tipografi Fungsional 3-Peran (2026-11-05)

Rilis ini menyederhanakan tampilan hasil skrining secara menyeluruh agar **minimalis,
ringkas, dan profesional secara medis** (*Executive Clinical Summary*), serta mengganti
font sistem generik dengan **sistem tipografi fungsional 3-peran** menggunakan 3 font
variabel resmi yang dibundel di dalam aplikasi.

### Sistem tipografi fungsional 3-peran (`res/font/` & `ui/theme/Type.kt`)

- **Bundel 3 aset font resmi (`app/src/main/res/font/`)**:
  - `plus_jakarta_sans.ttf` (**Plus Jakarta Sans**) — `HeadingFont` untuk Display,
    Headline, Title, dan tombol utama (`labelLarge`); menghadirkan otoritas klinis yang
    modern, hangat, dan relevan dengan produk digital Indonesia.
  - `inter.ttf` (**Inter**) — `BodyFont` untuk `bodyLarge`, `bodyMedium`, dan
    `bodySmall`; memastikan keterbacaan optimal pada kuesioner, ringkasan hasil, dan chat.
  - `jetbrains_mono.ttf` (**JetBrains Mono**) — `MonoFont` untuk `labelMedium`,
    `labelSmall`, lencana status (`RiskBadge`, `StatusPill`, `SectionChip`), penghitung
    langkah kuesioner, tanggal riwayat, dan Nomor Induk Pegawai (NIP).
- **`Components.kt`, `HistoryScreen.kt`, `ProfileScreen.kt`**: penerapan `MonoFont` pada
  `RiskBadge` (uppercase berpresisi tinggi), tanggal evaluasi pada daftar riwayat, serta
  baris nilai NIP ASN.

### Minimalisme klinis pada hasil skrining (`ResultScreen.kt` & `UserResultPresenter.kt`)

- **Pemadatan teks klinis (`UserResultPresenter.kt`)**:
  - Memangkas paragraf panjang menjadi kesimpulan 1 kalimat padat yang langsung pada
    inti temuan medis.
  - Memendekkan label faktor risiko hipertensi menjadi tag ringkas: `"Riwayat keluarga"`,
    `"Kondisi medis & tensi"`, `"Pola makan & aktivitas"`, dan `"Stres & kualitas tidur"`.
  - Merangkum saran tindak lanjut menjadi maksimal 3 poin kalimat aktif yang ringkas.
  - Menyederhanakan disclaimer menjadi 1 kalimat catatan kaki (*footnote*).
- **Redesain `ModuleResultScreen` (`ResultScreen.kt`)**:
  - Menggabungkan kesimpulan utama dan temuan faktor risiko ke dalam **1 Kartu Ringkasan
    Klinis Utama** menggunakan `FlowRow` berisi `ClinicalTagChip` (menggantikan daftar
    berpoin panjang).
  - Menyajikan langkah tindak lanjut dalam `ActionPlanCard` yang bersih.
  - Menampilkan `SupportStrip` (SAPA 129) secara kontekstual hanya ketika terdapat
    catatan dukungan (`supportNote != null`), diikuti `MinimalDisclaimer` 1 baris.
- **Redesain `IntegratedResultScreen` (`ResultScreen.kt`)**:
  - Mengganti 6 kartu teks yang berulang dengan **Ringkasan Eksekutif 3-Blok**.
  - Menambahkan **Matriks Status 2-Kolom (`ModuleStatusCell`)** untuk menampilkan status
    `HIPERTENSI` dan `KESEJAHTERAAN` secara berdampingan di dalam kartu utama, lengkap
    dengan deretan `ClinicalTagChip` faktor utama.

### Dokumentasi

- Ditambahkan dokumen baru `TYPOGRAPHY_AND_CLINICAL_UX.md`.
- Diperbarui `FRONTEND_DESIGN.md` (v2.3), `EXPERT_SYSTEM.md`, `README.md`, dan
  `CHANGELOG.md`.

### Verifikasi & pengujian

- Kompilasi Kotlin Debug bersih (**0 error, 0 warning**).
- Seluruh **36 unit test** (`CfEngineTest` + `UserPresentationTest`) lulus (**0 gagal**).

---

## v2.2.0 — Modernisasi UI/UX Material You, Animasi Halus & Dokumentasi Teknis (2026-11-05)

Rilis ini memperbarui seluruh lapisan antarmuka pengguna (*front-end*) agar mengikuti
standar **Material 3 (Material You)** modern dengan estetika editorial yang tenang,
terstruktur, bebas *AI slop* (tanpa gradien dekoratif, bayangan mencolok, atau warna
klise), serta menambahkan animasi transisi dan *micro-interactions* di seluruh alur.

### Sistem desain & tema (`ui/theme/`)

- **`Color.kt`**: perluasan palet `Mauve` (*dusty plum*), `Teal`, `Amber`, `Coral`,
  serta penambahan token hirarki permukaan tonal untuk mode terang dan gelap
  (`SurfaceContainerLowest`, `SurfaceContainerLow`, `SurfaceContainerHigh`,
  `BorderSubtle`).
- **`Theme.kt`**: pemetaan penuh `surfaceContainer*` dan `outlineVariant` pada
  `LightColorScheme` dan `DarkColorScheme`, serta penambahan `EmpatiShapes`
  (`extraSmall` 8dp s/d `extraLarge` 28dp).
- **`Type.kt`**: penambahan gaya `displaySmall` dan `headlineLarge` (serif `Lora`)
  serta penyempurnaan *line-height* dan *letter-spacing* di seluruh skala tipografi.

### Komponen & *micro-interactions* (`ui/components/Components.kt`)

- **`TonalCard`**: kontainer permukaan tonal Material 3 dengan border `outlineVariant`
  1dp dan efek skala pegas (`spring`) saat ditekan pada kartu interaktif.
- **`PrimaryButton` & `GhostButton`**: umpan balik tekan berbasis pegas
  (`collectIsPressedAsState` + `animateFloatAsState`) serta dukungan ikon pengarah
  (*trailing icon*).
- **`EmpatiLogo`**: animasi cincin ganda berputar disertai efek *gentle breathing pulse*.
- **`SectionChip` & `RiskBadge`**: menggunakan permukaan kapsul semantik dengan
  kontras dan border halus.
- **`EmpatiAvatar`**: tipografi serif `Lora` pada inisial dengan cincin border tonal.

### Navigasi & animasi transisi (`EmpatiApp.kt`, `MainScaffold.kt`)

- **Transisi antar-layar (`EmpatiApp.kt`)**: `AnimatedContent` berbasis urutan hirarki
  layar (`screenRank`) yang menghadirkan animasi *shared-axis horizontal slide + fade*
  saat maju maupun kembali (*Back*).
- **Bottom navigation (`MainScaffold.kt`)**: migrasi ke komponen asli Material 3
  `NavigationBar` & `NavigationBarItem` dengan indikator kapsul `primaryContainer`,
  pasangan ikon *Outlined / Rounded Filled*, serta transisi *crossfade* antar-tab.
- Penggunaan seluruh ikon `Icons.AutoMirrored` untuk kompatibilitas arah tata letak
  dan kompilasi bersih tanpa *deprecation warnings*.

### Pembaruan layar (`ui/screens/`)

- **`SplashScreen.kt`**: animasi masuk (`fadeIn` + `scaleIn` + `slideInVertically`)
  dan chip identitas `KESEHATAN PEREMPUAN ASN`.
- **`OnboardingScreen.kt`**: indikator langkah (`Langkah X dari 3`), efek paralaks
  skala & opasitas saat menggeser `HorizontalPager`, indikator halaman pegas
  (`animateDpAsState`), serta tombol `Selanjutnya` / `Mulai Sekarang` yang selalu
  hadir di setiap slide.
- **`HomeScreen.kt`**: kartu ringkasan risiko dengan penjelasan kualitatif awam,
  tombol cepat `Skrining Ulang`, grid layanan utama, dan banner rujukan privasi /
  SAPA 129.
- **`ConsentScreen.kt`**: daftar komitmen persetujuan berbasis ikon semantik (tanpa
  penomoran dekoratif) yang dipisahkan oleh `HorizontalDivider`.
- **`QuestionnaireScreen.kt`**:
  - Transisi geser horizontal antar-pertanyaan (`AnimatedContent`).
  - Progress bar beranimasi mulus (`animateFloatAsState`).
  - Baris opsi (`OptionRow`) dengan animasi warna latar, ketebalan border, tinggi
    bilah aksen vertikal (`animateDpAsState`), dan kemunculan ikon centang (`scaleIn`).
  - Tombol lanjut muncul secara halus dengan `AnimatedVisibility`.
- **`ResultScreen.kt`**: kartu kesimpulan utama, `SectionCard` berikon semantik,
  kartu dukungan rahasia (`secondaryContainer`), dan `DisclaimerBox` tonal—tetap
  menjaga pemisahan ketat tanpa menampilkan nilai CF/MB/MD.
- **`ChatScreen.kt`**: header permukaan tonal, deretan `SuggestionChip` Material 3,
  animasi item pesan (`Modifier.animateItem`), dan komposer input kapsul modern
  dengan tombol kirim yang berubah warna secara reaktif.
- **`HistoryScreen.kt`**: kartu riwayat kronologis dengan penanda evaluasi terbaru
  dan *empty state* yang dilengkapi tombol mulai skrining.
- **`ProfileScreen.kt`**: pengelompokan kartu *Data Kepegawaian* dan *Pengaturan &
  Keamanan*, dialog edit modern, serta tombol hapus data permanen berwarna
  `errorContainer`.

### Dokumentasi proyek

- Ditambahkan `README.md`, `FRONTEND_DESIGN.md` (diperbarui ke v2.2), dan
  `EXPERT_SYSTEM.md`.

### Verifikasi & pengujian

- Kompilasi Kotlin Debug bersih (**0 error, 0 warning**).
- Seluruh **36 unit test** (`CfEngineTest` + `UserPresentationTest`) lulus (**0 gagal**).

---

## v2.1.0 — Pemisahan Output Penelitian dan Output Pengguna (2026-11-04)

Pengguna kini hanya melihat kesimpulan skrining berbahasa awam. Seluruh nilai teknis
Certainty Factor tetap dihitung dan disimpan untuk kepentingan penelitian, tetapi
**tidak lagi ditampilkan di UI**.

### Lapisan presentasi baru (user-facing)

- `data/presentation/UserPresentation.kt`:
  `UserModulePresentation`, `UserIntegratedPresentation`, `UserRiskSummary`,
  `PresentationTone`.
- `data/presentation/UserResultPresenter.kt`: menerjemahkan model internal menjadi
  bahasa awam tanpa meneruskan CF, MB/MD, bobot, ID evidence/rule, atau istilah teknis.
- UI hanya menerima model presentasi; tidak mengakses model perhitungan internal.

### Yang dihapus dari tampilan pengguna

- Ring gauge dan nilai CF desimal/persentase.
- Kartu ringkasan CF per modul.
- Rincian "CF per kategori" dan label "(CF 0.xx)".
- Jumlah "Terjawab X dari Y" pada hasil.
- Skor numerik pada kartu Home (kini hanya tingkat risiko kualitatif + tanggal).

### Yang ditampilkan kepada pengguna

- Kesimpulan skrining sederhana per modul.
- Faktor yang perlu diperhatikan (tanpa angka internal).
- Jenis pengalaman KBG yang terindikasi (bahasa tidak menghakimi).
- Saran tindak lanjut dan dukungan (termasuk SAPA 129).
- Kategori analisis terintegrasi versi awam:
  - Risiko Hipertensi dengan Dominasi Faktor Kesehatan dan Gaya Hidup
  - Risiko Hipertensi dengan Indikasi Keterkaitan Pengalaman KBG
  - Risiko Hipertensi Rendah
  - Indikasi KBG Tanpa Indikasi Risiko Hipertensi
  - Hasil Belum Dapat Disimpulkan
- Disclaimer: skrining, bukan diagnosis, bukan penetapan sebab-akibat.

### Data penelitian internal (tetap tersimpan)

- `data/research/ResearchModels.kt`: `ResearchRecord`, `ResearchModule`,
  `ResearchCategory`, `ResearchEvidence`, `ResearchRecordFactory`.
- `ScreeningStore.saveResearchRecord()` / `loadResearchRecords()` menyimpan
  CF, level, kategori, evidence (questionId, cfUser, cfRule, cfEvidence), dan
  klasifikasi internal pada penyimpanan privat aplikasi.
- Data internal tetap dapat diakses untuk penelitian; tidak dirender di UI.
- Tidak ada pencatatan jawaban sensitif ke log aplikasi.

### Privasi

- Detail KBG tidak ditampilkan melebihi ringkasan awam.
- `FLAG_SECURE` tetap aktif.
- Data penelitian dipisahkan dari tampilan publik dan hanya tersedia secara programatik.

### Pengujian

- Ditambah `UserPresentationTest` — 6 test (bahasa awam, tidak ada leak CF/MB/MD/%/desimal,
  pemisahan modul, konsistensi label kategori, ringkasan risiko, retensi data internal).
- Total **36 unit test, 0 gagal**.

---

## v2.0.0 — Dual-Module Certainty Factor Expert System (2026-11-04)

Rilis ini menggantikan mesin skor tunggal (24 pertanyaan) dengan **sistem pakar
deterministik dua modul berbasis Certainty Factor (CF)**. ID pertanyaan mengikuti
referensi `EMPATIDualInputParser`.

### Ringkasan perubahan

- **Modul 1 — Hipertensi**: 16 pertanyaan `M01–M16`.
- **Modul 2 — Kekerasan Berbasis Gender (KBG)**: 10 pertanyaan `K01–K10`.
- **Total 26 pertanyaan**; seluruh ID, kategori, tipe jawaban, dan makna dipertahankan.
- Mesin skor lama tetap ada sebagai kode legacy tetapi tidak lagi dipakai alur UI.

### Input parser (padanan Python)

- Dikotomi: `Ya = 1.0`, `Tidak = 0.0`.
- Likert 5: `1=0.0, 2=0.2, 3=0.5, 4=0.8, 5=1.0`.
- Reverse-scored `M08`: `1=1.0, 2=0.8, 3=0.5, 4=0.2, 5=0.0`.
- Jawaban kosong **tidak** dikonversi menjadi 0.0 (tetap `Unanswered`).
- Input tidak valid menghasilkan `ValidationIssue` yang eksplisit.

### Knowledge base & inference

- `CfRule(id, questionId, hypothesis, MB, MD, source, expertValidated, note)`.
- CF pakar = `MB - MD`; CF evidence = `CF_User × CF_Rule`.
- Kombinasi: positif `CF1 + CF2×(1-CF1)`, negatif `CF1 + CF2×(1+CF1)`,
  konflik `(CF1+CF2)/(1-min(|CF1|,|CF2|))`.
- Inferensi dipisah per **sub-hipotesis/kategori** (bukan satu nilai untuk 26 pertanyaan).
- Modul hipertensi: Riwayat & Genetik, Kondisi Medis, Gaya Hidup, Faktor Psikososial.
- Modul KBG: Fisik, Psikologis, Seksual, Ekonomi, Digital.

### Analisis terintegrasi

- Kategori target: Hipertensi Murni Gaya Hidup, Hipertensi dengan Indikasi Keterkaitan KBG,
  Risiko Rendah/Normal.
- Kategori tambahan: Indikasi KBG Tanpa Hipertensi, Hipertensi dengan Riwayat/Kondisi Medis,
  dan `INCONCLUSIVE` bila data belum lengkap.
- Keterkaitan KBG–hipertensi dinyatakan sebagai pola yang perlu diteliti, **bukan kausalitas**.

### UI Android

- Alur baru: Persetujuan → Kuesioner Hipertensi (M01–M16) → Hasil Hipertensi →
  Kuesioner KBG (K01–K10) → Hasil KBG → Analisis Terintegrasi → Simpan.
- Progress bar per modul, jawaban tersimpan saat berpindah pertanyaan, kembali tanpa kehilangan jawaban.
- Pertanyaan KBG menampilkan penanda privasi. Fitur AI Assistant (ChatEngine) tetap berfungsi.

### Pengujian

- `CfEngineTest` — **30 unit test, 0 gagal** (parser, mapping, reverse M08, validasi,
  jawaban kosong, formula CF positif/negatif/konflik, hasil modul, integrasi, determinisme).
- Contoh Python diverifikasi identik: `{M01:1.0, M07:0.8, M08:1.0}` dan `{K03:0.8, K01:0.0}`.

### Catatan validasi pakar

- Seluruh nilai `MB/MD` masih **PLACEHOLDER** dan ditandai `NEEDS_EXPERT_VALIDATION`.
  Bobot, ambang kategori, dan aturan integrasi wajib ditinjau pakar sebelum klaim klinis.

---

# Riwayat Rilis

## v1.0.0 — Rilis Awal

Semua perubahan di bawah ini merujuk pada rilis **v1.0.0** aplikasi Android AI-EMPATI
(skrining risiko hipertensi berbasis gender untuk perempuan ASN di Indonesia).

---

## 1. Ringkasan (Overview)

Rilis v1.0.0 menghadirkan dua alur kerja utama yang saling terhubung:

1. **Skrining deterministik (expert system)** — kuesioner 24 pertanyaan yang
   menghitung skor risiko 0–100, mengelompokkannya ke dalam empat tingkat risiko,
   lalu menghasilkan kartu wawasan dan rekomendasi tindak lanjut.
2. **Chatbot kesehatan terbatas** — mesin aturan (*rule-based*) yang menjawab
   pertanyaan seputar hipertensi, gaya hidup, stres, dan kekerasan berbasis gender,
   termasuk mengaitkan jawaban dengan hasil skrining pengguna.

Perubahan kunci pada rilis ini mencakup penambahan mesin skrining lengkap, mesin
chatbot, tema Material 3 (mode terang/gelap), perlindungan privasi, serta
penyimpanan data lokal.

> **Catatan model AI:** chatbot pada v1.0.0 **bukan** model bahasa besar (LLM)
> eksternal. Ia adalah *intent engine* deterministik berbasis kata kunci yang
> dirancang agar respons tetap dalam cakupan (*scoped*) dan dapat diprediksi.
> Bagian "AI Model" di bawah mendokumentasikan parameter mesin aturan ini serta
> titik integrasi untuk LLM di masa mendatang.

---

## 2. Pembaruan Logika Skrining (Screening Logic / Algorithmic Updates)

### 2.1 Kuesioner ahli deterministik

- Ditambahkan bank pertanyaan **24 item** yang dikelompokkan dalam empat bagian:

  | Bagian | Rentang ID | Jumlah | Sifat |
  |---|---|---|---|
  | Riwayat Kesehatan | 1–6 | 6 | Umum |
  | Gaya Hidup | 7–11 | 5 | Umum |
  | Faktor Risiko | 12–16 | 5 | Umum |
  | Kekerasan & Kesejahteraan | 17–24 | 8 | Sensitif (GBV) |

- Jenis jawaban yang didukung:
  - **Ya/Tidak** (`yesNo`) — bobot poin positif 0–4.
  - **Frekuensi 5 tingkat** (`freq`) — "Tidak pernah" s/d "Sangat sering",
    poin 0–4 (beberapa pertanyaan dinilai terbalik, mis. aktivitas fisik).

### 2.2 Formula skor risiko

Skor risiko dihitung sebagai persentase poin yang diperoleh terhadap poin
maksimum yang mungkin:

```
skor = round((totalPoin / maxPoin) * 100), dijepit 0–100
```

- `totalPoin` = jumlah poin dari semua jawaban terpilih.
- `maxPoin` = jumlah poin tertinggi setiap pertanyaan.
- Nilai `maxPoin` untuk kuesioner saat ini adalah **78** (42 poin dari pertanyaan
  Ya/Tidak + 36 poin dari 9 pertanyaan frekuensi × maks 4).

### 2.3 Klasifikasi tingkat risiko

| Skor | Tingkat Risiko | Warna indikator |
|---|---|---|
| 0–24 | Rendah | Teal |
| 25–49 | Sedang | Amber |
| 50–74 | Tinggi | Coral |
| 75–100 | Sangat Tinggi | Coral (lebih pekat) |

### 2.4 Bobot faktor GBV

Pertanyaan kekerasan berbasis gender (17–24) diberi bobot lebih tinggi untuk
mencerminkan dampaknya terhadap hipertensi:

- Kekerasan fisik/verbal/emosional (Q17): bobot maks **4**.
- Perasaan dipaksa oleh pasangan (Q21): bobot maks **4**.
- Kurang dukungan emosional (Q19): bobot maks **3**.
- Tidak memiliki akses aman untuk bantuan (Q23): **"Tidak" = 3 poin**,
  "Ya" = 0 poin (skor protektif).

### 2.5 Pembangkitan wawasan dan rekomendasi

- **Wawasan (insight)** dipilih dari empat kandidat berdasarkan poin yang diperoleh:
  Tekanan Darah, Pengalaman Stres & Kekerasan, Riwayat Keluarga, dan Gaya Hidup &
  Stres. Maksimal **3 kartu** ditampilkan, dengan fallback default bila kandidat
  kurang dari tiga.
- **Rekomendasi** dibedakan menurut tingkat risiko; bila risiko GBV terdeteksi,
  rekomendasi tambahan berisi rujukan ke **SAPA 129** ditambahkan.

---

## 3. Model AI Chatbot (Chatbot AI Models)

### 3.1 Versi mesin

| Komponen | Versi | Tipe | Status |
|---|---|---|---|
| Mesin chatbot | `rule-engine v1.0` | Deterministik, berbasis kata kunci | Aktif |
| LLM eksternal | — | Belum terhubung | Rencana |

### 3.2 Pipeline pemrosesan input

1. Normalisasi teks menjadi huruf kecil dengan locale `id-ID`.
2. Pencocokan kata kunci berurutan (aturan pertama yang cocok menang).
3. Penyisipan hasil skrining bila pertanyaan menyebut "hasil", "skor", atau "skrining".
4. Fallback terbatas bila tidak ada aturan yang cocok.

### 3.3 Taksonomi intent & kata kunci

Urutan prioritas (dari atas ke bawah):

| Prioritas | Intent | Kata kunci pemicu | Respons |
|---|---|---|---|
| 1 | Hasil skrining | hasil, skor, skrining | Ringkasan risiko terakhir atau ajakan skrining |
| 2 | Pencegahan | cegah, pencegahan, mencegah | Tips pencegahan hipertensi |
| 3 | Gejala | gejala, tanda | Penjelasan gejala hipertensi |
| 4 | Pola makan | garam, diet, makan | Anjuran pembatasan garam & gizi |
| 5 | Stres | stres, cemas, tekan | Manajemen stres |
| 6 | Aktivitas | olahraga, aktivitas, gerak | Anjuran aktivitas fisik |
| 7 | Kekerasan/keamanan | kekerasan, aman, bantuan, sapa, kdrt | Dukungan & rujukan SAPA 129 |
| 8 | Definisi | hipertensi, darah tinggi | Definisi hipertensi |
| 9 | Sapaan | halo, hai, selamat, assalam | Sambutan pembuka |
| — | Fallback | (tidak cocok) | Pernyataan cakupan terbatas |

### 3.4 Parameter dan cakupan

- **Cakupan topik:** hipertensi, gaya hidup, stres, dan kesehatan gender saja.
- **Konteks hasil:** respons intent "hasil" menggunakan `lastResult` untuk
  menyisipkan `level` dan `score` (contoh: "risiko Sedang dengan skor 62 dari 100").
- **Respons aman:** seluruh respons menegaskan sifat edukatif (bukan diagnosis).
- **Determinisme:** respons untuk input yang sama selalu identik.

### 3.5 Titik integrasi LLM (untuk rilis berikutnya)

Arsitektur saat ini memisahkan `ChatEngine.reply()` dari UI, sehingga mesin
aturan dapat diganti dengan model eksternal tanpa mengubah lapisan tampilan.
Jika LLM dihubungkan nanti, disarankan mempertahankan:
- lapisan *scoping* topik yang sama (penolakan di luar hipertensi/GBV),
- pengingat "bukan diagnosis medis",
- injeksi `lastResult` sebagai konteks skrining.

---

## 4. Perubahan Antarmuka & Pengalaman Pengguna (UX/UI)

### 4.1 Layar baru

- **Splash** — logo perisai + siluet, nama "AI-EMPATI" (serif), tagline, ring teal
  beranimasi.
- **Onboarding** — 3 slide yang dapat digeser (HorizontalPager): Ruang Aman,
  Skrining Risiko, Tanya AI-EMPATI, dengan CTA "Mulai Sekarang".
- **Home Dashboard** — sapaan berbasis waktu, kartu status risiko (termasuk state
  "Belum ada data skrining"), dua kartu fitur, navigasi bawah 5 tab.
- **Persetujuan (Informed Consent)** — dokumen yang dapat digulir, ikon gembok,
  tombol "Saya Setuju" / "Batalkan".
- **Kuesioner** — satu pertanyaan per layar, bar progres, chip bagian, opsi kartu
  besar, notifikasi "Jawaban Anda aman & rahasia" untuk pertanyaan sensitif.
- **Hasil** — ring gauge skor 0–100, label kategori risiko, kartu wawasan, rekomendasi,
  CTA "Simpan Hasil" dan "Tanya AI-EMPATI".
- **Chat** — header dengan status online, badge disclaimer, gelembung pengguna/bot,
  chip balasan cepat, kolom input + tombol kirim.
- **Riwayat** — timeline hasil (terbaru di atas) + state kosong.
- **Profil** — avatar inisial, kolom NIP/Instansi/Jabatan (dapat diedit), pengaturan
  Keamanan/Notifikasi/Tentang, zona berbahaya "Hapus Semua Data Saya".

### 4.2 Tema & aksesibilitas

- Sistem desain Material 3 dengan palet mauve (primer), teal (sekunder), amber/coral
  (tingkat risiko). Mode **terang dan gelap** didukung.
- Tipografi: serif untuk judul/konten penting, sans untuk isi; skala 24/18/14/12sp.
- Target sentuh minimum 48dp pada tombol dan item navigasi.
- Penghormatan area aman (status bar & navigation bar) melalui `safeDrawingPadding()`.

### 4.3 Privasi & keamanan

- `FLAG_SECURE` diaktifkan global agar konten sensitif tidak tampil di app switcher.
- Data disimpan lokal (SharedPreferences) untuk riwayat skrining, profil, dan status
  onboarding.
- Pertanyaan GBV menampilkan penanda privasi eksplisit.

---

## 5. Analisis Dampak (Impact Analysis)

### 5.1 Performa

- **Latensi skrining:** perhitungan skor bersifat O(n) atas 24 pertanyaan dan
  berjalan dalam mikrodetik; tidak ada beban jaringan.
- **Latensi chatbot:** pencocokan kata kunci O(k × r) dengan k jumlah kata kunci
  dan r jumlah aturan; respons instan tanpa panggilan jaringan.
- **Ukuran paket:** meningkat karena dependensi `material-icons-extended`
  (sekumpulan ikon vektor) — dapat dioptimalkan dengan ikon kustom bila diperlukan.

### 5.2 Akurasi

- **Skrining:** sepenuhnya deterministik dan dapat direproduksi; akurasi bergantung
  pada validitas bobot klinis yang dipilih. Perlu validasi oleh tenaga kesehatan
  sebelum digunakan sebagai alat kesehatan formal.
- **Chatbot:** akurasi intent bergantung pada kecocokan kata kunci dan rentan
  terhadap parafrase yang tidak tercakup; determinisme menjamin konsistensi tetapi
  membatasi pemahaman bahasa alami.

### 5.3 Interaksi pengguna

- Satu pertanyaan per layar mengurangi beban kognitif pada topik sensitif.
- Tombol "Lanjut" hanya muncul setelah jawaban dipilih, mencegah jawaban terlewat.
- State kosong (Home & Riwayat) memberi arah tindakan yang jelas.
- Tanda privasi (gembok, notifikasi rahasia, FLAG_SECURE) meningkatkan rasa aman.

---

## 6. Detail Teknis (Model Versions & Parameters)

### 6.1 Parameter skrining

| Parameter | Nilai |
|---|---|
| Jumlah pertanyaan | 24 |
| Bagian | 4 |
| Skala skor | 0–100 |
| Denominator `maxPoin` | 78 |
| Ambang Rendah | < 25 |
| Ambang Sedang | 25–49 |
| Ambang Tinggi | 50–74 |
| Ambang Sangat Tinggi | ≥ 75 |
| Bobot Ya/Tidak | 2–4 |
| Bobot frekuensi | 0–4 (beberapa terbalik) |
| Bobot GBV tertinggi | 4 (Q17, Q21) |
| Kartu wawasan maksimal | 3 |

### 6.2 Parameter mesin chatbot

| Parameter | Nilai |
|---|---|
| Versi mesin | rule-engine v1.0 |
| Jumlah intent | 9 + fallback |
| Normalisasi input | lowercase, locale `id-ID` |
| Strategi pencocokan | kata kunci, first-match-wins |
| Cakupan topik | hipertensi, gaya hidup, stres, kesehatan gender |
| Konteks skrining | `lastResult.level`, `lastResult.score` |
| Sifat respons | edukatif, bukan diagnosis |

### 6.3 Tumpukan teknologi

| Komponen | Versi |
|---|---|
| Kotlin | 2.2.10 |
| Android Gradle Plugin | 9.5.0 |
| Compose BOM | 2026.02.01 |
| Material 3 | (dikelola BOM) |
| material-icons-extended | (dikelola BOM) |
| lifecycle-viewmodel-compose | 2.6.1 |
| minSdk / targetSdk / compileSdk | 24 / 37 / 37 |

---

## 7. Catatan & Rencana Berikutnya

- Validasi bobot klinis dan ambang risiko bersama tenaga kesehatan.
- Pertimbangkan penggantian mesin aturan chatbot dengan LLM eksternal sambil
  mempertahankan cakupan topik dan pengaman respons.
- Bundel font **Lora** asli (saat ini menggunakan `FontFamily.Serif` sebagai
  pengganti) untuk kesetiaan tipografi penuh.
- Tambahkan enkripsi penyimpanan lokal dan mekanisme ekspor/hapus data yang lebih
  granular.
