# 🎨 AI-EMPATI — Panduan Desain Front-End (v3.1)

Dokumen ini mendokumentasikan sistem desain, tipografi fungsional 3-peran, komponen UI, animasi, dan spesifikasi layar aplikasi AI-EMPATI. Dibangun dengan **Jetpack Compose** dan **Material 3 (Material You)** dengan pendekatan **Minimalisme Klinis (*Clinical Minimalism*)** yang tenang, terstruktur, inklusif untuk semua kalangan, dan bebas *AI slop*.

> Untuk penjelasan mendalam mengenai rasional pemilihan 3 font dan reduksi informasi pada hasil skrining, lihat [`TYPOGRAPHY_AND_CLINICAL_UX.md`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/TYPOGRAPHY_AND_CLINICAL_UX.md).

---

## 📋 Daftar Isi

- [Prinsip Desain](#prinsip-desain)
- [Design System](#design-system)
  - [Palet Warna & Hirarki Tonal](#palet-warna--hirarki-tonal)
  - [Tipografi Editorial](#tipografi-editorial)
  - [Spacing & Sizing](#spacing--sizing)
  - [Bentuk (Shapes)](#bentuk-shapes)
- [Sistem Animasi & Micro-Interactions](#sistem-animasi--micro-interactions)
- [Komponen Reusable](#komponen-reusable)
- [Layar Aplikasi](#layar-aplikasi)
  - [1. Splash Screen](#1-splash-screen)
  - [2. Onboarding](#2-onboarding)
  - [3. Home Dashboard](#3-home-dashboard)
  - [4. Persetujuan (Consent)](#4-persetujuan-consent)
  - [5. Kuesioner](#5-kuesioner)
  - [6. Hasil Modul](#6-hasil-modul)
  - [7. Analisis Terintegrasi](#7-analisis-terintegrasi)
  - [8. Chat (Konsultasi)](#8-chat-konsultasi)
  - [9. Riwayat](#9-riwayat)
  - [10. Profil & Privasi](#10-profil--privasi)
- [Navigasi](#navigasi)
- [Privasi & Keamanan Visual](#privasi--keamanan-visual)
- [Aksesibilitas](#aksesibilitas)
- [Referensi File](#referensi-file)

---

## Prinsip Desain

1. **Android Material You Native (Bukan iOS)** — Menggunakan komponen dan pola Material 3 autentik seperti `NavigationBar` dengan indikator kapsul aktif, `SuggestionChip`, dan hirarki permukaan tonal (`surfaceContainerLow` hingga `surfaceContainerHigh`).
2. **Bebas *AI Slop*** — Menghindari latar belakang gradien dekoratif, bayangan (*drop shadow*) mencolok, efek neon/glow, dan penomoran dekoratif (`01/02/03`) pada bagian yang bukan urutan langkah.
3. **Palet Mauve / Dusty Plum & Teal** — Menghindari klise warna merah muda (*pink*) untuk produk kesehatan perempuan; memadukan *dusty plum* (`Mauve`) yang tenang dan berwibawa dengan aksen `Teal` untuk progres dan indikator aman.
4. **Pemisahan Ketat Data Penelitian vs Tampilan Pengguna** — Nilai teknis internal (*Certainty Factor*, MB/MD, bobot, rumus, ID aturan) tidak pernah dirender di UI pengguna.

---

## Design System

### Palet Warna & Hirarki Tonal

Sistem warna menggunakan pendekatan **semantic color roles** dari Material 3 dengan hirarki permukaan tonal lengkap untuk mode terang maupun gelap.

#### Warna Utama

| Peran | Token | Hex | Kegunaan |
|---|---|---|---|
| **Primary** | `Mauve500` | `#7B4F97` | Tombol utama, indikator aktif, aksen utama |
| **Primary Container** | `Mauve100` | `#EADFF2` | Indikator tab aktif, chip utama, badge ikon |
| **Secondary** | `Teal600` / `Teal500` | `#21857B` / `#2E9C8F` | Progress KBG, indikator online, status positif |
| **Secondary Container** | `Teal50` | `#EAF6F4` | Kartu dukungan rahasia, banner `LockNotice` |
| **Tertiary** | `Amber600` / `Amber500` | `#B57E1C` / `#D99A2B` | Risiko sedang, status perlu perhatian |
| **Error** | `Coral600` | `#C64F3D` | Risiko tinggi, zona hapus data |

#### Permukaan Tonal (Neutral Surfaces)

| Mode | Token | Hex | Peran Material 3 |
|---|---|---|---|
| **Light** | `WarmOffWhite` | `#FBF9F7` | `background` |
| | `SurfaceContainerLowestLight` | `#FFFFFF` | `surface` / kartu utama |
| | `SurfaceContainerLowLight` | `#F3EFEA` | `surfaceContainerLow` (kotak ikon, input chat, banner) |
| | `SurfaceLight` | `#F7F4F0` | `surfaceContainer` |
| | `SurfaceVariantLight` | `#ECE6DF` | `surfaceVariant` (track progress) |
| | `SurfaceContainerHighLight` | `#E5DFD7` | `surfaceContainerHigh` |
| | `TextDark` | `#221A26` | `onBackground` / `onSurface` |
| | `TextMutedDark` | `#665B70` | `onSurfaceVariant` |
| | `BorderLight` | `#E2DAD0` | `outline` |
| | `BorderSubtleLight` | `#EDE7DF` | `outlineVariant` (border kartu halus) |
| **Dark** | `DarkBg` | `#15111A` | `background` |
| | `SurfaceContainerLowestDark` | `#100C14` | `surfaceContainerLowest` |
| | `SurfaceContainerLowDark` | `#1B1521` | `surfaceContainerLow` |
| | `DarkSurface` | `#1F1926` | `surface` / `surfaceContainer` |
| | `DarkSurfaceVariant` | `#2B2235` | `surfaceVariant` |
| | `SurfaceContainerHighDark` | `#352B41` | `surfaceContainerHigh` |
| | `TextLight` | `#F3EFF7` | `onBackground` / `onSurface` |
| | `TextMutedLight` | `#BBB0C7` | `onSurfaceVariant` |
| | `BorderDark` | `#3B2F4A` | `outline` |
| | `BorderSubtleDark` | `#2B2235` | `outlineVariant` |

---

### Tipografi Fungsional 3-Peran

AI-EMPATI menggunakan **tiga keluarga font variabel resmi (Open Font License)** yang dibundel di `app/src/main/res/font/`, masing-masing memiliki fungsi kognitif spesifik untuk membedakan hierarki klinis, narasi bacaan, dan metadata diagnostik:

| Peran Fungsional | Font Bundel | Variabel di `Type.kt` | File Resource | Kegunaan |
|---|---|---|---|---|
| **Heading & Otoritas Klinis** | **Plus Jakarta Sans** | `HeadingFont` | `res/font/plus_jakarta_sans.ttf` | Display, Headline, Title, dan tombol aksi (`labelLarge`) |
| **Body & Bacaan Klinis** | **Inter** | `BodyFont` | `res/font/inter.ttf` | Kalimat kesimpulan, opsi kuesioner, poin saran, dan percakapan chat |
| **Metadata & Label Diagnostik** | **JetBrains Mono** | `MonoFont` | `res/font/jetbrains_mono.ttf` | `SectionChip`, `RiskBadge`, `StatusPill`, indeks soal (`3 / 16`), tanggal, dan NIP ASN |

#### Type Scale

| Style | Font | Weight | Size | Line Height | Letter Spacing | Kegunaan |
|---|---|---|---|---|---|---|
| `displaySmall` | Plus Jakarta Sans | Bold | 30sp | 36sp | -0.6sp | Display utama |
| `headlineLarge` | Plus Jakarta Sans | Bold | 26sp | 32sp | -0.5sp | Heading besar |
| `headlineMedium` | Plus Jakarta Sans | Bold | 22sp | 28sp | -0.4sp | Sapaan nama pengguna, judul halaman utama |
| `headlineSmall` | Plus Jakarta Sans | SemiBold | 19sp | 25sp | -0.3sp | Teks pertanyaan kuesioner, judul kartu utama |
| `titleLarge` | Plus Jakarta Sans | SemiBold | 17sp | 23sp | -0.2sp | Kesimpulan klinis utama, judul dialog |
| `titleMedium` | Plus Jakarta Sans | SemiBold | 15sp | 21sp | -0.1sp | Sub-section header, judul top bar chat |
| `titleSmall` | Plus Jakarta Sans | SemiBold | 14sp | 19sp | 0sp | Judul kartu bagian (`ActionPlanCard`), `FeatureCard` |
| `bodyLarge` | Inter | Normal | 15sp | 22sp | 0sp | Label opsi jawaban, nilai field profil |
| `bodyMedium` | Inter | Normal | 14sp | 20sp | 0.05sp | Ringkasan klinis 1 kalimat, poin langkah, gelembung chat |
| `bodySmall` | Inter | Normal | 12sp | 17sp | 0.1sp | Teks pendukung, `ClinicalTagChip`, disclaimer 1 baris |
| `labelLarge` | Plus Jakarta Sans | SemiBold | 14sp | 19sp | 0.05sp | Teks tombol utama (`PrimaryButton`), tautan aksi |
| `labelMedium` | JetBrains Mono | SemiBold | 11sp | 15sp | 0.5sp | `SectionChip`, `RiskBadge`, `StatusPill`, indikator progres |
| `labelSmall` | JetBrains Mono | Medium | 10sp | 14sp | 0.4sp | Label header matriks modul, tanggal riwayat, label field profil |

---

### Spacing & Sizing

- **Padding horizontal layar standar:** `20dp` (`28dp` pada konten slide Onboarding).
- **Padding internal kartu (`TonalCard`):** `18dp–22dp` (memberikan ruang bernapas yang lega).
- **Jarak antar kartu:** `12dp–16dp`.
- **Tinggi tombol utama (`PrimaryButton`):** `54dp` dengan ikon panah pengarah (`18dp`).
- **Target sentuh minimum:** `48dp` pada seluruh kontrol interaktif.

---

### Bentuk (Shapes)

| Elemen | Radius | Catatan |
|---|---|---|
| Kartu utama (`Hero` / `RiskCard`) | `24dp` | `RoundedCornerShape(24.dp)` |
| Kartu bagian (`TonalCard` / `SectionCard`) | `20dp–22dp` | Dengan border `1dp` `outlineVariant` |
| Tombol primer & ghost | `18dp` | `RoundedCornerShape(18.dp)` |
| Baris opsi kuesioner (`OptionRow`) | `18dp` | Border beranimasi saat dipilih |
| Tombol kembali (`BackIconButton`) | `16dp` | `48dp × 48dp`, `surfaceContainerLow` |
| Gelembung chat | `20dp` (ekor `6dp`) | Sudut asimetris sesuai pengirim |
| Komposer input chat | `24dp` | Kapsul penuh dengan tombol kirim bulat `48dp` |

---

## Sistem Animasi & Micro-Interactions

Seluruh animasi dirancang fungsional untuk memperjelas orientasi ruang dan umpan balik sentuhan:

1. **Navigasi Antar-Layar (`EmpatiApp.kt`)**
   - Menggunakan `AnimatedContent` berdasarkan hirarki urutan layar (`screenRank`).
   - Berpindah maju (mis. *Home → Consent → Kuesioner*) menggunakan transisi *shared-axis horizontal slide* + *fade* (`FastOutSlowInEasing`).
   - Kembali (*Back*) memutar arah slide secara proporsional.
2. **Pergantian Tab Utama (`MainScaffold.kt`)**
   - Crossfade halus (`220ms` in / `160ms` out) antar tab *Beranda, Konsultasi, Riwayat, Profil*.
3. **Transisi Pertanyaan Kuesioner (`QuestionnaireScreen.kt`)**
   - `AnimatedContent` menggeser pertanyaan berikutnya dari kanan (atau dari kiri saat kembali ke pertanyaan sebelumnya).
   - `LinearProgressIndicator` beranimasi mulus dengan `animateFloatAsState` (`Spring.StiffnessLow`).
   - Memilih jawaban memicu animasi warna latar, ketebalan border, tinggi bilah aksen vertikal (`animateDpAsState`), serta kemunculan ikon centang (`scaleIn` + `fadeIn`).
   - Tombol "Selanjutnya" muncul dengan `AnimatedVisibility` (`slideInVertically` + `fadeIn`).
4. **Umpan Balik Sentuh (`Components.kt`)**
   - `PrimaryButton`, `GhostButton`, `TonalCard` interaktif, dan `OptionRow` merespons tekanan jari (`collectIsPressedAsState`) dengan skala pegas halus (`0.975f–0.985f`, `Spring.DampingRatioMediumBouncy`).
5. **Onboarding Parallax & Pager Indicator (`OnboardingScreen.kt`)**
   - Ilustrasi dan teks merespons `currentPageOffsetFraction` dengan interpolasi skala dan opasitas.
   - Indikator halaman melebar secara pegas (`8dp ↔ 28dp`) saat aktif.
6. **Daftar Pesan Chat (`ChatScreen.kt`)**
   - Gelembung pesan baru muncul menggunakan `Modifier.animateItem()` dan tombol kirim berubah warna secara reaktif (`animateColorAsState`) ketika teks diisi.

---

## Komponen Reusable

Berada di [`Components.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/components/Components.kt):

- **`TonalCard`** — Kontainer permukaan Material 3 dengan `outlineVariant` border 1dp, mendukung mode statis maupun klik dengan animasi skala pegas.
- **`PrimaryButton` & `GhostButton`** — Tombol aksi penuh dengan umpan balik tekan dan ikon panah opsional.
- **`BackIconButton`** — Tombol kembali `48dp` berbentuk squircle (`16dp`) dengan permukaan `surfaceContainerLow`.
- **`RiskBadge`** — Lencana kapsul status risiko dengan titik warna semantik dan border tipis.
- **`SectionChip`** — Penanda kategori kapsul dengan tipografi `labelMedium`.
- **`LockNotice`** — Pita informasi kerahasiaan berwarna `secondaryContainer` untuk pertanyaan sensitif KBG.
- **`EmpatiLogo`** — Logo perisai & siluet dengan animasi cincin ganda berputar dan *gentle breathing pulse*.
- **`EmpatiAvatar`** — Avatar inisial pengguna dengan tipografi serif `Lora` dan cincin border halus.

---

## Layar Aplikasi

### 1. Splash Screen
**File:** [`SplashScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/SplashScreen.kt)
- Menampilkan `EmpatiLogo` beranimasi, lencana `SKRINING KESEHATAN & KESEJAHTERAAN`, judul `AI-EMPATI` (`36sp`), dan subjudul inklusif dengan animasi masuk `fadeIn` + `scaleIn` + `slideInVertically`.

### 2. Onboarding & Portal Otentikasi Multi-Format (Semua Kalangan)
**File:** [`OnboardingScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/OnboardingScreen.kt) & [`AuthScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/AuthScreen.kt)
- **Onboarding**: Header menampilkan chip progres langkah (`Langkah 1 dari 3`), tombol `Lewati`, ilustrasi squircle berlapis (`204dp`), dan tombol `Selanjutnya` / `Mulai Sekarang`.
- **Portal Login & Registrasi Inklusif (`AuthScreen.kt`)**:
  - Switcher mode **Masuk Akun** dan **Registrasi Baru**.
  - Input **ID Akun Multi-Format** (Email, Nomor HP, Username, NIK 16 digit, atau NIP 18 digit) disertai indikator deteksi format otomatis (serta *Live NIP Anatomy Inspector* bila pengguna memasukkan 18 digit angka).
  - Pilihan terstruktur **Kategori Latar Belakang** (*Masyarakat Umum*, *Karyawan / Pekerja*, *Wiraswasta / UMKM*, *Mahasiswa / Pelajar*, *Ibu Rumah Tangga*, *ASN / Instansi Publik*, *Tenaga Medis / Pendidik*) dan **Jenis Kelamin** (*Perempuan* / *Laki-laki*).
  - Pita informasi **Pseudonymization Firewall (`subject_hash`)** dan tombol **Masuk Cepat Akun Demo**.

### 3. Home Dashboard
**File:** [`HomeScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/HomeScreen.kt)
- Header sapaan waktu, nama lengkap, instansi, serta avatar yang dapat diketuk menuju Profil.
- **Kartu Status Risiko (`RiskCard` / `EmptyRiskCard`)**: menampilkan tingkat risiko kualitatif terakhir, tanggal, deskripsi singkat berbahasa awam, tombol cepat `Skrining Ulang`, dan tautan `Lihat riwayat`.
- **Grid Layanan Utama**: dua kartu `FeatureCard` berdampingan (*Mulai Skrining* & *Tanya AI-EMPATI*).
- **Banner Ruang Aman & SAPA 129**: informasi penyimpanan lokal dan akses cepat ke bantuan.

### 4. Persetujuan (Consent)
**File:** [`ConsentScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ConsentScreen.kt)
- Top bar dengan `BackIconButton` dan chip `INFORMED CONSENT`.
- Daftar 5 komitmen kerahasiaan dan tujuan skrining disajikan menggunakan ikon semantik di tiap bagian (tanpa angka dekoratif), dipisahkan oleh `HorizontalDivider`.

### 5. Kuesioner
**File:** [`QuestionnaireScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/QuestionnaireScreen.kt)
- Header menampilkan nama modul, rasio pertanyaan (`3 / 16`), dan progress bar beranimasi.
- Satu pertanyaan per layar dengan transisi geser horizontal.
- Opsi jawaban (`OptionRow`) memiliki bilah aksen kiri yang memanjang saat dipilih dan indikator bulatan centang.

### 6. Hasil Modul (Minimalis Klinis)
**File:** [`ResultScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ResultScreen.kt) → `ModuleResultScreen`
- **Kartu Ringkasan Klinis Utama**: menggabungkan `StatusPill` (`MonoFont`), ikon modul, kesimpulan 1 kalimat padat (`titleLarge`), dan deretan `FlowRow` berisi `ClinicalTagChip` untuk faktor risiko / kategori temuan yang teridentifikasi.
- **`ActionPlanCard`**: menampilkan maksimal 3 langkah tindak lanjut ringkas dengan ikon centang.
- **`SupportStrip` (Kondisional)**: pita bantuan rahasia SAPA 129 yang hanya muncul bila terdapat catatan dukungan (`supportNote != null`).
- **`MinimalDisclaimer`**: catatan kaki edukatif 1 baris di bagian bawah layar.

### 7. Analisis Terintegrasi (Ringkasan Eksekutif)
**File:** [`ResultScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ResultScreen.kt) → `IntegratedResultScreen`
- **Kartu Sintesis Utama**: menampilkan `StatusPill`, kategori analisis terintegrasi (`titleLarge`), dan ringkasan 1 kalimat (`bodyMedium`).
- **Matriks Status 2-Kolom (`ModuleStatusCell`)**: merangkum status modul **`HIPERTENSI`** dan **`KESEJAHTERAAN`** secara berdampingan di dalam kartu utama tanpa mengulang paragraf panjang dari layar sebelumnya.
- **Deretan Faktor Utama (`FlowRow` `ClinicalTagChip`)**: menampilkan faktor kontribusi secara ringkas di bawah matriks.
- **`ActionPlanCard` & `SupportStrip`**: langkah rekomendasi singkat dan akses bantuan bila relevan.

### 8. Chat (Konsultasi AI-EMPATI Berbasis LLM Murni)
**File:** [`ChatScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ChatScreen.kt)
- Header permukaan `surface` minimalis dengan `BotAvatar` berindikator aktif, judul **`AI-EMPATI`** (tanpa kata `"Asisten"` dan tanpa subjudul di bawahnya), serta lencana status koneksi **`• QWEN 2.5 LLM`**.
- Banner cakupan topik di puncak daftar pesan (`AI-EMPATI · Ruang lingkup: Kesehatan, Psikologi & Kekerasan`).
- Gelembung respons bot bersih dari tempelan korpus/chip sitasi yang mengganggu keterbacaan, dengan jaminan kalimat utuh (`num_predict: 512` + `ensureCompleteResponse`).
- Deretan `SuggestionChip` Material 3 untuk pertanyaan cepat dan komposer pesan kapsul (`BasicTextField`) dengan tombol kirim bulat beranimasi warna.

### 9. Riwayat
**File:** [`HistoryScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/HistoryScreen.kt)
- Daftar kronologis kartu `HistoryItem` yang menampilkan tingkat risiko kualitatif, tanggal, `RiskBadge`, dan catatan singkat (tanpa membocorkan skor CF). Item terbaru diberi aksen border primer halus.
- State kosong (`EmptyHistory`) dilengkapi tombol CTA untuk langsung memulai skrining pertama.

### 10. Profil & Privasi
**File:** [`ProfileScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ProfileScreen.kt)
- Kartu identitas pengguna dengan `EmpatiAvatar` `64dp`, nama, kategori latar belakang, pekerjaan/aktivitas, dan domisili/instansi.
- Kartu grup **Data Profil & Demografi** dengan ikon per baris (*Nama Lengkap, ID Akun, Domisili / Instansi, Pekerjaan / Aktivitas*) yang dapat diketuk untuk membuka dialog edit.
- Kartu grup **Pengaturan & Keamanan** (*Perlindungan Layar FLAG_SECURE*, *Pengingat Pemeriksaan*, *Tentang AI-EMPATI*).
- Tombol `Keluar Sesi / Ganti Akun` serta tindakan destruktif `Hapus Seluruh Data Saya dari Perangkat` dengan warna `errorContainer` dan dialog konfirmasi.

---

## Navigasi

Menggunakan komponen Material 3 `NavigationBar` dan `NavigationBarItem` di [`MainScaffold.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/MainScaffold.kt) dengan indikator kapsul `primaryContainer` dan pasangan ikon *Outlined* (tidak aktif) vs *Rounded Filled* (aktif):

| Tab | Label | Ikon Aktif / Tidak Aktif | Route |
|---|---|---|---|
| 1 | Beranda | `Icons.Rounded.Home` / `Icons.Outlined.Home` | `Screen.Home` |
| 2 | Skrining | `Icons.Rounded.FactCheck` / `Icons.Outlined.FactCheck` | → `Screen.Consent` |
| 3 | Konsultasi | `Icons.Rounded.ChatBubble` / `Icons.Outlined.ChatBubbleOutline` | `Screen.Chat` |
| 4 | Riwayat | `Icons.Rounded.History` / `Icons.Outlined.History` | `Screen.History` |
| 5 | Profil | `Icons.Rounded.Person` / `Icons.Outlined.Person` | `Screen.Profile` |

---

## Privasi & Keamanan Visual

| Mekanisme | Implementasi | File |
|---|---|---|
| **FLAG_SECURE** | `window.addFlags(FLAG_SECURE)` — mencegah tangkapan layar & pratinjau di *app switcher* | [`MainActivity.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/MainActivity.kt#L13) |
| **LockNotice** | Pita informasi keamanan lokal pada setiap pertanyaan sensitif KBG | [`Components.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/components/Components.kt) |
| **Pemisahan Presentasi** | UI hanya menerima `UserModulePresentation` & `UserIntegratedPresentation` | [`UserPresentation.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/data/presentation/UserPresentation.kt) |
| **Kendali Hapus Data** | Penghapusan permanen seluruh data lokal dengan konfirmasi eksplisit | [`ProfileScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ProfileScreen.kt) |

---

## Aksesibilitas

- **Target Sentuh Minimum:** `48dp` pada seluruh tombol, chip, dan item navigasi.
- **Kontras Warna:** Teks utama `TextDark` (`#221A26`) di atas `WarmOffWhite` (`#FBF9F7`) memenuhi standar WCAG AAA.
- **Mode Gelap Penuh:** Skema warna gelap tonal yang nyaman di mata tanpa kontras menyilaukan.
- **Beban Kognitif Rendah:** Satu pertanyaan per layar dengan animasi progres yang menenangkan.

---

## Referensi File

| File | Deskripsi |
|---|---|
| [`Color.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Color.kt) | Palet warna Mauve, Teal, Amber, Coral, dan permukaan tonal Light/Dark |
| [`Theme.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Theme.kt) | `LightColorScheme`, `DarkColorScheme`, `EmpatiShapes`, dan `EmpatiTheme` |
| [`Type.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Type.kt) | Sistem tipografi fungsional 3-peran (`Plus Jakarta Sans`, `Inter`, `JetBrains Mono`) |
| [`Components.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/components/Components.kt) | Komponen reusable (`TonalCard`, `PrimaryButton`, `RiskBadge`, `EmpatiLogo`, dll.) |
| [`EmpatiApp.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/EmpatiApp.kt) | Root composable & animasi transisi antar-layar |
| [`MainScaffold.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/MainScaffold.kt) | Material 3 `NavigationBar` & crossfade antar-tab |
| [`SplashScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/SplashScreen.kt) | Layar pembuka dengan animasi masuk halus |
| [`OnboardingScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/OnboardingScreen.kt) | Pager 3 langkah dengan efek paralaks & indikator pegas |
| [`AuthScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/AuthScreen.kt) | Portal Login & Registrasi ASN 18 digit NIP dengan *Live NIP Anatomy Inspector* |
| [`HomeScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/HomeScreen.kt) | Beranda dengan ringkasan risiko, layanan utama, dan banner privasi |
| [`ConsentScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ConsentScreen.kt) | Layar persetujuan berbasis ikon semantik |
| [`QuestionnaireScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/QuestionnaireScreen.kt) | Layar kuesioner dengan animasi geser & seleksi interaktif |
| [`ResultScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ResultScreen.kt) | Layar hasil per modul & analisis terintegrasi berbahasa awam |
| [`ChatScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ChatScreen.kt) | Layar konsultasi dengan `SuggestionChip` & komposer modern |
| [`HistoryScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/HistoryScreen.kt) | Daftar riwayat evaluasi & state kosong interaktif |
| [`ProfileScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ProfileScreen.kt) | Profil kepegawaian, status keamanan layar, dan penghapusan data |
