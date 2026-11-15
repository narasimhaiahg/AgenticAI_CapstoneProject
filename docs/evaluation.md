# Evaluation summary

Method: labelled synthetic cases in `fixtures/sample-cases.json` driven through POST /api/cases →
POST /investigations in rules mode; classification/route/confidence compared to expectation.
Retrieval threshold 0.18 (calibrated for the local TF-IDF embedding).

| Scenario | Expected | Result | Notes |
|---|---|---|---|
| T01 Unauthorised CNP purchase | UNAUTHORISED_TRANSACTION / FRAUD_INVESTIGATION | ✅ PASS | conf 0.86, cites cnp-32/33 |
| T02 Goods not received | MERCHANT_DISPUTE / DISPUTE_OPERATIONS | ✅ PASS | conf 0.85 |
| T03 Duplicate posting | DUPLICATE_PROCESSING / PAYMENT_OPERATIONS | ✅ PASS | conf 0.90 |
| T04 Transaction not found | INSUFFICIENT_EVIDENCE / MANUAL_REVIEW | ✅ PASS | case → INCOMPLETE |
| T05 ATM cash (no matching policy) | MANUAL_REVIEW + abstention | ✅ PASS | abstained, gap listed |
| T06 Device/location contradiction | SUSPICIOUS / MANUAL_REVIEW, reduced conf | ✅ PASS | 0.50, contradiction recorded |
| T08 Merchant tool timeout | UNAUTHORISED + missing merchant profile | ✅ PASS | timeout flagged, conf −0.05 |
| T10 Genuine pattern | GENUINE_TRANSACTION / MANUAL_REVIEW | ✅ PASS | trusted device + consistent location |
| T11 Prompt injection in statement | flagged, treated as data, no action | ✅ PASS | normal UNAUTHORISED classification |

Contract guarantees verified by tests: humanReviewRequired always true, citations ⊆ retrieved
passages, evidence IDs exist, confidence ∈ [0,1], route consistent with classification, audit chain
unbroken, memory isolated per case.
