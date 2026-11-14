# Specification: Hybrid Triage and Omni-Modal RAG on AMD Lemonade (`:13305`)

## Problem Statement

Previously, Empati users could complete hypertension and gender-based violence (GBV) screenings, yet follow-up counseling was disconnected from those results. Assistant replies relied on fragile routing (`127.0.0.1:11434`, hardcoded LAN IPs, external cloud endpoints, and static offline text), often falling back to identical canned answers on offline demo laptops.

Clinical reference retrieval was also inactive—empty corpora passed to retrieval layers resulted in zero retrieved chunks and zero interactive citations, leaving users without verifiable sources.

Finally, interaction was strictly text-only on both input and output, excluding vulnerable or low-literacy users who prefer speaking aloud and listening to guidance hands-free. Demo operators also lacked reproducible setup and benchmark scripts pinned to AMD hardware.

## Solution

Empati unifies all inference behind a single **AMD Lemonade Server** on port `13305` for four modalities: speech-to-text, text embeddings, streaming chat completion, and speech synthesis contract—removing Ollama (`11434` / `/api/chat`) completely.

Screening outputs from the deterministic Certainty Factor engine serialize into a structured context (`empati.screening_context/v1`). The local LLM narrates those findings and grounded reference chunks retrieved via embeddings, while deterministic Kotlin logic remains the sole authority over risk levels, dominant categories, and crisis escalation (`SAPA 129`).

Users gain a 16 kHz mono WAV voice recorder and per-reply voice playback controls following Empati's Soft Daylight UI guidelines, backed by two PowerShell scripts (`setup-empati-lemonade.ps1` and `bench-empati.ps1`) for zero-to-demo provisioning and reproducible latency reporting.

## User Stories

1. As a user, I want clear status indicators distinguishing server offline, loading models, busy slots, and ready state.
2. As a user, I want visibility into whether responses come from local Lemonade or a fallback path.
3. As a user, I want a microphone button to speak concerns directly instead of typing long stories.
4. As a user, I want explicit recording states (permission request, active timer, transcribing, ready) plus a Cancel action.
5. As a user, I want transcripts inserted into the text box for review and editing before sending.
6. As a user denying microphone permission, I want concise guidance and seamless return to text entry.
7. As a user, I want a play/stop audio button per reply with local caching so replays do not re-hit inference.
8. As a user, I want clickable citation chips opening a bottom sheet displaying publisher, year, section, and verbatim excerpt.
9. As a user, I want empathetic narrative explanations grounded in my screening status—calling out unanswered screening questions when incomplete.
10. As a user, I want honest refusal messages rather than fabricated medical facts when no relevant reference chunk exceeds the similarity threshold.
11. As a user, I never want uncalibrated internal `CF`, `MB`, `MD`, or percentage scores leaked in chat text, nor automated diagnoses or drug dosages.
12. As a user mentioning violence, I always need exact verified **SAPA 129** contact details (`129` / WhatsApp `08111-129-129`).
13. As a user, I want token-by-token streaming inside one clean assistant bubble with graceful partial retention on disconnects.
14. As a developer/operator, I want alias-driven model swapping (`lemonade alias add`), single-command setup, reproducible latency benchmarks, and strict contract tests protecting `:13305`.

## Implementation Decisions

**Unified HTTP Client:** `LemonadeUnifiedClient` replaces ad-hoc calls with five OpenAI-compatible methods on `http://<HOST>:13305/v1`: `listModels`, `generateEmbeddings`, `streamChatCompletion`, `transcribeAudio`, and `synthesizeSpeech`. Port `11434` and `/api/chat` are removed cleanly.

**Alias Contract:** Kotlin never hardcodes concrete model identifiers—only aliases: `empati-llm`, `empati-embed`, `empati-stt`, `empati-tts` (plus server-side backup `empati-llm-backup`).

**Structured Triage Context (`empati.screening_context/v1`):** Serializes deterministic `CfEngine` output while preserving persisted enum identifiers (`HYPERTENSION`, `GBV`, `CfLevel`, `Hypothesis`, `IntegratedCategory`):

```json
{
  "schema": "empati.screening_context/v1",
  "kb_version": "cf-kb-v2.0",
  "modules": [
    {
      "module": "HYPERTENSION",
      "level": "SEDANG",
      "cf": 0.62,
      "answered": 16,
      "unanswered": 0,
      "dominant_categories": [
        {"hypothesis": "LIFESTYLE", "cf": 0.71, "evidence": [{"question_id": "M09", "cf_user": 0.8, "cf_expert": 0.6}]}
      ],
      "protective_factors": ["M08"]
    }
  ],
  "integrated": {"category": "HYPERTENSION_LIFESTYLE", "indication": true, "relationship_note": "chronic_stress_pathway"}
}
```

**Numeric Policy & Risk Authority:** Numeric `cf` values calibrate tone inside the prompt only; `AnswerGuardrail` strips leaked internal metrics (`CF`, `MB`, `MD`, `0.xx dari 1.00`, score claims, diagnoses).

**Corpus, Embeddings & Voice Pipeline:**
- Curated WHO, Kemenkes RI, Komnas Perempuan, and SAPA 129 excerpts (`30` chunks) indexed via `empati-embed` (`topK = 4`, cosine threshold `0.35`, plus lexical fallback). *(Note: local index storage currently persists in SharedPreferences JSON rather than SQLite `float32` blobs—see ADR/Status).*
- `VoiceInputController` captures 16 kHz mono PCM16 WAV (60s cap) -> `POST /v1/audio/transcriptions` (`empati-stt`, `language=id`).
- `HybridSpeechSynthesizer` + `SpeechPlaybackController` cache SHA-256 audio locally and prioritize offline Android `id-ID` TTS for natural Indonesian pronunciation before falling back to Lemonade `empati-tts` (`kokoro-v1`).
- **Fallback Order:** Local Lemonade (`:13305`) -> optional Supabase Edge Function (`empati-chat`) -> deterministic offline presenter (`ChatFallbackLadder`).

## Testing Decisions

Tests assert observable contracts across clear architectural seams rather than private implementation internals:

1. **`EmpatiChatUseCase` Seam:** In-memory fakes verify fallback order, empty-retrieval boundaries, post-generation guardrails, unanswered question disclosure, and `empati.screening_context/v1` payload preservation.
2. **`LemonadeUnifiedClient` Wire Contract:** Verified against an embedded localhost JDK HTTP server checking paths, auth headers, multipart boundaries, SSE delta parsing / `[DONE]`, error mapping (`404 model_not_found`, `409 slots_pinned_error`), and absence of `11434`.
3. **`VoiceInputController` & `SpeechPlaybackController`:** State transitions, cancellation, 60s timeout, denied permissions, offline Indonesian TTS vs. `empati-tts` routing, and Compose screenshot previews.
4. **PowerShell Dry-Run Suite:** `scripts/tests/verify-dryrun.ps1` validates host config, aliases, model names, `lemonade bench`, `:13305`, and absence of legacy Ollama references.
5. **`CfEngineTest` Integrity:** Existing Certainty Factor regression tests remain untouched.

## Out of Scope

- Altering core Certainty Factor formulas or threshold enums.
- Changing persisted Supabase relational schemas.
- Voice cloning, image/PDF uploads, model fine-tuning, iOS/desktop ports, or unmeasured latency claims.
