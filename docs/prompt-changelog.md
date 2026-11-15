# Prompt & schema changelog

| Version | File | Change |
|---|---|---|
| v1 | prompts/fraud-system.txt | Initial: advisory-only role, JSON output, exact citations, abstention → MANUAL_REVIEW |
| v1 | prompts/fraud-user.txt | Slots: case summary, tool evidence, gaps, contradictions, policy context, schema |
| v1 | prompts/followup-system.txt / followup-user.txt | Case-scoped Q&A from recorded facts only |
| v1 | schemas/fraud-recommendation.schema.json | Output contract: enums, confidence 0–1, humanReviewRequired, citations |

Notes: `{{var}}` templating; customer statement rendered via `neutralise()` (escapes `{{`/`}}`);
prompt-injection phrases in input are flagged and listed as indicators, never executed.
