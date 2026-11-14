# Empati Demo Video Script (English Presentation Guide)

Target length: under 3 minutes (`1080p`, 30/60 fps), featuring live Android screen recordings alongside clean architecture slides, measured AMD Lemonade hardware benchmarks, English AI narration, and synced subtitles.

## Pre-Recording Checklist

- Run `scripts/setup-empati-lemonade.ps1` and confirm all five aliases (`empati-llm`, `empati-llm-backup`, `empati-embed`, `empati-stt`, `empati-tts`) appear in `/v1/models`.
- Warm up the server once so multi-gigabyte cold model loads do not stall recording frames.
- Use a demo account with a synthetic test profile; **never expose personal email addresses, IDs, or private Android status-bar notifications** (crop Android status/nav bars cleanly in production).
- For USB real-device verification, keep `adb reverse tcp:13305 tcp:13305` active and build with `LEMONADE_BASE_URL=http://127.0.0.1:13305`.
- Cite latency numbers strictly from real runs of `scripts/bench-empati.ps1` on the target hardware.

## Storyboard (6 Chapters — ~2m 03s Produced Runtime)

### 01 · Problem & Vision (~20s)
- **Visuals:** Android Home screen (`Beranda`) showing `"Risiko sangat tinggi"`, timeline history card, and local server card (`http://127.0.0.1:13305`).
- **Narration:** Introduces the dual burden of domestic violence and hypertension, explains why privacy fears silence high-risk survivors, and presents **Empati** running locally on AMD Lemonade Server port `13305`.

### 02 · Neuro-Symbolic Architecture (~18s)
- **Visuals:** Diagram splitting **Layer 1 (Pure Kotlin, 26 Certainty Factor rules)** from **Layer 2 (AMD Lemonade `:13305` — 4 specialized local models)** synced with Screening consent and Q1/Q2 UI.
- **Narration:** Explains why separating deterministic clinical math from language-model narration guarantees zero hallucinated risk scores.

### 03 · Deterministic Screening & History (~20s)
- **Visuals:** Longitudinal screening history (`Riwayat skrining`) + follow-up consultation surfacing verified **SAPA 129** emergency hotline numbers (`129` / WhatsApp `08111-129-129`).
- **Narration:** Highlights hidden expert rule weights (`CF`, `MB`, `MD` never leak to patients) and deterministic hotline protection.

### 04 · Live Local RAG & Verifiable Citations (~18s)
- **Visuals:** Tapping `"Apa itu hipertensi?"` -> clean single-bubble transition from `"Empati sedang mengetik…"` to live SSE token streaming -> tapping the `WHO · 2023` citation chip -> inspecting the `World Health Organization` excerpt bottom sheet.
- **Narration:** Walks through `nomic-embed-text-v1-GGUF` retrieval over Kemenkes & WHO guidelines and single-bubble UI streaming.

### 05 · Omni-Modal Voice STT & Natural Indonesian TTS (~19s)
- **Visuals:** Tapping speaker playback button -> tapping microphone (`16 kHz` WAV capture bar `Merekam…`) with one-tap Cancel.
- **Narration:** Explains `Whisper-Large-v3-Turbo` (`empati-stt`) transcription and Empati's contract-preserving routing between offline Android `id-ID` synthesis and Lemonade `empati-tts`.

### 06 · AMD Hardware Benchmarks & Closing (~22s)
- **Visuals:** Profile screen (`Status layanan: Siap dipakai`, tapping `Periksa ulang`) beside real 5x measured benchmark results (`Ryzen 7 7735HS`, `15.25 GB RAM`, Lemonade `v2026.46.2`) and `203 / 203` passing unit tests.
- **Narration:** Summarizes alias-driven portability across AMD hosts, 1-command reproducible setup, and MIT open-source availability.

## Production Rules

- **Never fabricate speedups:** Cut silent wait/swap pauses cleanly rather than artificially accelerating token streaming animation.
- **Protect user privacy:** Mask or omit real email/account cards and notification trays before capture.
- **Preserve evidence:** Retain raw screen captures, `narration.json`, `.srt`/`.ass` subtitle tracks, and measured benchmark tables alongside final video exports.
