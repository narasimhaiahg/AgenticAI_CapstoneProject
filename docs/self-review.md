# Self-review

**Strengths**
- Fully offline-deterministic path (rules + TF-IDF) makes the whole flow reproducible and testable.
- Strict validation layer between model output and persistence; citations verified against the
  retrieved set; abstention path exercises MANUAL_REVIEW correctly.
- Append-only hash-chained audit; redacted tool traces; case-scoped bounded memory; role-gated
  review with reason requirement.

**Limitations**
- TF-IDF embeddings are weak vs provider models — threshold recalibrated to 0.18; with OpenAI
  embeddings use ~0.72.
- Spring AI / LangChain4j adapters compile and wire but were not exercised against a live model in
  this environment (no API key); contract + fallback paths are tested instead.
- H2 for demo; production would want Postgres + per-user credentials.
- The rules engine is a deterministic baseline — coverage mirrors the fixture scenarios, not all
  fraud typologies.
- UI is functional Thymeleaf, not a SPA; screenshots in docs/screenshots.
