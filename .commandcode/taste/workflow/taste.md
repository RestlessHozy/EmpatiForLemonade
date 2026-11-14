# Workflow taste

- Expects changes to be implemented directly in the existing project files — not delivered as example code snippets or explanations. Confidence: 0.85
- Wants the existing project (directory structure, architecture, current implementation) analyzed before any new implementation is written; explicitly says not to start building before understanding the codebase. Confidence: 0.8
- Expects project markdown documentation to stay in sync with the actual code; flags stale docs (especially frontend/UI-UX descriptions) and wants documentation regenerated from the real implementation rather than left describing an older version. Confidence: 0.75
- Wants an explicit approval gate before code changes: present the audit/plan, then wait for an explicit confirmation (e.g. "CONFIRM") instead of editing files straight away. Confidence: 0.7
- For technical research, wants facts pinned to the target platform's own official docs/model registry rather than carried over from a previously used stack — explicitly forbids assuming ports/endpoints (e.g. reusing Ollama's `11434` instead of Lemonade's `13305`). Confidence: 0.65
- Grants broad discretion beyond the enumerated scope: expects the agent to also propose/do whatever it judges will most improve the outcome (e.g. "do everything you think raises the project's chances of winning") rather than strictly limiting work to the listed items. Confidence: 0.6
- Definition of done includes verification: new logic must ship with unit tests, the project must be built successfully, and changelog/docs updated — not just code written. Confidence: 0.75
- Expects work to keep moving through the remaining tickets rather than stopping early; when the agent pauses and suggests resuming in a new session, the correction is to continue and finish the remaining items now, all the way to done. Confidence: 0.7
- Works test-first: wants tests written and confirmed failing at the agreed seams before implementation, and wants the test suite plus type checks re-run after each ticket before moving to the next one. Confidence: 0.7
