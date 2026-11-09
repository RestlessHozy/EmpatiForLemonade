# 🩺 SRYA Emora — Tipografi 3-Suara & UX Empati Klinis "Soft Daylight" (v4.1.5)

Dokumen ini menjelaskan rasional di balik **Sistem Tipografi 3-Suara (*3-Voice Typography*: Fraunces + Inter + JetBrains Mono)**, pemetaan **Semantik Afektif (`MaterialTheme.empati`)**, visualisasi **`StateOrb`**, ergonomi antarmuka obrolan klinis (*IME keyboard placement & clipboard actions*), dan pendekatan **UX Empati Klinis & Bahasa Manusiawi (*Anti AI-Slop*)** pada aplikasi **SRYA Emora**.

---

## 📋 Daftar Isi

1. [Mengapa UX Empati Klinis & "Soft Daylight"?](#1-mengapa-ux-empati-klinis--soft-daylight)
2. [Arsitektur Tipografi 3-Suara (`Type.kt`)](#2-arsitektur-tipografi-3-suara-typekt)
   - [Aset Font Bundel (`res/font/`)](#aset-font-bundel-resfont)
   - [Pemetaan 3 Suara Kognitif](#pemetaan-3-suara-kognitif)
   - [Mengapa Sentence-Case & Angka Tabular?](#mengapa-sentence-case--angka-tabular)
3. [Semantik Afektif & `StateOrb` pada Hasil Skrining (`ResultScreen.kt`)](#3-semantik-afektif--stateorb-pada-hasil-skrining-resultscreenkt)
   - [Pemetaan Nada Afektif (`MaterialTheme.empati`)](#pemetaan-nada-afektif-materialthemeempati)
   - [Layar Hasil per Modul (`ModuleResultScreen`)](#layar-hasil-per-modul-moduleresultscreen)
   - [Layar Analisis Terintegrasi (`IntegratedResultScreen`)](#layar-analisis-terintegrasi-integratedresultscreen)
4. [Ergonomi Konsultasi Chatbot: Penempatan Chatbox & Salin Rujukan (`ChatScreen.kt`)](#4-ergonomi-konsultasi-chatbot-penempatan-chatbox--salin-rujukan-chatscreenkt)
5. [Standar Copywriting Klinis Ringkas & *Zero-Leak* (`UserResultPresenter.kt`)](#5-standar-copywriting-klinis-ringkas--zero-leak-userresultpresenterkt)
6. [Referensi File](#6-referensi-file)

---

## 1. Mengapa UX Empati Klinis & "Soft Daylight"?

Pada aplikasi kesehatan yang menangani topik sensitif (risiko hipertensi dan pengalaman Kekerasan Berbasis Gender / KBG), dua kesalahan desain sering terjadi:
1. **Kesan dingin seperti laboratorium** — penggunaan huruf kapital penuh (*ALL-CAPS*), font monospace pada setiap lencana status, dan warna merah menyala yang memicu kecemasan kesehatan (*health anxiety*).
2. **Tumpukan teks panjang (*wall of text*)** — pengulangan paragraf yang memicu beban kognitif tinggi (*cognitive overload*).

Desain **"Soft Daylight" (v4.0)** menyelesaikan keduanya melalui:
- **Hierarki Afektif Terlebih Dahulu (*How It Feels First*)**: Bagian puncak layar hasil (`ResultHero`) membuka dengan **`StateOrb`** yang bernapas lembut dan judul hangat ber-font **Fraunces**, diikuti 1 kalimat kesimpulan utama.
- **Progressive Disclosure (`FindingChip` → `FindingDetailSheet`)**: Faktor risiko disajikan sebagai chip kapsul yang dapat diketuk untuk membuka penjelasan awam di dalam `ModalBottomSheet` tanpa memenuhi layar utama.
- **Penguatan Positif (`StrengthsCard` — "Yang sudah baik")**: Kebiasaan sehat responden ditampilkan dalam kartu bernada `calm` (Sage) untuk menyeimbangkan persepsi risiko.
- **Zero Redundancy**: Pada layar Analisis Terintegrasi, status kedua modul dirangkum berdampingan melalui **2 ubin `ModuleStatusTile`** (**Hipertensi** & **Kesejahteraan**).

---

## 2. Arsitektur Tipografi 3-Suara (`Type.kt`)

AI-EMPATI membundel tiga keluarga font variabel resmi (Open Font License) langsung di dalam `app/src/main/res/font/`, di mana setiap font mewakili **suara psikologis dan fungsi kognitif yang berbeda**:

### Aset Font Bundel (`res/font/`)

| Suara | Nama Font | File Resource | Variabel di `Type.kt` | Karakteristik Visual & Peran |
|---|---|---|---|---|
| **1. Suara Ekspresif & Manusiawi** | **Fraunces** | [`fraunces.ttf`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/res/font/fraunces.ttf) | `DisplayFont` | Variable soft-serif dengan sumbu `opsz=72`, `SOFT=100`, `WONK=0`. Menghadirkan kehangatan manusiawi dan ketenangan editorial pada sapaan waktu, pertanyaan kuesioner, pernyataan hasil skrining, inisial `EmpatiAvatar`, dan nama `AI-EMPATI`. |
| **2. Suara Fungsional & Bacaan** | **Inter** | [`inter.ttf`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/res/font/inter.ttf) | `BodyFont` + `EmpatiNumeralStyle` | Sans-serif ber-*x-height* tinggi dengan *line-height* ~1.6x untuk bacaan klinis, opsi kuesioner, lencana *sentence-case*, dan percakapan chat. |
| **3. Suara Telemetri & Identitas** | **JetBrains Mono** | [`jetbrains_mono.ttf`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/res/font/jetbrains_mono.ttf) | `MetricFont` (`EmpatiMetricStyle`) | Monospace `13sp/18sp` yang digunakan **khusus** untuk data pengenal mentah (ID akun di Profil, rincian anatomi NIP 18 digit, kode dokumen)—**tidak pernah** dipakai untuk label UI atau badge. |

### Pemetaan 3 Suara Kognitif

Diimplementasikan di [`Type.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Type.kt):

```
┌───────────────────────────────────────────────────────────────────────────┐
│ 1. DisplayFont — Fraunces (opsz=72, SOFT=100, WONK=0)                     │
│    • displayLarge (48/54), displayMedium (40/46), displaySmall (34/40)    │
│    • headlineLarge (30/38), headlineMedium (26/34), headlineSmall (22/30) │
│    → Digunakan pada: Sapaan Beranda, pertanyaan kuesioner, ResultHero,    │
│      judul halaman (ScreenHeader), judul bottom sheet, & EmpatiAvatar     │
├───────────────────────────────────────────────────────────────────────────┤
│ 2. BodyFont — Inter (+ EmpatiNumeralStyle tabular numerals 'tnum')        │
│    • titleLarge (20/28), titleMedium (16/24), titleSmall (14/20)          │
│    • bodyLarge (16/26), bodyMedium (14/22), bodySmall (12/18)             │
│    • labelLarge (14/20), labelMedium (12/16), labelSmall (11/16)          │
│    • EmpatiNumeralStyle (12/16, tnum)                                     │
│    → Digunakan pada: Narasi klinis, tombol, chip/badge sentence-case,     │
│      percakapan AI-EMPATI, & penghitung langkah kuesioner (3 / 16)        │
├───────────────────────────────────────────────────────────────────────────┤
│ 3. MetricFont — JetBrains Mono (EmpatiMetricStyle 13/18)                  │
│    → Digunakan HANYA pada: ID akun pengguna & anatomi NIP 18 digit        │
└───────────────────────────────────────────────────────────────────────────┘
```

### Mengapa Sentence-Case & Angka Tabular?

1. **Sentence-Case yang Menenangkan (`String.toSentenceCase()`)**:
   - Label seperti `"Rendah · Batas Wajar"`, `"Sedang · Perlu Pemantauan"`, `"Modul hipertensi"`, dan `"Faktor kardiovaskular · gaya hidup"` ditulis dalam *sentence-case* menggunakan **Inter** (`BodyFont`), bukan huruf kapital penuh ber-monospace yang terasa seperti peringatan mesin.
2. **Angka Tabular Bebas Jitter (`EmpatiNumeralStyle`)**:
   - Penghitung langkah kuesioner (`3 / 16`) menggunakan `BodyFont` dengan `fontFeatureSettings = "tnum"` sehingga lebar digit selalu konstan saat pengguna berpindah pertanyaan tanpa perlu berganti ke font monospace.

---

## 3. Semantik Afektif & `StateOrb` pada Hasil Skrining (`ResultScreen.kt`)

### Pemetaan Nada Afektif (`MaterialTheme.empati`)

Status risiko diselaraskan 100% antara `ResultScreen`, `HomeScreen`, dan `HistoryScreen`:

| Tingkat Risiko (`CfLevel` / `RiskLevel`) | Label Status Konsisten | Nada Afektif (`MaterialTheme.empati`) | Palet Warna |
|---|---|---|---|
| `RENDAH` / `LOW` (`CF < 0.25`) | **`Rendah · Batas Wajar`** | `calm` | **Sage** (`#2E6A57` / `#D6EEE3`) |
| `SEDANG` / `MEDIUM` (`0.25 ≤ CF < 0.50` atau hipertensi terkendali) | **`Sedang · Perlu Pemantauan`** | `attentive` | **Honey** (`#D29A2E` / `#FBE8BE`) |
| `TINGGI` / `HIGH` (`0.50 ≤ CF < 0.75`) | **`Tinggi · Perlu Perhatian`** | `concerned` (atau `attentive` pada modul) | **Terracotta** (`#A8402B` / `#FCE6E0`) |
| `SANGAT_TINGGI` / `VERY_HIGH` (`CF ≥ 0.75`) | **`Sangat Tinggi · Perlu Perhatian`** | `urgent` | **Berry** (`#8E2F55` / `#F9D9E4`) |
| Pendamping AI / Modul Kesejahteraan | **`Terdapat indikasi`** / **`AI-EMPATI`** | `presence` | **Mist** (`#5A5896` / `#F0EFFD`) |

### Layar Hasil per Modul (`ModuleResultScreen`)

1. **`ResultHero` + `StateOrb`**:
   - Menampilkan `StateOrb` (`148.dp`) yang menggambar cincin progres melingkar saat pertama kali muncul dan bernapas halus (atau statis bila `LocalReducedMotion` aktif), diikuti `ToneBadge`, judul `headlineLarge` (**Fraunces**), dan kesimpulan 1 kalimat (`bodyLarge` **Inter**).
2. **Kartu Faktor Interaktif (`FindingChip` → `FindingDetailSheet`)**:
   - Menampilkan `FlowRow` berisi `FindingChip` (dengan ikon `Icons.Outlined.Info`) yang saat diketuk membuka `ModalBottomSheet` (`FindingDetailSheet`) berisi penjelasan edukatif awam, pengingat keterkaitan kronis non-kausal, dan tombol `"Diskusikan dengan AI-EMPATI"`.
3. **`StrengthsCard` ("Yang sudah baik")**:
   - Kartu permukaan `calm.container` yang merayakan hingga 5 kebiasaan pelindung (*protective factors*) responden.
4. **`SoftNotice` Dukungan (Kondisional) & `NextStepsCard` ("Langkah selanjutnya")**:
   - Menampilkan hingga 3 langkah tindak lanjut konkret yang dipisahkan oleh `SoftDivider()`.

### Layar Analisis Terintegrasi (`IntegratedResultScreen`)

1. **`ResultHero`**: Menampilkan `StateOrb`, judul kategori terintegrasi (`headlineLarge` **Fraunces**), dan ringkasan 1 kalimat.
2. **2 Ubin Status Modul Berdampingan (`ModuleStatusTile`)**:
   - Menampilkan ubin **Hipertensi** dan **Kesejahteraan** secara berdampingan dengan `SoftIconTile`, titik warna afektif (`tone.accent`), dan status singkat.
3. **`SoftNotice` ("Bagaimana keduanya berkaitan")**:
   - Menjelaskan jalur stres kronis (*chronic stress pathway*) secara empatik dan non-kausal dengan nada `presence` (Mist).
4. **Kartu Faktor Kunci (`FindingChip`), `StrengthsCard`, `NextStepsCard`, serta CTA `Simpan ke riwayat` & `Tanyakan ke AI-EMPATI`**.

---

## 4. Ergonomi Konsultasi Chatbot: Penempatan Chatbox & Salin Rujukan (`ChatScreen.kt`)

Dalam pengalaman konsultasi kesehatan dan ruang aman, interaksi percakapan membutuhkan kenyamanan motorik dan kemudahan penyimpanan informasi tanpa membebani responden:

1. **Penempatan Chatbox Tepat di Atas Keyboard (`Modifier.imePadding()` & Adaptasi Navbar)**:
   - Responden yang sedang mengetik keluhan kesehatan atau pertanyaan sensitif membutuhkan visibilitas penuh terhadap apa yang mereka tulis tanpa terhalang keyboard virtual atau bilah navigasi.
   - Menggunakan `Modifier.imePadding()` pada kanvas utama `ChatContent` dan menyembunyikan `BottomNavBar` saat keyboard aktif (`WindowInsets.isImeVisible == true`) menjamin komposer obrolan (*chatbox*) dan teks placeholder selalu berada tepat dan nyaman di atas keyboard.
   - `LaunchedEffect(isImeVisible)` secara otomatis menggulir daftar pesan ke item terakhir sehingga konteks obrolan terdahulu tidak hilang saat keyboard terangkat.
2. **Kemudahan Menyalin Informasi Klinis & Kontak Darurat (`SelectionContainer` & `ContentCopy`)**:
   - Informasi penting seperti tips manajemen stres, panduan diet DASH, maupun nomor darurat hotline **SAPA 129** (WhatsApp 08111-129-129) sering kali perlu disimpan atau dibagikan ke anggota keluarga.
   - Teks pada seluruh gelembung pesan dibungkus dalam `SelectionContainer` untuk memungkinkan seleksi teks sebagian secara leluasa.
   - Disediakan tombol salin satu sentuhan berikon `Icons.Rounded.ContentCopy` pada setiap pesan dengan umpan balik toast menenangkan (*"Pesan disalin ke clipboard"*).
   - Disediakan pula tombol salin draf pada bilah chatbox saat pengguna sedang menyusun pertanyaan panjang sehingga mereka tidak khawatir kehilangan draf pesan yang sedang ditulis.

---

## 5. Standar Copywriting Klinis Ringkas & *Zero-Leak* (`UserResultPresenter.kt`)

Seluruh teks pada [`UserResultPresenter.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/data/presentation/UserResultPresenter.kt) diverifikasi otomatis oleh `UserPresentationTest`:

- **Label Faktor Hipertensi Ringkas**:
  - `Riwayat keluarga`
  - `Kondisi medis & tensi`
  - `Pola makan & aktivitas`
  - `Stres & kualitas tidur`
- **Penjelasan Tag Klinis Universal (`explainClinicalTag`)**:
  - Memberikan penjelasan 1–2 kalimat berbahasa awam untuk setiap tag hipertensi maupun KBG tanpa jargon medis rumit.
- **Jaminan *Zero-Leak***:
  - Tidak ada nilai `CF`, `MB`, `MD`, persentase (`%`), bobot, atau angka desimal (`0.xx`) yang bocor ke permukaan UI.

---

## 6. Referensi File

| File | Peran |
|---|---|
| [`fraunces.ttf`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/res/font/fraunces.ttf) | Font variabel soft-serif bundel untuk `DisplayFont` (judul, sapaan, pernyataan hasil) |
| [`inter.ttf`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/res/font/inter.ttf) | Font variabel sans-serif bundel untuk `BodyFont` & `EmpatiNumeralStyle` (`tnum`) |
| [`jetbrains_mono.ttf`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/res/font/jetbrains_mono.ttf) | Font variabel monospace bundel untuk `MetricFont` (`EmpatiMetricStyle` pada ID akun/NIP) |
| [`Type.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Type.kt) | Konfigurasi `DisplayFont`, `BodyFont`, `MetricFont`, `EmpatiNumeralStyle`, `EmpatiMetricStyle`, dan `EmpatiTypography` |
| [`Color.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Color.kt) & [`Theme.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Theme.kt) | Palet "Soft Daylight" v4 & sistem nada afektif `MaterialTheme.empati` |
| [`Components.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/components/Components.kt) | `StateOrb`, `ToneBadge`, `RiskBadge`, `SoftNotice`, `toSentenceCase()` |
| [`ResultScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ResultScreen.kt) | `ModuleResultScreen`, `IntegratedResultScreen`, `ResultHero`, `FindingChip`, `StrengthsCard`, & `FindingDetailSheet` |
| [`UserResultPresenter.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/data/presentation/UserResultPresenter.kt) | Copywriting klinis ringkas bebas istilah teknis CF & penyelarasan status risiko |
