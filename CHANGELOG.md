# Changelog — Empati (formerly SRYA Emora / AI-EMPATI)

> **Current Competition Mainline (`AMD Lemonade Developer Challenge 2026`):**
> Beyond the historical **v1.0.0–v4.1.5** release log preserved below, the current repository has been migrated and rebranded to **Empati** for local-first neuro-symbolic inference on **AMD Lemonade Server (`:13305`)**:
> - Unified OpenAI-compatible client (`LemonadeUnifiedClient`: `Qwen3-4B` / `Qwen3-1.7B` chat, `nomic-embed-text-v1` embeddings, `Whisper-Large-v3-Turbo` STT, `kokoro-v1` TTS).
> - Hybrid dense vector + lexical RAG (`EmpatiVectorRag`) over 30 evidence chunks (`EmpatiCorpus`), plus Indonesian offline TTS fallback (`IndonesianSpeechSynthesizer`) and push-to-talk STT (`VoiceInputManager`).
> - Competition offline mode (`ENABLE_CLOUD_SYNC = false`, zero external network dependency when connected to Lemonade `:13305`), automated PowerShell setup & benchmarking (`scripts/`), **203 passing unit tests across 24 suites**, and recorded English demo video (`Empati-Demo-EN.mp4`).
> - See [`README.md`](README.md), [`docs/adr/001-lemonade-hybrid-architecture.md`](docs/adr/001-lemonade-hybrid-architecture.md), and [`docs/specs/lemonade-hybrid-triage.md`](docs/specs/lemonade-hybrid-triage.md).

---

## v4.1.5 — Composer Ergonomics Above Virtual Keyboard (`imePadding` & Auto-Hide Navbar) & Full Text Copy (`SelectionContainer` & `ContentCopy`) (2026-11-09)

*(Released under the prior title **SRYA Emora**.)* Improved typing comfort in the consultation screen so the chat composer and placeholder always remain lifted cleanly above the IME keyboard, paired with comprehensive copy actions on bubbles and draft inputs.

### 1. Composer Docked Cleanly Above Keyboard (`ChatScreen.kt` & `MainScaffold.kt`)
- **Dynamic Inset Adaptation (`Modifier.imePadding()`)**: Applied `Modifier.imePadding()` to `ChatContent` in [`ChatScreen.kt`](app/src/main/java/com/example/empati/ui/screens/ChatScreen.kt) so `ChatComposer` lifts directly above the virtual keyboard.
- **Smart Bottom Navigation Hiding (`MainScaffold.kt`)**: Hid `BottomNavBar` automatically in [`MainScaffold.kt`](app/src/main/java/com/example/empati/ui/screens/MainScaffold.kt) when `Screen.Chat` is active and `WindowInsets.isImeVisible == true`, maximizing vertical reading space.
- **Auto-Scroll on IME Open**: Added `LaunchedEffect(isImeVisible)` to scroll the chat `LazyColumn` to the latest message automatically.

### 2. Full Message & Draft Copy Support (`ChatScreen.kt`)
- **Partial Text Selection (`SelectionContainer`)**: Wrapped message bubble text in `SelectionContainer` so users can highlight phone numbers, referral details, or advice seamlessly.
- **One-Tap Message Copy (`Icons.Rounded.ContentCopy`)**: Added a dedicated copy action per bubble via `LocalClipboardManager` with calm toast feedback.
- **Draft Copy Action**: Added a quick copy action alongside Send inside `ChatComposer` whenever `value.isNotEmpty()`.

---

## v4.1.4 — Contextual Screening Analysis & Synthesis via LLM (`ScreeningContextSnapshot`) (2026-11-09)

Separated quick status checks from deep contextual clinical synthesis so users asking for analysis of their screening receive tailored medical/psychosocial explanations grounded in `ScreeningContextSnapshot`.

### 1. Intent Separation (`RagKnowledgeRepository.kt` & `ChatEngine.kt`)
- **`isSimpleResultLookupQuery(query)` vs. `isScreeningAnalysisQuery(query)`**:
  - Quick lookups return deterministic zero-leak status summaries immediately.
  - Analytical queries (*"analisis hasil screening saya"*, *"apa yang bisa anda simpulkan..."*, etc.) route to the LLM together with the user's rich qualitative screening snapshot.

### 2. Enriched `ScreeningContextSnapshot` (`UserResultPresenter.kt`, `ScreeningStore.kt`, `EmpatiViewModel.kt`, `ChatScreen.kt`)
- Included `riskLevelLabel`, `integratedCategoryLabel`, `screeningDate`, `summaryNarrative`, `hypertensionStatusLabel`, `gbvStatusLabel`, `keyFactors`, `protectiveFactors`, `relationshipExplanation`, and `keyRecommendations` persistently per account with zero CF/MB/MD leakage, plus a quick-reply shortcut chip in `ChatScreen.kt`.

---

## v4.1.3 — Supabase Cloud Sync Migration (`public.user_accounts`, `public.research_cf_records`, & Cross-Device Account Restore) (2026-11-09)

Fixed cloud synchronization permissions and cross-device account recovery in the v4.1 cloud backend:
- Applied `create_user_accounts_and_fix_research_sync_grants` with `id bigint generated always as identity primary key`, unique `account_identifier`, indexes, update trigger, and validated RLS policies.
- Enabled idempotent upsert permissions (`SELECT`, `INSERT`, `UPDATE`, RLS policies checking `subject_hash like 'sub_%'`) on `public.research_cf_records` with clean security advisor status.
- Updated `syncUserAccount()`, background `fetchUserAccountByIdentifier()`, and `syncUserAccountsToSupabase()` so local account recovery works off-main-thread.

---

## v4.1.2 — "Anti-AI-Slop" Copywriting Audit, Production Polish, Cloud Account Deletion & Optional `FLAG_SECURE` (2026-11-09)

Completed all Audit A (human-toned copywriting) and Audit B (production readiness) items while permitting screen capture/recording via a preserved toggle:
- **Screen Protection Toggle (`MainActivity.ENABLE_SCREENSHOT_PROTECTION = false`)**: Enables smooth screenshots and demo recording by default while preserving `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)` in `MainActivity.kt` for easy re-activation, synchronized with `ProfileScreen.kt`.
- **Audit A — Humanized Bahasa Indonesia Copy**: Rewrote robotic template text across `ChatScreen.kt`, `ChatEngine.kt`, `UserResultPresenter.kt`, `SplashScreen.kt`, `OnboardingScreen.kt`, `ConsentScreen.kt`, `HomeScreen.kt`, `HistoryScreen.kt`, and offline synthesizers into warm, non-judgmental everyday Indonesian while preserving zero-leak guarantees and `SAPA 129` crisis info.
- **Audit B — Privacy Notices, Account Deletion & Release Metadata**: Aligned privacy copy across onboarding/auth/profile, wired full local + remote account wipe (`deleteUserAccount`), unified client headers, and bumped release versioning (`versionCode = 5`).

---

## v4.1.1 — Demo Account Removal, Chatbot Self-Introduction & Pre-Screening Account Persistence (2026-11-09)

Prepared production clean state:
- Removed hardcoded demo credentials (`seedDefaultAsnAccount()`, demo shortcuts, pre-filled fields in `AuthFormState`, fictional default `UserProfile`) with SQLite `DB_VERSION = 4` cleanup migration.
- Added warm companion self-introduction handling (`isIdentityQuery`) and empathetic out-of-scope refusal copy.
- Persisted `updated_at`, `is_active_session`, and `cloud_synced` at registration/profile update time prior to completing any screening.

---

## v4.1.0 — Rebranding to "SRYA Emora" & Multi-Density Logo Assets (2026-11-08)

*(Historically preceded the subsequent **Empati** competition rebrand.)*
- Renamed UI titles and assistant copy to `SRYA Emora` across launcher, screens, and system prompts.
- Generated cropped, bicubic-resampled `mdpi`–`xxxhdpi` drawables (transparent and solid-background variants) plus adaptive/legacy launcher mipmaps (`ic_launcher_foreground`, `ic_launcher`, `ic_launcher_round`).

---

## v4.0.0 — "Soft Daylight" Design System, 3-Voice Typography, New Component Library & WCAG 2.2 Accessibility (2026-11-06)

Overhauled the visual front-end to **"Soft Daylight" v4.0**:
- **3-Layer Color Architecture (`Color.kt` & `Theme.kt`)**: Replaced Mauve/Teal with **Sage** (`#2E6A57`), **Clay** (`#93482E`), **Mist** (`#5A5896`), **Honey** (`#D29A2E`), **Terracotta** (`#A8402B`), **Berry** (`#8E2F55`), and **Stone** neutrals (`#F7F5F0` Light / `#121512` Dark), plus `MaterialTheme.empati` affective tokens (`calm`, `attentive`, `concerned`, `urgent`, `neutral`, `presence`, `aura`, `shadowTint`).
- **3-Voice Typography (`Type.kt` & `res/font/`)**: Bundled `fraunces.ttf` (`DisplayFont`), `inter.ttf` (`BodyFont` + tabular numerals `tnum`), and `jetbrains_mono.ttf` (`MetricFont` strictly for raw IDs).
- **Motion & Component Library (`Tokens.kt` & `Components.kt`)**: Introduced `EmpatiSpace`, `EmpatiMotion`, `LocalReducedMotion`, `Modifier.softShadow`, `Modifier.focusHalo`, `ScreenFrame`, `StateOrb`, `ToneBadge`, `SegmentedSwitch`, `ChoiceChip`, `EmpatiTextField`, `SoftNotice`, `BreathingDots`, `Reveal`, and stateless `*Content` composables.
- **Production Cloud & Release Build**: Added OpenRouter proxy Edge Function `empati-chat` (Free-Tier Smart Router), write-only research telemetry sync (`public.research_cf_records`, `public.research_chat_logs`), hidden technical model badges in chat UI, and R8/ProGuard release rules.

---

## v3.1.0 — Pure Scoped LLM, Risk Status Synchronization & Anti-Truncated Generation (2026-11-06)

- **Cross-Screen Risk Level Synchronization**: Aligned 4 qualitative risk tiers (`Rendah · Batas Wajar`, `Sedang · Perlu Pemantauan`, `Tinggi · Perlu Perhatian`, `Sangat Tinggi · Perlu Perhatian`) across `ResultScreen`, `HomeScreen`, and `HistoryScreen`.
- **Controlled Hypertension Calibration**: Classified controlled diagnosed hypertension (`M01=Yes`, `M05=No`, `M03=No`, other risk `< 0.50`) consistently as `Sedang · Perlu Pemantauan` (`RiskLevel.MEDIUM`).
- **Auto-Persist on Completion**: Automatically persisted screening outcomes upon completion of both modules (`persistCurrentScreeningIfNeeded`).
- **Pure LLM Flow & Anti-Truncation (`ensureCompleteResponse`)**: Increased context/generation tokens, stripped dangling Markdown markers (`**`), trimmed incomplete sentence fragments, and simplified the `ChatScreen` header.

---

## v3.0.0 — Inclusive Transformation for General Public & Multi-Occupation Cohorts (2026-11-06)

Expanded access beyond female civil servants to all members of the public while keeping demographic cohort tracking optional:
- **Inclusive Multi-Format Auth (`AsnNipValidator.kt` & `AuthScreen.kt`)**: Added support for Email, Phone/WhatsApp, Username, 16-digit NIK, and 18-digit NIP (both genders) with live format detection and optional NIP anatomy breakdown.
- **SQLite v3 Migration (`DB_VERSION = 3`)**: Universalized demographics (`UserOccupationCategory`, `UserGenderOption`) and `AsnSecurityHasher.deriveSubjectHash(accountId)`.
- **Inclusive Corpus, Live Web RAG & Input Privacy Firewall (`WebRagSearchService.kt`)**: Added optional Indonesian Wikipedia & Europe PMC literature caching, broadened questionnaire phrasing (`M06`, `M12`, `K01`–`K10`), and expanded `sanitizePrivacyInput` to redact NIP, NIK, emails, and phone numbers.

---

## v2.5.0 — Local Ollama (`qwen2.5:3b-instruct`) On-Device RAG Integration (2026-11-05)

Integrated local Ollama inference via `127.0.0.1:11434` / `10.0.2.2:11434` *(later superseded by AMD Lemonade `:13305`)* alongside `Modelfile.empati-qwen`, layered topic-scoping guardrails, `sanitizePrivacyInput`, chronic-stress non-causality framing, mandatory `SAPA 129` hotline reminders, and deterministic offline fallback.

---

## v2.4.0 — Multi-Domain Zero-Collision SQLite Database, ASN Auth & RAG Foundation (2026-11-05)

- Introduced 4 isolated SQLite domains in `EmpatiDatabase.kt` (`auth_asn_users` with PBKDF2-HMAC-SHA256 + 16-byte salt, `user_screening_summaries`, `research_cf_records` keyed by `subject_hash`, and `rag_knowledge_chunks` / `rag_chat_logs`).
- Built initial RAG knowledge corpus (`DOC-HT-01` through `DOC-GBV-01`), `ScreeningContextSnapshot`, interactive citation bottom sheets, protective habits cards (`ProtectiveStrengthSection`), and clinical tag detail sheets (`explainClinicalTag`).
- Added `AsnAndRagArchitectureTest.kt` and `EmpatiDatabaseInstrumentedTest.kt` (41 JVM unit tests + 6 SQLite instrumented tests at that time).

---

## v2.3.0 — Clinical Minimalism & 3-Role Functional Typography (2026-11-05)

Introduced `Executive Clinical Summary` scannable cards (`ModuleStatusCell`, `ClinicalTagChip` `FlowRow`, `SupportStrip`, `MinimalDisclaimer`), condensed 1-sentence conclusions and max 3 action items in `UserResultPresenter.kt`, and bundled custom variable fonts (`Plus Jakarta Sans` *(replaced by `Fraunces` in v4.0)*, `Inter`, `JetBrains Mono`) — 36 passing unit tests.

---

## v2.2.0 — Material You Modernization, Motion & Technical Docs (2026-11-05)

Upgraded surfaces and navigation to Material 3 (`TonalCard`, spring `PrimaryButton`/`GhostButton`, shared-axis transitions in `EmpatiApp.kt`, `MainScaffold.kt` navigation bar, horizontal questionnaire slides, timeline history, and technical documentation).

---

## v2.1.0 — Separation of Research Telemetry & End-User Presentation (2026-11-04)

Introduced `UserPresentation.kt`, `UserResultPresenter.kt`, and `ResearchModels.kt` to guarantee zero CF/MB/MD/decimal leakage to end users while preserving complete internal research records—adding `UserPresentationTest` (36 unit tests total).

---

## v2.0.0 — Dual-Module Certainty Factor Expert System (2026-11-04)

Replaced the single v1 score calculator with the deterministic 26-question Shortliffe & Buchanan Certainty Factor engine (`M01`–`M16` Hypertension + `K01`–`K10` GBV, reverse-scored `M08`, dichotomous/Likert parsing, sub-hypothesis aggregation, 6-category cross-module integration, and `CfEngineTest` 30 unit tests verified against the reference Python model).

---

## v1.0.0 — Initial Release (AI-EMPATI Prototype)

Established the initial Android Material 3 application featuring a 24-question v1 scoring prototype (`ScreeningEngine.kt`), deterministic 9-intent rule-based chatbot (`ChatEngine.kt` v1), dark/light theme support, `FLAG_SECURE`, and `SharedPreferences` local storage.
