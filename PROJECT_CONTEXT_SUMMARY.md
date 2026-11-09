# 📘 SRYA Emora (sebelumnya AI-EMPATI) — Master Context & Project Knowledge Base (v4.1 — Soft Daylight Design System & Inklusif Semua Kalangan)

Dokumen ini adalah **ringkasan komprehensif dan mendalam (*Master Context Document*)** dari seluruh proyek **SRYA Emora** (sebelumnya **AI-EMPATI**). Dokumen ini dirancang khusus untuk dijadikan **sumber konteks utama (*source context*)** pada **Gemini Notebook / NotebookLM / LLM Workspace**, mencakup arsitektur aplikasi mobile yang kini **inklusif untuk semua kalangan masyarakat**, sistem pakar determinan hipertensi & KBG (Certainty Factor), sistem desain **"Soft Daylight" (v4.1)** dengan semantik afektif (`MaterialTheme.empati`), aset logo multi-resolusi (`srya_emora_logo_transparent` & `srya_emora_logo_bg`), tipografi 3-suara (**Fraunces + Inter + JetBrains Mono**), database multi-domain terisolasi + sinkronisasi Cloud Supabase (`empati-research`), otentikasi multi-format (Email/HP/Username/NIK/NIP), serta chatbot konsultasi **SRYA Emora** berbasis **LLM Murni Terbatasi Topik (*Pure Scoped LLM* via OpenRouter & Supabase Edge Function `empati-chat`)**.

---

## 📋 Daftar Isi

1. [Identitas & Ringkasan Eksekutif Proyek](#1-identitas--ringkasan-eksekutif-proyek)
2. [Tech Stack & Arsitektur Perangkat Lunak](#2-tech-stack--arsitektur-perangkat-lunak)
3. [Sistem Pakar Certainty Factor (Determinan Dual-Modul)](#3-sistem-pakar-certainty-factor-determinan-dual-modul)
   - [3.1 Teori & Formula Matematis Certainty Factor](#31-teori--formula-matematis-certainty-factor)
   - [3.2 Bank Pertanyaan & Skala Jawaban (26 Pertanyaan)](#32-bank-pertanyaan--skala-jawaban-26-pertanyaan)
   - [3.3 Knowledge Base (26 Aturan MB/MD)](#33-knowledge-base-26-aturan-mbmd)
   - [3.4 Matriks Analisis Terintegrasi (6 Kategori Hasil)](#34-matriks-analisis-terintegrasi-6-kategori-hasil)
4. [Lapisan Presentasi Klinis vs Data Riset (Etika & Keamanan)](#4-lapisan-presentasi-klinis-vs-data-riset-etika--keamanan)
5. [Desain UI/UX & Sistem Tipografi Fungsional](#5-desain-uiux--sistem-tipografi-fungsional)
6. [Otentikasi & Manajemen Identitas Inklusif (Semua Kalangan + Kohort Disertasi)](#6-otentikasi--manajemen-identitas-inklusif-semua-kalangan--kohort-disertasi)
7. [Arsitektur Database SQLite Multi-Domain (*Zero-Collision*)](#7-arsitektur-database-sqlite-multi-domain-zero-collision)
8. [Sistem RAG & Integrasi Ollama (`qwen2.5:3b-instruct`)](#8-sistem-rag--integrasi-ollama-qwen253b-instruct)
9. [Struktur Repositori & Indeks Kode Sumber](#9-struktur-repositori--indeks-kode-sumber)

---

## 1. Identitas & Ringkasan Eksekutif Proyek

* **Nama Proyek & Asisten AI**: **SRYA Emora** (sebelumnya *AI-EMPATI* — *Evaluasi Mandiri dan Pemantauan Ancaman Tekanan darah tInggi & Kesejahteraan*)
* **Versi Saat Ini**: **v4.1.5** (Ergonomi Penempatan Chatbox di Atas Keyboard via `Modifier.imePadding()` & Penonaktifan Cerdas BottomNavBar saat Mengetik, Opsi Salin Teks Menyeluruh via `SelectionContainer` & `ContentCopy` pada Pesan dan Chatbox, Analisis & Kesimpulan Kontekstual Hasil Skrining oleh LLM via `ScreeningContextSnapshot` Kaya, Migrasi & Sinkronisasi Live Database Cloud Supabase `public.user_accounts` + `public.research_cf_records`, Penyelesaian Audit Copywriting Anti "AI-Slop", Kesiapan Deploy Produksi, Penghapusan Akun Cloud, Dukungan Screenshot/Screen Record dengan Kode `FLAG_SECURE` Dipertahankan via `ENABLE_SCREENSHOT_PROTECTION = false`, Sistem Desain "Soft Daylight" v4.1, & Pure Scoped LLM via **OpenRouter** + Supabase Edge Function `empati-chat`)
* **Tujuan Ganda (Layanan Publik Universal & Riset Disertasi)**:
  1. **Aplikasi Kesehatan untuk Semua Kalangan**: Menyediakan platform skrining kesehatan mandiri (*self-screening*) dan edukasi preventif yang dapat digunakan oleh **siapa saja di masyarakat umum** (pekerja swasta/kantoran, wiraswasta/UMKM, mahasiswa/pelajar, ibu rumah tangga, tenaga medis/pendidik, maupun ASN dari semua gender) untuk mengevaluasi risiko hipertensi, pola gaya hidup, stres harian/pekerjaan, serta keterkaitannya dengan pengalaman **Kekerasan Berbasis Gender (KBG)** di lingkungan rumah, kerja, maupun sosial.
  2. **Dukungan Kohort Riset Disertasi**: Meskipun aplikasi terbuka untuk semua kalangan, sistem tetap merekam kategori demografi (jenis kelamin, latar belakang pekerjaan, domisili/instansi) serta mendukung validasi opsional 18 digit NIP sehingga peneliti tetap dapat memfilter dan menganalisis kohort khusus untuk disertasi *"Skrining Risiko Hipertensi Berbasis Gender untuk Perempuan ASN di Indonesia"*.
* **Prinsip Dasar**:
  1. **Skrining Bukan Diagnosis**: Aplikasi bertindak sebagai deteksi dini dan edukasi risiko; bukan penentu diagnosis medis definitif.
  2. **Non-Kausalitas Determinan**: Hubungan antara pengalaman KBG/stres kronis dan hipertensi disajikan sebagai pola keterkaitan psikososial (*chronic stress pathway*), bukan hubungan sebab-akibat langsung (*direct causality*).
  3. **Privasi Maksimal & Kontrol Layar Fleksibel**: Menggunakan enkripsi kredensial PBKDF2, firewall privasi input LLM, **Pseudonymization Firewall** (`subject_hash`), penghapusan akun menyeluruh (lokal + cloud), serta izin screenshot/screen-record aktif dengan kode `FLAG_SECURE` yang tetap disimpan (`MainActivity.ENABLE_SCREENSHOT_PROTECTION = false`) agar dapat diaktifkan kembali kapan saja di masa depan.

---

## 2. Tech Stack & Arsitektur Perangkat Lunak

### 2.1 Komponen Teknologi
* **Bahasa**: Kotlin 2.2.10
* **Target Platform**: Android (Min SDK: 24 / Android 7.0, Target & Compile SDK: 37)
* **UI Framework**: Jetpack Compose dengan Material 3 (Material You Design System)
* **Arsitektur Aplikasi**: Clean Architecture + MVVM (Model-View-ViewModel) dengan Unidirectional Data Flow (UDF)
* **State Management**: AndroidViewModel Compose (`EmpatiViewModel`) + Compose Runtime State
* **Penyimpanan Lokal & Cloud**: SQLite Multi-Domain (`EmpatiDatabase` via SQLiteOpenHelper) + Sinkronisasi Cloud **Supabase Postgres (`empati-research` — `https://qnvcuhmvrrffzdxxdloc.supabase.co`)** melalui `SupabaseCloudSyncService` + `SharedPreferences` (flag sesi & antrean sinkronisasi)
* **LLM & Edge Infrastructure**: **OpenRouter API** (`https://openrouter.ai/api/v1/chat/completions`, dengan prioritas **Free-Tier Smart Router `openrouter/free`** → `meta-llama/llama-3.3-70b-instruct:free` → `google/gemma-3-12b-it:free` → `qwen/qwen3-8b:free` → model berbayar opsional → *keyless cloud fallback*) yang diakses melalui **Supabase Edge Function (`empati-chat`)** + dukungan Ollama lokal (`qwen2.5:3b-instruct`). Nama model disembunyikan dari UI pengguna (`ChatScreen.kt`) agar pergantian model otomatis tidak membingungkan pengguna, namun tetap dicatat di kolom `model_name` pada `public.research_chat_logs` untuk audit peneliti.
* **Keamanan Kriptografi**: PBKDF2-HMAC-SHA256 (120.000 iterasi) + Salt 16-byte acak + SHA-256 One-Way Pseudonym (`subject_hash`) + Row-Level Security (RLS) *Write-Only Telemetry* (`INSERT` murni dengan `Prefer: return=minimal` dan penanganan idempoten `201` / `409`) di Supabase
* **Pengujian**: JUnit 4 (49 unit test JVM) + AndroidX Test Runner (6 instrumented SQLite tests)

### 2.2 Diagram Blok Arsitektur

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION / UI LAYER                         │
│  Jetpack Compose • Material 3 • 3-Role Functional Typography (Bundled) │
│  Screens: Splash ➔ Onboarding ➔ Auth (ASN) ➔ Home ➔ Consent ➔         │
│           DualQuestionnaire ➔ ModuleResult ➔ IntegratedResult ➔        │
│           Chat (RAG) ➔ History ➔ Profile                              │
├────────────────────────────────────────────────────────────────────────┤
│                           VIEWMODEL LAYER                              │
│  EmpatiViewModel: Navigasi, UDF State, Session Management, Chat Engine │
├────────────────────────────────────────────────────────────────────────┤
│                       DOMAIN / INFERENCE LAYER                         │
│  ┌───────────────────────────────┐  ┌────────────────────────────────┐ │
│  │     CERTAINTY FACTOR ENGINE   │  │           RAG ENGINE           │ │
│  │  • CfInputParser (Reverse M08)│  │  • ScreeningContextSnapshot    │ │
│  │  • CfKnowledgeBase (26 Rules) │  │  • RagKnowledgeRepository      │ │
│  │  • CfInferenceEngine          │  │  • Hybrid Lexical Matcher      │ │
│  │  • CfResultGenerator          │  │  • Guardrails & Topic Scoping  │ │
│  └───────────────────────────────┘  └────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │                     USER PRESENTATION LAYER                       │ │
│  │  UserResultPresenter: Menerjemahkan CF internal ke bahasa awam    │ │
│  │  (Zero Leaks: Tanpa angka CF, MB, MD, %, atau desimal)            │ │
│  └───────────────────────────────────────────────────────────────────┘ │
├────────────────────────────────────────────────────────────────────────┤
│                   DATA & PERSISTENCE LAYER (ISOLATED)                  │
│  EmpatiDatabase (SQLite Multi-Domain: empati_multi_domain.db v2)       │
│  ├── Domain 1: auth_asn_users (Kredensial PBKDF2 + NIP 18-digit)       │
│  ├── Domain 2: user_screening_summaries (Riwayat UI per user_id)       │
│  ├── Domain 3: research_cf_records (Audit Riset via subject_hash)     │
│  └── Domain 4: rag_knowledge_chunks & rag_chat_logs (Dokumen & Chat)   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Sistem Pakar Certainty Factor (Determinan Dual-Modul)

### 3.1 Teori & Formula Matematis Certainty Factor
Metode Certainty Factor (Shortliffe & Buchanan) mengakomodasi ketidakpastian derajat keyakinan pakar klinis dan jawaban responden:

1. **CF Rule (Pakar)**:
   $$CF_{rule} = MB - MD$$
   * $MB$ (*Measure of Belief*): Rentang $[0.0, 1.0]$
   * $MD$ (*Measure of Disbelief*): Rentang $[0.0, 1.0]$
2. **CF Evidence (Pengguna $\times$ Aturan)**:
   $$CF_{evidence} = CF_{user} \times CF_{rule}$$
3. **Kombinasi Evidence Beruntun ($CF_{combine}$)**:
   * Jika keduanya positif ($CF_1 > 0$ dan $CF_2 > 0$):
     $$CF_{combine} = CF_1 + CF_2 \times (1 - CF_1)$$
   * Jika keduanya negatif ($CF_1 < 0$ dan $CF_2 < 0$):
     $$CF_{combine} = CF_1 + CF_2 \times (1 + CF_1)$$
   * Jika bertolak belakang (konflik tanda):
     $$CF_{combine} = \frac{CF_1 + CF_2}{1 - \min(|CF_1|, |CF_2|)}$$

### 3.2 Bank Pertanyaan & Skala Jawaban (26 Pertanyaan)

#### Modul 1: Hipertensi (M01–M16)
* **Kategori Riwayat Kesehatan & Klinis**:
  * `M01` (Dikotomi): Pernah didiagnosis hipertensi oleh nakes ($MB=0.95, MD=0.0$)
  * `M02` (Dikotomi): Riwayat hipertensi orang tua/saudara kandung ($MB=0.70, MD=0.0$)
  * `M03` (Dikotomi): Riwayat diabetes melitus ($MB=0.75, MD=0.0$)
  * `M04` (Dikotomi): Rutin mengonsumsi obat tekanan darah ($MB=0.90, MD=0.0$)
  * `M05` (Dikotomi): Tensi terakhir $> 130/85\text{ mmHg}$ ($MB=0.85, MD=0.0$)
  * `M06` (Dikotomi): Sedang hamil / nifas 6 bulan terakhir ($MB=0.60, MD=0.0$)
* **Kategori Gaya Hidup**:
  * `M07` (Likert): Frekuensi konsumsi asin / garam ($MB=0.70, MD=0.0$)
  * `M08` (Likert - **REVERSE SCORED**): Frekuensi olahraga $\ge 30$ menit ($MB=0.75, MD=0.0$). Semakin sering olahraga, $CF_{user}$ semakin rendah.
  * `M09` (Dikotomi): Merokok / terpapar asap rokok ($MB=0.65, MD=0.0$)
  * `M10` (Likert): Konsumsi kafein / kopi berlebih ($MB=0.50, MD=0.0$)
  * `M11` (Dikotomi): Obesitas / berat badan berlebih ($MB=0.70, MD=0.0$)
* **Kategori Faktor Psikososial & Beban Kerja**:
  * `M12` (Likert): Stres berat akibat pekerjaan kantor ($MB=0.75, MD=0.0$)
  * `M13` (Dikotomi): Tidur $< 7$ jam per malam ($MB=0.60, MD=0.0$)
  * `M14` (Likert): Cemas/tertekan tanpa sebab jelas ($MB=0.65, MD=0.0$)
  * `M15` (Dikotomi): Jarang periksa tensi ($< 1$ kali setahun) ($MB=0.50, MD=0.0$)
  * `M16` (Dikotomi): Makan makanan cepat saji $> 3$ kali/minggu ($MB=0.60, MD=0.0$)

#### Modul 2: Kekerasan Berbasis Gender / KBG (K01–K10) — Seluruhnya Sensitif
* **Kekerasan Fisik**: `K01` (Dorongan/pukulan di tempat kerja), `K02` (Lemparan benda/ancaman fisik).
* **Kekerasan Psikologis**: `K03` (Dihina/dimaki/dipermalukan), `K04` (Dikucilkan/diisolasi rekan/atasan).
* **Kekerasan Seksual**: `K05` (Komentar/candaan seksual non-konsensual), `K06` (Sentuhan/ajakan seksual yang dipaksakan).
* **Kekerasan Ekonomi**: `K07` (Gaji/upah dikuasai tanpa izin), `K08` (Dihalangi promosi/karier tanpa alasan sah).
* **Kekerasan Digital**: `K09` (Penguntitan/spamming digital kerjaan), `K10` (Intimidasi/ancaman media sosial).

#### Pemetaan Nilai Jawaban ke $CF_{user}$
* **Dikotomi**: `Ya = 1.0`, `Tidak = 0.0`.
* **Likert Normal (1–5)**: $1=0.0$, $2=0.2$, $3=0.5$, $4=0.8$, $5=1.0$.
* **Likert Reverse (M08)**: $1=1.0$, $2=0.8$, $3=0.5$, $4=0.2$, $5=0.0$.

### 3.3 Knowledge Base (26 Aturan MB/MD)
Aturan pakar dipetakan ke sub-hipotesis spesifik:
* `HT_GENETIC` (Riwayat keluarga): Ditopang M02.
* `HT_MEDICAL` (Kondisi klinis): Ditopang M01, M03, M04, M05, M06, M15.
* `HT_LIFESTYLE` (Gaya hidup): Ditopang M07, M08, M09, M10, M11, M16.
* `HT_PSYCHOSOCIAL` (Beban kerja & stres): Ditopang M12, M13, M14.
* `GBV_PHYSICAL`: Ditopang K01, K02.
* `GBV_PSYCHOLOGICAL`: Ditopang K03, K04.
* `GBV_SEXUAL`: Ditopang K05, K06.
* `GBV_ECONOMIC`: Ditopang K07, K08.
* `GBV_DIGITAL`: Ditopang K09, K10.

Ambang batas level risiko CF Modul (`CfInferenceEngine.levelOf`):
* $CF \ge 0.75 \rightarrow$ **Sangat Tinggi (`SANGAT_TINGGI` / `VERY_HIGH`)** — `Sangat Tinggi · Perlu Perhatian`
* $0.50 \le CF < 0.75 \rightarrow$ **Tinggi (`TINGGI` / `HIGH`)** — `Tinggi · Perlu Perhatian`
* $0.25 \le CF < 0.50 \rightarrow$ **Sedang (`SEDANG` / `MEDIUM`)** — `Sedang · Perlu Pemantauan` (termasuk kalibrasi hipertensi terkendali: `M01=Ya` dengan `M05=Tidak`, `M03=Tidak`, dan faktor lain $< 0.50$)
* $CF < 0.25 \rightarrow$ **Rendah (`RENDAH` / `LOW`)** — `Rendah · Batas Wajar`

### 3.4 Matriks Analisis Terintegrasi (6 Kategori Hasil)
Inferensi terintegrasi mengevaluasi irisan kedua modul menjadi 6 profil temuan kualitatif:
1. **Risiko Hipertensi Rendah**: Baik CF Hipertensi maupun KBG berada di bawah ambang batas signifikan ($< 0.25$).
2. **Risiko Hipertensi dengan Dominasi Faktor Kesehatan dan Gaya Hidup**: CF Hipertensi tinggi/sedang, namun tidak terdapat indikasi KBG.
3. **Indikasi KBG Tanpa Indikasi Risiko Hipertensi**: CF KBG terindikasi ($\ge 0.25$), namun tensi dan faktor risiko fisik hipertensi rendah.
4. **Risiko Hipertensi dengan Indikasi Keterkaitan Pengalaman KBG**: Keduanya terindikasi secara simultan ($\ge 0.25$). Memberikan rujukan ganda (klinis tensi + pendampingan aman SAPA 129).
5. **Hasil Belum Dapat Disimpulkan**: Salah satu atau kedua kuesioner modul belum diisi lengkap.

---

## 4. Lapisan Presentasi Klinis vs Data Riset (Etika & Keamanan)

Untuk melindungi pengguna dari kecemasan kognitif (*health anxiety*) dan menjaga etika medis, sistem menerapkan **pemisahan mutlak** antara model perhitungan internal dan model tampilan:

| Komponen Teknis Internal | Tampilan Pengguna Awam (`UserResultPresenter`) |
|---|---|
| Nilai desimal CF (mis. `0.842`), MB, MD | **DILARANG TAMPIL** (Lolos verifikasi regex `\d+\.\d+` di `UserPresentationTest`) |
| Persentase numerik (mis. `84%`) | Diganti status kualitatif: `Dalam batas wajar`, `Perlu pemantauan`, `Perlu perhatian` |
| ID Aturan (`R-M01`, `R-K03`) | Diganti chip tag klinis ringkas (mis. `Riwayat keluarga`, `Pola makan & aktivitas`) |
| Penyimpanan Riset Lengkap | Disimpan di tabel SQLite `research_cf_records` terisolasi berversi `kb_version = 'cf-kb-v2.0'` |

---

## 5. Desain UI/UX "Soft Daylight" (v4.0) & Sistem Tipografi 3-Suara

### 5.1 Arsitektur Warna 3-Lapis (`Color.kt` & `Theme.kt`)
* **Lapis 1 — Palet Tonal Mentah**: **Sage** (ketenangan & brand utama, `#2E6A57`), **Clay** (kehangatan manusiawi, `#93482E`), **Mist** (kehadiran AI-EMPATI & dukungan, `#5A5896`), **Honey** (`#D29A2E`), **Terracotta** (`#A8402B`), **Berry** (`#8E2F55`), serta **Stone** (permukaan kertas hangat berbias hijau `#F7F5F0` di mode terang & `#121512` di mode gelap).
* **Lapis 2 — Peran Material 3**: `LightColorScheme` dan `DarkColorScheme` lengkap dengan hirarki `surfaceContainerLowest` hingga `surfaceContainerHighest`.
* **Lapis 3 — Semantik Afektif (`MaterialTheme.empati`)**:
  * `calm` (Sage — risiko rendah & faktor pelindung), `attentive` (Honey — perlu pemantauan), `concerned` (Terracotta — perlu perhatian), `urgent` (Berry — sangat tinggi), `neutral` (Stone — belum lengkap/netral), dan `presence` (Mist — identitas AI-EMPATI & rujukan SAPA 129).
* **Token Desain & Aksesibilitas (`Tokens.kt`)**:
  * `EmpatiSpace` (grid 4dp, `gutter = 24.dp`, `readableWidth = 560.dp`, `touchTarget = 48.dp`), `EmpatiElevation` + `Modifier.softShadow`, `EmpatiMotion` (`EaseOutSoft`, `EaseInOutSoft`), `LocalReducedMotion` (menghormati pengaturan sistem *Remove animations*), dan `Modifier.focusHalo` (cincin fokus keyboard/D-Pad WCAG 2.4.7).

### 5.2 Sistem Tipografi 3-Suara (Bundled Fonts di `res/font/` & `Type.kt`)
1. **`DisplayFont` — Fraunces** (`fraunces.ttf`, variable soft-serif `opsz=72, SOFT=100, WONK=0`): Dipakai untuk `displayLarge` s/d `headlineSmall` (sapaan waktu, pertanyaan kuesioner, judul halaman, pernyataan hasil di `ResultHero`, dan inisial `EmpatiAvatar`).
2. **`BodyFont` — Inter** (`inter.ttf` + `EmpatiNumeralStyle` tabular numerals `tnum`): Dipakai untuk `titleLarge` s/d `labelSmall` dengan *sentence-case* percakapan (`toSentenceCase()`) serta penghitung langkah kuesioner (`3 / 16`) agar angka tidak bergeser.
3. **`MetricFont` — JetBrains Mono** (`jetbrains_mono.ttf` via `EmpatiMetricStyle`): Dipakai **hanya** untuk pengenal mentah (ID akun di Profil & anatomi NIP 18 digit)—tidak pernah untuk badge atau label UI.

---

## 6. Otentikasi & Manajemen Identitas Inklusif (Semua Kalangan + Kohort Disertasi)

Dirancang pada [`AsnNipValidator.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/data/auth/AsnNipValidator.kt) dan [`AuthScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/AuthScreen.kt):

### 6.1 Validasi Identitas Multi-Format (`validateGeneralIdentity`)
Agar dapat digunakan oleh seluruh lapisan masyarakat tanpa hambatan, sistem menerima 5 format ID Akun:
1. **Alamat Email**: mis. `ratna.andari@empati.id`
2. **Nomor HP / WhatsApp**: mis. `081234567890` atau `+6281234567890` (otomatis dinormalisasi ke `08...`)
3. **Username Unik**: minimal 4 karakter alfanumerik (mis. `budi_santoso99`)
4. **NIK KTP (16 Digit Angka)**: untuk masyarakat umum
5. **NIP / NI PPPK (18 Digit Angka — Standar BKN)**: mengaktifkan *Live NIP Anatomy Inspector* yang otomatis mengurai Tanggal Lahir (`YYYYMMDD`), TMT Pengangkatan (`YYYYMM`), dan Jenis Kelamin (`1` = Laki-laki, `2` = Perempuan) tanpa memblokir gender tertentu.

### 6.2 Atribut Demografi Inklusif & Kriptografi
* **Kategori Latar Belakang (`jenis_asn` / `UserOccupationCategory`)**: `Masyarakat Umum`, `Karyawan / Pekerja`, `Wiraswasta / UMKM`, `Mahasiswa / Pelajar`, `Ibu Rumah Tangga`, `ASN / Instansi Publik`, `Tenaga Medis / Pendidik`.
* **Jenis Kelamin (`golongan` / `UserGenderOption`)**: `Perempuan` atau `Laki-laki` (memungkinkan pemisahan kohort analisis gender pada disertasi).
* **Pengamanan PIN**: Disimpan menggunakan **PBKDF2-HMAC-SHA256** dengan 120.000 iterasi dan salt unik 16-byte (`password_salt`).
* **Pseudonymization Firewall (`subject_hash`)**:
  $$subject\_hash = \text{"sub\_"} + \text{SHA256}(\text{"empati-asn-research-v2:"} + \text{normalizedAccountId}).take(20)$$
  Menghubungkan catatan inferensi riset tanpa pernah menyimpan ID akun, email, nomor HP, NIK, NIP, atau nama pengguna mentah di tabel CF.

---

## 7. Arsitektur Database SQLite Multi-Domain (*Zero-Collision*)

Database tunggal `empati_multi_domain.db` (versi 3) di [`EmpatiDatabase.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/data/db/EmpatiDatabase.kt) mengisolasi data menjadi 4 domain terpisah:

```
                               empati_multi_domain.db (v3)
                                        │
    ┌───────────────────┬───────────────┴───────────────┬───────────────────┐
    ▼                   ▼                               ▼                   ▼
Domain 1            Domain 2                        Domain 3            Domain 4
[auth_asn_users]    [user_screening_summaries]      [research_cf_records] [rag_knowledge_chunks]
• user_id (PK)      • summary_id (PK)               • screening_id (PK)   • chunk_id (PK)
• nip_clean (UK)    • user_id (FK)                  • subject_hash (FK)   • doc_source
• full_name         • score (0..100)                • kb_version          • topic_scope
• jenis_asn/gol     • risk_level (LOW..VERY_HIGH)   • hypertension_cf     • content_text
• password_hash     • screening_date                • gbv_cf              • keywords_csv
• password_salt     • created_at                    • payload_json        [rag_chat_logs]
• subject_hash (UK)                                                       • log_id (PK)
                                                                          • user_id (FK)
                                                                          • retrieved_chunk_ids
```

### Matriks Isolasi Hak Akses
* **Engine CF**: Membaca aturan dari file Kotlin statis $\rightarrow$ Menulis ke `user_screening_summaries` dan `research_cf_records`. **Tidak menyentuh tabel RAG**.
* **Engine RAG & Ollama**: Membaca referensi dari `rag_knowledge_chunks` $\rightarrow$ Menulis log ke `rag_chat_logs`. Menerima konteks skrining via snapshot baca saja. **Dilarang menulis ke tabel CF**.

---

## 8. Sistem RAG & Integrasi Ollama (`qwen2.5:3b-instruct`)

Diimplementasikan pada [`RagKnowledgeRepository.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/data/rag/RagKnowledgeRepository.kt), [`OllamaRagService.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/data/rag/OllamaRagService.kt), [`Modelfile.empati-qwen`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/ollama/Modelfile.empati-qwen), dan dihubungkan ke [`ChatEngine.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/data/ChatEngine.kt):

### 8.1 Jembatan Konteks Satu Arah (`ScreeningContextSnapshot`)
RAG/LLM tidak pernah menghitung ulang CF atau memodifikasi diagnosa. Konteks yang disalurkan bersifat *read-only* namun kaya secara kualitatif (`UserResultPresenter.buildContextSnapshot`):
```kotlin
data class ScreeningContextSnapshot(
    val riskLevelLabel: String,                 // Mis. "Tinggi"
    val integratedCategoryLabel: String?,       // Mis. "Risiko Hipertensi dengan Dominasi Faktor Kesehatan dan Gaya Hidup"
    val screeningDate: String,                  // Mis. "09 Nov 2026"
    val summaryNarrative: String? = null,
    val hypertensionStatusLabel: String? = null,
    val gbvStatusLabel: String? = null,
    val keyFactors: List<String> = emptyList(), // Mis. ["Pola makan & aktivitas", "Stres & kualitas tidur"]
    val protectiveFactors: List<String> = emptyList(),
    val relationshipExplanation: String? = null,
    val keyRecommendations: List<String> = emptyList()
)
```
* **Pemisahan Cek Cepat vs Analisis Mendalam LLM**:
  - Ketika pengguna memilih `"Lihat hasil skrining saya"` (`isSimpleResultLookupQuery`), chatbot menampilkan ringkasan status singkat (`snapshot-zero-leak`).
  - Ketika pengguna bertanya `"analisis hasil screening saya"`, `"apa yang bisa anda simpulkan dari hasil screening saya"`, atau pertanyaan analitis sejenis (`isScreeningAnalysisQuery`), `ChatEngine` meneruskan permintaan ke LLM beserta `[KONTEKS HASIL SKRINING PENGGUNA SAAT INI]` sehingga LLM benar-benar menganalisis keterkaitan faktor pemicu dan menyimpulkan kondisi pengguna secara cerdas.

### 8.2 Korpus Pengetahuan Terverifikasi (`rag_knowledge_chunks`)
1. `DOC-HT-01` (*Pedoman Tatalaksana Hipertensi Kemenkes RI*): Batas tensi normal, pra-hipertensi, hipertensi stage 1/2.
2. `DOC-HT-02` (*Panduan Diet DASH & Konsumsi Natrium WHO/Kemenkes*): Batas aman garam $< 1$ sdt/hari ($2.000\text{ mg}$ natrium), asupan kalium.
3. `DOC-HT-03` (*Rekomendasi Aktivitas Fisik Kardiovaskular*): Olahraga aerobik 150 menit/minggu & *micro-break* aktif bagi pekerja, wiraswasta, mahasiswa, maupun ibu rumah tangga.
4. `DOC-ASN-01` (*Pedoman Manajemen Stres, Beban Kerja & Kesehatan Tidur*): Korelasi stres harian/pekerjaan, tidur $< 7$ jam, lonjakan hormon kortisol, dan reaktivitas kardiovaskular.
5. `DOC-GBV-01` (*UU TPKS & Layanan KemenPPPA SAPA 129*): Hak korban kekerasan, prosedur pengaduan aman dan rahasia via hotline **SAPA 129** / WhatsApp **08111-129-129**.

### 8.3 Integrasi Generator LLM Murni Terbatasi Topik (`OllamaRagService.kt` & `qwen2.5:3b-instruct`)
* **Model**: `qwen2.5:3b-instruct` / `empati-qwen2.5:3b` via Ollama (`num_predict: 512`, `num_ctx: 2048`) tanpa injeksi korpus kaku atau chip sitasi di UI, sehingga jawaban jauh lebih cerdas, alami, dan utuh.
* **Guardrails Berlapis**:
  1. **Input Privacy Firewall (`sanitizePrivacyInput`)**: Menyensor otomatis 18 digit NIP, 16 digit NIK, alamat Email, dan Nomor HP sebelum dikirim ke generator.
  2. **Topic Scoping (3 Ranah Inti)**: Hanya menjawab topik **Kesehatan** (hipertensi, tekanan darah, jantung, nutrisi, olahraga), **Psikologi** (stres, kecemasan, kualitas tidur, kesehatan mental), dan **Perlindungan Kekerasan** (KBG/KDRT & layanan SAPA 129).
  3. **Anti-Truncated Generation (`ensureCompleteResponse`)**: Menghapus simbol markdown `**` dan memastikan setiap jawaban selesai pada kalimat yang utuh hingga tanda titik terakhir.
  4. **Zero-Leak & Non-Kausalitas**: Menghapus angka desimal, simbol `%`, serta istilah `CF`/`MB`/`MD`, dan menyajikan hubungan KBG-hipertensi sebagai *chronic stress pathway*.
  5. **Mandatory SAPA 129**: Menyertakan hotline rahasia **SAPA 129 (Telepon 129 / WhatsApp 08111-129-129)** khusus ketika pertanyaan menyangkut kekerasan/KBG.

---

## 9. Struktur Repositori & Indeks Kode Sumber

```
Empati/
├── app/src/main/java/com/example/empati/
│   ├── MainActivity.kt                       # Entry point Android, FLAG_SECURE
│   ├── EmpatiApp.kt                          # Composable navigasi, rank screen, animasi transisi
│   ├── EmpatiViewModel.kt                    # UDF State, auth session, screening flow, chat RAG
│   ├── data/
│   │   ├── Models.kt                         # Data class UserProfile, ScreeningResult, RiskLevel
│   │   ├── ChatEngine.kt                     # Chat engine yang terhubung ke korpus RAG & snapshot
│   │   ├── ScreeningStore.kt                 # Repositori tunggal penghubung ViewModel & SQLite
│   │   ├── auth/
│   │   │   └── AsnNipValidator.kt            # Parser anatomi 18-digit NIP, PBKDF2, subject_hash
│   │   ├── db/
│   │   │   └── EmpatiDatabase.kt             # SQLite Multi-Domain 5 tabel (Zero-Collision)
│   │   ├── cloud/
│   │   │   └── SupabaseCloudSyncService.kt   # Sinkronisasi Cloud Supabase (research_cf_records & research_chat_logs) + proxy empati-chat
│   │   ├── rag/
│   │   │   ├── RagKnowledgeRepository.kt     # Korpus chunk, ScreeningContextSnapshot, lexical retriever
│   │   │   ├── WebRagSearchService.kt        # Live Internet RAG (Wikipedia Medis ID & Europe PMC)
│   │   │   └── OllamaRagService.kt           # OpenRouter + Supabase Edge Function + Ollama lokal & guardrails klinis
│   │   ├── cf/                               # 🧠 ENGINE SISTEM PAKAR CERTAINTY FACTOR
│   │   │   ├── CfModels.kt                   # Domain enums & data class inferensi
│   │   │   ├── CfQuestions.kt                # 26 pertanyaan dual-modul & label skala
│   │   │   ├── CfInputParser.kt              # Parser jawaban ke CF user (dikotomi, likert, reverse M08)
│   │   │   ├── CfKnowledgeBase.kt            # 26 aturan pakar (MB, MD per sub-hipotesis)
│   │   │   ├── CfInferenceEngine.kt          # Formula kombinasi CF & agregasi modul
│   │   │   └── CfResultGenerator.kt          # Klasifikasi 6 kategori analisis terintegrasi
│   │   ├── presentation/
│   │   │   ├── UserPresentation.kt           # DTO model aman untuk UI (tanpa istilah CF)
│   │   │   └── UserResultPresenter.kt        # Translator model teknis CF ke bahasa awam
│   │   └── research/
│   │       ├── ResearchModels.kt             # DTO data riset lengkap (tersimpan via subject_hash)
│   │       └── ResearchRecordFactory.kt      # Factory pembentuk record audit riset
│   └── ui/
│       ├── components/Components.kt          # ScreenFrame, TonalCard, ToneBadge, StateOrb, EmpatiTextField, dll.
│       ├── screens/                          # 11 layar aplikasi Compose (Stateless Content + Stateful Wrapper)
│       │   ├── SplashScreen.kt               # Splash screen dengan halo aura & EmpatiLogo bernapas
│       │   ├── OnboardingScreen.kt           # 3 slide pengenalan dengan OnboardingArtwork & nada afektif
│       │   ├── AuthScreen.kt                 # Portal Login/Registrasi multi-format (Email/HP/Username/NIK/NIP)
│       │   ├── HomeScreen.kt                 # Dashboard status risiko selaras, layanan, & SAPA 129
│       │   ├── ConsentScreen.kt              # Informed consent berbasis ubin ikon & ekspansi teks
│       │   ├── QuestionnaireScreen.kt        # Kuesioner bertahap dengan EmpatiNumeralStyle & semantik radio
│       │   ├── ResultScreen.kt               # ModuleResultScreen & IntegratedResultScreen dengan StateOrb
│       │   ├── ChatScreen.kt                 # Layar konsultasi AI-EMPATI dengan BreathingDots
│       │   ├── HistoryScreen.kt              # Riwayat skrining kronologis terisolasi per akun
│       │   ├── ProfileScreen.kt              # Profil demografi, privasi, ganti akun, & hapus data
│       │   └── MainScaffold.kt               # BottomNavBar beraksesibilitas tinggi & transisi tab
│       └── theme/
│           ├── Color.kt                      # Palet "Soft Daylight" v4 (Sage, Clay, Mist, Honey, Terra, Berry, Stone)
│           ├── Theme.kt                      # Material 3 ColorScheme, EmpatiColors (MaterialTheme.empati), EmpatiShapes
│           ├── Tokens.kt                     # EmpatiSpace, EmpatiIcon, EmpatiElevation, EmpatiMotion, LocalReducedMotion
│           └── Type.kt                       # 3-Voice Typography (Fraunces, Inter + tnum, JetBrains Mono)
├── app/src/main/res/font/                    # Aset font variabel resmi (OFL)
│   ├── fraunces.ttf                          # DisplayFont (Fraunces soft-serif opsz=72, SOFT=100, WONK=0)
│   ├── inter.ttf                             # BodyFont & EmpatiNumeralStyle (Inter)
│   └── jetbrains_mono.ttf                    # MetricFont / EmpatiMetricStyle (JetBrains Mono)
├── app/src/test/                             # 49 Unit Test JVM (100% Pass)
│   ├── cf/CfEngineTest.kt                    # 30 test parser, kombinasi CF, & determinisme
│   ├── presentation/UserPresentationTest.kt  # 7 test zero-leak CF & penyelarasan status risiko
│   └── auth/AsnAndRagArchitectureTest.kt     # 11 test identitas multi-format, PBKDF2, subject_hash, payload Supabase/OpenRouter, & LLM guardrails
├── app/src/androidTest/                      # 6 Instrumented SQLite Test
│   └── EmpatiDatabaseInstrumentedTest.kt     # Test SQLite sungguhan di emulator/device
├── supabase/                                 # Infrastruktur Cloud Supabase (empati-research)
│   ├── migrations/20261108000001_create_empati_research_schema.sql
│   └── functions/empati-chat/index.ts        # Edge Function proxy OpenRouter (Free-Tier Smart Router openrouter/free)
├── app/proguard-rules.pro                    # Aturan R8/ProGuard untuk build rilis APK
├── project_analysis.md                       # Analisis arsitektur & perkembangan proyek menyeluruh (v4.0.0)
├── PROJECT_CONTEXT_SUMMARY.md                # 📄 Dokumen ini (Master context untuk LLM/Notebook)
├── DATABASE_AUTH_AND_RAG_ARCHITECTURE.md     # Spesifikasi teknis database & RAG/LLM
├── EXPERT_SYSTEM.md                          # Dokumentasi mendalam sistem pakar Certainty Factor
├── FRONTEND_DESIGN.md                        # Panduan sistem desain "Soft Daylight" v4.0
├── TYPOGRAPHY_AND_CLINICAL_UX.md             # Filosofi tipografi 3-suara & UX empati klinis
├── CHANGELOG.md                              # Log riwayat rilis v1.0 s/d v4.0.0
└── README.md                                 # Ringkasan repositori utama
```

