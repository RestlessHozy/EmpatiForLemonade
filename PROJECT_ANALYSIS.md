# Analisis Project — SRYA Emora (sebelumnya AI-EMPATI, kondisi aktual v4.1.5 — Siap Deploy dengan Supabase Cloud & OpenRouter)

> **Catatan sinkronisasi.** Dokumen ini mencerminkan kondisi aktual kode pada
> `app/src/main/java/com/example/empati/` dan `supabase/` per rilis **v4.1.5**
> (termasuk ergonomi penempatan chatbox selalu di atas keyboard via `imePadding` dan penonaktifan navbar saat mengetik,
> opsi salin teks menyeluruh via `SelectionContainer` dan `ContentCopy` pada bubble dan composer,
> analisis & kesimpulan hasil skrining kontekstual oleh LLM melalui `ScreeningContextSnapshot`,
> migrasi live tabel `public.user_accounts` di server Supabase `qnvcuhmvrrffzdxxdloc`,
> perbaikan izin upsert pada `public.research_cf_records`, pemulihan akun dari cloud saat login,
> rebranding nama aplikasi & chatbot menjadi **SRYA Emora**, penghapusan akun demo,
> humanisasi bahasa anti *AI-slop* di seluruh layar, serta dukungan *screenshot/screen record* dengan kode `FLAG_SECURE`
> yang tetap disimpan di `MainActivity.ENABLE_SCREENSHOT_PROTECTION = false`).
> Seluruh dokumentasi pendamping (`FRONTEND_DESIGN.md`, `TYPOGRAPHY_AND_CLINICAL_UX.md`,
> `DATABASE_AUTH_AND_RAG_ARCHITECTURE.md`, `EXPERT_SYSTEM.md`, `PROJECT_CONTEXT_SUMMARY.md`,
> `README.md`, dan `CHANGELOG.md`) telah disinkronkan penuh dengan dokumen ini.

---

## 1. Ringkasan

**SRYA Emora** (sebelumnya *AI-EMPATI*) adalah aplikasi Android (Jetpack Compose, Material 3, sistem desain **"Soft Daylight" v4.1**) untuk skrining risiko hipertensi dan kesejahteraan psikososial dengan dua modul: skrining hipertensi (M01–M16) dan skrining Kekerasan Berbasis Gender / KBG (K01–K10). Aplikasi memadukan:

- **Sistem pakar deterministik** berbasis *Certainty Factor* (CF) — internal, untuk penelitian disertasi.
- **Lapisan presentasi** (`UserResultPresenter`) yang menerjemahkan hasil CF ke bahasa awam — satu-satunya yang dilihat pengguna.
- **Chatbot LLM Murni (`SRYA Emora`)** yang terhubung ke **OpenRouter Cloud LLM** (melalui **Supabase Edge Function `empati-chat`** dengan prioritas *Free-Tier Smart Router* `openrouter/free`, dukungan Ollama lokal, dan *keyless cloud fallback*), dilengkapi pembatasan topik (*Kesehatan, Psikologi, Kekerasan*) dan guardrail privasi.
- **Autentikasi multi-format inklusif** (Email, No. HP, Username, NIK 16 digit, atau NIP 18 digit + PIN PBKDF2) serta penyimpanan **Hybrid: SQLite Lokal v3 + Sinkronisasi Otomatis ke Cloud Supabase (`empati-research`)**.
- **Aset Logo Multi-Resolusi (`srya_emora_logo_transparent` & `srya_emora_logo_bg`)**: Monogram **"SA"** bergradasi biru-toska dengan ruang negatif berbentuk hati dan ekor balon percakapan, tersedia dalam varian tanpa background (untuk `SplashScreen`, `AuthScreen`, dan `ic_launcher_foreground` 108dp–432dp) serta varian dengan background putih (untuk `CompanionAvatar` 48dp, `CompanionMark` 32dp di `ChatScreen`, serta `ic_launcher` / `ic_launcher_round` 48dp–192dp).

---

## 2. Arsitektur & struktur paket

```text
com.example.empati
├── MainActivity.kt              # ComponentActivity, edge-to-edge, FLAG_SECURE
├── EmpatiApp.kt                 # Root composable + routing (Screen sealed interface)
├── EmpatiViewModel.kt           # State & navigasi; jembatan internal ↔ presentasi
├── data
│   ├── Models.kt                # ScreeningResult, RiskLevel, UserProfile, ChatMessage
│   ├── ScreeningStore.kt        # SharedPreferences: sesi, riwayat, chat log, research record
│   ├── ChatEngine.kt            # Orkestrasi balasan chat → OllamaRagService
│   ├── ScreeningEngine.kt       # (legacy) mesin skor lama
│   ├── ScreeningQuestions.kt    # (legacy) bank 24 pertanyaan lama
│   ├── auth/AsnNipValidator.kt  # Validasi NIP/identitas ASN
│   ├── db/EmpatiDatabase.kt     # Akun, kredensial (hash), sesi, akun demo
│   ├── cf/                      # Sistem pakar Certainty Factor (INTERNAL)
│   │   ├── CfModels.kt
│   │   ├── CfQuestions.kt        # M01–M16, K01–K10
│   │   ├── CfInputParser.kt      # CF_User (dikotomi/Likert/reverse M08)
│   │   ├── CfKnowledgeBase.kt    # CfRule (MB/MD) — placeholder NEEDS_EXPERT_VALIDATION
│   │   ├── CfInferenceEngine.kt  # kombinasi CF positif/negatif/konflik
│   │   └── CfResultGenerator.kt  # ModuleResult + IntegratedAnalysis
│   ├── presentation/            # Model user-facing (TANPA nilai CF)
│   │   ├── UserPresentation.kt
│   │   └── UserResultPresenter.kt
│   ├── research/ResearchModels.kt  # ResearchRecord (internal, utk disertasi)
│   ├── cloud/SupabaseCloudSyncService.kt # Sinkronisasi ke Supabase (research_cf_records & research_chat_logs) + proxy empati-chat
│   └── rag/
│       ├── OllamaRagService.kt     # Klien LLM (OpenRouter + Supabase Edge Function + Ollama lokal), guardrail, sanitasi
│       ├── WebRagSearchService.kt  # RAG web: Wikipedia ID + Europe PMC
│       └── RagKnowledgeRepository.kt # Korpus lokal, topic scoping, deteksi GBV
├── supabase/
│   ├── migrations/20261108000001_create_empati_research_schema.sql # Skema Postgres & RLS Write-Only
│   └── functions/empati-chat/index.ts # Edge Function OpenRouter proxy (Free-Tier Smart Routing)
└── ui
    ├── theme/  (Color, Type, Theme, Tokens)   # Design system "Soft Daylight" v4
    ├── components/Components.kt               # Pustaka komponen
    └── screens/  (11 layar)
```

**Prinsip lapisan:** UI → hanya membaca model `presentation/`. Perhitungan CF, MB/MD,
bobot, dan ID evidence/rule tetap hidup di `data/cf/` dan `data/research/` tetapi tidak
pernah dirender ke pengguna.

---

## 3. Frontend / UI-UX (kondisi aktual)

### 3.1 Design system "Soft Daylight" (v4)

Tiga lapisan warna satu arah: **Palettes** (ram tonal mentah) → **Roles** (Material 3
`ColorScheme`) → **Semantics** (`MaterialTheme.empati`, tone afektif).

Cerita hue: **Sage** (calm/brand) · **Clay** (kehangatan manusiawi) · **Mist** (presensi AI)
· **Stone** (netral warm-grey kehijauan). Tema terang = "daylight on paper"; tema gelap =
"quiet evening" (kanvas green-black, bukan ungu).

### 3.2 Tipografi (`ui/theme/Type.kt`)

| Peran | Font | Penggunaan |
|---|---|---|
| `DisplayFont` | **Fraunces** (serif, optical-size, sumbu SOFT/WONK) | sapaan, headline, pernyataan hasil, nama aplikasi |
| `BodyFont` | **Inter** | judul, isi, label, tombol, chip |
| `MetricFont` | **JetBrains Mono** | HANYA data mentah: ID akun, kode sitasi |

Skala: `displayLarge 48/54` … `labelSmall 11/16`. `EmpatiMetricStyle` (13sp mono) untuk
identifier; `EmpatiNumeralStyle` (Inter tabular `tnum`) untuk angka yang berubah agar tidak "goyang".
Label memakai sentence-case (bukan ALL-CAPS).

> Catatan: dokumen lama menyebut **Lora** — sudah tidak dipakai.

### 3.3 Token (`ui/theme/Tokens.kt`)

- **Space** (grid 4dp): `xxs 4 · xs 8 · sm 12 · md 16 · lg 20 · xl 24 · xxl 32 · huge 40 · giant 56`; `gutter 24`; `readableWidth 560`; `touchTarget 48`.
- **Icon**: `sm 16 · md 20 · lg 24 · xl 32`.
- **Elevation**: `resting 0 · raised 3 · floating 10` (mayoritas tonal, bayangan nyata hanya untuk elemen mengambang).
- **Motion**: `Instant 90 · Quick 160 · Standard 280 · Expressive 520 · Reveal 720`; easing `EaseOutSoft (0.22,1,0.36,1)`; `press()` spring low-bouncy; `settle()` spring no-bouncy.
- **Shapes** (`EmpatiShapes`): `xs 10 · sm 14 · md 20 · lg 28 · xl 36`.
- **Reduced motion**: `LocalReducedMotion` + `rememberSystemReducedMotion()` (mengikuti setelan "Remove animations"); motion dekoratif collapse ke end-state.
- `Modifier.softShadow(elevation, shape, tint)` — bayangan ambient berwarna brand.

### 3.4 Warna & tone afektif (`MaterialTheme.empati`)

| Token | Arti | Accent (light) |
|---|---|---|
| `calm` | risiko rendah / positif / protektif | Sage40 `#2E6A57` |
| `attentive` | risiko sedang / perlu dipantau | Honey60 `#D29A2E` |
| `concerned` | risiko tinggi / perlu perhatian | Terra40 `#A8402B` |
| `urgent` | sangat tinggi / segera cari pertolongan | Berry40 `#8E2F55` |
| `neutral` | belum lengkap / informasional | Stone outline |
| `presence` | identitas AI companion (chat, "thinking") | Mist40 `#5A5896` |

Netral: background terang `#F7F5F0`, gelap `#121512`. Setiap pasangan teks/latar ≥ 4.5:1;
outline kontrol ≥ 3:1.

### 3.5 Peta layar & navigasi

Routing memakai `Screen` (sealed interface) + `backStack` di `EmpatiViewModel`
(bukan Navigation-Compose). Alur awal: `Splash → (Onboarding) → Auth → Home`.

| Layar | File | Isi utama |
|---|---|---|
| Splash | SplashScreen.kt | logo + nama + tagline, ring animasi |
| Onboarding | OnboardingScreen.kt | 3 slide swipeable |
| **Auth** | AuthScreen.kt | Masuk/Daftar (segmented), validasi NIP/identitas real-time, PIN (show/hide), progressive disclosure data diri, bersih siap produksi tanpa akun demo, notice privasi |
| Home | HomeScreen.kt | sapaan, kartu status risiko (kualitatif), dua kartu fitur |
| Consent | ConsentScreen.kt | dokumen persetujuan, ikon gembok |
| Kuesioner dual-modul | QuestionnaireScreen.kt | satu pertanyaan/layar, progress, chip kategori, opsi besar, notice sensitif |
| Hasil modul | ResultScreen.kt (`ModuleResultScreen`) | kesimpulan awam, faktor, saran (TANPA CF) |
| Hasil terintegrasi | ResultScreen.kt (`IntegratedResultScreen`) | kategori awam, ringkasan kedua modul, rekomendasi |
| **Chat** | ChatScreen.kt | header presensi (`CompanionAvatar` + titik online + judul `AI-EMPATI`, tanpa lencana nama model teknis), scope notice, bubble, thinking indicator (`BreathingDots`), quick replies (4, termasuk SAPA 129), composer mengambang |
| Riwayat | HistoryScreen.kt | timeline (tanggal + badge tingkat) |
| Profil | ProfileScreen.kt | identitas, pengaturan, logout, hapus data |

Bottom navigation (MainScaffold): Home · Skrining · Chat · Riwayat · Profil.

### 3.6 Pustaka komponen (`ui/components/Components.kt`)

`ScreenFrame`, `TonalCard`, `SoftNotice`, `SoftDivider`, `SectionLabel`, `SegmentedSwitch`,
`ChoiceChip`, `EmpatiTextField`, `VisibilityToggle`, `Reveal` (staggered entrance),
`PrimaryButton` (dukung `loading`/`enabled`/`trailingIcon`), `GhostButton`, `EmpatiLogo`,
`BreathingDots`, `EmpatiSurfaces` (card/inset/hairline), `focusHalo`, `pressScale`, `errorTone`,
`RiskBadge`, `BackIconButton`, `LockNotice`, `EmpatiAvatar`.

### 3.7 Aksesibilitas & privasi tampilan

- **Screenshot & Screen Record Diizinkan (`ENABLE_SCREENSHOT_PROTECTION = false`)**: Tangkapan dan perekaman layar diizinkan secara default, namun kode `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)` tetap disimpan di `MainActivity.kt` agar dapat diaktifkan kembali kapan saja di masa depan.
- Target sentuh ≥ 48dp; `minimumInteractiveComponentSize()`.
- `liveRegion` pada notice penting; `semantics { heading() }` pada judul.
- Hasil sensitif (KBG) disajikan dengan bahasa sehari-hari yang hangat dan tidak menghakimi; nilai internal tidak ditampilkan.

---

## 4. Sistem pakar Certainty Factor (INTERNAL)

- **Modul 1 Hipertensi**: M01–M16 (Riwayat Kesehatan, Gaya Hidup, Faktor Risiko).
- **Modul 2 KBG**: K01–K10 (Fisik, Psikologis, Seksual, Ekonomi, Digital).
- **CF_User**: dikotomi Ya=1.0/Tidak=0.0; Likert 1..5 → 0.0/0.2/0.5/0.8/1.0; M08 reverse.
- **CF evidence** = `CF_User × (MB − MD)`; kombinasi positif/negatif/konflik sesuai formula CF.
- **Ambang tingkat**: <0.25 Rendah · 0.25–0.49 Sedang · 0.50–0.74 Tinggi · ≥0.75 Sangat Tinggi.
- **Bobot MB/MD masih placeholder** (`NEEDS_EXPERT_VALIDATION`).

## 5. Lapisan presentasi (pemisahan riset vs pengguna)

`UserResultPresenter` memetakan `ModuleResult`/`IntegratedAnalysis` → `UserModulePresentation`,
`UserIntegratedPresentation`, `UserRiskSummary`. Tidak ada CF/MB/MD/bobot/ID rule yang diteruskan.
Kategori terintegrasi versi awam: (1) Dominasi Faktor Kesehatan & Gaya Hidup, (2) Indikasi
Keterkaitan KBG, (3) Risiko Rendah, (4) KBG Tanpa Hipertensi, (5) Belum Dapat Disimpulkan.

## 6. Auth, Penyimpanan Lokal (SQLite v4) & Sinkronisasi Cloud (Supabase)

- **Lokal (`EmpatiDatabase` v4 & `ScreeningStore`)**: Menyimpan akun pengguna secara persisten (tanpa akun demo) dengan kredensial satu arah (PBKDF2-HMAC-SHA256 + salt 16-byte), sesi login aktif (`is_active_session`), status sinkronisasi cloud (`cloud_synced`), riwayat kualitatif (`user_screening_summaries`), log chat (`rag_chat_logs`), serta `ResearchRecord` (`research_cf_records`). `AsnNipValidator` memvalidasi identitas umum (Email, No. HP, Username, NIK 16 digit, atau NIP 18 digit).
- **Cloud (`SupabaseCloudSyncService` → Project `empati-research`, ID: `qnvcuhmvrrffzdxxdloc`)**:
  - Menyimpan akun & profil demografi pengguna ke tabel `public.user_accounts` (serta baseline demografi terpseudonimisasi di `public.research_cf_records`) sejak saat registrasi atau pembaruan profil, sehingga **data akun dan profil pengguna tidak pernah hilang walaupun pengguna belum melakukan skrining**.
  - Mengunggah otomatis data riset terpseudonimisasi (`subject_hash`, tanpa PII) ke tabel Postgres `public.research_cf_records` dan `public.research_chat_logs`.
  - Sinkronisasi berjalan otomatis di latar belakang (`Dispatchers.IO`) saat registrasi/login (`enterSession`), saat profil diperbarui (`updateProfile`), saat skrining selesai (`persistCurrentScreeningIfNeeded`), dan saat aplikasi dibuka kembali (`finishSplash`).

---

## 7. Chatbot AI-EMPATI (OpenRouter + Supabase Edge Function `empati-chat` + Ollama)

### 7.1 Arsitektur (`data/rag/OllamaRagService.kt` & `supabase/functions/empati-chat/index.ts`)

Alur balasan (`ChatEngine.replyWithRag` → `generateRagResponse`):

1. **Sanitasi privasi (`sanitizePrivacyInput`)**: NIP/NIK/email/no-HP disensor sebelum dikirim ke jaringan.
2. **Topic scoping**: Hanya ranah **Kesehatan**, **Psikologi**, dan **Perlindungan Kekerasan**; di luar itu ditolak secara sopan tanpa memanggil LLM.
3. **Jalur 1 — Direct OpenRouter API (Opsional)**: Bila `BuildConfig.OPENROUTER_API_KEY` diisi melalui `local.properties`, memanggil langsung `https://openrouter.ai/api/v1/chat/completions`.
4. **Jalur 2 — Supabase Edge Function Proxy (`empati-chat`) [Utama saat Deploy]**:
   - Memanggil `https://qnvcuhmvrrffzdxxdloc.supabase.co/functions/v1/empati-chat` yang menyimpan secret `OPENROUTER_API_KEY` secara aman di sisi server Supabase.
   - **Urutan Pemilihan Model Otomatis (Free-Tier First)**:
     1. `openrouter/free` (*OpenRouter Smart Free Router* — otomatis memilih model `:free` aktif tercepat tanpa antrean).
     2. `meta-llama/llama-3.3-70b-instruct:free`
     3. `google/gemma-3-12b-it:free`
     4. `qwen/qwen3-8b:free`
     5. Model berbayar opsional (`qwen/qwen-2.5-7b-instruct`, `openai/gpt-4o-mini`) bila akun memiliki kredit.
     6. *Keyless Cloud Fallback* otomatis bila kuota harian gratis OpenRouter sedang habis.
5. **Jalur 3 — Ollama Lokal**: Mencoba `127.0.0.1:11434`, `10.0.2.2:11434`, dan LAN (`qwen2.5:3b-instruct`) untuk pengembangan lokal.
6. **Jalur 4 — Offline Synthesizer**: Jawaban klinis deterministik bila perangkat sama sekali tidak memiliki koneksi internet.
7. **Guardrail Output (`enforceOutputGuardrails` & `ensureCompleteResponse`)**: Menghapus CF/MB/MD/%/desimal, menyertakan kontak rahasia **SAPA 129** untuk topik KBG, serta memastikan kalimat selesai secara utuh hingga tanda titik terakhir.
8. **Kenyamanan UI vs Audit Peneliti**: Nama model teknis disembunyikan dari header `ChatScreen.kt` agar pergantian model dinamis OpenRouter tidak membingungkan pengguna, namun nama model aktual yang menjawab setiap pesan tetap dicatat di kolom `model_name` pada tabel `public.research_chat_logs`.

---

## 8. Privasi & keamanan

- **Kontrol Layar Fleksibel (`ENABLE_SCREENSHOT_PROTECTION = false`)**: Screenshot dan screen record diizinkan, dengan kode `FLAG_SECURE` tetap disimpan di `MainActivity.kt` bila ingin diaktifkan kembali di masa depan.
- Sanitasi input sebelum ke LLM dan sebelum ke log cloud (NIP/NIK/email/HP).
- Zero-leak: CF/MB/MD/% tidak keluar ke UI maupun ke output LLM (guardrail).
- Data riset (`ResearchRecord`) dipisahkan dari tampilan awam dan diunggah ke Supabase hanya menggunakan pseudonim satu arah `subject_hash` (`sub_<20-hex>`) tanpa nama, email, nomor HP, NIK, atau NIP mentah.
- Penghapusan data pengguna (`clearAllData`) menghapus data lokal SQLite sekaligus menghapus akun pengguna dari tabel `public.user_accounts` di Cloud Supabase (`deleteUserAccount`).

## 9. Ringkasan Evolusi ke v4.0.0

| Aspek | Versi Awal | Kondisi Aktual (v4.0.0) |
|---|---|---|
| Palet | Mauve + Teal | Soft Daylight: Sage/Clay/Mist/Stone + tone afektif (`MaterialTheme.empati`) |
| Font | Lora | 3-Suara: Fraunces + Inter (`tnum`) + JetBrains Mono |
| Chatbot | Rule-based / Ollama lokal | OpenRouter (`openrouter/free` via Supabase Edge Function `empati-chat`) + Ollama + Guardrail |
| Badge Model di UI | Ditampilkan (`"Qwen 2.5 LLM"`) | Disembunyikan di UI pengguna, dicatat di kolom `model_name` (`research_chat_logs`) |
| Auth | Hanya NIP Perempuan ASN | Multi-format inklusif (Email, No. HP, Username, NIK 16 digit, NIP 18 digit) + PBKDF2 |
| Database | SharedPreferences / SQLite lokal | Hybrid: SQLite Multi-Domain v3 + Cloud Supabase (`empati-research`) ber-RLS *Write-Only* |
| Build & Deploy | Debug | Siap rilis (`assembleRelease` dengan R8/ProGuard `proguard-rules.pro`) |

## 10. Pengujian

Suite pengujian otomatis (49 unit test JVM + 6 instrumented test):
- `cf/CfEngineTest` (30 test: parser, kombinasi CF, integrasi, determinisme)
- `presentation/UserPresentationTest` (7 test: pemisahan internal vs UI, anti-leak, penyelarasan status risiko)
- `auth/AsnAndRagArchitectureTest` (11 test: identitas multi-format, PBKDF2, `subject_hash`, sanitasi PII, payload Supabase & OpenRouter, serta guardrail kalimat utuh & SAPA 129)
- `ExampleUnitTest` (1 test)

Jalankan: `./gradlew testDebugUnitTest assembleRelease`.
