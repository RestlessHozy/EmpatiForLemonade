# [Historical Archive v3.1] — Functional Typography & Clinical Minimalism

> **Superseded Historical Archive:** This document describes the **v3.1** design iteration (which used *Plus Jakarta Sans + Inter + JetBrains Mono*). That typography system was later superseded in **v4.0+ ("Soft Daylight")** by **Fraunces + Inter + JetBrains Mono** (`fraunces.ttf`, keeping *JetBrains Mono* strictly for raw telemetry/IDs). See the current active guide in [`TYPOGRAPHY_AND_CLINICAL_UX.md`](../../TYPOGRAPHY_AND_CLINICAL_UX.md) and [`FRONTEND_DESIGN.md`](../../FRONTEND_DESIGN.md).

---

## Table of Contents

1. [Why Clinical Minimalism?](#1-why-clinical-minimalism)
2. [3-Role Functional Typography Architecture](#2-3-role-functional-typography-architecture)
   - [Bundled Font Assets (v3.1)](#bundled-font-assets-v31)
   - [Functional Role Mapping](#functional-role-mapping)
   - [Why This Pairing Avoided "AI Slop"](#why-this-pairing-avoided-ai-slop)
3. [Information Reduction on Result Screens (`ResultScreen.kt`)](#3-information-reduction-on-result-screens-resultscreenkt)
4. [Concise Clinical Copywriting Standards (`UserResultPresenter.kt`)](#4-concise-clinical-copywriting-standards-userresultpresenterkt)
5. [Historical File References](#5-historical-file-references)

---

## 1. Why Clinical Minimalism?

On sensitive health screens (hypertension risk and Gender-Based Violence / GBV exposure), long walls of prose trigger cognitive overload and fatigue, while repeating identical phrasing across multiple stacked cards produces generic "AI slop".

**Clinical Minimalism** restructured result screens like a modern executive clinical summary:
- **Scannable in 3 Seconds**: Primary status, 1-sentence summary, and contributing factor tags fit cleanly in a single glance.
- **Zero Redundancy**: On Integrated Analysis, individual module summaries collapsed into a **2-Column Status Matrix** (**HYPERTENSION** | **WELL-BEING**).
- **Chips Over Bullet Paragraphs**: Risk factors and protective findings appeared as compact `ClinicalTagChip` items inside a `FlowRow`.
- **1-Line Footnote Disclaimer**: Educational disclaimers shortened to a single understated footer line.

---

## 2. 3-Role Functional Typography Architecture

In v3.1, AI-EMPATI replaced generic system fonts with three bundled variable typefaces:

### Bundled Font Assets (v3.1)

| Role | Font Family | Resource Asset (v3.1) | Visual Character |
|---|---|---|---|
| **1. Headings & Clinical Authority** | **Plus Jakarta Sans** | `res/font/plus_jakarta_sans.ttf` *(replaced by `fraunces.ttf` in v4.0)* | Contemporary geometric humanist sans giving calm authority to titles and headings. |
| **2. Body & Clinical Reading** | **Inter** | [`inter.ttf`](../../app/src/main/res/font/inter.ttf) | High x-height sans-serif optimized for questionnaire legibility, summaries, and chat. |
| **3. Metadata & Diagnostic Labels** | **JetBrains Mono** | [`jetbrains_mono.ttf`](../../app/src/main/res/font/jetbrains_mono.ttf) | Monospace precision used in v3.1 for badges, step counters (`3 / 16`), module codes, citations, and account IDs *(narrowed exclusively to raw IDs in v4.0)*. |

### Functional Role Mapping

- **`HeadingFont` (Plus Jakarta Sans)**: `displaySmall`, `headlineLarge..headlineSmall`, `titleLarge..titleSmall`, `labelLarge`.
- **`BodyFont` (Inter)**: `bodyLarge`, `bodyMedium`, `bodySmall`.
- **`MonoFont` (JetBrains Mono)**: `labelMedium`, `labelSmall`, status pills, and numeric NIP telemetry.

### Why This Pairing Avoided "AI Slop"

1. **Function-Driven Contrast**: Distinct fonts separated narrative copy from system indicators.
2. **Modern Clinical Instrumentation Feel**: Measured monospace metadata recalled modern digital clinic instruments without decorative gradients.
3. **Cross-Device Consistency**: Bundled `.ttf` variable fonts rendered identically regardless of OEM system fonts.

---

## 3. Information Reduction on Result Screens (`ResultScreen.kt`)

- **`ModuleResultScreen`**: Condensed 4–5 verbose cards into **1 Unified Clinical Summary Card** + **1 Action Plan Card**, replacing long lists with `ClinicalTagChip` `FlowRow` chips, a conditional `SupportStrip`, and a single-line `MinimalDisclaimer`.
- **`IntegratedResultScreen`**: Collapsed 6 stacked cards into **3 Executive Blocks** featuring a side-by-side 2-column module status matrix (`ModuleStatusCell`), up to 4 factor chips, max 3 one-line action items, conditional `SupportStrip`, and `MinimalDisclaimer`.

---

## 4. Concise Clinical Copywriting Standards (`UserResultPresenter.kt`)

Runtime copy emitted in Bahasa Indonesia enforced strict brevity and zero-leak guarantees verified by `UserPresentationTest`:
- **Concise Hypertension Tags**: `Riwayat keluarga`, `Kondisi medis & tensi`, `Pola makan & aktivitas`, `Stres & kualitas tidur`.
- **Single-Sentence Conclusions** and **Max 3 Action Items** phrased actively.
- **Zero Technical Leakage**: No `CF`, `MB`, `MD`, percentages (`%`), rule weights, or decimal scores (`0.xx`).

---

## 5. Historical File References

- `res/font/plus_jakarta_sans.ttf` *(v3.1 historical font asset — superseded by `fraunces.ttf`)*
- [`inter.ttf`](../../app/src/main/res/font/inter.ttf)
- [`jetbrains_mono.ttf`](../../app/src/main/res/font/jetbrains_mono.ttf)
- [`Type.kt`](../../app/src/main/java/com/example/empati/ui/theme/Type.kt)
- [`ResultScreen.kt`](../../app/src/main/java/com/example/empati/ui/screens/ResultScreen.kt)
- [`UserResultPresenter.kt`](../../app/src/main/java/com/example/empati/data/presentation/UserResultPresenter.kt)
- [`Components.kt`](../../app/src/main/java/com/example/empati/ui/components/Components.kt)
