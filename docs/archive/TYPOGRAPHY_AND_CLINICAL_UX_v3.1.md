# 🩺 AI-EMPATI — Tipografi Fungsional & Minimalisme Klinis (v3.1)

Dokumen ini menjelaskan arsitektur **Tipografi Fungsional 3-Peran (*3-Role Functional Typography*)** dan prinsip **Minimalisme Klinis (*Clinical Minimalism*)** yang diterapkan pada layar hasil skrining, penyelarasan status risiko lintas layar (`Rendah · Batas Wajar`, `Sedang · Perlu Pemantauan`, `Tinggi · Perlu Perhatian`, `Sangat Tinggi · Perlu Perhatian`), serta header bersih pada layar konsultasi **`AI-EMPATI`**.

---

## 📋 Daftar Isi

1. [Mengapa Minimalisme Klinis?](#1-mengapa-minimalisme-klinis)
2. [Arsitektur Tipografi Fungsional 3-Peran](#2-arsitektur-tipografi-fungsional-3-peran)
   - [Aset Font Bundel (`res/font/`)](#aset-font-bundel-resfont)
   - [Pemetaan Peran Fungsional](#pemetaan-peran-fungsional)
   - [Mengapa Kombinasi Ini Bebas *AI Slop*?](#mengapa-kombinasi-ini-bebas-ai-slop)
3. [Reduksi Informasi pada Layar Hasil (`ResultScreen.kt`)](#3-reduksi-informasi-pada-layar-hasil-resultscreenkt)
   - [Layar Hasil per Modul (`ModuleResultScreen`)](#layar-hasil-per-modul-moduleresultscreen)
   - [Layar Analisis Terintegrasi (`IntegratedResultScreen`)](#layar-analisis-terintegrasi-integratedresultscreen)
4. [Standar Copywriting Klinis Ringkas (`UserResultPresenter.kt`)](#4-standar-copywriting-klinis-ringkas-userresultpresenterkt)
5. [Referensi File](#5-referensi-file)

---

## 1. Mengapa Minimalisme Klinis?

Pada aplikasi kesehatan yang menangani topik sensitif (risiko hipertensi dan pengalaman Kekerasan Berbasis Gender / KBG), tumpukan paragraf panjang (*wall of text*) memicu **beban kognitif tinggi (*cognitive overload*)** dan kelelahan membaca. Selain itu, pengulangan kalimat yang sama di beberapa kartu memberikan kesan seperti teks hasil *prompt* generatif (*AI slop*).

Prinsip **Minimalisme Klinis** menyusun ulang hasil skrining layaknya ringkasan rekam medis modern (*Executive Clinical Summary*):

- **Scannable dalam 3 detik**: Status utama, kesimpulan 1 kalimat, dan tag faktor risiko terlihat dalam satu pandangan tanpa harus menggulir panjang.
- **Zero Redundancy**: Pada layar Analisis Terintegrasi, kesimpulan masing-masing modul tidak lagi ditulis ulang sebagai paragraf panjang, melainkan diringkas menjadi **Matriks Status 2-Kolom** (`HIPERTENSI` | `KESEJAHTERAAN`).
- **Chips menggantikan Bullet Paragraphs**: Faktor risiko dan kategori temuan disajikan sebagai `ClinicalTagChip` di dalam `FlowRow` yang ringkas dan presisi.
- **1-Line Footnote Disclaimer**: Catatan edukatif diringkas menjadi satu baris kaki (*footnote*) yang tenang di bagian bawah layar.

---

## 2. Arsitektur Tipografi Fungsional 3-Peran

AI-EMPATI meninggalkan font sistem generik (`FontFamily.Serif` / `FontFamily.Default`) dan beralih ke **tiga keluarga font variabel resmi (Open Font License)** yang dibundel langsung di dalam APK. Setiap font dipilih khusus untuk **fungsi kognitif yang berbeda**:

### Aset Font Bundel (`res/font/`)

| Peran | Nama Font | File Resource | Karakteristik Visual |
|---|---|---|---|
| **1. Heading & Otoritas Klinis** | **Plus Jakarta Sans** | [`plus_jakarta_sans.ttf`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/res/font/plus_jakarta_sans.ttf) | Geometris-humanis kontemporer dengan bukaan (*aperture*) tegas; sangat relevan dengan ekosistem produk digital Indonesia modern dan memberikan wibawa medis yang hangat. |
| **2. Body & Bacaan Klinis** | **Inter** | [`inter.ttf`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/res/font/inter.ttf) | *X-height* tinggi dan proporsi netral yang dirancang khusus untuk layar berkerapatan tinggi; menjaga kejelasan bacaan pada kuesioner, kesimpulan, dan konsultasi chat. |
| **3. Metadata & Label Diagnostik** | **JetBrains Mono** | [`jetbrains_mono.ttf`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/res/font/jetbrains_mono.ttf) | Monospace teknis berpresisi tinggi untuk lencana status (`PERLU PERHATIAN`), penghitung langkah (`3 / 16`), kode modul (`MODUL HIPERTENSI`), tanggal skrining, sitasi dokumen RAG (`DOC-HT-02`), dan ID Akun pengguna. |

### Pemetaan Peran Fungsional

Diimplementasikan di [`Type.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Type.kt):

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 1. HeadingFont (Plus Jakarta Sans)                                      │
│    • displaySmall, headlineLarge, headlineMedium, headlineSmall         │
│    • titleLarge, titleMedium, titleSmall, labelLarge                    │
│    → Digunakan pada: Judul layar, kesimpulan utama, pertanyaan, CTA     │
├─────────────────────────────────────────────────────────────────────────┤
│ 2. BodyFont (Inter)                                                     │
│    • bodyLarge, bodyMedium, bodySmall                                   │
│    → Digunakan pada: Kalimat ringkasan klinis, poin tindakan, chat      │
├─────────────────────────────────────────────────────────────────────────┤
│ 3. MonoFont (JetBrains Mono)                                            │
│    • labelMedium, labelSmall (+ eksplisit pada StatusPill & NIP)        │
│    → Digunakan pada: SectionChip, RiskBadge, StatusPill, counter, NIP   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Mengapa Kombinasi Ini Bebas *AI Slop*?

1. **Kontras Berbasis Fungsi, Bukan Hiasan**: Perbedaan antara **Plus Jakarta Sans** (judul), **Inter** (narasi), dan **JetBrains Mono** (label/metadata) membantu mata pengguna membedakan *apa yang harus dibaca sebagai narasi* vs *apa yang merupakan indikator/status sistem*.
2. **Estetika Instrumen Medis Kontemporer**: Penggunaan monospace (`JetBrains Mono`) secara terukur pada kapsul status (`DALAM BATAS WAJAR`, `PERLU PEMANTAUAN`) dan nomor indeks kuesioner menghadirkan presisi ala perangkat diagnostik klinis modern (seperti One Medical, Oura, atau Apple Health).
3. **Konsistensi Lintas Perangkat**: Karena ketiga file `.ttf` dibundel di `app/src/main/res/font/`, tampilan tipografi identik 100% di seluruh perangkat Android tanpa bergantung pada font OEM bawaan pabrik.

---

## 3. Reduksi Informasi pada Layar Hasil (`ResultScreen.kt`)

### Layar Hasil per Modul (`ModuleResultScreen`)

| Komponen Sebelumnya (Verbose) | Komponen Baru (Minimalis Klinis) | Manfaat UX |
|---|---|---|
| 4–5 kartu terpisah dengan paragraf panjang | **1 Kartu Ringkasan Klinis Terpadu** + **1 Kartu Langkah Tindak Lanjut** | Mengurangi tinggi layar hingga ~45% dan menghilangkan pengulangan |
| Daftar berpoin kalimat panjang untuk faktor risiko | **`FlowRow` berisi `ClinicalTagChip`** (`Riwayat keluarga`, `Stres & kualitas tidur`) | Dapat dipindai (*scannable*) seketika seperti tag rekam medis |
| Kartu dukungan muncul dengan teks panjang | **`SupportStrip` kontekstual** — hanya tampil bila ada catatan dukungan (`supportNote != null`) | Menjaga layar tetap bersih saat risiko rendah, namun tetap responsif saat dibutuhkan |
| Kotak `DisclaimerBox` 3 kalimat | **`MinimalDisclaimer` 1 baris** di bagian bawah | Tidak mendominasi hierarki visual utama |

### Layar Analisis Terintegrasi (`IntegratedResultScreen`)

Sebelumnya menampilkan **6 kartu bertumpuk** yang mengulang kesimpulan Modul 1, kesimpulan Modul 2, paragraf hubungan, faktor, saran, dan disclaimer. Kini disederhanakan menjadi **3 blok eksekutif**:

1. **Kartu Sintesis Utama + Matriks Status 2-Kolom (`ModuleStatusCell`)**:
   - Menampilkan kategori terintegrasi dan 1 kalimat ringkasan.
   - Di bawahnya terdapat matriks 2 kolom berdampingan: **`HIPERTENSI`** dan **`KESEJAHTERAAN`** lengkap dengan indikator titik warna semantik dan status singkat (`Perlu perhatian` / `Tidak ada indikasi`).
   - Jika terdapat faktor kontribusi, ditampilkan langsung di bagian bawah kartu menggunakan `FlowRow` `ClinicalTagChip` (maksimal 4 tag utama).
2. **Kartu Langkah yang Disarankan (`ActionPlanCard`)**:
   - Menampilkan maksimal 3 langkah tindakan konkret dalam kalimat singkat 1 baris.
3. **Catatan Bantuan (`SupportStrip` — kondisional) & `MinimalDisclaimer`**.

---

## 4. Standar Copywriting Klinis Ringkas (`UserResultPresenter.kt`)

Seluruh teks pada [`UserResultPresenter.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/data/presentation/UserResultPresenter.kt) telah dipadatkan dengan tetap mematuhi aturan pengujian unit (`UserPresentationTest`):

- **Label Faktor Hipertensi Ringkas**:
  - `Riwayat keluarga`
  - `Kondisi medis & tensi`
  - `Pola makan & aktivitas`
  - `Stres & kualitas tidur`
- **Kesimpulan 1 Kalimat Padat**:
  - Contoh: *"Ditemukan beberapa faktor risiko hipertensi yang perlu mulai dikendalikan."* (menggantikan paragraf 3 kalimat).
- **Saran Tindak Lanjut Maksimal 3 Poin**:
  - Setiap poin ditulis sebagai kalimat aktif yang langsung dapat ditindaklanjuti (*actionable*), seperti *"Periksa tekanan darah secara berkala di fasilitas kesehatan."*
- **Keamanan Data Penelitian Tetap Terjamin**:
  - Tidak ada nilai `CF`, `MB`, `MD`, persentase (`%`), bobot, atau angka desimal (`0.xx`) yang bocor ke permukaan UI.

---

## 5. Referensi File

| File | Peran |
|---|---|
| [`plus_jakarta_sans.ttf`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/res/font/plus_jakarta_sans.ttf) | Font variabel bundel untuk Heading & Title |
| [`inter.ttf`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/res/font/inter.ttf) | Font variabel bundel untuk Body & narasi klinis |
| [`jetbrains_mono.ttf`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/res/font/jetbrains_mono.ttf) | Font variabel bundel untuk Metadata, Badge, & kode klinis |
| [`Type.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Type.kt) | Konfigurasi `HeadingFont`, `BodyFont`, `MonoFont`, dan `EmpatiTypography` |
| [`ResultScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ResultScreen.kt) | Implementasi minimalis `ModuleResultScreen` & `IntegratedResultScreen` |
| [`UserResultPresenter.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/data/presentation/UserResultPresenter.kt) | Copywriting klinis ringkas bebas istilah teknis CF |
| [`Components.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/components/Components.kt) | `RiskBadge` & `SectionChip` dengan tipografi `MonoFont` |

