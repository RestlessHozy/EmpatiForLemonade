# 🎨 SRYA Emora (sebelumnya AI-EMPATI) — Panduan Desain Front-End & Sistem UI/UX "Soft Daylight" (v4.1.5)

Dokumen ini mendokumentasikan sistem desain **SRYA Emora "Soft Daylight" (v4.1.5)**, arsitektur warna 3-lapis (*Palettes → Material 3 Roles → Affective Semantics*), sistem tipografi 3-suara (**Fraunces + Inter + JetBrains Mono**), aset logo multi-resolusi (`srya_emora_logo_transparent` & `srya_emora_logo_bg`), token desain (`Tokens.kt`), pustaka komponen (`Components.kt`), sistem gerak beraksesibilitas (`LocalReducedMotion`), penanganan ergonomi keyboard & insets (`WindowInsets.ime` & `imePadding`), kapabilitas salin pesan (`SelectionContainer` & `ContentCopy`), serta spesifikasi 11 layar aplikasi **SRYA Emora**.

> Untuk penjelasan mendalam mengenai filosofi tipografi 3-suara, pemetaan nada afektif (*affective tones*), dan desain hasil skrining berorientasi empati klinis, lihat [`TYPOGRAPHY_AND_CLINICAL_UX.md`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/TYPOGRAPHY_AND_CLINICAL_UX.md) dan [`project_analysis.md`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/project_analysis.md).

---

## 📋 Daftar Isi

- [Filosofi & Prinsip Desain "Soft Daylight" (v4.0)](#filosofi--prinsip-desain-soft-daylight-v40)
- [Arsitektur Warna 3-Lapis (`Color.kt` & `Theme.kt`)](#arsitektur-warna-3-lapis-colorkt--themekt)
  - [Lapis 1 — Palet Tonal Mentah (Raw Tonal Ramps)](#lapis-1--palet-tonal-mentah-raw-tonal-ramps)
  - [Lapis 2 — Peran Material 3 (`LightColorScheme` & `DarkColorScheme`)](#lapis-2--peran-material-3-lightcolorscheme--darkcolorscheme)
  - [Lapis 3 — Semantik Afektif (`EmpatiColors` via `MaterialTheme.empati`)](#lapis-3--semantik-afektif-empaticolors-via-materialthemeempati)
- [Sistem Tipografi 3-Suara (`Type.kt` & `res/font/`)](#sistem-tipografi-3-suara-typekt--resfont)
- [Token Desain: Spasi, Elevasi, Bentuk & Gerak (`Tokens.kt`)](#token-desain-spasi-elevasi-bentuk--gerak-tokenskt)
- [Pustaka Komponen Reusable (`Components.kt`)](#pustaka-komponen-reusable-componentskt)
- [Spesifikasi 11 Layar Aplikasi (`ui/screens/`)](#spesifikasi-11-layar-aplikasi-uiscreens)
- [Navigasi & Transisi (`EmpatiApp.kt` & `MainScaffold.kt`)](#navigasi--transisi-empatiappkt--mainscaffoldkt)
- [Aksesibilitas (WCAG 2.2) & Keamanan Visual](#aksesibilitas-wcag-22--keamanan-visual)
- [Referensi File](#referensi-file)

---

## Filosofi & Prinsip Desain "Soft Daylight" (v4.0)

1. **Suasana "Soft Daylight" & "Quiet Evening"** — Meninggalkan palet ungu/mauve lama dan menggantinya dengan kanvas kertas hangat bernuansa batu-hijau alami (**Stone** `#F7F5F0` di mode terang, `#121512` di mode gelap) yang dipadukan dengan **Sage** (ketenangan medis), **Clay** (kehangatan manusiawi), dan **Mist** (kehadiran pendamping AI).
2. **Semantik Afektif (*Affective Tones*), Bukan Alarm Klinis** — Status kesehatan diekspresikan melalui nada emosional yang menenangkan namun jujur (`calm`, `attentive`, `concerned`, `urgent`, `neutral`, `presence`) lengkap dengan visualisasi `StateOrb` yang bernapas halus, tanpa angka mentah yang memicu kecemasan (*health anxiety*).
3. **Suara Percakapan yang Manusiawi (*Conversational Sentence-Case*)** — Seluruh label, badge, dan chip menggunakan *sentence-case* (`String.toSentenceCase()`), bukan huruf kapital penuh (*ALL-CAPS*) yang terkesan menghakimi.
4. **Arsitektur Stateless Content + Stateful Wrapper** — Setiap layar memisahkan *stateful entry point* (yang terhubung ke `EmpatiViewModel`) dari *stateless composable* (`*Content`) sehingga mendukung *Compose Previews* penuh dan pengujian UI terisolasi.
5. **Pemisahan Ketat Data Penelitian vs Tampilan Pengguna (*Zero-Leak*)** — Nilai teknis internal (*Certainty Factor*, `MB`, `MD`, bobot, persen `%`, dan angka desimal) tidak pernah dirender di permukaan UI pengguna.

---

## Arsitektur Warna 3-Lapis (`Color.kt` & `Theme.kt`)

Sistem warna di [`Color.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Color.kt) dan [`Theme.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Theme.kt) disusun dalam tiga lapisan: **Palettes** (nilai mentah), **Roles** (skema Material 3), dan **Affective Semantics** (`MaterialTheme.empati`).

### Lapis 1 — Palet Tonal Mentah (*Raw Tonal Ramps*)

| Keluarga Warna | Peran Psikologis | Token & Hex Utama |
|---|---|---|
| **Sage** (Primary) | Ketenangan, kepercayaan, identitas utama | `Sage10` `#05241B` · `Sage20` `#0A3A2C` · `Sage30` `#1F4F40` · `Sage40` `#2E6A57` · `Sage80` `#9CCFB8` · `Sage90` `#BAEDD6` · `Sage95` `#D6EEE3` · `Sage98` `#EAF6F0` |
| **Clay** (Secondary) | Kehangatan manusiawi, dukungan empatik | `Clay10` `#3B1808` · `Clay30` `#6E3A25` · `Clay40` `#93482E` · `Clay80` `#F2B79C` · `Clay90` `#FFDBCB` · `Clay95` `#FBE4D8` |
| **Mist** (Tertiary / AI) | Kehadiran AI-EMPATI, ruang refleksi & informasi | `Mist10` `#1B1948` · `Mist30` `#42407F` · `Mist40` `#5A5896` · `Mist80` `#C6C4F4` · `Mist90` `#E3E1FA` · `Mist95` `#F0EFFD` |
| **Honey** (Attentive) | Perhatian lembut (*worth monitoring*) | `Honey10` `#3D2800` · `Honey25` `#5C3C00` · `Honey40` `#85570A` · `Honey60` `#D29A2E` · `Honey80` `#F3C56A` · `Honey90` `#FBE8BE` |
| **Terracotta** (Concerned) | Perlu perhatian klinis / validasi input | `Terra10` `#400A02` · `Terra25` `#6E2415` · `Terra40` `#A8402B` · `Terra80` `#FFB4A3` · `Terra90` `#FFDAD1` · `Terra95` `#FCE6E0` |
| **Berry** (Urgent) | Perhatian segera / sangat tinggi | `Berry10` `#3F0A22` · `Berry25` `#68183B` · `Berry40` `#8E2F55` · `Berry80` `#F2A9C2` · `Berry90` `#F9D9E4` |

#### Permukaan Netral **Stone** (Warm-Grey Berbias Hijau Halus)

| Mode | Token | Hex | Peran Material 3 (`ColorScheme`) |
|---|---|---|---|
| **Light** (*Daylight on Paper*) | `StoneLightBackground` | `#F7F5F0` | `background` / `surface` |
| | `StoneLightLowest` | `#FFFFFF` | `surfaceContainerLowest` (`EmpatiSurfaces.card()` di Light) |
| | `StoneLightLow` | `#F2F0EA` | `surfaceContainerLow` (`EmpatiSurfaces.inset()` di Light) |
| | `StoneLightMid` | `#EDEAE3` | `surfaceContainer` (`EmpatiSurfaces.sunken()`) |
| | `StoneLightHigh` | `#E7E4DC` | `surfaceContainerHigh` |
| | `StoneLightHighest` | `#E1DED5` | `surfaceContainerHighest` / `surfaceVariant` |
| | `StoneLightOnSurface` | `#1B1C19` | `onBackground` / `onSurface` (Kontras 14.3:1 WCAG AAA) |
| | `StoneLightOnVariant` | `#474B44` | `onSurfaceVariant` (Kontras 7.6:1 WCAG AAA) |
| | `StoneLightOutline` | `#747970` | `outline` |
| | `StoneLightOutlineVariant` | `#D0D3C9` | `outlineVariant` (`EmpatiSurfaces.hairline()`) |
| **Dark** (*Quiet Evening*) | `StoneDarkBackground` | `#121512` | `background` / `surface` |
| | `StoneDarkLowest` | `#0C0F0C` | `surfaceContainerLowest` |
| | `StoneDarkLow` | `#181B18` | `surfaceContainerLow` (`EmpatiSurfaces.card()` di Dark) |
| | `StoneDarkMid` | `#1C1F1C` | `surfaceContainer` (`EmpatiSurfaces.sunken()`) |
| | `StoneDarkHigh` | `#262A26` | `surfaceContainerHigh` (`EmpatiSurfaces.inset()` di Dark) |
| | `StoneDarkHighest` | `#313531` | `surfaceContainerHighest` / `surfaceVariant` |
| | `StoneDarkOnSurface` | `#E3E3DB` | `onBackground` / `onSurface` |
| | `StoneDarkOnVariant` | `#BEC3B9` | `onSurfaceVariant` |
| | `StoneDarkOutline` | `#889084` | `outline` |
| | `StoneDarkOutlineVariant` | `#40443F` | `outlineVariant` |

### Lapis 2 — Peran Material 3 (`LightColorScheme` & `DarkColorScheme`)

Dikonfigurasi di [`Theme.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Theme.kt):
- `primary` = `Sage40` (`#2E6A57` Light) / `Sage80` (`#9CCFB8` Dark)
- `secondary` = `Clay40` (`#93482E` Light) / `Clay80` (`#F2B79C` Dark)
- `tertiary` = `Mist40` (`#5A5896` Light) / `Mist80` (`#C6C4F4` Dark)
- `error` = `Terra40` (`#A8402B` Light) / `Terra80` (`#FFB4A3` Dark)

### Lapis 3 — Semantik Afektif (`EmpatiColors` via `MaterialTheme.empati`)

Setiap nada afektif dibungkus dalam `ToneColors(accent, container, onContainer, border)` dan diakses melalui `MaterialTheme.empati`:

| Properti `MaterialTheme.empati` | Makna Klinis & Afektif | Palet Dasar | Pemetaan `RiskLevel` / `PresentationTone` |
|---|---|---|---|
| **`calm`** | Aman, positif, faktor pelindung (*protective*) | **Sage** (`Sage40` / `Sage95`) | `RiskLevel.LOW`, `PresentationTone.POSITIVE` |
| **`attentive`** | Perlu pemantauan berkala (*worth monitoring*) | **Honey** (`Honey60` / `Honey90`) | `RiskLevel.MEDIUM`, `PresentationTone.ATTENTION` |
| **`concerned`** | Perlu perhatian klinis (*needs attention*) | **Terracotta** (`Terra40` / `Terra95`) | `RiskLevel.HIGH` |
| **`urgent`** | Sangat tinggi, disarankan segera berkonsultasi | **Berry** (`Berry40` / `Berry90`) | `RiskLevel.VERY_HIGH` |
| **`neutral`** | Belum lengkap / catatan informatif netral | **Stone** (`StoneLightLow` / `StoneLightOutline`) | `PresentationTone.NEUTRAL`, `PresentationTone.INCOMPLETE` |
| **`presence`** | Identitas pendamping AI-EMPATI, modul kesejahteraan, & rujukan dukungan | **Mist** (`Mist40` / `Mist95`) | Avatar AI, `LockNotice`, `SoftNotice` SAPA 129 |

---

## Sistem Tipografi 3-Suara (`Type.kt` & `res/font/`)

Diimplementasikan di [`Type.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Type.kt) menggunakan tiga font variabel berlisensi terbuka yang dibundel di `app/src/main/res/font/`:

| Suara Tipografi | Font Bundel | Variabel di `Type.kt` | Karakteristik & Kegunaan |
|---|---|---|---|
| **1. Suara Ekspresif & Manusiawi** | **Fraunces** (`res/font/fraunces.ttf`) | `DisplayFont` | Variable soft-serif dengan sumbu `opsz=72`, `SOFT=100`, `WONK=0`. Digunakan untuk momen manusiawi: sapaan waktu, judul halaman, pernyataan hasil utama, inisial `EmpatiAvatar`, dan nama `AI-EMPATI`. |
| **2. Suara Fungsional & Bacaan** | **Inter** (`res/font/inter.ttf`) | `BodyFont` + `EmpatiNumeralStyle` | Sans-serif humanis ber-*x-height* tinggi untuk kuesioner, ringkasan klinis, langkah tindak lanjut, gelembung chat, serta `EmpatiNumeralStyle` (`fontFeatureSettings = "tnum"`) untuk penghitung langkah (`3 / 16`) agar angka tidak bergeser. |
| **3. Suara Telemetri & Identitas** | **JetBrains Mono** (`res/font/jetbrains_mono.ttf`) | `MetricFont` (`EmpatiMetricStyle`) | Monospace `13sp/18sp` yang digunakan **hanya** untuk pengenal mentah (ID akun, anatomi NIP 18 digit, kode dokumen)—**tidak pernah** dipakai untuk label UI atau badge. |

### Skala Tipografi (`EmpatiTypography`)

| Style Material 3 | Font | Weight | Size / Line Height | Letter Spacing | Kegunaan Utama |
|---|---|---|---|---|---|
| `displayLarge` | Fraunces (`DisplayFont`) | Normal | `48sp / 54sp` | `-0.8sp` | Hero display |
| `displayMedium` | Fraunces (`DisplayFont`) | Normal | `40sp / 46sp` | `-0.6sp` | Display medium |
| `displaySmall` | Fraunces (`DisplayFont`) | Medium | `34sp / 40sp` | `-0.4sp` | Judul `AI-EMPATI` di Splash & Auth |
| `headlineLarge` | Fraunces (`DisplayFont`) | Medium | `30sp / 38sp` | `-0.3sp` | Sapaan utama Beranda, `ScreenHeader`, `ResultHero` |
| `headlineMedium` | Fraunces (`DisplayFont`) | Medium | `26sp / 34sp` | `-0.2sp` | Pertanyaan kuesioner (≤90 karakter), judul slide Onboarding, judul bottom sheet |
| `headlineSmall` | Fraunces (`DisplayFont`) | Medium | `22sp / 30sp` | `-0.1sp` | Pertanyaan kuesioner panjang (>90 karakter), kartu status utama |
| `titleLarge` | Inter (`BodyFont`) | SemiBold | `20sp / 28sp` | `-0.15sp` | Judul dialog & sub-bagian utama |
| `titleMedium` | Inter (`BodyFont`) | SemiBold | `16sp / 24sp` | `0sp` | Judul kartu, nama di Profil, header `AI-EMPATI` di Chat |
| `titleSmall` | Inter (`BodyFont`) | SemiBold | `14sp / 20sp` | `0.05sp` | Judul `ModuleStatusTile`, item persetujuan |
| `bodyLarge` | Inter (`BodyFont`) | Normal | `16sp / 26sp` | `0sp` | Narasi utama (~1.6x line-height), opsi kuesioner, input form |
| `bodyMedium` | Inter (`BodyFont`) | Normal | `14sp / 22sp` | `0.05sp` | Poin rekomendasi, gelembung chat, deskripsi kartu |
| `bodySmall` | Inter (`BodyFont`) | Normal | `12sp / 18sp` | `0.1sp` | Teks pendukung, disclaimer, keterangan field |
| `labelLarge` | Inter (`BodyFont`) | SemiBold | `14sp / 20sp` | `0.05sp` | Tombol utama (`PrimaryButton`), `FindingChip`, eyebrow header |
| `labelMedium` | Inter (`BodyFont`) | Medium | `12sp / 16sp` | `0.15sp` | `ToneBadge`, `RiskBadge`, `SectionChip`, `SectionLabel`, label tab bawah |
| `labelSmall` | Inter (`BodyFont`) | Medium | `11sp / 16sp` | `0.2sp` | Metadata halus |
| `EmpatiNumeralStyle` | Inter (`BodyFont` + `tnum`) | SemiBold | `12sp / 16sp` | `0.2sp` | Indeks langkah kuesioner (`3 / 16`) dengan angka tabular |
| `EmpatiMetricStyle` | JetBrains Mono (`MetricFont`) | Medium | `13sp / 18sp` | `0sp` | Nilai ID akun di Profil & inspektor anatomi NIP |

---

## Token Desain: Spasi, Elevasi, Bentuk & Gerak (`Tokens.kt`)

Didefinisikan secara terpusat di [`Tokens.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Tokens.kt):

### 1. Spasi & Tata Letak (`EmpatiSpace` — Grid 4dp)
- **Skala Spasi**: `xxs = 4.dp`, `xs = 8.dp`, `sm = 12.dp`, `md = 16.dp`, `lg = 20.dp`, `xl = 24.dp`, `xxl = 32.dp`, `huge = 40.dp`, `giant = 56.dp`.
- **Dimensi Semantik**:
  - `gutter = 24.dp` (margin horizontal layar)
  - `readableWidth = 560.dp` (lebar maksimum kolom bacaan agar tetap nyaman di tablet & mode lanskap)
  - `touchTarget = 48.dp` (target sentuh minimum sesuai standar aksesibilitas Android & WCAG)
  - `cardPadding = 24.dp`, `cardPaddingCompact = 18.dp`

### 2. Ukuran Ikon (`EmpatiIcon`) & Elevasi (`EmpatiElevation`)
- `EmpatiIcon`: `sm = 16.dp`, `md = 20.dp`, `lg = 24.dp`, `xl = 32.dp`.
- `EmpatiElevation`: `resting = 0.dp`, `raised = 3.dp`, `floating = 10.dp`.
- **`Modifier.softShadow`**: Bayangan lembut berwarna `MaterialTheme.empati.shadowTint` (`Sage10` transparan di mode terang, otomatis nonaktif di mode gelap karena kedalaman diekspresikan melalui kecerahan permukaan).

### 3. Bentuk Kerikil Halus (`EmpatiShapes` di `Theme.kt`)
- `extraSmall = 10.dp`, `small = 14.dp`, `medium = 20.dp`, `large = 28.dp`, `extraLarge = 36.dp`, serta `CircleShape` (kapsul penuh) untuk tombol utama, chip, dan lencana.

### 4. Sistem Gerak Beraksesibilitas (`EmpatiMotion` & `LocalReducedMotion`)
- **Durasi**: `Instant = 90ms`, `Quick = 160ms`, `Standard = 280ms`, `Expressive = 520ms`, `Reveal = 720ms`.
- **Kurva Easing**: `EaseOutSoft = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)` (mendarat lembut tanpa pantulan tajam) dan `EaseInOutSoft = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)`.
- **Pegas Fisika**: `EmpatiMotion.press()` (`DampingRatioNoBouncy`, `StiffnessMedium`) dan `EmpatiMotion.settle()` (`StiffnessLow`).
- **Dukungan `LocalReducedMotion`**: Membaca pengaturan sistem `Settings.Global.TRANSITION_ANIMATION_SCALE` melalui `rememberSystemReducedMotion()`. Jika pengguna mengaktifkan *Remove animations*, seluruh komponen (`Reveal`, `StateOrb`, `EmpatiLogo`, `BreathingDots`) langsung tampil pada state akhir tanpa gerakan berulang.

---

## Pustaka Komponen Reusable (`Components.kt`)

Berada di [`Components.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/components/Components.kt):

- **Permukaan & Nada**:
  - `EmpatiSurfaces.card()`, `EmpatiSurfaces.inset()`, `EmpatiSurfaces.sunken()`, `EmpatiSurfaces.hairline()`.
  - `RiskLevel.tone()`, `PresentationTone.toneColors()`, `errorTone()`.
- **Scaffolding Layar**:
  - `ScreenFrame(insets, scrollable, horizontalPadding)`: Kanvas halaman standar dengan `widthIn(max = 560.dp)` dan padding `24.dp`.
  - `ScreenTopBar(onBack, trailing)` & `ScreenHeader(title, eyebrow, subtitle)`.
- **Kartu, Lencana & Chip**:
  - `TonalCard`: Kontainer kartu ber-radius `28.dp` (`MaterialTheme.shapes.large`) dengan `softShadow`, border `hairline()`, dan dukungan `pressScale` + `focusHalo` bila dapat diklik.
  - `ToneBadge` & `RiskBadge`: Kapsul status afektif dengan titik warna semantik (`tone.accent`) dan tipografi `labelMedium` *sentence-case*.
  - `SectionChip`, `SectionLabel`, `SoftIconTile`, `ChoiceChip` (chip seleksi radio/filter dengan animasi ikon centang).
  - `SegmentedSwitch`: Kontrol segmen 2-opsi dengan *gliding pill thumb* beranimasi (`animateDpAsState`).
- **Tombol & Kontrol Input**:
  - `PrimaryButton`: Tombol kapsul utama (`56.dp`) dengan `softShadow`, dukungan state `loading` (`CircularProgressIndicator`), dan ikon panah opsional.
  - `GhostButton`, `BackIconButton` (`48.dp` lingkaran), `VisibilityToggle` (tampil/sembunyikan PIN).
  - `EmpatiTextField`: Kolom input permukaan `inset()` dengan dukungan autofill (`ContentType`), halo fokus `2.dp`, garis indikator bawah yang tumbuh dari tengah (`animateFloatAsState`), serta pesan error ber-`LiveRegionMode.Polite`.
- **Pemberitahuan & Identitas Visual**:
  - `SoftNotice` (kartu pemberitahuan lembut dengan `liveRegion` opsional) & `LockNotice` (pita kerahasiaan lokal untuk pertanyaan sensitif KBG).
  - `EmpatiLogo(size, animated, withBackground)`: Merender logo resmi **SRYA Emora** (monogram `"SA"` bergradasi biru-toska dengan ruang negatif berbentuk hati dan ekor balon percakapan) dalam 2 varian multi-resolusi (`mdpi` s/d `xxxhdpi`):
    - **Tanpa Background (`srya_emora_logo_transparent.png`, `withBackground = false`)**: Digunakan pada `SplashScreen` (`112.dp`) dan `AuthScreen` (`44.dp`) dengan animasi napas lembut (`scale 0.96f..1.04f`).
    - **Dengan Background Putih (`srya_emora_logo_bg.png`, `withBackground = true`)**: Digunakan pada avatar chatbot `CompanionAvatar` (`48.dp`) dan penanda gelembung pesan `CompanionMark` (`32.dp`) di `ChatScreen.kt`, serta ikon peluncur aplikasi (`ic_launcher` & `ic_launcher_round`).
  - `EmpatiAvatar` (inisial pengguna dengan `DisplayFont` Fraunces di dalam lingkaran `primaryContainer`).
  - `StateOrb` (orb afektif di layar Hasil dengan aura radial yang bernapas, cincin progres melingkar, dan ikon inti).
  - `BreathingDots` (tiga titik berdenyut lembut saat SRYA Emora sedang menyusun jawaban) & `Reveal` (animasi masuk bertahap per indeks).

---

## Spesifikasi 11 Layar Aplikasi (`ui/screens/`)

### 1. Splash Screen ([`SplashScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/SplashScreen.kt))
- Menampilkan `EmpatiLogo(size = 112.dp, animated = true)` di atas dua cincin halo `MaterialTheme.empati.aura`, judul `AI-EMPATI` (`displaySmall` Fraunces), dan satu baris pengantar tenang dengan animasi `Reveal`.

### 2. Onboarding ([`OnboardingScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/OnboardingScreen.kt))
- Pager 3 langkah (`HorizontalPager`) dengan nada afektif per slide (`SlideTone.Calm`, `Attentive`, `Presence`), indikator langkah `Langkah X dari 3`, tombol `Lewati`, medali ilustrasi berlapis dengan cincin bernapas (`OnboardingArtwork`), indikator titik pegas (`8.dp ↔ 28.dp`), dan tata letak responsif yang beradaptasi untuk layar kompak maupun lanskap.

### 3. Portal Otentikasi Multi-Format ([`AuthScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/AuthScreen.kt))
- Menggunakan `AuthFormState` (`@Stable`, bersih tanpa kredensial demo pra-isi) dan `SegmentedSwitch` untuk beralih antara **Masuk** dan **Daftar**.
- Kolom `EmpatiTextField` menerima **Email**, **Nomor HP**, **Username**, **NIK 16 digit**, atau **NIP 18 digit**, dilengkapi konfirmasi format otomatis (`SoftNotice` — termasuk penguraian tanggal lahir dan gender untuk NIP 18 digit).
- Mode registrasi menyajikan `ChoiceChip` untuk **Jenis kelamin** dan **Latar belakang**, diikuti `SoftNotice` informasi enkripsi dan tombol utama `Masuk` / `Daftar dan masuk` (tanpa tombol akun demo).

### 4. Home Dashboard ([`HomeScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/HomeScreen.kt))
- Sapaan waktu (`greetingForHour`), nama pengguna (`headlineLarge` Fraunces), dan `EmpatiAvatar` yang menuju Profil.
- **Kartu Status Risiko (`LatestScreeningCard` / `EmptyScreeningCard`)**: Menampilkan `RiskBadge` (`Rendah · Batas Wajar`, `Sedang · Perlu Pemantauan`, `Tinggi · Perlu Perhatian`, atau `Sangat Tinggi · Perlu Perhatian`), ringkasan awam, tombol `Skrining ulang`, dan tautan `Lihat riwayat`.
- Kartu pintasan layanan (*Mulai skrining* & *Konsultasi AI-EMPATI*) serta kartu privasi & rujukan **SAPA 129**.

### 5. Persetujuan / Informed Consent ([`ConsentScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ConsentScreen.kt))
- Menampilkan `ScreenHeader` (`"Persetujuan skrining"`), daftar 5 komitmen etika & privasi di dalam `TonalCard` dengan `SoftIconTile` (item layanan darurat menggunakan ubin hangat `secondaryContainer` Clay) yang dapat diperluas saat diketuk (`animateContentSize`), serta tombol `Saya setuju dan mulai` yang terpin di bagian bawah.

### 6. Kuesioner Dual-Modul ([`QuestionnaireScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/QuestionnaireScreen.kt))
- Header menampilkan nama modul, penghitung langkah tabular `EmpatiNumeralStyle` (`3 / 16`), dan progress bar beranimasi dengan semantik `progressBarRangeInfo`.
- Pertanyaan ditampilkan satu per layar (`headlineMedium` / `headlineSmall` Fraunces) dengan transisi geser horizontal (`AnimatedContent`), disertai `LockNotice` pada pertanyaan sensitif KBG.
- Opsi jawaban (`OptionCard`) memiliki semantik radio (`Role.RadioButton`), `pressScale`, `focusHalo`, dan animasi seleksi halus.

### 7 & 8. Hasil Modul & Analisis Terintegrasi ([`ResultScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ResultScreen.kt))
- **`ModuleResultScreen` (`ModuleResultContent`)**:
  - `ResultHero`: Menampilkan **`StateOrb`** (`148.dp`), `ToneBadge` status, judul (`headlineLarge` Fraunces), dan kesimpulan 1 kalimat (`bodyLarge`).
  - Kartu **Yang perlu diperhatikan / Yang teridentifikasi**: Deretan `FlowRow` berisi `FindingChip` interaktif yang membuka `FindingDetailSheet` (`ModalBottomSheet` penjelasan awam + pintasan diskusi AI-EMPATI).
  - `StrengthsCard` (**"Yang sudah baik"** dengan nada `calm` untuk faktor pelindung), `SoftNotice` dukungan, dan `NextStepsCard` (**"Langkah selanjutnya"**).
- **`IntegratedResultScreen` (`IntegratedResultContent`)**:
  - `ResultHero` dengan kategori sintesis utama, diikuti 2 kolom `ModuleStatusTile` berdampingan (**Hipertensi** & **Kesejahteraan**), `SoftNotice` penjelasan keterkaitan non-kausal (*"Bagaimana keduanya berkaitan"*), `FindingChip` faktor kunci, `StrengthsCard`, `NextStepsCard`, serta tombol `Simpan ke riwayat` dan `Tanyakan ke AI-EMPATI`.

### 9. Konsultasi Chatbot SRYA Emora ([`ChatScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ChatScreen.kt))
- Header bersih menampilkan avatar pendamping (`CompanionAvatar` berlogo `srya_emora_logo_bg` bulat dengan titik indikator aktif berdenyut `StatusDot`) dan judul **`SRYA Emora`** (tanpa kata `"Asisten"`, tanpa subjudul di bawahnya, dan **tanpa lencana nama model teknis** agar pergantian model otomatis di sisi server OpenRouter tidak membingungkan pengguna; nama model aktual hanya dicatat di database riset `public.research_chat_logs`).
- **Penanganan Keyboard & Insets Ergonomis (*IME-Aware Chatbox*)**:
  - Kolom chat utama (`ChatContent`) menerapkan `Modifier.imePadding()` secara langsung sehingga baris komposer (`ChatComposer`) dan placeholder input (*"Ceritakan yang Anda rasakan atau tanyakan sesuatu…"*) selalu terangkat dan berada tepat di atas keyboard saat pengguna mulai mengetik.
  - Saat keyboard aktif (`WindowInsets.isImeVisible == true`), `MainScaffold` secara cerdas menyembunyikan `BottomNavBar` untuk memaksimalkan ruang vertikal obrolan dan mencegah tumpang-tindih visual.
  - Komponen `ChatScreen` dilengkapi `LaunchedEffect(isImeVisible)` yang secara otomatis menggulir daftar pesan (`LazyColumn`) ke item terakhir saat keyboard terbuka, memastikan konteks pesan tidak tertutup.
- **Opsi Salin Teks Menyeluruh (*Text Selection & Clipboard Action*)**:
  - Teks di setiap gelembung pesan (`ChatBubble`) dibungkus dalam `SelectionContainer` dari Jetpack Compose sehingga pengguna dapat menyeleksi kata atau kalimat tertentu secara presisi.
  - Setiap gelembung pesan (baik respons SRYA Emora maupun pesan pengguna) dilengkapi tombol salin satu ketukan (`Icons.Rounded.ContentCopy`) yang menyalin teks pesan utuh ke clipboard sistem melalui `LocalClipboardManager` dengan notifikasi toast lembut (*"Pesan disalin ke clipboard"*).
  - Pada bilah komposer (`ChatComposer`), ketika terdapat draf teks (`value.isNotEmpty()`), muncul tombol salin cepat draf berikon `Icons.Rounded.ContentCopy` di sebelah tombol Kirim dengan umpan balik toast (*"Draf pesan disalin ke clipboard"*).
- Daftar percakapan dengan banner cakupan topik yang hangat (*Teman cerita seputar kesehatan, ketenangan pikiran, dan ruang aman*), indikator mengetik `BreathingDots` (*"SRYA Emora sedang mengetik…"*), deretan chip pertanyaan cepat, dan komposer kapsul mengambang dengan bayangan lembut `softShadow`.

### 10. Riwayat Skrining ([`HistoryScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/HistoryScreen.kt))
- Daftar kronologis kartu evaluasi yang menampilkan `RiskBadge` selaras (`Rendah · Batas Wajar` s/d `Sangat Tinggi · Perlu Perhatian`), tanggal, dan ringkasan kualitatif (item terbaru ditandai secara halus), serta *empty state* dengan CTA memulai skrining.

### 11. Profil & Privasi ([`ProfileScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ProfileScreen.kt))
- Kartu identitas pengguna dengan `EmpatiAvatar(size = 72.dp)`, nama (`headlineSmall` Fraunces), chip latar belakang & gender, serta domisili.
- Kartu **Data profil dan demografi** (dapat diedit melalui dialog dengan `EmpatiTextField`; baris ID akun menggunakan `EmpatiMetricStyle`), kartu **Pengaturan dan keamanan** (`FLAG_SECURE`, pengingat, tentang aplikasi), tombol `Keluar atau ganti akun`, dan kartu zona bahaya `Hapus seluruh data saya dari perangkat`.

---

## Navigasi & Transisi (`EmpatiApp.kt` & `MainScaffold.kt`)

- **Navigasi Hirarkis (`EmpatiApp.kt`)**: Menggunakan `AnimatedContent` berbasis `screenRank` (0 = Splash s/d 7 = IntegratedResult) dengan kurva `EmpatiMotion.expressive()` (geser horizontal lembut `it / 14` + fade) yang otomatis beralih ke crossfade sederhana bila `LocalReducedMotion` aktif.
- **Bottom Navigation (`MainScaffold.kt` → `BottomNavBar`)**: Bilah bawah bertona `EmpatiSurfaces.card()` dengan pembatas `SoftDivider()`, kapsul aktif `primaryContainer`, dukungan `pressScale` + `focusHalo`, dan 5 tab: **Beranda**, **Skrining** (membuka alur `Screen.Consent`), **Konsultasi**, **Riwayat**, dan **Profil**. Pada tab Konsultasi Chat, bilah bawah disembunyikan otomatis saat keyboard muncul (`WindowInsets.isImeVisible`) sehingga tidak menghalangi input pengguna.

---

## Aksesibilitas (WCAG 2.2) & Keamanan Visual

| Aspek | Implementasi di Codebase |
|---|---|
| **Kontras Warna (WCAG AAA)** | `StoneLightOnSurface` (`#1B1C19`) di atas `StoneLightBackground` (`#F7F5F0`) menghasilkan rasio kontras **14.3:1**; `StoneLightOnVariant` (`#474B44`) menghasilkan **7.6:1**. |
| **Target Sentuh Minimum** | `EmpatiSpace.touchTarget = 48.dp` dan `minimumInteractiveComponentSize()` diterapkan pada seluruh tombol, chip, dan item navigasi. |
| **Fokus Keyboard / D-Pad (WCAG 2.4.7)** | `Modifier.focusHalo` menggambar cincin fokus `2.dp` berwarna `primary` pada seluruh elemen interaktif kustom. |
| **Ergonomi Keyboard & Chat IME** | `Modifier.imePadding()` mengangkat chatbox secara dinamis tepat di atas keyboard; `isImeVisible` auto-scroll menjaga keterbacaan pesan saat mengetik. |
| **Seleksi & Salin Teks (Clipboard)** | `SelectionContainer` pada gelembung pesan, tombol salin pesan satu ketukan (`ContentCopy`), dan tombol salin draf teks pada chatbox dengan toast feedback. |
| **Screen Reader & Live Region** | `LiveRegionMode.Polite` pada pesan error `EmpatiTextField` dan `SoftNotice`; `Role.RadioButton` & `ProgressBarRangeInfo` pada kuesioner; `mergeDescendants = true` pada kolom input. |
| **Reduced Motion** | `LocalReducedMotion` menghormati pengaturan aksesibilitas sistem Android untuk mematikan animasi berulang/transisi panjang. |
| **Keamanan Layar (`FLAG_SECURE` Opsional)** | `MainActivity.ENABLE_SCREENSHOT_PROTECTION = false` mengizinkan screenshot & screen recording secara default, sembari menyimpan kode `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)` agar dapat diaktifkan kembali kapan saja di masa depan. |

---

## Referensi File

| File | Deskripsi |
|---|---|
| [`Color.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Color.kt) | Palet warna "Soft Daylight" v4 (Sage, Clay, Mist, Honey, Terracotta, Berry, Stone Light/Dark) |
| [`Theme.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Theme.kt) | `LightColorScheme`, `DarkColorScheme`, `EmpatiColors` (`MaterialTheme.empati`), `EmpatiShapes`, dan `EmpatiTheme` |
| [`Type.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Type.kt) | Sistem tipografi 3-suara (`DisplayFont` Fraunces, `BodyFont` Inter + `EmpatiNumeralStyle`, `MetricFont` JetBrains Mono) |
| [`Tokens.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/theme/Tokens.kt) | `EmpatiSpace`, `EmpatiIcon`, `EmpatiElevation`, `EmpatiMotion`, `LocalReducedMotion`, dan `Modifier.softShadow` |
| [`Components.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/components/Components.kt) | Pustaka komponen (`ScreenFrame`, `TonalCard`, `ToneBadge`, `RiskBadge`, `StateOrb`, `EmpatiTextField`, `SegmentedSwitch`, `EmpatiLogo`, dll.) |
| [`EmpatiApp.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/EmpatiApp.kt) | Root composable & animasi transisi antar-layar berbasis `screenRank` |
| [`MainScaffold.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/MainScaffold.kt) | `BottomNavBar` kustom beraksesibilitas tinggi & transisi antar-tab |
| [`SplashScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/SplashScreen.kt) | Layar pembuka dengan halo `aura` dan `EmpatiLogo` bernapas |
| [`OnboardingScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/OnboardingScreen.kt) | Pager 3 langkah dengan `OnboardingArtwork` bernada afektif & tata letak adaptif |
| [`AuthScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/AuthScreen.kt) | Portal Login & Registrasi multi-format dengan `AuthFormState`, `SegmentedSwitch`, & `IdentityConfirmationCard` |
| [`HomeScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/HomeScreen.kt) | Beranda dengan kartu status risiko selaras, pintasan layanan, dan kartu privasi SAPA 129 |
| [`ConsentScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ConsentScreen.kt) | Layar persetujuan dengan kartu komitmen yang dapat diperluas & ubin ikon hangat |
| [`QuestionnaireScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/QuestionnaireScreen.kt) | Layar kuesioner bertahap dengan `EmpatiNumeralStyle` tabular & semantik aksesibilitas penuh |
| [`ResultScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ResultScreen.kt) | `ModuleResultScreen` & `IntegratedResultScreen` dengan `StateOrb`, `FindingChip`, `StrengthsCard`, & `FindingDetailSheet` |
| [`ChatScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ChatScreen.kt) | Layar konsultasi `AI-EMPATI` dengan `BreathingDots` & jaminan kalimat utuh |
| [`HistoryScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/HistoryScreen.kt) | Daftar riwayat evaluasi kronologis & *empty state* |
| [`ProfileScreen.kt`](file:///c:/Users/ADVAN/AndroidStudioProjects/Empati/app/src/main/java/com/example/empati/ui/screens/ProfileScreen.kt) | Profil demografi, pengaturan keamanan layar, ganti akun, dan hapus data permanen |
