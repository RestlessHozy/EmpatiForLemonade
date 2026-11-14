# Evaluation of PageIndex as an Empati RAG Layer

Date: November 12, 2026  
Status: recommendation, not implemented  
Question: Should [VectifyAI/PageIndex](https://github.com/VectifyAI/PageIndex) be adopted inside Empati's RAG stack without compromising eligibility or privacy guarantees for the AMD Lemonade Developer Challenge?

## Verified Findings

Inspected directly from the PageIndex repository on November 12, 2026:

| Dimension | Finding |
|---|---|
| License | MIT (compatible with Empati's MIT license) |
| Runtime & distribution | Python (`pip install pageindex`). No Kotlin or native Android library exists |
| Architecture | Builds hierarchical tree tables-of-contents with per-node summaries over long documents; an agentic LLM navigates the tree instead of embedding/chunking |
| Dependencies | `litellm`, `openai`, `openai-agents`, `mcp`, `PyPDF2`, `pypdfium2` |
| Model requirements | Even local SDK mode requires external or OpenAI-compatible LLM calls per traversal step |
| Cloud services | PageIndex Cloud & Page Search API route user queries and documents off-device |

## Open Policy Questions

Because complete competition legalese varies by track, our repository anchor remains clear: all inference should execute locally through `:13305` (chat, embeddings, STT, TTS) with zero involuntary cloud exfiltration. Any addition that introduces external cloud dependencies conflicts directly with Empati's privacy posture.

## Technical Fit Assessment

1. **Does not run on Android:** Device-side retrieval runs natively in Kotlin. PageIndex would only run offline at build time on Python.
2. **Current corpus size is small & curated:** `EmpatiCorpus` holds 30 short, domain-curated excerpts. Hierarchical tree traversal offers no advantage over exact cosine similarity here.
3. **Additional latency tax:** Tree navigation adds extra sequential LLM passes before answer generation begins—adding noticeable delay on edge 4B models (`Qwen3-4B-GGUF`).
4. **Unverified reliability on 4B models:** Published PageIndex results rely on frontier models; agentic tree routing behavior on 4B/1.7B GGUF models is unverified.
5. **Determinism tension with ADR-001:** Cosine similarity (`threshold 0.35`) is deterministic and reproducible. LLM-driven tree navigation can select varying sections across runs for identical queries.

## Options Considered

- **Option A — Retain `empati-embed` vector search (current default):** Zero new failure modes; fast (~44 ms embeddings), deterministic, and 100% handled by Lemonade `:13305`.
- **Option B — Build-time tree pruning only:** Pre-index large multi-hundred-page WHO PDFs on desktop via LiteLLM -> Lemonade `:13305`, bundle static JSON TOC in Android assets, and narrow vector search inside selected branches. Only worthwhile if corpus scales up dramatically.
- **Option C — PageIndex Cloud / Page Search API:** Rejected—routes private health queries externally, contradicting local-first safety guarantees.

## Recommendation

Keep **Option A** for submission. Revisit **Option B** only if the clinical knowledge corpus expands to multi-hundred-page guidelines, gated behind benchmarked hit-rate and end-to-end latency comparisons.
