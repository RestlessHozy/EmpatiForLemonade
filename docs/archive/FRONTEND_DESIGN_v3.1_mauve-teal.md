# [Historical Archive v3.1] — Front-End Design Guide (Mauve + Teal Palette)

> **Superseded Historical Archive:** This document preserves the **v3.1** Material You design guide (which used the dusty plum `Mauve` + `Teal` palette, `Plus Jakarta Sans`, and `FLAG_SECURE` enabled globally). In **v4.0+**, the palette and typography migrated to **"Soft Daylight"** (`Sage` / `Clay` / `Mist` / `Stone`) paired with **Fraunces + Inter + JetBrains Mono**. See [`FRONTEND_DESIGN.md`](../../FRONTEND_DESIGN.md) and [`TYPOGRAPHY_AND_CLINICAL_UX.md`](../../TYPOGRAPHY_AND_CLINICAL_UX.md) for the active design system.

---

## Table of Contents

- [Design Principles](#design-principles)
- [Design System](#design-system)
  - [Color Palette & Tonal Hierarchy](#color-palette--tonal-hierarchy)
  - [Editorial Typography](#editorial-typography)
  - [Spacing & Sizing](#spacing--sizing)
  - [Shapes](#shapes)
- [Animation & Micro-Interactions](#animation--micro-interactions)
- [Reusable Components](#reusable-components)
- [Application Screens](#application-screens)
- [Navigation](#navigation)
- [Privacy & Visual Security](#privacy--visual-security)
- [Accessibility](#accessibility)
- [Historical File References](#historical-file-references)

---

## Design Principles

1. **Native Android Material You (Strictly Non-iOS)** — Uses authentic Material 3 components such as `NavigationBar` with pill indicator, `SuggestionChip`, and tonal surface hierarchy (`surfaceContainerLow` to `surfaceContainerHigh`).
2. **Zero "AI Slop"** — Avoids decorative gradient backgrounds, heavy drop shadows, neon glow effects, and decorative `01/02/03` markers on non-sequential content.
3. **Mauve / Dusty Plum & Teal Palette** — Avoids cliché pink palettes for women's health, pairing calm `Mauve` with `Teal` progress and safe-space indicators.
4. **Strict Research vs. User Presentation Isolation** — Internal technical metrics (*Certainty Factor*, MB/MD, weights, formulas, rule IDs) never render in user UI.

---

## Design System

### Color Palette & Tonal Hierarchy

Semantic Material 3 color roles across Light and Dark surfaces:

#### Primary Roles

| Role | Token (v3.1) | Hex | Usage |
|---|---|---|---|
| **Primary** | `Mauve500` | `#7B4F97` | Primary buttons, active indicators, primary accents |
| **Primary Container** | `Mauve100` | `#EADFF2` | Active tab pill, primary chips, icon containers |
| **Secondary** | `Teal600` / `Teal500` | `#21857B` / `#2E9C8F` | GBV progress, online status dot, protective indicators |
| **Secondary Container** | `Teal50` | `#EAF6F4` | Confidential support strip, `LockNotice` banner |
| **Tertiary** | `Amber600` / `Amber500` | `#B57E1C` / `#D99A2B` | Moderate risk, worth-monitoring status |
| **Error** | `Coral600` | `#C64F3D` | High risk, destructive data-wipe zone |

#### Neutral Tonal Surfaces

| Mode | Token (v3.1) | Hex | Material 3 Role |
|---|---|---|---|
| **Light** | `WarmOffWhite` | `#FBF9F7` | `background` |
| | `SurfaceContainerLowestLight` | `#FFFFFF` | `surface` / primary cards |
| | `SurfaceContainerLowLight` | `#F3EFEA` | `surfaceContainerLow` (icon boxes, chat input, banners) |
| | `SurfaceLight` | `#F7F4F0` | `surfaceContainer` |
| | `SurfaceVariantLight` | `#ECE6DF` | `surfaceVariant` (progress track) |
| | `SurfaceContainerHighLight` | `#E5DFD7` | `surfaceContainerHigh` |
| | `TextDark` | `#221A26` | `onBackground` / `onSurface` |
| | `TextMutedDark` | `#665B70` | `onSurfaceVariant` |
| | `BorderLight` | `#E2DAD0` | `outline` |
| | `BorderSubtleLight` | `#EDE7DF` | `outlineVariant` |
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

### Editorial Typography (v3.1)

Bundled three variable OFL font families inside `app/src/main/res/font/`:
- **Heading & Clinical Authority**: `Plus Jakarta Sans` (`HeadingFont`, `res/font/plus_jakarta_sans.ttf` — superseded by `Fraunces` in v4.0).
- **Body & Clinical Reading**: `Inter` (`BodyFont`, `res/font/inter.ttf`).
- **Metadata & Diagnostic Labels**: `JetBrains Mono` (`MonoFont`, `res/font/jetbrains_mono.ttf`).

### Spacing, Sizing & Shapes

- **Horizontal gutter**: `20dp` (`28dp` on Onboarding slides); **Card padding**: `18dp–22dp`; **Card gap**: `12dp–16dp`.
- **Primary CTA height**: `54dp`; **Minimum touch target**: `48dp`.
- **Corner radii**: Hero cards `24dp`, `TonalCard` `20dp–22dp` with `1dp` outline, buttons/options `18dp`, `BackIconButton` `16dp`, chat bubbles `20dp` (tail `6dp`), pill composer `24dp`.

---

## Animation & Micro-Interactions

1. **Hierarchical Screen Transitions (`EmpatiApp.kt`)**: `AnimatedContent` driven by `screenRank` using horizontal shared-axis slide + fade (`FastOutSlowInEasing`), reversing on Back.
2. **Tab Crossfading (`MainScaffold.kt`)**: Smooth `220ms` in / `160ms` out crossfade across main tabs.
3. **Questionnaire Progression (`QuestionnaireScreen.kt`)**: Horizontal question slide, spring-animated `LinearProgressIndicator`, border/bar highlight on `OptionRow` selection, and animated Next button.
4. **Press Springs (`Components.kt`)**: Subtle `0.975f–0.985f` spring scale on interactive cards and buttons.
5. **Onboarding Parallax (`OnboardingScreen.kt`)**: Offset-based illustration interpolation and spring indicator expansion (`8dp ↔ 28dp`).
6. **Chat Timeline (`ChatScreen.kt`)**: `Modifier.animateItem()` bubble entrance and reactive Send button color transition.

---

## Reusable Components

Located in [`Components.kt`](../../app/src/main/java/com/example/empati/ui/components/Components.kt): `TonalCard`, `PrimaryButton`, `GhostButton`, `BackIconButton`, `RiskBadge`, `SectionChip`, `LockNotice`, `EmpatiLogo`, and `EmpatiAvatar`.

---

## Application Screens (v3.1)

1. **Splash (`SplashScreen.kt`)**: Animated `EmpatiLogo`, clinical badge, serif title, and smooth entrance transition.
2. **Onboarding & Multi-Format Auth (`OnboardingScreen.kt`, `AuthScreen.kt`)**: 3-step onboarding pager plus multi-format sign-in/registration supporting Email, Phone, Username, 16-digit NIK, and 18-digit NIP (with live anatomy inspector and demo quick login in v3.1).
3. **Home Dashboard (`HomeScreen.kt`)**: Greeting header, qualitative `RiskCard`, dual feature shortcuts, and safe-space SAPA 129 banner.
4. **Informed Consent (`ConsentScreen.kt`)**: Five semantic privacy/ethics commitments divided by `HorizontalDivider`.
5. **Questionnaire (`QuestionnaireScreen.kt`)**: Single-question paginated cards with animated progress and privacy banners.
6. **Module Results (`ResultScreen.kt` → `ModuleResultScreen`)**: Unified clinical summary card with `FlowRow` `ClinicalTagChip` items, `ActionPlanCard`, conditional `SupportStrip`, and `MinimalDisclaimer`.
7. **Integrated Analysis (`ResultScreen.kt` → `IntegratedResultScreen`)**: Executive summary card with 2-column `ModuleStatusCell` matrix, factor chips, action plan, and support banner.
8. **Chat (`ChatScreen.kt`)**: Minimal surface header (`BotAvatar` + online dot + connection badge), scope banner, suggestion chips, and rounded composer.
9. **History (`HistoryScreen.kt`)**: Chronological `HistoryItem` cards showing qualitative risk level and date without CF scores.
10. **Profile & Privacy (`ProfileScreen.kt`)**: Demographic overview, edit dialogs, `FLAG_SECURE` screen-protection toggle info, sign-out, and permanent data wipe confirmation.

---

## Navigation & Privacy

- **Material 3 Navigation (`MainScaffold.kt`)**: 5 tabs (*Home*, *Screening*, *Consultation*, *History*, *Profile*) using Outlined inactive and Rounded Filled active icons inside `primaryContainer` pills.
- **Visual Privacy & Accessibility**: `FLAG_SECURE` window protection (made toggleable via `ENABLE_SCREENSHOT_PROTECTION` in v4.1.2+), `LockNotice` on sensitive GBV prompts, zero CF presentation leakage (`UserPresentation.kt`), `48dp` minimum touch targets, WCAG AAA text contrast on Paper/Warm-Off-White, and single-question screen pacing.

---

## Historical File References

- [`Color.kt`](../../app/src/main/java/com/example/empati/ui/theme/Color.kt)
- [`Theme.kt`](../../app/src/main/java/com/example/empati/ui/theme/Theme.kt)
- [`Type.kt`](../../app/src/main/java/com/example/empati/ui/theme/Type.kt)
- [`Components.kt`](../../app/src/main/java/com/example/empati/ui/components/Components.kt)
- [`EmpatiApp.kt`](../../app/src/main/java/com/example/empati/EmpatiApp.kt)
- [`MainScaffold.kt`](../../app/src/main/java/com/example/empati/ui/screens/MainScaffold.kt)
- [`SplashScreen.kt`](../../app/src/main/java/com/example/empati/ui/screens/SplashScreen.kt), [`OnboardingScreen.kt`](../../app/src/main/java/com/example/empati/ui/screens/OnboardingScreen.kt), [`AuthScreen.kt`](../../app/src/main/java/com/example/empati/ui/screens/AuthScreen.kt), [`HomeScreen.kt`](../../app/src/main/java/com/example/empati/ui/screens/HomeScreen.kt), [`ConsentScreen.kt`](../../app/src/main/java/com/example/empati/ui/screens/ConsentScreen.kt), [`QuestionnaireScreen.kt`](../../app/src/main/java/com/example/empati/ui/screens/QuestionnaireScreen.kt), [`ResultScreen.kt`](../../app/src/main/java/com/example/empati/ui/screens/ResultScreen.kt), [`ChatScreen.kt`](../../app/src/main/java/com/example/empati/ui/screens/ChatScreen.kt), [`HistoryScreen.kt`](../../app/src/main/java/com/example/empati/ui/screens/HistoryScreen.kt), [`ProfileScreen.kt`](../../app/src/main/java/com/example/empati/ui/screens/ProfileScreen.kt)
