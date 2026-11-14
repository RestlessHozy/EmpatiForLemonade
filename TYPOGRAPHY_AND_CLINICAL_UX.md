# Empati — 3-Voice Typography & "Soft Daylight" Clinical UX (v4.1.5 Design Baseline)

> **Note:** This document originated during the v4.1.5 "Soft Daylight" UI redesign (historically titled *SRYA Emora* / *AI-EMPATI*, now branded **Empati** and powered locally by AMD Lemonade `:13305`; see [`README.md`](README.md)). The typography tokens, affective color system, `StateOrb`, IME ergonomics, and zero-leak presenter rules documented below remain active in production. Patient-facing runtime copy shown in quotes (e.g., `"Rendah · Batas Wajar"`, `"Yang sudah baik"`) intentionally remains in Bahasa Indonesia inside the app to serve Indonesian users and match the clinical knowledge base.

This document explains the design rationale behind the **3-Voice Typography System** (`Fraunces` + `Inter` + `JetBrains Mono`), **Affective Semantics (`MaterialTheme.empati`)**, the **`StateOrb`** visualization, clinical chat interface ergonomics (IME keyboard placement & clipboard actions), and the **Clinical Empathy UX & Human Language (Anti-AI-Slop)** approach in Empati.

---

## Table of Contents

1. [Why Clinical Empathy UX & "Soft Daylight"?](#1-why-clinical-empathy-ux--soft-daylight)
2. [3-Voice Typography Architecture (`Type.kt`)](#2-3-voice-typography-architecture-typekt)
   - [Bundled Font Assets (`res/font/`)](#bundled-font-assets-resfont)
   - [Mapping the 3 Cognitive Voices](#mapping-the-3-cognitive-voices)
   - [Why Sentence-Case & Tabular Numerals?](#why-sentence-case--tabular-numerals)
3. [Affective Semantics & `StateOrb` on Screening Results (`ResultScreen.kt`)](#3-affective-semantics--stateorb-on-screening-results-resultscreenkt)
   - [Affective Tone Mapping (`MaterialTheme.empati`)](#affective-tone-mapping-materialthemeempati)
   - [Per-Module Result Screen (`ModuleResultScreen`)](#per-module-result-screen-moduleresultscreen)
   - [Integrated Analysis Screen (`IntegratedResultScreen`)](#integrated-analysis-screen-integratedresultscreen)
4. [Chatbot Consultation Ergonomics: Composer Placement & Referral Copying (`ChatScreen.kt`)](#4-chatbot-consultation-ergonomics-composer-placement--referral-copying-chatscreenkt)
5. [Concise Clinical Copywriting & Zero-Leak Standards (`UserResultPresenter.kt`)](#5-concise-clinical-copywriting--zero-leak-standards-userresultpresenterkt)
6. [File References](#6-file-references)

---

## 1. Why Clinical Empathy UX & "Soft Daylight"?

In health apps addressing sensitive topics (hypertension risk and Gender-Based Violence / GBV experiences), two design mistakes occur frequently:
1. **Cold, laboratory-like atmosphere** — ALL-CAPS headings, monospace fonts across every status badge, and alarming bright reds that trigger health anxiety.
2. **Walls of text** — repetitive clinical paragraphs that cause high cognitive overload.

The **"Soft Daylight" (v4.0)** design resolves both through:
- **Affective Hierarchy First ("How It Feels First")**: The top of the result screen (`ResultHero`) opens with a gently breathing **`StateOrb`** and a warm **Fraunces** headline, followed by a single primary summary sentence.
- **Progressive Disclosure (`FindingChip` → `FindingDetailSheet`)**: Risk factors are presented as tappable pill chips that open plain-language explanations in a `ModalBottomSheet` without cluttering the primary canvas.
- **Positive Reinforcement (`StrengthsCard` — *"Yang sudah baik"* / What Is Already Good)**: Healthy protective habits are highlighted in a `calm` (Sage) surface card to balance risk perception.
- **Zero Redundancy**: On the Integrated Analysis screen, both module statuses are summarized side by side via **2 `ModuleStatusTile` cards** (**Hypertension** & **Well-being**).

---

## 2. 3-Voice Typography Architecture (`Type.kt`)

Empati bundles three official variable font families (Open Font License) directly under `app/src/main/res/font/`, where each typeface represents a **distinct psychological voice and cognitive function**:

### Bundled Font Assets (`res/font/`)

| Voice | Font Name | Resource File | Variable in `Type.kt` | Visual Characteristics & Role |
|---|---|---|---|---|
| **1. Expressive & Human Voice** | **Fraunces** | [`fraunces.ttf`](app/src/main/res/font/fraunces.ttf) | `DisplayFont` | Variable soft-serif with `opsz=72`, `SOFT=100`, `WONK=0`. Brings human warmth and editorial calm to time-of-day greetings, questionnaire prompts, screening result headlines, `EmpatiAvatar` initials, and screen titles. |
| **2. Functional & Reading Voice** | **Inter** | [`inter.ttf`](app/src/main/res/font/inter.ttf) | `BodyFont` + `EmpatiNumeralStyle` | High x-height sans-serif with ~1.6x line height for clinical reading, questionnaire options, sentence-case badges, and chat conversations. |
| **3. Telemetry & Identity Voice** | **JetBrains Mono** | [`jetbrains_mono.ttf`](app/src/main/res/font/jetbrains_mono.ttf) | `MetricFont` (`EmpatiMetricStyle`) | Monospace `13sp/18sp` reserved **exclusively** for raw identifiers (account IDs in Profile, 18-digit NIP anatomy breakdowns, document codes)—**never** used for UI labels or badges. |

### Mapping the 3 Cognitive Voices

Implemented in [`Type.kt`](app/src/main/java/com/example/empati/ui/theme/Type.kt):

```text
┌───────────────────────────────────────────────────────────────────────────┐
│ 1. DisplayFont — Fraunces (opsz=72, SOFT=100, WONK=0)                     │
│    • displayLarge (48/54), displayMedium (40/46), displaySmall (34/40)    │
│    • headlineLarge (30/38), headlineMedium (26/34), headlineSmall (22/30) │
│    → Used for: Home greetings, questionnaire prompts, ResultHero,         │
│      ScreenHeader titles, bottom sheet headers, & EmpatiAvatar            │
├───────────────────────────────────────────────────────────────────────────┤
│ 2. BodyFont — Inter (+ EmpatiNumeralStyle tabular numerals 'tnum')        │
│    • titleLarge (20/28), titleMedium (16/24), titleSmall (14/20)          │
│    • bodyLarge (16/26), bodyMedium (14/22), bodySmall (12/18)             │
│    • labelLarge (14/20), labelMedium (12/16), labelSmall (11/16)          │
│    • EmpatiNumeralStyle (12/16, tnum)                                     │
│    → Used for: Clinical narrative, buttons, sentence-case chips/badges,   │
│      Empati chat messages, & questionnaire step counters (3 / 16)         │
├───────────────────────────────────────────────────────────────────────────┤
│ 3. MetricFont — JetBrains Mono (EmpatiMetricStyle 13/18)                  │
│    → Used ONLY for: User account IDs & 18-digit NIP breakdowns            │
└───────────────────────────────────────────────────────────────────────────┘
```

### Why Sentence-Case & Tabular Numerals?

1. **Calming Sentence-Case (`String.toSentenceCase()`)**:
   - Labels such as `"Rendah · Batas Wajar"` (Low · Within Normal Limits), `"Sedang · Perlu Pemantauan"` (Moderate · Needs Monitoring), `"Modul hipertensi"`, and `"Faktor kardiovaskular · gaya hidup"` are rendered in sentence-case using **Inter** (`BodyFont`), avoiding machine-like ALL-CAPS monospace warnings.
2. **Jitter-Free Tabular Numerals (`EmpatiNumeralStyle`)**:
   - Questionnaire step counters (`3 / 16`) use `BodyFont` with `fontFeatureSettings = "tnum"` so digit widths stay constant as the user advances through questions without switching into monospace.

---

## 3. Affective Semantics & `StateOrb` on Screening Results (`ResultScreen.kt`)

### Affective Tone Mapping (`MaterialTheme.empati`)

Risk status labels and tones are 100% aligned across `ResultScreen`, `HomeScreen`, and `HistoryScreen`:

| Risk Level (`CfLevel` / `RiskLevel`) | Runtime Status Label (ID) | Affective Tone (`MaterialTheme.empati`) | Color Palette |
|---|---|---|---|
| `RENDAH` / `LOW` (`CF < 0.25`) | **`Rendah · Batas Wajar`** | `calm` | **Sage** (`#2E6A57` / `#D6EEE3`) |
| `SEDANG` / `MEDIUM` (`0.25 ≤ CF < 0.50` or controlled hypertension) | **`Sedang · Perlu Pemantauan`** | `attentive` | **Honey** (`#D29A2E` / `#FBE8BE`) |
| `TINGGI` / `HIGH` (`0.50 ≤ CF < 0.75`) | **`Tinggi · Perlu Perhatian`** | `concerned` (or `attentive` per module) | **Terracotta** (`#A8402B` / `#FCE6E0`) |
| `SANGAT_TINGGI` / `VERY_HIGH` (`CF ≥ 0.75`) | **`Sangat Tinggi · Perlu Perhatian`** | `urgent` | **Berry** (`#8E2F55` / `#F9D9E4`) |
| AI Companion / Well-being Module | **`Terdapat indikasi`** / **`Empati`** | `presence` | **Mist** (`#5A5896` / `#F0EFFD`) |

### Per-Module Result Screen (`ModuleResultScreen`)

1. **`ResultHero` + `StateOrb`**:
   - Displays a `148.dp` `StateOrb` that draws a circular progress ring on first appear and breathes softly (or stays static when `LocalReducedMotion` is active), followed by a `ToneBadge`, `headlineLarge` (**Fraunces**) title, and 1-sentence conclusion (`bodyLarge` **Inter**).
2. **Interactive Factor Chips (`FindingChip` → `FindingDetailSheet`)**:
   - Renders a `FlowRow` of `FindingChip` items (with `Icons.Outlined.Info`) that open a `ModalBottomSheet` (`FindingDetailSheet`) containing a plain-language educational explanation, a non-causal chronic-stress reminder, and an action button to discuss with Empati.
3. **`StrengthsCard` (*"Yang sudah baik"*)**:
   - A `calm.container` surface card celebrating up to 5 protective lifestyle habits reported by the respondent.
4. **Conditional Support `SoftNotice` & `NextStepsCard` (*"Langkah selanjutnya"*)**:
   - Lists up to 3 concrete follow-up steps separated by `SoftDivider()`.

### Integrated Analysis Screen (`IntegratedResultScreen`)

1. **`ResultHero`**: Renders `StateOrb`, the integrated category headline (`headlineLarge` **Fraunces**), and a 1-sentence summary.
2. **Side-by-Side Module Status Tiles (`ModuleStatusTile`)**:
   - Displays **Hypertension** and **Well-being** tiles side by side with `SoftIconTile`, affective color dot (`tone.accent`), and concise status text.
3. **`SoftNotice` (*"Bagaimana keduanya berkaitan"* / How Both Relate)**:
   - Explains the chronic stress pathway empathetically and non-causally in the `presence` (Mist) tone.
4. **Key Factor Chips (`FindingChip`), `StrengthsCard`, `NextStepsCard`, plus `Save to History` & `Ask Empati` CTAs**.

---

## 4. Chatbot Consultation Ergonomics: Composer Placement & Referral Copying (`ChatScreen.kt`)

In health consultations and safe-space support, conversational interaction requires comfortable motor ergonomics and effortless information capture without overwhelming the user:

1. **Composer Docked Directly Above Keyboard (`Modifier.imePadding()` & Navbar Adaptation)**:
   - Respondents typing sensitive health questions need unobstructed visibility of their input without virtual keyboard or bottom navigation bar overlap.
   - Applying `Modifier.imePadding()` on `ChatContent` and hiding `BottomNavBar` while the keyboard is open (`WindowInsets.isImeVisible == true`) keeps the chat composer and placeholder anchored comfortably above the IME.
   - `LaunchedEffect(isImeVisible)` automatically scrolls the message list to the latest item so prior conversation context remains visible when the keyboard opens.
2. **Easy Copying of Clinical Guidance & Crisis Hotlines (`SelectionContainer` & `ContentCopy`)**:
   - Critical details such as stress-management tips, DASH dietary guidance, or the **SAPA 129** hotline (`129` / WhatsApp `08111-129-129`) often need to be saved or shared with trusted family members.
   - Message bubble text is wrapped in `SelectionContainer` for partial text selection.
   - A one-tap `Icons.Rounded.ContentCopy` button appears on messages with calm toast feedback, alongside a draft-copy button on the composer bar for longer questions.

---

## 5. Concise Clinical Copywriting & Zero-Leak Standards (`UserResultPresenter.kt`)

All user-facing copy emitted by [`UserResultPresenter.kt`](app/src/main/java/com/example/empati/data/presentation/UserResultPresenter.kt) is automatically verified by `UserPresentationTest`:

- **Concise Hypertension Factor Labels** (runtime Bahasa Indonesia):
  - `Riwayat keluarga` (Family history)
  - `Kondisi medis & tensi` (Medical conditions & blood pressure)
  - `Pola makan & aktivitas` (Diet & physical activity)
  - `Stres & kualitas tidur` (Stress & sleep quality)
- **Universal Clinical Tag Explanations (`explainClinicalTag`)**:
  - Provides 1–2 sentence layperson explanations for every hypertension and GBV tag without dense medical jargon.
- **Zero-Leak Guarantee**:
  - No `CF`, `MB`, `MD`, percentages (`%`), rule weights, or decimal numbers (`0.xx`) ever leak onto user-facing screens.

---

## 6. File References

| File | Role |
|---|---|
| [`fraunces.ttf`](app/src/main/res/font/fraunces.ttf) | Bundled variable soft-serif font for `DisplayFont` (headlines, greetings, result statements) |
| [`inter.ttf`](app/src/main/res/font/inter.ttf) | Bundled variable sans-serif font for `BodyFont` & `EmpatiNumeralStyle` (`tnum`) |
| [`jetbrains_mono.ttf`](app/src/main/res/font/jetbrains_mono.ttf) | Bundled variable monospace font for `MetricFont` (`EmpatiMetricStyle` for account ID/NIP) |
| [`Type.kt`](app/src/main/java/com/example/empati/ui/theme/Type.kt) | Configures `DisplayFont`, `BodyFont`, `MetricFont`, `EmpatiNumeralStyle`, `EmpatiMetricStyle`, and `EmpatiTypography` |
| [`Color.kt`](app/src/main/java/com/example/empati/ui/theme/Color.kt) & [`Theme.kt`](app/src/main/java/com/example/empati/ui/theme/Theme.kt) | "Soft Daylight" v4 palette & `MaterialTheme.empati` affective tone system |
| [`Components.kt`](app/src/main/java/com/example/empati/ui/components/Components.kt) | `StateOrb`, `ToneBadge`, `RiskBadge`, `SoftNotice`, `toSentenceCase()` |
| [`ResultScreen.kt`](app/src/main/java/com/example/empati/ui/screens/ResultScreen.kt) | `ModuleResultScreen`, `IntegratedResultScreen`, `ResultHero`, `FindingChip`, `StrengthsCard`, & `FindingDetailSheet` |
| [`UserResultPresenter.kt`](app/src/main/java/com/example/empati/data/presentation/UserResultPresenter.kt) | Concise clinical copywriting with zero CF technical leaks and aligned risk status labels |
