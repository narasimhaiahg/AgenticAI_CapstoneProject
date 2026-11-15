# Architecture

```
Browser (Thymeleaf UI)          REST client
       |                              |
   UiController                CaseApiController  ── Security (2 chains: UI form-login+CSRF / API basic stateless)
       |                              |
       +------------+-----------------+
                    |
   FraudCaseService (intake, state machine NEW→ENRICHING→READY_FOR_REVIEW|INCOMPLETE→UNDER_REVIEW→ROUTED|CLOSED)
                    |
        InvestigationOrchestrator
          |   |          |            |           |
      Planner  EvidenceToolkit   PolicyRetriever  ProcessorRegistry   ReviewService / FollowUpService
          |   (8 allow-listed   (local TF-IDF,    (rules | spring-ai  |
          |    tools, timeout,   threshold +      | langchain4j;      CaseMemoryService
          |    traced, redacted) abstention)      | validation +      (per-case bounded history)
          |        |                 |            |  rules fallback)         |
          +--------+--------+--------+            +-----------+----------------+
                   |        |                                 |
            SyntheticFixtureRepository                 RecommendationValidator
            ToolTraceSink → tool_traces                (schema, citation ⊆ retrieved,
                   |                                    humanReviewRequired, action-claim guard)
                   +----------------+-------------------+
                                    |
                 JPA (Flyway V1) + AuditWriter (append-only, hash-chained events)
```

- **Contract**: `agent/common/FraudCaseProcessor` — `analyze(FraudCaseContext)` and
  `answerFollowUp(FollowUpContext)`; adapters: `agent/rules`, `agent/springai`, `agent/langchain4j`.
- **Determinism**: rules engine and TF-IDF retrieval need no network; AI modes are optional.
- **Safety**: recommendation schema validated; procedural claims require citations; action claims in
  output are rejected; statement markup/PAN rejected at intake; prompt injection flagged as data.
