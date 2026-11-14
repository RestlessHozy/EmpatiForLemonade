# Empati (formerly SRYA Emora) — Front-End Design Guide & "Soft Daylight" UI/UX System (v4.1.5 Design System)

> **Historical Note:** This design guide documents the **"Soft Daylight" (v4.1.5)** UI/UX system originally created under the title *SRYA Emora* (preceded by *AI-EMPATI*). The app is now branded **Empati** and routes local AI chat, vector embeddings, STT, and TTS through **AMD Lemonade Server (`:13305`)** (see [`README.md`](README.md) and [`docs/specs/lemonade-hybrid-triage.md`](docs/specs/lemonade-hybrid-triage.md)), while the color architecture, 3-voice typography, motion tokens, reusable component library, and 11-screen layouts documented here remain active in production.
>
> For a deeper dive into the 3-voice typography philosophy, affective tone mapping, and clinical-empathy result presentation, see [`TYPOGRAPHY_AND_CLINICAL_UX.md`](TYPOGRAPHY_AND_CLINICAL_UX.md).

---

## Table of Contents

- [Philosophy & "Soft Daylight" Design Principles (v4.0)](#philosophy--soft-daylight-design-principles-v40)
- [3-Layer Color Architecture (`Color.kt` & `Theme.kt`)](#3-layer-color-architecture-colorkt--themekt)
  - [Layer 1 — Raw Tonal Ramps](#layer-1--raw-tonal-ramps)
  - [Layer 2 — Material 3 Roles (`LightColorScheme` & `DarkColorScheme`)](#layer-2--material-3-roles-lightcolorscheme--darkcolorscheme)
  - [Layer 3 — Affective Semantics (`EmpatiColors` via `MaterialTheme.empati`)](#layer-3--affective-semantics-empaticolors-via-materialthemeempati)
- [3-Voice Typography System (`Type.kt` & `res/font/`)](#3-voice-typography-system-typekt--resfont)
- [Design Tokens: Spacing, Elevation, Shapes & Motion (`Tokens.kt`)](#design-tokens-spacing-elevation-shapes--motion-tokenskt)
- [Reusable Component Library (`Components.kt`)](#reusable-component-library-componentskt)
- [11-Screen Application Specification (`ui/screens/`)](#11-screen-application-specification-uiscreens)
- [Navigation & Transitions (`EmpatiApp.kt` & `MainScaffold.kt`)](#navigation--transitions-empatiappkt--mainscaffoldkt)
- [Accessibility (WCAG 2.2) & Visual Safety](#accessibility-wcag-22--visual-safety)
- [File References](#file-references)

---

## Philosophy & "Soft Daylight" Design Principles (v4.0)

1. **"Soft Daylight" & "Quiet Evening" Atmosphere** — Replaces the legacy purple/mauve palette with a warm paper-like natural stone-green canvas (**Stone** `#F7F5F0` in Light Mode, `#121512` in Dark Mode) paired with **Sage** (clinical calm), **Clay** (human warmth), and **Mist** (AI companion presence).
2. **Affective Semantics Over Clinical Alarms** — Health risk states are communicated through calm, honest affective tones (`calm`, `attentive`, `concerned`, `urgent`, `neutral`, `presence`) paired with a gently breathing `StateOrb`, avoiding raw decimals that trigger health anxiety.
3. **Conversational Sentence-Case** — Labels, badges, and chips use sentence-case (`String.toSentenceCase()`) rather than harsh ALL-CAPS strings.
4. **Stateless Content + Stateful Wrapper Architecture** — Every screen decouples its stateful entry point (wired to `EmpatiViewModel`) from a pure `*Content` composable for full Compose Preview support and isolated UI testing.
5. **Strict Separation of Research Data vs. User Presentation (Zero-Leak)** — Internal computational metrics (`Certainty Factor`, `MB`, `MD`, weights, percentages `%`, rule IDs) never render in user-facing UI surfaces.

---

## 3-Layer Color Architecture (`Color.kt` & `Theme.kt`)

The color system in [`Color.kt`](app/src/main/java/com/example/empati/ui/theme/Color.kt) and [`Theme.kt`](app/src/main/java/com/example/empati/ui/theme/Theme.kt) flows unidirectionally across three layers: **Palettes** (raw tonal values) → **Roles** (Material 3 color schemes) → **Affective Semantics** (`MaterialTheme.empati`).

### Layer 1 — Raw Tonal Ramps

| Color Family | Psychological Role | Primary Tokens & Hex |
|---|---|---|
| **Sage** (Primary) | Calmness, clinical trust, core identity | `Sage10` `#05241B` · `Sage20` `#0A3A2C` · `Sage30` `#1F4F40` · `Sage40` `#2E6A57` · `Sage80` `#9CCFB8` · `Sage90` `#BAEDD6` · `Sage95` `#D6EEE3` · `Sage98` `#EAF6F0` |
| **Clay** (Secondary) | Human warmth, empathetic support | `Clay10` `#3B1808` · `Clay30` `#6E3A25` · `Clay40` `#93482E` · `Clay80` `#F2B79C` · `Clay90` `#FFDBCB` · `Clay95` `#FBE4D8` |
| **Mist** (Tertiary / AI) | AI companion presence, reflection & safe-space cues | `Mist10` `#1B1948` · `Mist30` `#42407F` · `Mist40` `#5A5896` · `Mist80` `#C6C4F4` · `Mist90` `#E3E1FA` · `Mist95` `#F0EFFD` |
| **Honey** (Attentive) | Gentle awareness ("worth monitoring") | `Honey10` `#3D2800` · `Honey25` `#5C3C00` · `Honey40` `#85570A` · `Honey60` `#D29A2E` · `Honey80` `#F3C56A` · `Honey90` `#FBE8BE` |
| **Terracotta** (Concerned) | Clinical attention / validation needed | `Terra10` `#400A02` · `Terra25` `#6E2415` · `Terra40` `#A8402B` · `Terra80` `#FFB4A3` · `Terra90` `#FFDAD1` · `Terra95` `#FCE6E0` |
| **Berry** (Urgent) | High priority / immediate attention | `Berry10` `#3F0A22` · `Berry25` `#68183B` · `Berry40` `#8E2F55` · `Berry80` `#F2A9C2` · `Berry90` `#F9D9E4` |

#### **Stone** Neutral Surfaces (Subtle Green-Biased Warm Grey)

| Mode | Token | Hex | Material 3 Role (`ColorScheme`) |
|---|---|---|---|
| **Light** (*Daylight on Paper*) | `StoneLightBackground` | `#F7F5F0` | `background` / `surface` |
| | `StoneLightLowest` | `#FFFFFF` | `surfaceContainerLowest` (`EmpatiSurfaces.card()` in Light) |
| | `StoneLightLow` | `#F2F0EA` | `surfaceContainerLow` (`EmpatiSurfaces.inset()` in Light) |
| | `StoneLightMid` | `#EDEAE3` | `surfaceContainer` (`EmpatiSurfaces.sunken()`) |
| | `StoneLightHigh` | `#E7E4DC` | `surfaceContainerHigh` |
| | `StoneLightHighest` | `#E1DED5` | `surfaceContainerHighest` / `surfaceVariant` |
| | `StoneLightOnSurface` | `#1B1C19` | `onBackground` / `onSurface` (Contrast 14.3:1 WCAG AAA) |
| | `StoneLightOnVariant` | `#474B44` | `onSurfaceVariant` (Contrast 7.6:1 WCAG AAA) |
| | `StoneLightOutline` | `#747970` | `outline` |
| | `StoneLightOutlineVariant` | `#D0D3C9` | `outlineVariant` (`EmpatiSurfaces.hairline()`) |
| **Dark** (*Quiet Evening*) | `StoneDarkBackground` | `#121512` | `background` / `surface` |
| | `StoneDarkLowest` | `#0C0F0C` | `surfaceContainerLowest` |
| | `StoneDarkLow` | `#181B18` | `surfaceContainerLow` (`EmpatiSurfaces.card()` in Dark) |
| | `StoneDarkMid` | `#1C1F1C` | `surfaceContainer` (`EmpatiSurfaces.sunken()`) |
| | `StoneDarkHigh` | `#262A26` | `surfaceContainerHigh` (`EmpatiSurfaces.inset()` in Dark) |
| | `StoneDarkHighest` | `#313531` | `surfaceContainerHighest` / `surfaceVariant` |
| | `StoneDarkOnSurface` | `#E3E3DB` | `onBackground` / `onSurface` |
| | `StoneDarkOnVariant` | `#BEC3B9` | `onSurfaceVariant` |
| | `StoneDarkOutline` | `#889084` | `outline` |
| | `StoneDarkOutlineVariant` | `#40443F` | `outlineVariant` |

### Layer 2 — Material 3 Roles (`LightColorScheme` & `DarkColorScheme`)

Configured in [`Theme.kt`](app/src/main/java/com/example/empati/ui/theme/Theme.kt):
- `primary` = `Sage40` (`#2E6A57` Light) / `Sage80` (`#9CCFB8` Dark)
- `secondary` = `Clay40` (`#93482E` Light) / `Clay80` (`#F2B79C` Dark)
- `tertiary` = `Mist40` (`#5A5896` Light) / `Mist80` (`#C6C4F4` Dark)
- `error` = `Terra40` (`#A8402B` Light) / `Terra80` (`#FFB4A3` Dark)

### Layer 3 — Affective Semantics (`EmpatiColors` via `MaterialTheme.empati`)

Each affective tone wraps `ToneColors(accent, container, onContainer, border)` accessed via `MaterialTheme.empati`:

| `MaterialTheme.empati` Property | Clinical & Affective Meaning | Base Palette | `RiskLevel` / `PresentationTone` Mapping |
|---|---|---|---|
| **`calm`** | Low risk, positive, protective habits | **Sage** (`Sage40` / `Sage95`) | `RiskLevel.LOW`, `PresentationTone.POSITIVE` |
| **`attentive`** | Moderate risk, worth monitoring | **Honey** (`Honey60` / `Honey90`) | `RiskLevel.MEDIUM`, `PresentationTone.ATTENTION` |
| **`concerned`** | High risk, warrants clinical attention | **Terracotta** (`Terra40` / `Terra95`) | `RiskLevel.HIGH` |
| **`urgent`** | Very high, prompt consultation advised | **Berry** (`Berry40` / `Berry90`) | `RiskLevel.VERY_HIGH` |
| **`neutral`** | Incomplete / neutral informational note | **Stone** (`StoneLightLow` / `StoneLightOutline`) | `PresentationTone.NEUTRAL`, `PresentationTone.INCOMPLETE` |
| **`presence`** | AI companion identity, well-being module & hotline card | **Mist** (`Mist40` / `Mist95`) | AI Avatar, `LockNotice`, `SoftNotice` SAPA 129 |

---

## 3-Voice Typography System (`Type.kt` & `res/font/`)

Implemented in [`Type.kt`](app/src/main/java/com/example/empati/ui/theme/Type.kt) using three open-licensed variable fonts bundled under `app/src/main/res/font/`:

| Typographic Voice | Bundled Font | Variable in `Type.kt` | Characteristics & Purpose |
|---|---|---|---|
| **1. Expressive & Human Voice** | **Fraunces** (`res/font/fraunces.ttf`) | `DisplayFont` | Variable soft-serif (`opsz=72`, `SOFT=100`, `WONK=0`). Reserved for human moments: time-of-day greetings, screen titles, result headlines, and `EmpatiAvatar` initials. |
| **2. Functional & Reading Voice** | **Inter** (`res/font/inter.ttf`) | `BodyFont` + `EmpatiNumeralStyle` | Humanist sans-serif with tall x-height for questionnaires, clinical summaries, chat bubbles, plus `EmpatiNumeralStyle` (`fontFeatureSettings = "tnum"`) so step counters (`3 / 16`) never jitter horizontally. |
| **3. Telemetry & Identity Voice** | **JetBrains Mono** (`res/font/jetbrains_mono.ttf`) | `MetricFont` (`EmpatiMetricStyle`) | Monospace `13sp/18sp` used **exclusively** for raw identifiers (account IDs, 18-digit NIP anatomy breakdowns, document codes)—**never** for UI labels or badges. |

### Typography Scale (`EmpatiTypography`)

| Material 3 Style | Font | Weight | Size / Line Height | Letter Spacing | Primary Usage |
|---|---|---|---|---|---|
| `displayLarge` | Fraunces (`DisplayFont`) | Normal | `48sp / 54sp` | `-0.8sp` | Hero display |
| `displayMedium` | Fraunces (`DisplayFont`) | Normal | `40sp / 46sp` | `-0.6sp` | Medium display |
| `displaySmall` | Fraunces (`DisplayFont`) | Medium | `34sp / 40sp` | `-0.4sp` | App brand title on Splash & Auth |
| `headlineLarge` | Fraunces (`DisplayFont`) | Medium | `30sp / 38sp` | `-0.3sp` | Home greeting, `ScreenHeader`, `ResultHero` |
| `headlineMedium` | Fraunces (`DisplayFont`) | Medium | `26sp / 34sp` | `-0.2sp` | Questionnaire prompts (≤90 chars), Onboarding slide titles, sheet titles |
| `headlineSmall` | Fraunces (`DisplayFont`) | Medium | `22sp / 30sp` | `-0.1sp` | Long questionnaire prompts (>90 chars), primary status cards |
| `titleLarge` | Inter (`BodyFont`) | SemiBold | `20sp / 28sp` | `-0.15sp` | Dialog headings & major subsections |
| `titleMedium` | Inter (`BodyFont`) | SemiBold | `16sp / 24sp` | `0sp` | Card titles, Profile display name, Chat header title |
| `titleSmall` | Inter (`BodyFont`) | SemiBold | `14sp / 20sp` | `0.05sp` | `ModuleStatusTile` titles, consent list headings |
| `bodyLarge` | Inter (`BodyFont`) | Normal | `16sp / 26sp` | `0sp` | Primary clinical narrative (~1.6x line-height), answer options, form inputs |
| `bodyMedium` | Inter (`BodyFont`) | Normal | `14sp / 22sp` | `0.05sp` | Recommendations, chat bubbles, card descriptions |
| `bodySmall` | Inter (`BodyFont`) | Normal | `12sp / 18sp` | `0.1sp` | Supporting copy, disclaimers, field helper hints |
| `labelLarge` | Inter (`BodyFont`) | SemiBold | `14sp / 20sp` | `0.05sp` | `PrimaryButton`, `FindingChip`, eyebrow labels |
| `labelMedium` | Inter (`BodyFont`) | Medium | `12sp / 16sp` | `0.15sp` | `ToneBadge`, `RiskBadge`, `SectionChip`, `SectionLabel`, bottom nav labels |
| `labelSmall` | Inter (`BodyFont`) | Medium | `11sp / 16sp` | `0.2sp` | Subtle metadata |
| `EmpatiNumeralStyle` | Inter (`BodyFont` + `tnum`) | SemiBold | `12sp / 16sp` | `0.2sp` | Questionnaire step index (`3 / 16`) with tabular digits |
| `EmpatiMetricStyle` | JetBrains Mono (`MetricFont`) | Medium | `13sp / 18sp` | `0sp` | Account ID values in Profile & 18-digit NIP anatomy inspector |

---

## Design Tokens: Spacing, Elevation, Shapes & Motion (`Tokens.kt`)

Defined centrally in [`Tokens.kt`](app/src/main/java/com/example/empati/ui/theme/Tokens.kt):

### 1. Spacing & Layout (`EmpatiSpace` — 4dp Grid)
- **Spacing Scale**: `xxs = 4.dp`, `xs = 8.dp`, `sm = 12.dp`, `md = 16.dp`, `lg = 20.dp`, `xl = 24.dp`, `xxl = 32.dp`, `huge = 40.dp`, `giant = 56.dp`.
- **Semantic Dimensions**:
  - `gutter = 24.dp` (horizontal screen margin)
  - `readableWidth = 560.dp` (max line length for tablet & landscape comfort)
  - `touchTarget = 48.dp` (minimum touch target per Android & WCAG accessibility guidelines)
  - `cardPadding = 24.dp`, `cardPaddingCompact = 18.dp`

### 2. Icon Sizes (`EmpatiIcon`) & Elevation (`EmpatiElevation`)
- `EmpatiIcon`: `sm = 16.dp`, `md = 20.dp`, `lg = 24.dp`, `xl = 32.dp`.
- `EmpatiElevation`: `resting = 0.dp`, `raised = 3.dp`, `floating = 10.dp`.
- **`Modifier.softShadow`**: Subtle ambient shadow tinted with `MaterialTheme.empati.shadowTint` (`Sage10` transparent in Light Mode; automatically omitted in Dark Mode where depth uses surface luminance).

### 3. Pebble Shapes (`EmpatiShapes` in `Theme.kt`)
- `extraSmall = 10.dp`, `small = 14.dp`, `medium = 20.dp`, `large = 28.dp`, `extraLarge = 36.dp`, plus `CircleShape` (full pill) for primary buttons, chips, and badges.

### 4. Accessible Motion System (`EmpatiMotion` & `LocalReducedMotion`)
- **Durations**: `Instant = 90ms`, `Quick = 160ms`, `Standard = 280ms`, `Expressive = 520ms`, `Reveal = 720ms`.
- **Easing Curves**: `EaseOutSoft = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)` (soft deceleration without bounce) and `EaseInOutSoft = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)`.
- **Physics Springs**: `EmpatiMotion.press()` (`DampingRatioNoBouncy`, `StiffnessMedium`) and `EmpatiMotion.settle()` (`StiffnessLow`).
- **`LocalReducedMotion` Support**: Observes system `Settings.Global.TRANSITION_ANIMATION_SCALE` via `rememberSystemReducedMotion()`. When users enable *Remove animations*, decorative loops (`Reveal`, `StateOrb`, `EmpatiLogo`, `BreathingDots`) settle cleanly into their resting state.

---

## Reusable Component Library (`Components.kt`)

Located in [`Components.kt`](app/src/main/java/com/example/empati/ui/components/Components.kt):

- **Surfaces & Tones**:
  - `EmpatiSurfaces.card()`, `EmpatiSurfaces.inset()`, `EmpatiSurfaces.sunken()`, `EmpatiSurfaces.hairline()`.
  - `RiskLevel.tone()`, `PresentationTone.toneColors()`, `errorTone()`.
- **Screen Scaffolding**:
  - `ScreenFrame(insets, scrollable, horizontalPadding)`: Standard page canvas constrained to `widthIn(max = 560.dp)` with `24.dp` padding.
  - `ScreenTopBar(onBack, trailing)` & `ScreenHeader(title, eyebrow, subtitle)`.
- **Cards, Badges & Chips**:
  - `TonalCard`: `28.dp` radius container (`MaterialTheme.shapes.large`) with `softShadow`, `hairline()` border, and optional `pressScale` + `focusHalo`.
  - `ToneBadge` & `RiskBadge`: Affective status pills with semantic dot (`tone.accent`) and sentence-case `labelMedium` typography.
  - `SectionChip`, `SectionLabel`, `SoftIconTile`, `ChoiceChip` (filter/radio selection chip with animated checkmark).
  - `SegmentedSwitch`: Two-option segmented control with an animated gliding pill thumb (`animateDpAsState`).
- **Buttons & Input Controls**:
  - `PrimaryButton`: Full-pill primary CTA (`56.dp`) with `softShadow`, `loading` state (`CircularProgressIndicator`), and optional arrow icon.
  - `GhostButton`, `BackIconButton` (`48.dp` circle), `VisibilityToggle` (PIN show/hide).
  - `EmpatiTextField`: `inset()` field with autofill support (`ContentType`), `2.dp` focus halo, center-expanding bottom indicator (`animateFloatAsState`), and `LiveRegionMode.Polite` validation errors.
- **Notices & Brand Identity**:
  - `SoftNotice` (calm notice banner with optional `liveRegion`) & `LockNotice` (on-device confidentiality strip for sensitive GBV prompts).
  - `EmpatiLogo(size, animated, withBackground)`: Multi-density app mark (`mdpi`–`xxxhdpi`) available with transparent background (for `SplashScreen` / `AuthScreen`) or solid background (for `CompanionAvatar` / `CompanionMark` in `ChatScreen` and launcher icons).
  - `EmpatiAvatar` (user initials set in Fraunces inside `primaryContainer`), `StateOrb`, `BreathingDots`, and `Reveal`.

---

## 11-Screen Application Specification (`ui/screens/`)

### 1. Splash Screen ([`SplashScreen.kt`](app/src/main/java/com/example/empati/ui/screens/SplashScreen.kt))
- Renders `EmpatiLogo(size = 112.dp, animated = true)` above twin `MaterialTheme.empati.aura` rings, brand headline (`displaySmall` Fraunces), and a calm single-line tagline via `Reveal`.

### 2. Onboarding ([`OnboardingScreen.kt`](app/src/main/java/com/example/empati/ui/screens/OnboardingScreen.kt))
- 3-step `HorizontalPager` with per-slide affective tones (`SlideTone.Calm`, `Attentive`, `Presence`), tabular step indicator, Skip action, layered breathing artwork (`OnboardingArtwork`), spring pagination pills (`8.dp ↔ 28.dp`), and compact/landscape adaptive layout.

### 3. Multi-Format Authentication Portal ([`AuthScreen.kt`](app/src/main/java/com/example/empati/ui/screens/AuthScreen.kt))
- Uses `@Stable` `AuthFormState` (clean production state with no pre-filled demo credentials) and `SegmentedSwitch` to toggle between Sign In and Register.
- `EmpatiTextField` accepts **Email**, **Phone Number**, **Username**, **16-digit NIK**, or **18-digit NIP**, with automatic real-time format confirmation (`SoftNotice` — parsing birth date and gender from 18-digit ASN NIPs).
- Registration mode adds `ChoiceChip` selectors for gender and background, encryption notices, and the primary CTA.

### 4. Home Dashboard ([`HomeScreen.kt`](app/src/main/java/com/example/empati/ui/screens/HomeScreen.kt))
- Time-of-day greeting (`greetingForHour`), user name (`headlineLarge` Fraunces), and `EmpatiAvatar` linking to Profile.
- **Risk Status Card (`LatestScreeningCard` / `EmptyScreeningCard`)**: Displays qualitative `RiskBadge`, plain-language summary, quick re-screen CTA, and History link.
- Quick-access service cards (Start Screening & Empati Consultation) plus the privacy & **SAPA 129** referral card.

### 5. Informed Consent ([`ConsentScreen.kt`](app/src/main/java/com/example/empati/ui/screens/ConsentScreen.kt))
- Displays `ScreenHeader`, five expandable ethical & privacy commitments inside `TonalCard` rows with `SoftIconTile` (`animateContentSize`), and a pinned bottom confirmation button.

### 6. Dual-Module Questionnaire ([`QuestionnaireScreen.kt`](app/src/main/java/com/example/empati/ui/screens/QuestionnaireScreen.kt))
- Header displays module name, `EmpatiNumeralStyle` tabular step counter (`3 / 16`), and animated progress bar with `progressBarRangeInfo` semantics.
- One question per screen (`headlineMedium` / `headlineSmall` Fraunces) with horizontal slide transitions (`AnimatedContent`) and `LockNotice` on sensitive GBV prompts.
- Answer options (`OptionCard`) carry `Role.RadioButton` semantics, `pressScale`, and `focusHalo`.

### 7 & 8. Module Results & Integrated Analysis ([`ResultScreen.kt`](app/src/main/java/com/example/empati/ui/screens/ResultScreen.kt))
- **`ModuleResultScreen` (`ModuleResultContent`)**: `ResultHero` with `StateOrb` (`148.dp`), `ToneBadge`, Fraunces conclusion, interactive `FindingChip` flow triggering `FindingDetailSheet` (`ModalBottomSheet`), `StrengthsCard` (`calm`), conditional `SoftNotice`, and `NextStepsCard`.
- **`IntegratedResultScreen` (`IntegratedResultContent`)**: Combined category `ResultHero`, side-by-side `ModuleStatusTile` columns (Hypertension & Well-being), non-causal chronic-stress explanation `SoftNotice`, key factors, strengths, next steps, plus Save to History & Ask Empati CTAs.

### 9. Empati Consultation Chatbot ([`ChatScreen.kt`](app/src/main/java/com/example/empati/ui/screens/ChatScreen.kt))
- Clean presence header showing `CompanionAvatar` with pulsing `StatusDot` and brand title (without cluttering user UI with raw model-switching badges).
- **IME-Aware Keyboard & Inset Ergonomics**:
  - `ChatContent` applies `Modifier.imePadding()` directly so `ChatComposer` sits cleanly above the soft keyboard.
  - When `WindowInsets.isImeVisible == true`, `MainScaffold` hides `BottomNavBar` to maximize vertical reading area.
  - `LaunchedEffect(isImeVisible)` auto-scrolls `LazyColumn` to the newest message when the keyboard opens.
- **Full Copy Support (`SelectionContainer` & `ContentCopy`)**:
  - Message text is wrapped in `SelectionContainer` for partial selection, paired with one-tap `Icons.Rounded.ContentCopy` on each bubble and draft copying on `ChatComposer`.
- Warm scope banner, `BreathingDots` typing indicator, quick-reply chips, and floating composer with `softShadow`.

### 10. Screening History ([`HistoryScreen.kt`](app/src/main/java/com/example/empati/ui/screens/HistoryScreen.kt))
- Chronological evaluation timeline displaying aligned `RiskBadge`, timestamps, qualitative summaries, and an empty state CTA.

### 11. Profile & Privacy ([`ProfileScreen.kt`](app/src/main/java/com/example/empati/ui/screens/ProfileScreen.kt))
- User identity card (`EmpatiAvatar(size = 72.dp)`, Fraunces name, demographic chips, domicile), editable demographic rows (`EmpatiMetricStyle` for account ID), security settings (`FLAG_SECURE` toggle/info), sign-out, and full local data deletion zone.

---

## Navigation & Transitions (`EmpatiApp.kt` & `MainScaffold.kt`)

- **Hierarchical Navigation (`EmpatiApp.kt`)**: Uses `AnimatedContent` keyed on `screenRank` (0 = Splash through 7 = IntegratedResult) with `EmpatiMotion.expressive()` horizontal slides (`it / 14` + fade), automatically falling back to crossfade under `LocalReducedMotion`.
- **Bottom Navigation (`MainScaffold.kt` → `BottomNavBar`)**: Surface-tinted `EmpatiSurfaces.card()` bar with `SoftDivider()`, active `primaryContainer` pill, `pressScale` + `focusHalo`, and 5 tabs (Home, Screening, Consultation, History, Profile), automatically hidden on Chat while the IME keyboard is visible.

---

## Accessibility (WCAG 2.2) & Visual Safety

| Aspect | Implementation in Codebase |
|---|---|
| **Color Contrast (WCAG AAA)** | `StoneLightOnSurface` (`#1B1C19`) on `StoneLightBackground` (`#F7F5F0`) achieves **14.3:1**; `StoneLightOnVariant` (`#474B44`) achieves **7.6:1**. |
| **Minimum Touch Targets** | `EmpatiSpace.touchTarget = 48.dp` and `minimumInteractiveComponentSize()` applied across buttons, chips, and nav tabs. |
| **Keyboard / D-Pad Focus (WCAG 2.4.7)** | `Modifier.focusHalo` renders a `2.dp` `primary` focus ring on custom interactive elements. |
| **IME & Chat Keyboard Ergonomics** | `Modifier.imePadding()` lifts the composer above the keyboard; `isImeVisible` auto-scrolls the timeline. |
| **Text Selection & Clipboard** | `SelectionContainer` inside bubbles, one-tap `ContentCopy` per message, and draft copy button with toast feedback. |
| **Screen Readers & Live Regions** | `LiveRegionMode.Polite` on `EmpatiTextField` errors and `SoftNotice`; `Role.RadioButton` & `ProgressBarRangeInfo` in questionnaires; `mergeDescendants = true` on form fields. |
| **Reduced Motion** | `LocalReducedMotion` respects Android system animation settings by disabling ambient loops and slide motion. |
| **Optional Screen Security (`FLAG_SECURE`)** | `MainActivity.ENABLE_SCREENSHOT_PROTECTION = false` permits screenshots/recordings for clinical/demo capture while retaining `FLAG_SECURE` opt-in code in `MainActivity.kt`. |

---

## File References

| File | Description |
|---|---|
| [`Color.kt`](app/src/main/java/com/example/empati/ui/theme/Color.kt) | "Soft Daylight" v4 color palette (Sage, Clay, Mist, Honey, Terracotta, Berry, Stone Light/Dark) |
| [`Theme.kt`](app/src/main/java/com/example/empati/ui/theme/Theme.kt) | `LightColorScheme`, `DarkColorScheme`, `EmpatiColors` (`MaterialTheme.empati`), `EmpatiShapes`, and `EmpatiTheme` |
| [`Type.kt`](app/src/main/java/com/example/empati/ui/theme/Type.kt) | 3-voice typography system (`DisplayFont` Fraunces, `BodyFont` Inter + `EmpatiNumeralStyle`, `MetricFont` JetBrains Mono) |
| [`Tokens.kt`](app/src/main/java/com/example/empati/ui/theme/Tokens.kt) | `EmpatiSpace`, `EmpatiIcon`, `EmpatiElevation`, `EmpatiMotion`, `LocalReducedMotion`, and `Modifier.softShadow` |
| [`Components.kt`](app/src/main/java/com/example/empati/ui/components/Components.kt) | Reusable UI library (`ScreenFrame`, `TonalCard`, `ToneBadge`, `RiskBadge`, `StateOrb`, `EmpatiTextField`, `SegmentedSwitch`, `EmpatiLogo`, etc.) |
| [`EmpatiApp.kt`](app/src/main/java/com/example/empati/EmpatiApp.kt) | Root composable & `screenRank`-driven animated transitions |
| [`MainScaffold.kt`](app/src/main/java/com/example/empati/ui/screens/MainScaffold.kt) | Accessible custom `BottomNavBar` & tab transitions |
| [`SplashScreen.kt`](app/src/main/java/com/example/empati/ui/screens/SplashScreen.kt) | Splash view with `aura` halos and breathing `EmpatiLogo` |
| [`OnboardingScreen.kt`](app/src/main/java/com/example/empati/ui/screens/OnboardingScreen.kt) | 3-slide pager with affective `OnboardingArtwork` & adaptive layout |
| [`AuthScreen.kt`](app/src/main/java/com/example/empati/ui/screens/AuthScreen.kt) | Multi-format sign-in & registration portal (`AuthFormState`, `SegmentedSwitch`, `IdentityConfirmationCard`) |
| [`HomeScreen.kt`](app/src/main/java/com/example/empati/ui/screens/HomeScreen.kt) | Dashboard with aligned risk status card, service shortcuts, and SAPA 129 privacy notice |
| [`ConsentScreen.kt`](app/src/main/java/com/example/empati/ui/screens/ConsentScreen.kt) | Informed consent screen with expandable commitment rows & warm icon tiles |
| [`QuestionnaireScreen.kt`](app/src/main/java/com/example/empati/ui/screens/QuestionnaireScreen.kt) | Stepped questionnaire with tabular `EmpatiNumeralStyle` & full semantics |
| [`ResultScreen.kt`](app/src/main/java/com/example/empati/ui/screens/ResultScreen.kt) | `ModuleResultScreen` & `IntegratedResultScreen` with `StateOrb`, `FindingChip`, `StrengthsCard`, & `FindingDetailSheet` |
| [`ChatScreen.kt`](app/src/main/java/com/example/empati/ui/screens/ChatScreen.kt) | Empati consultation screen with `BreathingDots`, `SelectionContainer`, and complete-sentence guarantees |
| [`HistoryScreen.kt`](app/src/main/java/com/example/empati/ui/screens/HistoryScreen.kt) | Chronological evaluation list & empty state |
| [`ProfileScreen.kt`](app/src/main/java/com/example/empati/ui/screens/ProfileScreen.kt) | Demographic profile, security toggles, account switch, and permanent data wipe |
