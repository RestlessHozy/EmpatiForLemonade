# ADR 001: Deterministic Triage Engine, Language Model for Narration

Status: accepted  
Date: November 11, 2026  
Context: AMD Lemonade integration for Empati

## Problem

Empati produces two very different kinds of output on one screen. The Certainty Factor expert engine computes risk levels from 26 structured screening items using fixed mathematical rules, while the language model generates conversational explanations. To a patient, both outputs can easily blur together into an undifferentiated "assessment."

Previously, that boundary was defended with an absolute rule: never pass any `CF` numbers to the LLM, and block any occurrence of `CF`, `MB`, `MD`, or `%`. While easy to assert in unit tests, this starved the model of severity calibration—it could not distinguish mild from borderline-severe scores when choosing tone.

Adding Retrieval-Augmented Generation (RAG) amplified the issue: retrieval queries derived from screening results perform far better when structured severity cues guide ranking.

## Decision

The Certainty Factor engine remains the **sole mathematical source of risk scores**. The language model receives screening results as structured JSON (`empati.screening_context/v1`)—including numeric `cf` values solely for internal tone calibration—but is strictly prohibited from printing, recomputing, or re-labeling numeric risk scores. All user-visible labels and categories originate strictly from deterministic Kotlin code.

Specifically:

1. Structured context travels under schema `empati.screening_context/v1`, preserving enum identifiers cleanly with research payloads.
2. Numeric `cf` values calibrate empathy and urgency in LLM responses, never appearing in rendered user text.
3. Post-generation validators (`AnswerGuardrail`) inspect generated responses—not just static prompts—for leaked `CF`, `MB`, `MD` acronyms, percentage signs, standalone risk scores, decimal confidence ratios, or medical diagnosis claims.
4. External clinical claims require a grounded citation chunk carrying `id` and `title`; empty retrieval forces an honest limitation statement rather than parametric fabrication.
5. Legacy prompt-only "zero-leak" tests were replaced with end-to-end output verification tests checking actual text delivered to the UI.

## Consequences

Benefits: Triage scoring remains deterministic and reproducible because scores never originate from stochastic token sampling. Patients are shielded from raw uncalibrated certainty numbers while receiving empathetically scaled guidance backed by inspectable clinical references.

Trade-offs: Requires maintaining a dual enforcement layer (prompt instructions + post-generation validation) and carefully handling false positives where legitimate clinical constants—such as systolic/diastolic thresholds (`140/90 mmHg`) or emergency phone numbers (`SAPA 129`)—appear verbatim inside retrieved reference chunks.

## Alternatives Rejected

- **Zero numeric context at all:** Simple to enforce, but blinds the LLM between borderline vs. extreme risk states (`cf = 0.30` vs. `cf = 0.70`).
- **Exposing raw CF numbers in UI:** Violates safety design—weights have not yet completed multi-site clinician calibration.
- **Letting the LLM classify risk from symptom summaries:** Sacrifices determinism; identical answers could yield non-reproducible risk levels across runs.

## References

Endpoint contracts and integration details: [`docs/research/lemonade-omnimodal-spec.md`](../research/lemonade-omnimodal-spec.md).
