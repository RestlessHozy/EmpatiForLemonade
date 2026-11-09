# Workflow taste

- Expects changes to be implemented directly in the existing project files — not delivered as example code snippets or explanations. Confidence: 0.85
- Wants the existing project (directory structure, architecture, current implementation) analyzed before any new implementation is written; explicitly says not to start building before understanding the codebase. Confidence: 0.8
- Expects project markdown documentation to stay in sync with the actual code; flags stale docs (especially frontend/UI-UX descriptions) and wants documentation regenerated from the real implementation rather than left describing an older version. Confidence: 0.75
- Definition of done includes verification: new logic must ship with unit tests, the project must be built successfully, and changelog/docs updated — not just code written. Confidence: 0.75
