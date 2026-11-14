# Project Analysis — v4.1.5 Pre-Lemonade Historical Snapshot

> **Historical Context Notice (AMD Lemonade Architecture):**
> This document records the architectural analysis as of release **v4.1.5** (historically titled *SRYA Emora*, using OpenRouter cloud routing + legacy Ollama `:11434`). For the **current** neuro-symbolic AMD Lemonade Challenge architecture—including **AMD Lemonade Server (`:13305`)**, `LemonadeUnifiedClient`, local `Qwen3-4B` / `Qwen3-1.7B` chat, `nomic-embed-text-v1` cosine vector RAG (`EmpatiVectorRag`, `30` chunks), `Whisper-Large-v3-Turbo` STT, `kokoro-v1` TTS + Android offline fallback, and `203` verified unit tests across `24` suites—please refer to:
> - [`README.md`](README.md)
> - [`docs/adr/001-lemonade-hybrid-architecture.md`](docs/adr/001-lemonade-hybrid-architecture.md)
> - [`docs/specs/lemonade-hybrid-triage.md`](docs/specs/lemonade-hybrid-triage.md)

---

## 1. Overview (v4.1.5 Baseline)

**SRYA Emora** (preceded by *AI-EMPATI*, now branded **Empati**) is an Android application built with Jetpack Compose, Material 3, and the **"Soft Daylight" v4.1** design system for hypertension risk screening and psychosocial well-being support across two modules: Hypertension Risk (`M01`–`M16`) and Gender-Based Violence / GBV (`K01`–`K10`). The v4.1.5 release combined:

- **Deterministic Expert System** based on *Certainty Factor* (CF) scoring—kept strictly internal for dissertation research.
- **Presentation Layer** (`UserResultPresenter`) translating CF outputs into empathetic layperson summaries—the only representation rendered to users.
- **Scoped Clinical Chatbot** originally connected to **OpenRouter Cloud LLM** (via **Supabase Edge Function `empati-chat`** with `openrouter/free` smart routing, optional local Ollama support, and cloud fallback), enforced with strict topic scoping (Health, Psychology, Violence Protection) and privacy guardrails. *(Superseded by local-first AMD Lemonade Server `:13305` in the current codebase.)*
- **Inclusive Multi-Format Authentication** (Email, Phone Number, Username, 16-digit NIK, or 18-digit NIP + PBKDF2 PIN) paired with **Hybrid Storage: Local SQLite + Pseudonymized Cloud Sync (`empati-research`)**.
- **Multi-Density Brand Assets**: Dual logo variants (transparent for Splash/Auth/adaptive foreground, solid background for chat avatars and launcher icons).

---

## 2. Package Architecture (v4.1.5 Snapshot)

```text
com.example.empati
├── MainActivity.kt              # ComponentActivity, edge-to-edge, optional FLAG_SECURE
├── EmpatiApp.kt                 # Root composable + routing (Screen sealed interface)
├── EmpatiViewModel.kt           # State & navigation; internal ↔ presentation bridge
├── data
│   ├── Models.kt                # ScreeningResult, RiskLevel, UserProfile, ChatMessage
│   ├── ScreeningStore.kt        # Session, history, chat logs, research records
│   ├── ChatEngine.kt            # Chat reply orchestration
│   ├── ScreeningEngine.kt       # (legacy) prior scoring engine
│   ├── ScreeningQuestions.kt    # (legacy) 24-question bank
│   ├── auth/AsnNipValidator.kt  # NIP / multi-format identity validation
│   ├── db/EmpatiDatabase.kt     # Local SQLite accounts, hashed credentials, sessions
│   ├── cf/                      # Certainty Factor Expert System (INTERNAL)
│   │   ├── CfModels.kt
│   │   ├── CfQuestions.kt        # M01–M16, K01–K10
│   │   ├── CfInputParser.kt      # CF_User (dichotomous / Likert / reverse M08)
│   │   ├── CfKnowledgeBase.kt    # CfRule (MB/MD) — placeholder NEEDS_EXPERT_VALIDATION
│   │   ├── CfInferenceEngine.kt  # positive / negative / conflicting CF combination
│   │   └── CfResultGenerator.kt  # ModuleResult + IntegratedAnalysis
│   ├── presentation/            # User-facing models (ZERO CF leakage)
│   │   ├── UserPresentation.kt
│   │   └── UserResultPresenter.kt
│   ├── research/ResearchModels.kt  # ResearchRecord (internal dissertation telemetry)
│   ├── cloud/SupabaseCloudSyncService.kt # Supabase sync (research_cf_records & research_chat_logs)
│   └── rag/
│       ├── OllamaRagService.kt     # Legacy v4.1.5 LLM client (later replaced/paired with Lemonade)
│       ├── WebRagSearchService.kt  # Web RAG: Wikipedia ID + Europe PMC
│       └── RagKnowledgeRepository.kt # Local corpus, topic scoping, GBV crisis detection
├── supabase/
│   ├── migrations/20261108000001_create_empati_research_schema.sql # Postgres schema & write-only RLS
│   └── functions/empati-chat/index.ts # Edge Function OpenRouter proxy (v4.1.5)
└── ui
    ├── theme/  (Color, Type, Theme, Tokens)   # "Soft Daylight" v4 design system
    ├── components/Components.kt               # Reusable UI components
    └── screens/  (11 screens)
```

**Layering Principle:** UI components read only `presentation/` models. CF scores, MB/MD values, weights, and evidence/rule IDs stay confined within `data/cf/` and `data/research/` and never render to the end user.

---

## 3. Frontend / UI-UX (v4.1.5 Baseline)

### 3.1 "Soft Daylight" Design System (v4)

Unidirectional three-layer color pipeline: **Palettes** (raw tonal ramps) → **Roles** (Material 3 `ColorScheme`) → **Semantics** (`MaterialTheme.empati`, affective tones).

Core hues: **Sage** (calm/brand) · **Clay** (human warmth) · **Mist** (AI presence) · **Stone** (warm green-tinted neutrals). Light theme = "daylight on paper" (`#F7F5F0`); Dark theme = "quiet evening" (`#121512`).

### 3.2 Typography (`ui/theme/Type.kt`)

| Role | Font | Usage |
|---|---|---|
| `DisplayFont` | **Fraunces** (serif, optical-size, SOFT/WONK axes) | greetings, headlines, result statements, app title |
| `BodyFont` | **Inter** | headings, body narrative, labels, buttons, chips |
| `MetricFont` | **JetBrains Mono** | ONLY raw telemetry: account IDs, NIP breakdowns, citation codes |

Scale: `displayLarge 48/54` through `labelSmall 11/16`. `EmpatiMetricStyle` (13sp mono) for identifiers; `EmpatiNumeralStyle` (Inter tabular `tnum`) so live counters never shift horizontally. Labels use sentence-case rather than ALL-CAPS.

> Note: Earlier prototype specs referenced **Lora**, which was replaced by **Fraunces** in v4.

### 3.3 Tokens (`ui/theme/Tokens.kt`)

- **Space** (4dp grid): `xxs 4 · xs 8 · sm 12 · md 16 · lg 20 · xl 24 · xxl 32 · huge 40 · giant 56`; `gutter 24`; `readableWidth 560`; `touchTarget 48`.
- **Icon**: `sm 16 · md 20 · lg 24 · xl 32`.
- **Elevation**: `resting 0 · raised 3 · floating 10` (predominantly tonal surfaces; drop shadows reserved for floating controls).
- **Motion**: `Instant 90 · Quick 160 · Standard 280 · Expressive 520 · Reveal 720`; easing `EaseOutSoft (0.22,1,0.36,1)`; `press()` spring; `settle()` spring.
- **Shapes** (`EmpatiShapes`): `xs 10 · sm 14 · md 20 · lg 28 · xl 36`.
- **Reduced Motion**: `LocalReducedMotion` + `rememberSystemReducedMotion()` honoring Android "Remove animations".
- `Modifier.softShadow(elevation, shape, tint)` — soft brand-tinted ambient shadow.

### 3.4 Affective Colors & Tones (`MaterialTheme.empati`)

| Token | Meaning | Accent (Light) |
|---|---|---|
| `calm` | low risk / positive / protective | Sage40 `#2E6A57` |
| `attentive` | moderate risk / worth monitoring | Honey60 `#D29A2E` |
| `concerned` | high risk / needs clinical attention | Terra40 `#A8402B` |
| `urgent` | very high / seek prompt help | Berry40 `#8E2F55` |
| `neutral` | incomplete / informational | Stone outline |
| `presence` | AI companion identity (chat, thinking state) | Mist40 `#5A5896` |

Neutrals: `#F7F5F0` light background, `#121512` dark background. Text contrast meets ≥ 4.5:1 (primary body ≥ 14.3:1 WCAG AAA); control outlines meet ≥ 3:1.

### 3.5 Screen Map & Navigation

Navigation uses `Screen` (sealed interface) + `backStack` inside `EmpatiViewModel` rather than Navigation-Compose. Initial flow: `Splash → (Onboarding) → Auth → Home`.

| Screen | File | Primary Content |
|---|---|---|
| Splash | `SplashScreen.kt` | logo + title + tagline, animated halo ring |
| Onboarding | `OnboardingScreen.kt` | 3 swipeable affective slides |
| Auth | `AuthScreen.kt` | Sign In/Register switch, real-time NIP/ID validation, show/hide PIN, progressive disclosure, privacy notice |
| Home | `HomeScreen.kt` | greeting, qualitative risk status card, two feature shortcuts |
| Consent | `ConsentScreen.kt` | informed consent commitments, privacy icon |
| Dual-Module Questionnaire | `QuestionnaireScreen.kt` | one question per screen, progress bar, category chip, large option cards, sensitive notice |
| Module Results | `ResultScreen.kt` (`ModuleResultScreen`) | layperson conclusion, factors, guidance (NO CF scores) |
| Integrated Results | `ResultScreen.kt` (`IntegratedResultScreen`) | layperson category, dual-module summary, recommendations |
| Chat | `ChatScreen.kt` | presence header (`CompanionAvatar` + online dot, no technical model badge), scope notice, message bubbles, `BreathingDots`, quick replies (including SAPA 129), floating composer |
| History | `HistoryScreen.kt` | chronological timeline (date + risk badge) |
| Profile | `ProfileScreen.kt` | demographics, security toggle, sign-out, wipe all data |

Bottom navigation (`MainScaffold`): Home · Screening · Chat · History · Profile.

### 3.6 Reusable Component Library (`ui/components/Components.kt`)

`ScreenFrame`, `TonalCard`, `SoftNotice`, `SoftDivider`, `SectionLabel`, `SegmentedSwitch`, `ChoiceChip`, `EmpatiTextField`, `VisibilityToggle`, `Reveal`, `PrimaryButton`, `GhostButton`, `EmpatiLogo`, `BreathingDots`, `EmpatiSurfaces`, `focusHalo`, `pressScale`, `errorTone`, `RiskBadge`, `BackIconButton`, `LockNotice`, `EmpatiAvatar`.

### 3.7 Accessibility & Display Privacy

- **Screenshots & Screen Recording Permitted (`ENABLE_SCREENSHOT_PROTECTION = false`)**: Allowed by default while keeping opt-in `FLAG_SECURE` code intact inside `MainActivity.kt`.
- Minimum touch target ≥ 48dp via `minimumInteractiveComponentSize()`.
- `liveRegion` on important notices; `semantics { heading() }` on titles.
- Sensitive GBV outcomes are framed in warm, non-judgmental lay language with zero internal rule/weight leakage.

---

## 4. Certainty Factor Expert System (INTERNAL)

- **Module 1 Hypertension**: `M01`–`M16` (Medical History, Lifestyle, Risk Factors).
- **Module 2 GBV**: `K01`–`K10` (Physical, Psychological, Sexual, Economic, Digital).
- **CF_User**: dichotomous Yes=1.0 / No=0.0; Likert 1..5 → 0.0/0.2/0.5/0.8/1.0; `M08` reverse-scored.
- **CF evidence** = `CF_User × (MB − MD)`; positive/negative/conflicting combination via standard MYCIN/Shortliffe & Buchanan formulas.
- **Risk Thresholds**: `<0.25` Low · `0.25–0.49` Moderate · `0.50–0.74` High · `≥0.75` Very High.
- **Placeholder Knowledge Base**: Weights carry `NEEDS_EXPERT_VALIDATION` markers awaiting clinical dissertation validation.

---

## 5. Presentation Layer (Research vs. User Separation)

`UserResultPresenter` maps `ModuleResult` / `IntegratedAnalysis` → `UserModulePresentation`, `UserIntegratedPresentation`, and `UserRiskSummary`. Zero CF/MB/MD/weights/rule IDs pass through. Layperson integrated categories: (1) Health & Lifestyle Factors Dominant, (2) GBV Stress-Pathway Indication, (3) Low Risk, (4) GBV Without Elevated Hypertension Risk, (5) Inconclusive.

---

## 6. Auth, Local Storage (SQLite) & Cloud Sync (Supabase)

- **Local (`EmpatiDatabase` & `ScreeningStore`)**: Stores user accounts with salted PBKDF2-HMAC-SHA256 credentials (16-byte salt), active session flag, cloud sync status, qualitative summaries, chat logs, and `ResearchRecord` entries. `AsnNipValidator` validates Email, Phone, Username, 16-digit NIK, or 18-digit NIP.
- **Cloud (`SupabaseCloudSyncService`)**: Uploads pseudonymized research payloads (`subject_hash`, zero raw PII) to `public.research_cf_records` and `public.research_chat_logs` (with `public.user_accounts` baseline tracking in the v4.1.5 snapshot) asynchronously on `Dispatchers.IO`.

---

## 7. v4.1.5 Chatbot Routing Pipeline (Historical OpenRouter + Ollama Flow)

> Note: In the current **AMD Lemonade Challenge** build, chat inference, vector embeddings, STT, and TTS run locally via `LemonadeUnifiedClient` on `http://<host>:13305/api/v1/*`. Below reflects the earlier v4.1.5 routing order before that migration.

1. **Privacy Redaction (`sanitizePrivacyInput`)**: Masks NIP/NIK/email/phone numbers before transmission or logging.
2. **Topic Scoping**: Restricted to **Health**, **Psychology**, and **Violence Protection**; out-of-scope prompts are politely declined before invoking an LLM.
3. **v4.1.5 Routing Order**: Direct OpenRouter API (optional) → Supabase Edge Function `empati-chat` (`openrouter/free` → `llama-3.3-70b-instruct:free` → `gemma-3-12b-it:free` → `qwen3-8b:free`) → Local Ollama `:11434` → Offline Deterministic Synthesizer.
4. **Output Guardrails (`enforceOutputGuardrails` & `ensureCompleteResponse`)**: Strips leaked CF/MB/MD/%/decimals, appends confidential **SAPA 129** contact info on GBV topics, and trims incomplete trailing fragments.

---

## 8. Privacy & Security Guarantees

- Optional `FLAG_SECURE` preserved in `MainActivity.kt` (`ENABLE_SCREENSHOT_PROTECTION = false` by default).
- Strict input PII redaction before network requests or research logging.
- Zero-leak guardrails across both UI presentation and LLM responses.
- Research records separated from clinical presentation and pseudonymized with one-way `subject_hash` (`sub_<20-hex>`).
- Full account/data deletion clears both local SQLite data and remote user account records.

---

## 9. Summary of v4.0.0 Evolution

| Dimension | Initial Version | v4.0.0–v4.1.5 Architecture |
|---|---|---|
| Palette | Mauve + Teal | Soft Daylight: Sage/Clay/Mist/Stone + affective tones (`MaterialTheme.empati`) |
| Fonts | Lora | 3-Voice: Fraunces + Inter (`tnum`) + JetBrains Mono |
| Chatbot | Rule-based / Local Ollama | OpenRouter via Supabase Edge Function *(now AMD Lemonade `:13305`)* |
| Model Badge in UI | Visible (`"Qwen 2.5 LLM"`) | Hidden in user UI; logged in backend telemetry only |
| Auth | Female ASN NIP only | Inclusive multi-format (Email, Phone, Username, NIK, NIP) + PBKDF2 |
| Database | SharedPreferences / Local SQLite | Hybrid SQLite + Supabase Write-Only RLS (`empati-research`) |
| Build & Deploy | Debug | Release-ready (`assembleRelease` with R8/ProGuard) |

---

## 10. Historical Test Baseline (v4.1.5 vs. Current)

- **At v4.1.5 snapshot**: 49 JVM unit tests (`CfEngineTest`, `UserPresentationTest`, `AsnAndRagArchitectureTest`, `ExampleUnitTest`) + 6 instrumented tests.
- **Current AMD Lemonade main branch**: Expanded to **203 unit tests across 24 JUnit suites** (all passing on `testDebugUnitTest`; see [`README.md`](README.md#testing) for exact commands and verification output).
