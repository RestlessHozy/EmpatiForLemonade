# Empati — Master Context & Project Knowledge Base (v4.1 Historical Baseline)

> **Historical Context Notice (AMD Lemonade Architecture):**
> This document captures the **v4.1 Master Context Snapshot** (historically authored under *SRYA Emora* / *AI-EMPATI* using OpenRouter cloud routing + legacy Ollama `:11434` and the v4.1 SQLite schema).
>
> For the **current AMD Lemonade Challenge (`:13305`) implementation**—featuring `LemonadeUnifiedClient`, on-LAN `Qwen3-4B` / `Qwen3-1.7B` chat, `nomic-embed-text-v1` cosine RAG over 30 evidence chunks (`EmpatiVectorRag`), `Whisper-Large-v3-Turbo` STT, `kokoro-v1` TTS, automated PowerShell provisioning (`setup-empati-lemonade.ps1`), and **203 passing unit tests across 24 suites**—see [`README.md`](README.md), [`docs/adr/001-lemonade-hybrid-architecture.md`](docs/adr/001-lemonade-hybrid-architecture.md), and [`docs/specs/lemonade-hybrid-triage.md`](docs/specs/lemonade-hybrid-triage.md).

---

## Table of Contents

1. [Project Identity & Executive Summary](#1-project-identity--executive-summary)
2. [Tech Stack & Software Architecture](#2-tech-stack--software-architecture)
3. [Certainty Factor Expert System (Dual-Module Determinants)](#3-certainty-factor-expert-system-dual-module-determinants)
   - [3.1 Theory & Certainty Factor Mathematical Formulas](#31-theory--certainty-factor-mathematical-formulas)
   - [3.2 Question Bank & Scoring Scales (26 Questions)](#32-question-bank--scoring-scales-26-questions)
   - [3.3 Knowledge Base (26 MB/MD Rules)](#33-knowledge-base-26-mbmd-rules)
   - [3.4 Integrated Analysis Matrix (Qualitative Outcome Categories)](#34-integrated-analysis-matrix-qualitative-outcome-categories)
4. [Clinical Presentation Layer vs. Research Telemetry (Ethics & Safety)](#4-clinical-presentation-layer-vs-research-telemetry-ethics--safety)
5. ["Soft Daylight" (v4.0) UI/UX Design & 3-Voice Typography](#5-soft-daylight-v40-uiux-design--3-voice-typography)
6. [Inclusive Identity & Authentication Management](#6-inclusive-identity--authentication-management)
7. [Multi-Domain Zero-Collision SQLite Database](#7-multi-domain-zero-collision-sqlite-database)
8. [RAG Architecture & Legacy LLM Client Baseline (v4.1)](#8-rag-architecture--legacy-llm-client-baseline-v41)
9. [Repository Layout (v4.1 Reference)](#9-repository-layout-v41-reference)

---

## 1. Project Identity & Executive Summary

* **Project Name & AI Companion**: **Empati** (preceded historically by *AI-EMPATI* and *SRYA Emora*).
* **Baseline Recorded Here**: **v4.1.5** (IME-aware composer ergonomics, `SelectionContainer` + `ContentCopy`, `ScreeningContextSnapshot` bridging into LLM chat, "Soft Daylight" UI, zero-leak CF presentation, and pseudonymized cohort analytics).
* **Dual Purpose (Public Health Companion + Clinical Dissertation Cohort)**:
  1. **Accessible Self-Screening for Everyone**: Empowers general users (private employees, MSMEs, students, homemakers, healthcare/educators, and civil servants of any gender) to screen hypertension risk, lifestyle habits, occupational stress, and potential **Gender-Based Violence (GBV)** experiences.
  2. **Dissertation Research Cohort Support**: Preserves structured demographic metadata (gender, occupational category, domicile) and optional 18-digit Indonesian civil-servant (ASN) NIP validation to support focused analysis on *"Gender-Based Hypertension Risk Screening for Female Civil Servants in Indonesia"*.
* **Core Guiding Principles**:
  1. **Screening, Not Diagnosis**: Provides early risk triage and educational guidance rather than a definitive clinical diagnosis.
  2. **Non-Causal Psychosocial Framing**: Presents links between chronic stress / GBV exposure and elevated blood pressure as a recognized *chronic stress pathway*, never as direct physical causation.
  3. **Privacy-First Controls**: Combines PBKDF2-HMAC-SHA256 credential hashing, PII redaction before LLM calls, a one-way `subject_hash` pseudonymization firewall, full account deletion, and configurable screen protection (`MainActivity.ENABLE_SCREENSHOT_PROTECTION = false` for demo/accessibility while retaining opt-in `FLAG_SECURE`).

---

## 2. Tech Stack & Software Architecture

### 2.1 Technology Stack (v4.1 Baseline)
* **Language**: Kotlin 2.2.10
* **Target Platform**: Android (Min SDK 24 / Android 7.0; Target & Compile SDK 37)
* **UI Framework**: Jetpack Compose with Material 3 (Material You)
* **Application Architecture**: Clean Architecture + MVVM with Unidirectional Data Flow (UDF)
* **State Management**: `EmpatiViewModel` (`AndroidViewModel`) + Compose Runtime State
* **Local & Cloud Persistence**: Multi-domain SQLite (`EmpatiDatabase` via `SQLiteOpenHelper`), `SharedPreferences`, and optional pseudonymized sync (`SupabaseCloudSyncService`)
* **LLM Infrastructure (v4.1 -> Current)**: In v4.1, chat used OpenRouter via Supabase Edge Function `empati-chat` (`openrouter/free`, `llama-3.3-70b-instruct:free`, `gemma-3-12b-it:free`, `qwen3-8b:free`) plus local Ollama (`qwen2.5:3b-instruct`). **Current competition main branch** routes chat, embeddings, STT, and TTS locally through **AMD Lemonade Server (`:13305`)** via `LemonadeUnifiedClient` (see [`README.md`](README.md)).
* **Security & Cryptography**: PBKDF2-HMAC-SHA256 (120,000 iterations) + random 16-byte salt + SHA-256 one-way pseudonym (`subject_hash`)
* **Testing**: Originally 49 JVM unit tests + 6 instrumented tests at v4.1; expanded on `main` to **203 JVM unit tests across 24 suites**

### 2.2 Architecture Block Diagram

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION / UI LAYER                         │
│  Jetpack Compose • Material 3 • 3-Voice Functional Typography (Bundled) │
│  Screens: Splash ➔ Onboarding ➔ Auth ➔ Home ➔ Consent ➔               │
│           DualQuestionnaire ➔ ModuleResult ➔ IntegratedResult ➔        │
│           Chat (RAG + Voice) ➔ History ➔ Profile                      │
├────────────────────────────────────────────────────────────────────────┤
│                           VIEWMODEL LAYER                              │
│  EmpatiViewModel: Navigation, UDF State, Session Mgmt, Chat & Voice    │
├────────────────────────────────────────────────────────────────────────┤
│                       DOMAIN / INFERENCE LAYER                         │
│  ┌───────────────────────────────┐  ┌────────────────────────────────┐ │
│  │     CERTAINTY FACTOR ENGINE   │  │           RAG ENGINE           │ │
│  │  • CfInputParser (Reverse M08)│  │  • ScreeningContextSnapshot    │ │
│  │  • CfKnowledgeBase (26 Rules) │  │  • RagKnowledgeRepository      │ │
│  │  • CfInferenceEngine          │  │  • EmpatiVectorRag (30 Chunks) │ │
│  │  • CfResultGenerator          │  │  • Guardrails & Topic Scoping  │ │
│  └───────────────────────────────┘  └────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │                     USER PRESENTATION LAYER                       │ │
│  │  UserResultPresenter: Translates internal CF into plain language  │ │
│  │  (Zero Leaks: No CF decimals, MB/MD, %, or rule IDs in UI)        │ │
│  └───────────────────────────────────────────────────────────────────┘ │
├────────────────────────────────────────────────────────────────────────┤
│                   DATA & PERSISTENCE LAYER (ISOLATED)                  │
│  EmpatiDatabase (SQLite Multi-Domain: empati_multi_domain.db)          │
│  ├── Domain 1: auth_asn_users (PBKDF2 Credentials + Optional NIP)      │
│  ├── Domain 2: user_screening_summaries (UI History per user_id)       │
│  ├── Domain 3: research_cf_records (Research Audit via subject_hash)   │
│  └── Domain 4: rag_knowledge_chunks & rag_chat_logs (Docs & Chat)      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Certainty Factor Expert System (Dual-Module Determinants)

### 3.1 Theory & Certainty Factor Mathematical Formulas
The Shortliffe & Buchanan Certainty Factor model captures clinical belief alongside graded respondent certainty:

1. **Rule CF (Expert Weight)**:
   $$CF_{rule} = MB - MD$$
   * $MB$ (*Measure of Belief*): Range $[0.0, 1.0]$
   * $MD$ (*Measure of Disbelief*): Range $[0.0, 1.0]$
2. **Evidence CF (User Answer $\times$ Rule Weight)**:
   $$CF_{evidence} = CF_{user} \times CF_{rule}$$
3. **Sequential Combination ($CF_{combine}$)**:
   * Both positive ($CF_1 > 0$ and $CF_2 > 0$):
     $$CF_{combine} = CF_1 + CF_2 \times (1 - CF_1)$$
   * Both negative ($CF_1 < 0$ and $CF_2 < 0$):
     $$CF_{combine} = CF_1 + CF_2 \times (1 + CF_1)$$
   * Opposing signs (conflict normalization):
     $$CF_{combine} = \frac{CF_1 + CF_2}{1 - \min(|CF_1|, |CF_2|)}$$

### 3.2 Question Bank & Scoring Scales (26 Questions)

#### Module 1: Hypertension (`M01`–`M16`)
* **Medical & Clinical History**:
  * `M01` (Dichotomous): Clinician-diagnosed hypertension ($MB=0.95, MD=0.0$)
  * `M02` (Dichotomous): Parental/sibling history of hypertension ($MB=0.70, MD=0.0$)
  * `M03` (Dichotomous): History of diabetes mellitus ($MB=0.75, MD=0.0$)
  * `M04` (Dichotomous): Regular antihypertensive medication use ($MB=0.90, MD=0.0$)
  * `M05` (Dichotomous): Last blood pressure reading $> 130/85\text{ mmHg}$ ($MB=0.85, MD=0.0$)
  * `M06` (Dichotomous): Currently pregnant / within 6 months postpartum ($MB=0.60, MD=0.0$)
* **Lifestyle Factors**:
  * `M07` (Likert): High-sodium / salty food frequency ($MB=0.70, MD=0.0$)
  * `M08` (Likert — **REVERSE SCORED**): Physical exercise $\ge 30$ min ($MB=0.75, MD=0.0$). More frequent exercise yields lower $CF_{user}$.
  * `M09` (Dichotomous): Smoking or secondhand smoke exposure ($MB=0.65, MD=0.0$)
  * `M10` (Likert): Excessive caffeine / coffee consumption ($MB=0.50, MD=0.0$)
  * `M11` (Dichotomous): Obesity / overweight ($MB=0.70, MD=0.0$)
* **Psychosocial & Workload Factors**:
  * `M12` (Likert): Severe occupational stress ($MB=0.75, MD=0.0$)
  * `M13` (Dichotomous): Habitual sleep $< 7$ hours/night ($MB=0.60, MD=0.0$)
  * `M14` (Likert): Unexplained anxiety / feeling pressured ($MB=0.65, MD=0.0$)
  * `M15` (Dichotomous): Rarely checks blood pressure ($< 1$/year) ($MB=0.50, MD=0.0$)
  * `M16` (Dichotomous): Fast food intake $> 3\times$/week ($MB=0.60, MD=0.0$)

#### Module 2: Gender-Based Violence / GBV (`K01`–`K10`) — All Sensitive
* **Physical Violence**: `K01` (Pushing/hitting at work), `K02` (Throwing objects / physical threats).
* **Psychological Violence**: `K03` (Verbal humiliation/insults), `K04` (Workplace ostracization/isolation).
* **Sexual Violence**: `K05` (Non-consensual sexual remarks/jokes), `K06` (Forced sexual touch/advances).
* **Economic Violence**: `K07` (Wages withheld without consent), `K08` (Unjustified career/promotion obstruction).
* **Digital Violence**: `K09` (Digital stalking/spamming), `K10` (Social-media intimidation/threats).

#### Answer-to-$CF_{user}$ Mapping
* **Dichotomous**: `Yes = 1.0`, `No = 0.0`.
* **Standard Likert (1–5)**: $1=0.0$, $2=0.2$, $3=0.5$, $4=0.8$, $5=1.0$.
* **Reverse Likert (`M08`)**: $1=1.0$, $2=0.8$, $3=0.5$, $4=0.2$, $5=0.0$.

### 3.3 Knowledge Base (26 MB/MD Rules)
Rules aggregate into clinical sub-hypotheses (`HT_GENETIC`, `HT_MEDICAL`, `HT_LIFESTYLE`, `HT_PSYCHOSOCIAL`) and GBV dimensions (`GBV_PHYSICAL`, `GBV_PSYCHOLOGICAL`, `GBV_SEXUAL`, `GBV_ECONOMIC`, `GBV_DIGITAL`).

Module CF risk thresholds (`CfInferenceEngine.levelOf`):
* $CF \ge 0.75 \rightarrow$ **Very High (`SANGAT_TINGGI` / `VERY_HIGH`)**
* $0.50 \le CF < 0.75 \rightarrow$ **High (`TINGGI` / `HIGH`)**
* $0.25 \le CF < 0.50 \rightarrow$ **Moderate (`SEDANG` / `MEDIUM`)** (including controlled hypertension calibration when `M01=Yes`, `M05=No`, `M03=No`, and other factors $< 0.50$)
* $CF < 0.25 \rightarrow$ **Low (`RENDAH` / `LOW`)**

### 3.4 Integrated Analysis Matrix (Qualitative Outcome Categories)
Cross-module inference classifies paired outcomes into qualitative categories:
1. **Low Risk**: Both Hypertension CF and GBV CF remain below significant thresholds ($< 0.25$).
2. **Hypertension Risk Driven Primarily by Health & Lifestyle Factors**: Elevated Hypertension CF without GBV indication.
3. **GBV Indication Without Elevated Physical Hypertension Risk**: Significant GBV indication ($\ge 0.25$) while blood-pressure risk factors remain low.
4. **Hypertension Risk With Psychosocial / GBV Stress-Pathway Indication**: Both modules indicate risk ($\ge 0.25$), triggering dual guidance (clinical BP follow-up + confidential **SAPA 129** referral).
5. **Inconclusive**: One or both modules remain incomplete.

---

## 4. Clinical Presentation Layer vs. Research Telemetry (Ethics & Safety)

To prevent health anxiety and protect medical ethics, the app strictly isolates computational telemetry from patient-facing surfaces:

| Internal Computation Artifact | Patient-Facing Output (`UserResultPresenter`) |
|---|---|
| Raw decimal CF (`0.842`), MB, MD | **Strictly omitted** (verified via regex `\d+\.\d+` in `UserPresentationTest`) |
| Numerical percentages (`84%`) | Replaced by calm qualitative status labels |
| Internal Rule IDs (`R-M01`, `R-K03`) | Replaced by plain-language factor chips (e.g., family history, diet & activity) |
| Full Research Telemetry | Isolated inside `research_cf_records` tagged with `kb_version = 'cf-kb-v2.0'` |

---

## 5. "Soft Daylight" (v4.0) UI/UX Design & 3-Voice Typography

- **3-Layer Color Architecture (`Color.kt` & `Theme.kt`)**: **Sage** (primary calm, `#2E6A57`), **Clay** (warmth, `#93482E`), **Mist** (AI companion & SAPA 129 presence, `#5A5896`), **Honey** (`#D29A2E`), **Terracotta** (`#A8402B`), **Berry** (`#8E2F55`), and **Stone** warm neutrals (`#F7F5F0` Light / `#121512` Dark).
- **Affective Semantics (`MaterialTheme.empati`)**: `calm`, `attentive`, `concerned`, `urgent`, `neutral`, `presence`.
- **3-Voice Typography (`Type.kt` + bundled OFL variable fonts in `res/font/`)**:
  1. `DisplayFont` (**Fraunces**, `fraunces.ttf`) for greetings, prompts, headlines, and `EmpatiAvatar`.
  2. `BodyFont` (**Inter**, `inter.ttf` + `tnum` `EmpatiNumeralStyle`) for readable prose and step counters (`3 / 16`).
  3. `MetricFont` (**JetBrains Mono**, `jetbrains_mono.ttf`) strictly for raw identifiers (account ID, NIP breakdown).

---

## 6. Inclusive Identity & Authentication Management

Implemented in [`AsnNipValidator.kt`](app/src/main/java/com/example/empati/data/auth/AsnNipValidator.kt) and [`AuthScreen.kt`](app/src/main/java/com/example/empati/ui/screens/AuthScreen.kt):
- Accepts **5 identifier formats**: Email, Phone/WhatsApp, Username (`≥ 4` chars), 16-digit NIK, or 18-digit NIP (activating the live Birth/TMT/Gender anatomy parser).
- Stores demographic attributes (occupation category, gender, domicile/role) alongside PBKDF2-HMAC-SHA256 credentials (120,000 iterations, 16-byte random salt).
- Computes a one-way pseudonym `subject_hash = "sub_" + SHA256("empati-asn-research-v2:" + normalizedAccountId).take(20)` so CF records never store raw account identifiers.

---

## 7. Multi-Domain Zero-Collision SQLite Database

Single SQLite file `empati_multi_domain.db` in [`EmpatiDatabase.kt`](app/src/main/java/com/example/empati/data/db/EmpatiDatabase.kt) enforces 4 isolated domains:
1. **`auth_asn_users`** (Identity & hashed credentials)
2. **`user_screening_summaries`** (UI summary history scoped to `user_id`)
3. **`research_cf_records`** (CF/MB/MD audit telemetry keyed by `subject_hash`)
4. **`rag_knowledge_chunks` & `rag_chat_logs`** (Guideline chunks and chat transcripts)

**Isolation Matrix:** The CF engine writes summaries + research records without touching RAG tables; the RAG/chatbot engine reads `rag_knowledge_chunks` and `ScreeningContextSnapshot` without ever modifying CF weights or records.

---

## 8. RAG Architecture & Legacy LLM Client Baseline (v4.1)

In v4.1, [`ChatEngine.kt`](app/src/main/java/com/example/empati/data/ChatEngine.kt), [`RagKnowledgeRepository.kt`](app/src/main/java/com/example/empati/data/rag/RagKnowledgeRepository.kt), and [`OllamaRagService.kt`](app/src/main/java/com/example/empati/data/rag/OllamaRagService.kt) enforced layered privacy and clinical guardrails:
1. **Input Privacy Firewall (`sanitizePrivacyInput`)**: Masks 18-digit NIPs, 16-digit NIKs, emails, and phone numbers.
2. **Strict Topic Scoping**: Restricts conversation to Health (hypertension/cardiovascular/nutrition/exercise), Psychology (stress/sleep/mental well-being), and Violence Protection (GBV & **SAPA 129**).
3. **Complete-Response & Zero-Leak Guardrails**: Removes leaked decimals/`%`/`CF`/`MB`/`MD`, includes **SAPA 129 (`129` / WA `08111-129-129`)** on GBV queries, and prevents truncated sentences.

> For the current AMD Lemonade (`:13305`) omnimodal chat, `nomic-embed-text-v1` cosine vector RAG (`EmpatiVectorRag`), `Whisper-Large-v3-Turbo` STT, and `kokoro-v1` TTS pipeline built atop these guardrails, see [`README.md`](README.md) and [`docs/specs/lemonade-hybrid-triage.md`](docs/specs/lemonade-hybrid-triage.md).

---

## 9. Repository Layout (v4.1 Reference)

For the up-to-date repository layout including `LemonadeUnifiedClient.kt`, `EmpatiVectorRag.kt`, `EmpatiCorpus.kt`, `VoiceInputManager.kt`, `IndonesianSpeechSynthesizer.kt`, and automated PowerShell tooling under `scripts/`, see [`README.md`](README.md#repository-layout).
