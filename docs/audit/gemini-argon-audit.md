# Deep Architecture & Submission Readiness Audit: Empati (AMD Lemonade Developer Challenge)

**Audit Date:** November 12, 2026  
**Target Environment:** AMD Lemonade Developer Challenge (`http://127.0.0.1:13305/v1` / `http://10.0.2.2:13305/v1`)  
**Audit Baseline Commit:** `a34f582`  
**Status:** Resolved & verified cleanly on hardware (`24 suites, 203 tests passing`)

---

## Executive Summary

Initial tickets `[T01]` through `[T11]` established `LemonadeUnifiedClient`, deterministic `HybridTriageBridge`, `EmbeddingIndex`, voice recording/playback controllers, and PowerShell setup scripts (`176` passing unit tests at audit time).

A deep cross-layer audit across Data, Domain, ViewModel, Compose UI, and Runtime Scripts identified **14 critical and medium blind spots** prior to final submission—spanning residual legacy branding, disconnected RAG triage context across app restarts, missing `enable_thinking: false` gating for `Qwen3-4B-GGUF`, UI/audio concurrency races, and Android LAN cleartext rules.

All critical and medium engineering findings have since been implemented and verified.

---

## Criterion 1: Dual-System Architecture Integrity (Certainty Factor & RAG)

### Verified Foundations
- **100% Deterministic Expert Engine Isolation:** `CfInputParser`, `CfKnowledgeBase`, `CfInferenceEngine`, and `CfResultGenerator` compute Shortliffe & Buchanan rules purely on-device (`CF(H,e) = CF(E) * (MB - MD)`), zero LLM calls inside scoring paths.
- **Structured Bridge Contract (`empati.screening_context/v1`):** Summarizes `HYPERTENSION` & `GBV` scores, top categories (max 3 evidence items each), protective factors, and `chronic_stress_pathway`.
- **Strict Citation Picking:** Only retrieved chunks actually grounded inside generated replies surface as interactive UI citation chips.

### Findings & Resolutions
1. **`[CRITICAL - C1.1]` `HybridTriageBridge.toRetrievalQuery` Disconnected in Vector Search:** Fixed by enriching self-assessment queries via `EmpatiChatUseCase.retrievalQueryFor` so clinical screening topics pull matching WHO/Kemenkes chunks cleanly.
2. **`[CRITICAL - C1.2]` Cross-Session Screening Context Dropped (`GroundedChatGateway` Ignored `snapshot`):** Fixed—when in-memory `TriageContext` is null after app restart, `EmpatiPromptBuilder` & `HybridTriageBridge.toPromptBlock(snapshot)` reconstruct structured grounding context from persisted snapshots.
3. **`[MEDIUM - C1.3]` Decimal CF Leak Pattern (`0.62 dari 1.00`):** Strengthened `AnswerGuardrail.enforce` to strip raw fractional score ratios and internal confidence labels.
4. **`[LOW - C1.4]` Coexistence with Legacy RAG Helpers:** Legacy self-intro constants and brand labels updated consistently to **Empati**; full removal of remaining legacy adapters (`OllamaRagService` / `ChatEngine`) kept decoupled cleanly.

---

## Criterion 2: AMD Lemonade Specification Compliance (`:13305`)

### Verified Foundations
- Unified `:13305` OpenAI-compatible wire client (`/v1/models`, `/v1/chat/completions`, `/v1/embeddings`, `/v1/audio/transcriptions`, `/v1/audio/speech`).
- Alias contract enforced (`empati-llm`, `empati-embed`, `empati-stt`, `empati-tts`, plus `empati-llm-backup`).
- Zero Ollama traces (`11434` / `/api/chat` eliminated across Kotlin & PowerShell tests).

### Findings & Resolutions
1. **`[CRITICAL - C2.1]` `enable_thinking: false` & `<think>` Sanitization for `Qwen3-4B-GGUF`:** Added `enable_thinking: false` to chat/stream payloads in `LemonadeUnifiedClient.kt` and `scripts/bench-empati.ps1`, plus client/guardrail `<think>...</think>` stripping.
2. **`[MEDIUM - C2.2]` False Positive `"Lokal siap"` Header Badge:** Updated `ChatScreen` readiness check to inspect `vm.lemonadeStatus?.isReady == true`.
3. **`[MEDIUM - C2.3]` Empty-Vector Pollution on Offline Indexing:** Updated `CorpusIndexer` to preserve valid stored embeddings rather than overwriting with empty `FloatArray(0)` when server is unreachable.
4. **`[LOW - C2.4 / C2.5]` WAV Header Width & `Float == Double` Compiler Warning:** Fixed `Write-Short` in `bench-empati.ps1` and float rounding in `MiniJson.kt`.

---

## Criterion 3: Android Runtime Readiness (Audio, Streaming & Local Network)

1. **`[MEDIUM - C3.1]` Mic vs. TTS Audio Collision:** Starting voice capture now stops active TTS playback; starting TTS cancels active recording cleanly.
2. **`[MEDIUM - C3.2]` Swallowed `MediaPlayer` Errors:** Error callbacks now propagate `SpeechPlaybackState.Error` cleanly to the UI.
3. **`[MEDIUM - C3.3]` Streaming Delta UI Race & Duplicate Bubble Fix:** Guarded late delta updates after stream close and ensured `"Empati sedang mengetik…"` transitions cleanly inside a single assistant bubble (`ChatStreamingBubbleTest`).
4. **`[MEDIUM - C3.4]` Cleartext Wi-Fi LAN Testing:** Added `app/src/debug/res/xml/network_security_config.xml` for flexible debug LAN testing while keeping release manifests locked to loopback hosts (`10.0.2.2`, `127.0.0.1`, `localhost`).

---

## Criterion 4: Rebranding Consistency & Language Verification

- Replaced residual retired names (`"SRYA Emora"`, `"AI-EMPATI"`) across all user-facing screens, strings, prompts, Edge Function headers, screenshot fixtures, and architecture tests, enforced automatically by `BrandConsistencyTest`.
- Added offline-first **Indonesian Speech Synthesis** (`IndonesianSpeechSynthesizer.kt` + `HybridSpeechSynthesizerTest.kt`) so patient audio replies pronounce Bahasa Indonesia naturally without calling external clouds, retaining full fallback to Lemonade `empati-tts`.

---

## Verification Progression

| Milestone | Scope | Result |
|---|---|---|
| Baseline Audit (`a34f582`) | 21 suites, 176 JVM tests | `176 / 176` passing |
| Post-Remediation | 22 suites, 192 JVM tests + `verify-dryrun.ps1` | `192 / 192` passing |
| Final Competition Release (`4e3a4ba+`) | **24 suites, 203 JVM tests** + `verify-dryrun.ps1` + live phone `5PT8QK6TUCOZXCSK` E2E + 5x AMD Ryzen 7 7735HS bench | **`203 / 203` passing (`0` failed, `0` skipped)** |

Remaining documented items not claimed as finished (e.g., designer vector icon rename from legacy drawable filenames, SQLite binary vector storage migration, clinician CF calibration, release signing keys) remain listed transparently in [`README.md`](../../README.md).
