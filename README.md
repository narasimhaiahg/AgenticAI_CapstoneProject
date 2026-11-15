# Fraud Investigation Assistant (Agentic AI Capstone)

An explainable agentic assistant for payment-fraud triage. It collects deterministic synthetic
evidence, retrieves applicable policy with exact citations, recommends a case route — and always
leaves the final disposition to a human reviewer. **It never blocks a card, issues a refund, moves
funds, files a report or contacts a customer.**

## Stack

Java 17 · Spring Boot 3.4.5 · Spring AI 1.0.1 & LangChain4j 1.0.1 (adapters, `spring-ai` /
`langchain4j` profiles) · deterministic rules engine · local TF-IDF embeddings (no API key needed
for RAG) · Spring Data JPA + Flyway + MySQL 8 · Thymeleaf UI · Spring Security.

## Quick start

```bash
docker compose up -d   # MySQL 8 on localhost:3306, database fraudcase_db
mvn package
java -jar target/fraud-investigation-agent-1.0.0-SNAPSHOT.jar
```

DB defaults: `jdbc:mysql://localhost:3306/fraudcase_db`, user `capstone`, password `capstone-dev`
(override with `CAPSTONE_DB_URL` / `CAPSTONE_DB_USER` / `CAPSTONE_DB_PASSWORD`). Flyway creates the
schema on first run. `mvn test` needs no MySQL — tests run on in-memory H2.

Open http://localhost:8080 → login. Demo users (password = `CAPSTONE_DEMO_PASSWORD`; if unset a
random one is generated and logged at startup):

| User | Roles |
|---|---|
| `agent` | create cases |
| `investigator` | run investigations, ask follow-ups |
| `reviewer` | review + disposition |
| `lead` | reviewer + close/reject + audit |
| `auditor` | read audit trail |

Run `CAPSTONE_DEMO_PASSWORD=demo-pass java -jar ...` for a known password.

## Modes

`CAPSTONE_PROCESSOR_MODE` = `rules` (default, fully offline) | `spring-ai` | `langchain4j`.
AI modes need `CAPSTONE_AI_API_KEY` (+ `CAPSTONE_AI_BASE_URL`, `CAPSTONE_AI_MODEL`). If the model is
unavailable or its output fails validation, the orchestrator falls back to rules
(`capstone.ai.fallback-to-rules=true`). The framework-neutral contract is
`agent/common/FraudCaseProcessor`.

## Flow

POST `/api/cases` (intake, masked refs only) → POST `/api/cases/{id}/investigations` (planned tool
calls → evidence → policy retrieval → processor → schema-validated recommendation) → human review
at `/ui/cases/{id}/review` (accept / reroute / escalate / request info / close). Every step writes a
hash-chained, append-only audit event; tool calls are traced with request hashes and redacted
results; conversation memory is isolated per case.

Evidence tools: `getTransactionDetails`, `getDeviceSignals`, `compareRecentLocations`,
`getMerchantRiskProfile`, `getPriorDisputes`, `calculateCaseSla`, `retrievePolicyEvidence`,
`saveAuditEvent` — allow-listed, timeout-bounded, deterministic fixtures (`fixtures/scenarios.json`,
seed `fraud-training-v1`).

Policies in `src/main/resources/policies/*.md` carry front-matter (documentId, version,
effectiveDate, chunkPrefix, appliesTo, declaredStatus). Only ACTIVE, effective, non-quarantined
chunks are retrievable; below-threshold retrieval abstains → MANUAL_REVIEW.

## Tests

```bash
mvn test   # 37 tests: rules, validator, guardrails, lifecycle, tools, policy pipeline, API, contract
```

## Docs

- [docs/architecture.md](docs/architecture.md) — component view
- [docs/evaluation.md](docs/evaluation.md) — scenario results
- [docs/self-review.md](docs/self-review.md) — known limitations
- [docs/prompt-changelog.md](docs/prompt-changelog.md) — prompt/schema versions
- [application-example.yml](application-example.yml) — config reference (no secrets)
- `src/main/resources/prompts/` — system/user/follow-up prompts + output JSON schema

All data is synthetic; PAN-like digit runs and markup are rejected at intake; customer text is
treated as untrusted data (prompt-injection indicators are flagged, never obeyed).
