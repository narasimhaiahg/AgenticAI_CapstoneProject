# AGENTS.md

## Build & run
- `mvn package`; `mvn test`. Java 17. Tests run on in-memory H2 — no MySQL needed for `mvn test`.
- Run: `docker compose up -d` (MySQL 8 on localhost:3306, db `fraudcase_db`, user `capstone`/`capstone-dev`), then `CAPSTONE_DEMO_PASSWORD=demo-pass java -jar target/fraud-investigation-agent-1.0.0-SNAPSHOT.jar`.
- Maven central may 429; `~/.m2/settings.xml` already mirrors to the GCS bucket.

## Conventions
- `agent/common/FraudCaseProcessor` is the stable contract; adapters must return `RawRecommendation`
  and pass through `RecommendationValidator` — never trust model output directly.
- All customer text is untrusted data: run `PromptInjectionDetector`, `MaskingRules`
  (no 12–19 digit runs, no markup), and neutralise `{{`/`}}` before templating.
- Recommendations must cite only retrieved `PolicyPassage` chunks; abstention → MANUAL_REVIEW.
- `humanReviewRequired` is always true; no financial actions or customer contact anywhere.
- Audit via `AuditWriter` only (append-only, hash-chained). Never update/delete audit rows.
- Evidence tool names must be in `ToolNames`; all calls go through `ToolExecutor` (timeout + trace).
- Fixtures are deterministic: `SyntheticFixtureRepository` with seed `fraud-training-v1`.
