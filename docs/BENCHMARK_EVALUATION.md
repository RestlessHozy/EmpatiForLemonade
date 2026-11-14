# Empati Performance Evaluation on AMD Lemonade

This document describes how Empati's pipeline performance is measured, reports directly observed benchmarks collected on an AMD developer laptop (`AMD Ryzen 7 7735HS`), and analyzes architectural capacity limits on unified-memory AMD hardware (`Ryzen AI Max+ 395 / Strix Halo`).

## What Is Measured

`scripts/bench-empati.ps1` measures end-to-end single-request latency across four local OpenAI-compatible endpoints on port `13305`:

| Metric Name | Endpoint | Request Payload |
|---|---|---|
| `chat` | `POST /v1/chat/completions` | Short health prompt, `max_tokens=64`, non-streaming, `enable_thinking=false` |
| `embeddings` | `POST /v1/embeddings` | Single health query string |
| `transcription` | `POST /v1/audio/transcriptions` | 1-second silent WAV, 16 kHz mono PCM16, `language=id` |
| `speech` | `POST /v1/audio/speech` | Single screening summary sentence, `mp3` |

The script runs one chat warmup request, executes `-Repeats` sequential iterations per endpoint, and reports the median latency. Outputs are written to `docs/BENCHMARK_RESULTS.md`.

What the script does **not** measure:

- Streaming Time-to-First-Token (`TTFT`). Because the benchmark invokes non-streaming completion, chat figures represent full 64-token completion latency rather than first-token response.
- Cold model initialization latency right after starting Lemonade Server.
- Throughput under concurrent multi-user traffic.
- Peak VRAM/RAM usage, power draw, or thermals (collect separately via `rocm-smi`, Windows Task Manager, or Lemonade server diagnostics).

To measure streaming TTFT, send `stream=true` and record the delta between request dispatch and the first SSE `data:` frame carrying `delta.content`.

## Methodology

1. Run `scripts/setup-empati-lemonade.ps1` on the target AMD machine and confirm all five aliases appear in `/v1/models`.
2. Allow initial model loading to complete before recording stable measurements (warmup sends one completion call).
3. Run `pwsh ./scripts/bench-empati.ps1 -BaseUrl http://127.0.0.1:13305 -Repeats 5 -ReportPath docs/BENCHMARK_RESULTS.md`.
4. Always record exact CPU, GPU/NPU, unified RAM size, driver version, Lemonade version, and active models alongside results.
5. Test both cold and warm runs if isolating model load penalties.

Medians prevent cold-start outliers from skewing representative steady-state speed; production SLAs should also capture 95th-percentile latency (`p95`).

## Measured Hardware Benchmark (`scripts/bench-empati.ps1 -Repeats 5`)

Measured directly on **2026-11-14 23:03:04 +08:00** on an AMD developer laptop (`AMD Ryzen 7 7735HS`, 8C/16T, `AMD Radeon(TM) Graphics` driver `32.0.21045.16010`, `15.25 GB` unified RAM, Lemonade Server `v2026.46.2`, `http://127.0.0.1:13305`, all four runtime models co-loaded concurrently):

| Metric | Endpoint / Alias | Hardware | Conditions | Median Result (Success) |
|---|---|---|---|---|
| Total 64-token completion | `chat` (`empati-llm` / `Qwen3-4B-GGUF`) | Ryzen 7 7735HS, Radeon Graphics, 15.25 GB | Warm, all 4 models co-resident | **33704.2 ms** (`5/5`) |
| Single-query embedding | `embeddings` (`empati-embed` / `nomic-embed-text-v1-GGUF`) | Ryzen 7 7735HS, Radeon Graphics, 15.25 GB | Warm | **44.1 ms** (`5/5`) |
| 1-second audio STT | `transcription` (`empati-stt` / `Whisper-Large-v3-Turbo`) | Ryzen 7 7735HS, Radeon Graphics, 15.25 GB | Warm | **10575.1 ms** (`5/5`) |
| Single-sentence TTS | `speech` (`empati-tts` / `kokoro-v1`) | Ryzen 7 7735HS, Radeon Graphics, 15.25 GB | Warm, CPU ONNX | **1436.3 ms** (`5/5`) |
| Streaming chat TTFT | `chat` (`stream=true`) | Ryzen 7 7735HS | — | Not measured by script |

Field observation: when all four models (`Qwen3-4B-GGUF` Q4_0, `nomic-embed-text-v1-GGUF`, `Whisper-Large-v3-Turbo`, and `kokoro-v1`) stayed resident simultaneously inside `15.25 GB` RAM, free physical memory dropped to `~0.41 GB`, inducing Windows page-file swapping during larger GPU/STT passes. Even under co-residency pressure, `nomic-embed` (**44.1 ms**) and `kokoro-v1` (**1.44 s**) stayed sub-2-second, with 100% request reliability (`5/5` across all endpoints).

Do not compare benchmark numbers across hosts without matching model quantizations, free RAM headroom, prompt length, and GPU driver versions.

## Theoretical Architecture Analysis on Ryzen AI Max+ 395 (`Strix Halo`)

The specifications below reflect public platform parameters rather than direct measurements performed in this repository:

- 16 Zen 5 cores / 32 threads.
- Radeon 8060S GPU (40 RDNA 3.5 compute units).
- XDNA 2 NPU (50 TOPS INT8).
- Unified LPDDR5X-8000 memory across a 256-bit bus (up to 128 GB).
- Theoretical peak memory bandwidth: `8000 MT/s x 256 bit / 8 = 256 GB/s` (sustained real-world bandwidth is lower).

Unified memory eliminates weight copies between system RAM and GPU VRAM. Chat, Whisper, Kokoro, and embedding weights share one pool cleanly without manual model eviction, removing the OS paging bottleneck observed on 16 GB hosts.

Consequently, all active models compete for shared memory bandwidth. During concurrent voice transcription and token decode, per-process throughput drops gracefully without running out of memory.

### Theoretical Decode Ceiling

Autoregressive decoding reads model weights plus KV cache once per generated token:

```text
tokens/sec <= memory_bandwidth / (weight_bytes + kv_bytes_per_token)
```

For a 4B Q4 model (~2.5 GB weights) at a 2048-token context, theoretical upper bounds sit around 90–100 tok/s at 256 GB/s, with typical real-world kernels achieving roughly 30–60% of theoretical bandwidth depending on GPU/NPU backend and context size. Prompt prefill is compute-bound rather than purely bandwidth-bound, so clinical screening context size shapes TTFT directly.

### Memory Budget

| Component | Estimate | Notes |
|---|---|---|
| Chat LLM 4B Q4 | ~2.5 GB | Model weights excluding KV cache |
| KV cache (2048 ctx) | Hundreds of MB | Depends on layer count and precision |
| Whisper Large v3 Turbo | 1.62 GB | Lemonade registry model footprint |
| `nomic-embed-text-v1-GGUF` | 0.078 GB | Lemonade registry footprint |
| `kokoro-v1` | < 1 GB | Depends on active voice binaries |
| OS & host overhead | 2–4 GB | Platform dependent |

Total co-resident footprint fits comfortably under 8 GB+ of model working set, meaning 32 GB+ Strix Halo configurations hold all models entirely in fast physical memory with ample breathing room.

### Multi-User Scalability

For a single patient session, requests serialize naturally (voice capture finishes before transcript submission; chat completion finishes before optional audio playback). Noticeable queuing happens if TTS synthesis or a new STT pass starts immediately while an active generation has not finalized.

Before sharing one AMD host across multiple clinic tablets, measure:

- Concurrent chat throughput across 2–4 simultaneous sessions.
- STT latency when GPU decode is concurrently active.
- Peak resident RAM with all models pinned.
- Frequency of `409 slots_pinned_error` responses from Lemonade.

Scale horizontally by throttling clients per host, isolating heavy STT onto a dedicated worker if saturated, or adding server replicas.

## Internal Engineering Targets

Reference targets used to evaluate demo-host readiness (not measured figures unless listed in the Measured table above):

| Scenario | Engineering Target |
|---|---|
| Warm 64-token chat | < 8 seconds (32 GB+ host without swapping) |
| 10-second audio transcription | < 4 seconds |
| Single-sentence TTS | < 3 seconds |
| Single-query embedding | < 300 ms |

## Reproducing Locally

```powershell
pwsh ./scripts/setup-empati-lemonade.ps1
pwsh ./scripts/bench-empati.ps1 -BaseUrl http://127.0.0.1:13305 -Repeats 5
pwsh ./scripts/tests/verify-dryrun.ps1
```

Raw machine results write to `docs/BENCHMARK_RESULTS.md` (gitignored so developer hardware runs never overwrite target demo hardware results inadvertently).
