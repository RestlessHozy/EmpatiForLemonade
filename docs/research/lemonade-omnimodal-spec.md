# Empati — AMD Lemonade Technical Integration Specification

Technical research specification for integrating **Empati** with **AMD Lemonade Server** (`lemonade-sdk/lemonade`, verified November 11, 2026). Claims not verified directly against upstream documentation are marked **unverified**.

Hard constraint: **no port `11434` and no Ollama-style `/api/chat` endpoints appear in Empati**. All local inference routes through AMD Lemonade Server on port `13305`.

## 1. Server and Networking

| Setting | Default Value | Source |
|---|---|---|
| HTTP Port | `13305` | Settings reference (`port`) |
| Bind Host | `"localhost"` | Settings reference (`host`) |
| Local Base URL | `http://localhost:13305` | Official API examples |
| Config File | `%USERPROFILE%\.config\lemonade\config.json` (Windows) | `config.json` guide |
| Runtime Override | `lemond --port 13305 --host 0.0.0.0` | `lemond CLI` guide |

Persistent LAN binding:

```bash
lemonade config set host=0.0.0.0
```

Binding `0.0.0.0` exposes endpoints (including `/internal/*`) on the LAN. Production/shared deployments should enforce firewall rules and configure `LEMONADE_API_KEY`.

Android connectivity modes:
- **Android Emulator:** `http://10.0.2.2:13305/v1` (host loopback alias).
- **Physical Device via USB (`adb reverse`):** `adb reverse tcp:13305 tcp:13305` + `http://127.0.0.1:13305/v1`.
- **Physical Device via Wi-Fi LAN:** `http://<LAPTOP_LAN_IP>:13305/v1` (permitted in Empati's debug build via `app/src/debug/res/xml/network_security_config.xml`).

## 2. Endpoints Used by Empati

OpenAI-compatible REST surface ([API Reference](https://lemonade-server.ai/docs/api/openai/)):

| Endpoint | Modality | Role in Empati |
|---|---|---|
| `GET /v1/models` | Registry | Startup alias readiness & model status check |
| `POST /v1/chat/completions` | Chat + SSE | Grounded counseling narration (`stream: true`, `enable_thinking: false`) |
| `POST /v1/embeddings` | Dense vectors | Corpus indexing & semantic query retrieval |
| `POST /v1/audio/transcriptions` | Speech-to-Text | 16 kHz mono WAV Indonesian voice transcription |
| `POST /v1/audio/speech` | Text-to-Speech | MP3 speech synthesis fallback (`kokoro-v1`) |

### 2.1 Chat & Reasoning Control

Minimum chat payload enforces `"enable_thinking": false` on `Qwen3-4B-GGUF` so reasoning traces do not consume `max_tokens` budgets or leak `<think>...</think>` tags into counseling replies or TTS output.

SSE streaming frames parse `data:` lines for `delta.content`, ignore `reasoning_content`, terminate cleanly on `data: [DONE]`, and map server errors cleanly (`404 model_not_found`, `409 slots_pinned_error`, `500 model_load_error`).

Contract aliases decouple the APK from underlying models:

```bash
lemonade alias add empati-llm Qwen3-4B-GGUF
lemonade alias add empati-llm-backup Qwen3-1.7B-GGUF
lemonade alias add empati-embed nomic-embed-text-v1-GGUF
lemonade alias add empati-stt Whisper-Large-v3-Turbo
lemonade alias add empati-tts kokoro-v1
```

### 2.2 Embeddings

`POST /v1/embeddings` returns 768-dimensional float vectors for `nomic-embed-text-v1-GGUF` (`0.0781 GB`). Kotlin vector normalization dynamically reads returned vector dimensionality rather than hardcoding 768.

### 2.3 Speech to Text

`POST /v1/audio/transcriptions` accepts `multipart/form-data` with 16 kHz mono WAV payloads (`language=id`, `model=empati-stt`). Note: explicit `language=id` ensures Indonesian transcription accuracy rather than relying on `auto` detection.

### 2.4 Text to Speech

`POST /v1/audio/speech` accepts JSON `{model: "empati-tts", input, voice, response_format: "mp3"}` and returns `audio/mpeg` bytes cached on disk by content hash. On Android, `HybridSpeechSynthesizer` prioritizes offline native `id-ID` synthesis for natural Indonesian pronunciation before falling back to Lemonade `empati-tts`.

### 2.5 Startup Safety Check

Before enabling chat/voice UI states, Empati calls `GET /v1/models` to verify registered aliases cleanly without accidentally triggering unwanted multi-gigabyte auto-downloads on misconfigured servers.

## 3. MCP Gateway Notes

Lemonade exposes `POST /mcp` (Streamable HTTP, `2025-06-18` spec) with `lemonade_chat`, `lemonade_transcribe_audio`, and `lemonade_list_models`, **without** embeddings or TTS tools. Consequently, Empati communicates via REST `/v1/*` directly from Android.

## 4. Neuro-Symbolic Hybrid Triage Architecture

Core principle: **Certainty Factor code owns all numeric risk computation; the LLM strictly narrates verified state and retrieved clinical excerpts.**

### 4.1 Role Boundaries

| Component | Responsibility | Forbidden Actions |
|---|---|---|
| `CfInferenceEngine`, `CfKnowledgeBase`, `CfResultGenerator` | Computes CF, risk level, dominant hypotheses, protective factors | Unaudited heuristic tweaks |
| `UserResultPresenter` | Formats human-readable UI summaries without raw scores | Leaking `CF`, `MB`, `MD`, or `%` |
| `EmbeddingIndex` | Retrieves top-k clinical chunks by cosine similarity (`>= 0.35`) | Fabricating references on empty results |
| `LemonadeUnifiedClient` (`Qwen3-4B-GGUF`) | Empathetic narration grounded in `[KONTEKS SKRINING]` + `[RUJUKAN]` | Computing risk scores, issuing diagnoses, or prescribing meds |

Supporting academic context for neuro-symbolic clinical auditability and citation-grounded RAG:
- [Neuro-symbolic AI for auditable cognitive information systems (Nature, 2025)](https://www.nature.com/articles/s43856-025-01194-x)
- [Retrieval-LLM-Symbolic: A Dynamic Neuro-Symbolic Framework for Medical QA](https://dl.acm.org/doi/full/10.1145/3804601.3804680)
- [Retrieval augmented generation for large language models in healthcare](https://pmc.ncbi.nlm.nih.gov/articles/PMC12157099/)
- [Reducing Hallucinations in Medical AI Through Citation Enforced Prompting](https://www.mdpi.com/2076-3417/16/6/3013)

### 4.2 Structured Context Schema (`empati.screening_context/v1`)

Preserves module levels (`HYPERTENSION`, `GBV`), top evidence items (capped at 3 per hypothesis to constrain prefill tokens), unanswered counts, protective factors, and `chronic_stress_pathway` markers—transmitting `cf` strictly for internal LLM urgency calibration while post-generation guardrails strip any numerical leak before display.

### 4.3 Reference Chunk Schema

Each curated chunk carries immutable `id`, `title`, `publisher`, `year`, `section`, `text`, and `url`. Empty retrieval sets switch responses into an honest limitation + screening guidance state rather than free-form medical completion.

## 5. Fallback Ladder & Network Security

Resolution order:
1. Local Lemonade (`:13305`) via contract aliases.
2. Optional Supabase Edge Function (`empati-chat`).
3. Deterministic offline response builder (`ChatFallbackLadder`).

OpenRouter direct paths and Pollinations dependencies are eliminated cleanly from the client. Client timeouts remain interactive (25–60s) with explicit UI differentiation between unreachable servers, loading weights, and busy model slots (`409`).

## 6. External Primary References

- [Lemonade OpenAI-Compatible API](https://lemonade-server.ai/docs/api/openai/)
- [Lemonade MCP Gateway](https://lemonade-server.ai/docs/api/mcp/)
- [Lemonade Server Configuration](https://lemonade-server.ai/docs/guide/configuration/)
- [Lemonade Custom Models & Aliases](https://lemonade-server.ai/docs/guide/configuration/custom-models/)
- [Android Emulator Networking](https://developer.android.com/studio/run/emulator-networking)
