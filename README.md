# 🛡️ SRYA Emora (v4.1 — Soft Daylight Design System & Inklusif Semua Kalangan)

**Skrining Risiko Hipertensi & Kesejahteraan Psikososial Berbasis AI untuk Semua Kalangan**

> Aplikasi Android berbasis sistem pakar *Certainty Factor* dua modul yang mengidentifikasi risiko hipertensi, gaya hidup, stres harian/pekerjaan, dan keterkaitan pengalaman Kekerasan Berbasis Gender (KBG) bagi **seluruh lapisan masyarakat** (pekerja kantoran, wiraswasta, mahasiswa, ibu rumah tangga, tenaga medis/pendidik, maupun ASN), dirancang dengan sistem desain **"Soft Daylight" (v4.1)** dan dilengkapi chatbot konsultasi **SRYA Emora** berbasis **LLM Murni (OpenRouter + Supabase Edge Function `empati-chat`)** ber-guardrail ketat pada ranah Kesehatan, Psikologi, dan Kekerasan. Mendukung penuh kebutuhan riset disertasi (*Skrining Risiko Hipertensi Berbasis Gender*).

---

## 📋 Daftar Isi

- [Tentang](#tentang)
- [Fitur Utama](#fitur-utama)
- [Arsitektur](#arsitektur)
- [Tech Stack](#tech-stack)
- [Struktur Proyek](#struktur-proyek)
- [Prasyarat](#prasyarat)
- [Instalasi & Build](#instalasi--build)
- [Pengujian](#pengujian)
- [Alur Aplikasi](#alur-aplikasi)
- [Sistem Pakar Certainty Factor](#sistem-pakar-certainty-factor)
- [Privasi & Keamanan](#privasi--keamanan)
- [Roadmap](#roadmap)
- [Lisensi](#lisensi)

---

## Tentang

**AI-EMPATI** (*Aplikasi Identifikasi — Evaluasi Mandiri dan Pemantauan Ancaman Tekanan darah tInggi*) adalah aplikasi skrining kesehatan inklusif yang dirancang **untuk semua kalangan masyarakat** di Indonesia (dengan dukungan atribut demografi & kohort riset disertasi). Aplikasi ini menggabungkan:

1. **Skrining hipertensi** — menilai faktor risiko riwayat kesehatan, gaya hidup, dan psikososial harian/pekerjaan.
2. **Skrining KBG & kesejahteraan interpersonal** — mengidentifikasi pengalaman kekerasan fisik, psikologis, seksual, ekonomi, dan digital di lingkungan rumah tangga, kerja, maupun sosial.
3. **Analisis terintegrasi** — mengeksplorasi pola keterkaitan antara stres kronis/pengalaman KBG dan risiko hipertensi *(bukan kausalitas)*.
4. **Chatbot AI-EMPATI berbasis LLM Murni (`qwen2.5:3b-instruct`)** — menjawab pertanyaan seputar **Kesehatan** (hipertensi, tekanan darah, nutrisi, olahraga), **Psikologi** (stres, kecemasan, kualitas tidur), dan **Perlindungan Kekerasan** (KBG/KDRT & layanan SAPA 129) secara alami, utuh, dan tanpa tempelan korpus kaku.

> [!IMPORTANT]
> Aplikasi ini adalah alat skrining edukatif, **bukan alat diagnosis medis**. Seluruh nilai bobot pakar (MB/MD) masih bersifat *placeholder* dan memerlukan validasi tenaga kesehatan sebelum digunakan secara klinis.

---

## Fitur Utama

| Fitur | Deskripsi |
|---|---|
| 🩺 **Skrining Dual-Modul Inklusif** | 26 pertanyaan universal: 16 hipertensi (M01–M16) + 10 KBG & keamanan relasi (K01–K10) |
| 🧠 **Certainty Factor Engine** | Inferensi per sub-hipotesis dengan formula CF kombinasi + faktor pelindung (*protective factors*) & penyelarasan status risiko lintas layar (`Rendah · Batas Wajar`, `Sedang · Perlu Pemantauan`, `Tinggi · Perlu Perhatian`, `Sangat Tinggi · Perlu Perhatian`) |
| 📊 **Analisis Terintegrasi** | 6 kategori klasifikasi kualitatif + `StateOrb` afektif + `FindingChip` interaktif dengan `FindingDetailSheet` (`ModalBottomSheet`) |
| 💬 **Chatbot SRYA Emora (OpenRouter + Edge Proxy + Ergonomi Chatbox & Analisis Hasil Skrining)** | Generator LLM murni via **OpenRouter (`openrouter/free` & `:free` models)** & **Supabase Edge Function (`empati-chat`)** yang mampu memperkenalkan dirinya secara hangat (`"siapa anda"`), menganalisis serta menyimpulkan hasil skrining pengguna secara mendalam (`"analisis hasil screening saya"`, `"apa yang bisa anda simpulkan dari hasil screening saya"`), dilengkapi kenyamanan pengetikan (*chatbox & placeholder selalu berada di atas keyboard via `imePadding`*) serta opsi salin teks menyeluruh (`SelectionContainer` & `ContentCopy` pada bubble dan chatbox) |
| 🔒 **Privasi & Kontrol Layar Fleksibel** | Enkripsi satu arah `subject_hash` & PBKDF2, firewall privasi input, Row-Level Security (RLS) Supabase, penghapusan akun penuh (lokal + cloud), serta izin screenshot/screen-record aktif dengan kode `FLAG_SECURE` yang tetap disimpan (`ENABLE_SCREENSHOT_PROTECTION = false`) bila ingin diaktifkan kembali di masa depan |
| 📱 **Sistem Desain "Soft Daylight" (v4.1)** | Arsitektur warna 3-lapis (Sage/Clay/Mist/Stone + `MaterialTheme.empati`), tipografi 3-suara (`Fraunces`, `Inter`, `JetBrains Mono`), `LocalReducedMotion`, & aksesibilitas WCAG 2.2 (48dp + `focusHalo`) |
| 📋 **Riwayat Skrining Sinkron** | Timeline hasil tersimpan otomatis secara lokal & konsisten antara layar Hasil, Beranda, dan Riwayat |
| 🪪 **Portal Auth Multi-Format Siap Pakai (Tanpa Akun Demo)** | Login & registrasi bersih siap produksi menggunakan **Email**, **No. HP**, **Username**, **NIK (16 digit)**, atau **NIP (18 digit)** untuk semua gender & profesi |
| 🗄️ **Hybrid SQLite v4 + Supabase Cloud Sync (Persisten Tanpa Skrining)** | 4 domain tabel SQLite lokal (`DB_VERSION = 4`) + sinkronisasi otomatis & pemulihan akun lintas perangkat ke **Supabase Postgres (`empati-research`: `user_accounts`, `research_cf_records`, `research_chat_logs`)** sehingga data akun & profil pengguna tetap tersimpan utuh meskipun pengguna belum melakukan skrining |
| 🔬 **Data Penelitian Terisolasi** | Output teknis CF berversi (`kb_version`) tersimpan melalui pseudonim `subject_hash` |

---

## Arsitektur

```
┌──────────────────────────────────────────────────┐
│                     UI Layer                      │
│  Jetpack Compose (Material 3)                     │
│  Screens: Splash → Onboarding → Consent →         │
│           Questionnaire → Result → Chat → Profile │
├──────────────────────────────────────────────────┤
│                  ViewModel                        │
│  EmpatiViewModel (state, navigasi, logika UI)     │
├──────────────────────────────────────────────────┤
│                  Domain Layer                     │
│  ┌────────────────┐  ┌─────────────────────────┐ │
│  │ CF Engine       │  │ Chat Engine             │ │
│  │ • InputParser   │  │ • Keyword matching      │ │
│  │ • KnowledgeBase │  │ • Intent classification │ │
│  │ • Inference     │  │ • Context injection     │ │
│  │ • ResultGen     │  └─────────────────────────┘ │
│  └────────────────┘                               │
├──────────────────────────────────────────────────┤
│                Presentation Layer                 │
│  UserResultPresenter (model internal → bahasa     │
│  awam, tanpa CF/MB/MD)                            │
├──────────────────────────────────────────────────┤
│                  Data Layer                       │
│  ScreeningStore (SharedPreferences)               │
│  ResearchModels (penyimpanan data penelitian)     │
└──────────────────────────────────────────────────┘
```

---

## Tech Stack

| Komponen | Versi |
|---|---|
| **Kotlin** | 2.2.10 |
| **Android Gradle Plugin** | 9.5.0 |
| **Jetpack Compose BOM** | 2026.02.01 |
| **Material 3** | via BOM |
| **Lifecycle ViewModel Compose** | 2.6.1 |
| **Min SDK** | 24 (Android 7.0) |
| **Target / Compile SDK** | 37 |
| **Build System** | Gradle 9.6 (Kotlin DSL) |
| **Testing** | JUnit 4, Compose UI Test |

---

## Struktur Proyek

```
Empati/
├── app/src/main/java/com/example/empati/
│   ├── MainActivity.kt              # Entry point, FLAG_SECURE
│   ├── EmpatiApp.kt                 # Root composable & routing
│   ├── EmpatiViewModel.kt           # State management & navigation
│   ├── data/
│   │   ├── Models.kt                # Data class: Question, ScreeningResult, UserProfile
│   │   ├── ChatEngine.kt            # Rule-based chatbot
│   │   ├── ScreeningEngine.kt       # Mesin skor legacy (v1)
│   │   ├── ScreeningQuestions.kt     # Bank pertanyaan legacy
│   │   ├── ScreeningStore.kt        # Persistensi lokal (SharedPreferences)
│   │   ├── auth/
│   │   │   └── AsnNipValidator.kt   # Validasi identitas multi-format (Email/HP/Username/NIK/NIP) & subject_hash
│   │   ├── db/
│   │   │   └── EmpatiDatabase.kt    # Multi-domain SQLite v3 (Zero-Collision)
│   │   ├── cloud/
│   │   │   └── SupabaseCloudSyncService.kt # Sinkronisasi Cloud Supabase (research_cf_records & research_chat_logs) + proxy empati-chat
│   │   ├── rag/
│   │   │   ├── RagKnowledgeRepository.kt # Korpus RAG dasar & ScreeningContextSnapshot
│   │   │   ├── WebRagSearchService.kt    # Live Internet RAG (Wikipedia Medis ID & PubMed/EuropePMC)
│   │   │   └── OllamaRagService.kt       # Integrasi OpenRouter + Supabase Edge Function + Ollama lokal & Guardrails klinis
│   │   ├── cf/                      # 🧠 Certainty Factor Engine (v2)
│   │   │   ├── CfModels.kt          #    Domain models & enums
│   │   │   ├── CfQuestions.kt       #    26 pertanyaan dual-modul
│   │   │   ├── CfInputParser.kt     #    Parser jawaban → CF user
│   │   │   ├── CfKnowledgeBase.kt   #    Rules: MB, MD per hipotesis
│   │   │   ├── CfInferenceEngine.kt #    Inferensi & kombinasi CF
│   │   │   └── CfResultGenerator.kt #    Analisis terintegrasi
│   │   ├── presentation/
│   │   │   ├── UserPresentation.kt  #    Model presentasi (user-safe)
│   │   │   └── UserResultPresenter.kt#   Translator internal → awam
│   │   └── research/
│   │       └── ResearchModels.kt    #    Data penelitian (CF lengkap)
│   └── ui/
│       ├── components/Components.kt  # Komponen UI reusable
│       ├── screens/                  # Layar-layar aplikasi
│       │   ├── SplashScreen.kt
│       │   ├── OnboardingScreen.kt
│       │   ├── AuthScreen.kt         # Login & Registrasi Inklusif (Email, No. HP, Username, NIK, NIP)
│       │   ├── ConsentScreen.kt
│       │   ├── HomeScreen.kt
│       │   ├── QuestionnaireScreen.kt
│       │   ├── ResultScreen.kt
│       │   ├── ChatScreen.kt
│       │   ├── HistoryScreen.kt
│       │   ├── ProfileScreen.kt
│       │   └── MainScaffold.kt
│       └── theme/
│           ├── Color.kt              # Palet "Soft Daylight" v4 (Sage, Clay, Mist, Honey, Terra, Berry, Stone)
│           ├── Theme.kt              # Material 3 ColorScheme, EmpatiColors (MaterialTheme.empati), EmpatiShapes
│           ├── Tokens.kt             # EmpatiSpace, EmpatiIcon, EmpatiElevation, EmpatiMotion, LocalReducedMotion
│           └── Type.kt               # 3-Voice Typography (Fraunces, Inter + tnum, JetBrains Mono)
├── app/src/main/res/font/            # Bundled variable TTF fonts
│   ├── fraunces.ttf                  # DisplayFont (Fraunces soft-serif opsz=72, SOFT=100, WONK=0)
│   ├── inter.ttf                     # BodyFont & EmpatiNumeralStyle (Inter)
│   └── jetbrains_mono.ttf            # MetricFont / EmpatiMetricStyle (JetBrains Mono)
├── app/src/test/                     # Unit tests (49 tests)
│   ├── auth/AsnAndRagArchitectureTest.kt # 11 test identitas multi-format, PBKDF2, subject_hash, payload Supabase/OpenRouter, & LLM guardrails
│   ├── cf/CfEngineTest.kt           # 30 test CF engine
│   └── presentation/UserPresentationTest.kt  # 7 test presentasi & penyelarasan status risiko
├── supabase/                         # Infrastruktur Cloud Supabase (empati-research)
│   ├── migrations/20261108000001_create_empati_research_schema.sql
│   └── functions/empati-chat/index.ts # Edge Function proxy OpenRouter (Free-Tier Smart Router openrouter/free)
├── app/proguard-rules.pro            # Aturan minifikasi R8/ProGuard untuk APK rilis
├── build.gradle.kts                  # Root build config
├── settings.gradle.kts
├── gradle.properties
├── project_analysis.md
├── FRONTEND_DESIGN.md
├── TYPOGRAPHY_AND_CLINICAL_UX.md
├── PROJECT_CONTEXT_SUMMARY.md
├── DATABASE_AUTH_AND_RAG_ARCHITECTURE.md
├── EXPERT_SYSTEM.md
└── CHANGELOG.md
```

---

## Prasyarat

- **Android Studio** Ladybug (2025.x) atau lebih baru
- **JDK 11+**
- **Android SDK** dengan API level 37

---

## Instalasi & Build

```bash
# Clone repositori
git clone <repository-url>
cd Empati

# Build debug APK
./gradlew assembleDebug

# Install ke perangkat/emulator
./gradlew installDebug
```

---

## Pengujian

```bash
# Jalankan seluruh unit test
./gradlew test

# Jalankan test spesifik
./gradlew test --tests "com.example.empati.cf.CfEngineTest"
./gradlew test --tests "com.example.empati.presentation.UserPresentationTest"
```

**Status pengujian saat ini:** 49 unit test JVM + 7 instrumented SQLite test ✅

| Suite | Jumlah Test | Cakupan |
|---|---|---|
| `CfEngineTest` | 30 | Parser, mapping, reverse scoring, validasi, formula CF, hasil modul, integrasi, determinisme |
| `UserPresentationTest` | 7 | Bahasa awam, penyelarasan status risiko (`Sedang · Perlu Pemantauan` s/d `Sangat Tinggi · Perlu Perhatian`), faktor protektif, penjelasan tag klinis, Zero-Leak |
| `AsnAndRagArchitectureTest` | 12 | Validasi identitas multi-format (Email, No. HP, Username, NIK, NIP semua gender), PBKDF2-HMAC-SHA256 salt acak, pseudonimisasi `subject_hash`, respons perkenalan diri `"siapa anda"` & penolakan topik yang manusiawi, persistensi akun tanpa skrining (`user_accounts`), Pure Scoped LLM guardrails |
| `EmpatiDatabaseInstrumentedTest` (`androidTest`) | 7 | Operasi SQLite sungguhan (`DB_VERSION = 4` tanpa akun demo): registrasi & persistensi profil tanpa skrining, login multi-format, penolakan duplikat/PIN salah, isolasi riwayat antar-akun, hapus data per pengguna |

---

## Alur Aplikasi

```
Splash → Onboarding (3 slide) → Home Dashboard
                                       │
                              ┌────────┼────────┐
                              ▼        ▼        ▼
                          Skrining   Chat    Riwayat
                              │
                     Informed Consent
                              │
                  Kuesioner Hipertensi (M01–M16)
                              │
                    Hasil Modul Hipertensi
                              │
                    Kuesioner KBG (K01–K10)
                              │
                      Hasil Modul KBG
                              │
                   Analisis Terintegrasi
                              │
                        Simpan Hasil
```

---

## Sistem Pakar Certainty Factor

### Formula Inti

| Operasi | Formula |
|---|---|
| CF pakar | `MB - MD` |
| CF evidence | `CF_User × CF_Rule` |
| Kombinasi (keduanya +) | $CF_1 + CF_2 \times (1 - CF_1)$ |
| Kombinasi (keduanya −) | $CF_1 + CF_2 \times (1 + CF_1)$ |
| Kombinasi (konflik) | $\frac{CF_1 + CF_2}{1 - \min(\|CF_1\|, \|CF_2\|)}$ |

### Sub-Hipotesis

**Modul Hipertensi:**
- Riwayat & Genetik
- Kondisi Medis
- Gaya Hidup
- Faktor Psikososial

**Modul KBG:**
- Fisik · Psikologis · Seksual · Ekonomi · Digital

### Klasifikasi Terintegrasi

| Kategori | Deskripsi |
|---|---|
| Hipertensi Murni Gaya Hidup | Risiko tinggi hipertensi tanpa indikasi KBG |
| Hipertensi + Indikasi KBG | Pola keterkaitan pengalaman KBG dan risiko hipertensi |
| Hipertensi + Riwayat Medis | Dominasi faktor riwayat/kondisi medis |
| Indikasi KBG Tanpa Hipertensi | Ada indikasi KBG, risiko hipertensi rendah |
| Risiko Rendah | Tidak ada indikasi signifikan |
| Inconclusive | Data belum lengkap untuk kesimpulan |

---

## Privasi & Keamanan

- 📸 **Screenshot & Screen Record Fleksibel (`ENABLE_SCREENSHOT_PROTECTION = false`)** — aplikasi mengizinkan tangkapan dan perekaman layar secara default, namun kode `WindowManager.LayoutParams.FLAG_SECURE` di `MainActivity.kt` tetap dipertahankan agar dapat diaktifkan kembali kapan saja di masa depan.
- 💾 **Penyimpanan Hybrid Terenkripsi** — riwayat skrining disimpan di SQLite perangkat (`empati_multi_domain.db` v4) dan akun/profil pengguna disinkronkan secara aman ke Supabase (`user_accounts`) dengan kredensial ter-hash PBKDF2-HMAC-SHA256.
- 🏷️ **Penanda privasi** — pertanyaan KBG menampilkan notifikasi kerahasiaan
- 🔬 **Pemisahan data & Pseudonimisasi (`subject_hash`)** — output penelitian (CF teknis) dipisahkan dari identitas asli menggunakan kode samaran satu arah (`sub_<20-hex>`)
- 🗑️ **Kendali Penghapusan Penuh** — tombol *"Hapus seluruh data akun & riwayat saya"* menghapus data dari perangkat lokal maupun tabel `user_accounts` di Cloud Supabase
- 👤 **Bahasa sehari-hari yang hangat & manusiawi** — pengguna hanya melihat kesimpulan empatik yang mudah dipahami, tanpa skor CF/MB/MD atau kalimat kaku (*AI-slop*)

---

## Roadmap

- [ ] Validasi bobot klinis (MB/MD) bersama tenaga kesehatan
- [x] Integrasi chatbot dengan LLM lokal Ollama (`qwen2.5:3b-instruct` / `empati-qwen2.5:3b`) ber-guardrail ketat
- [x] Transformasi inklusif untuk semua kalangan masyarakat (dukungan multi-format ID & multi-profesi)
- [x] Sistem Desain **EMPATI "Soft Daylight" (v4.0)** dengan palet Sage/Clay/Mist/Stone & nada afektif `MaterialTheme.empati`
- [x] Sistem Tipografi 3-Suara (`Fraunces`, `Inter` + angka tabular `tnum`, `JetBrains Mono`)
- [x] Redesain hasil skrining dengan `StateOrb`, `FindingChip` interaktif (`FindingDetailSheet`), & `StrengthsCard` (*Yang sudah baik*)
- [ ] Enkripsi penyimpanan lokal penuh (SQLCipher)
- [ ] Mekanisme ekspor/hapus data yang lebih granular
- [ ] Instrumental test (UI test) untuk alur end-to-end

---

## Lisensi

*Silakan tambahkan lisensi yang sesuai.*

---

<p align="center">
  <sub>Dibuat untuk mendukung kesehatan tekanan darah & kesejahteraan psikososial masyarakat Indonesia 🇮🇩</sub>
</p>

