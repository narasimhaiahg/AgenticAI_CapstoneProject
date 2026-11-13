---
documentId: POL-ESC-004
title: Escalation, Insufficient Evidence and Manual Review Standard
version: 1.1
effectiveDate: 2026-08-01
owner: Payment Fraud Operations
classification: SYNTHETIC_TRAINING
status: ACTIVE
chunkPrefix: esc
appliesTo: INSUFFICIENT_EVIDENCE, GENUINE_TRANSACTION, SUSPICIOUS_TRANSACTION, MANUAL_REVIEW
---
# Escalation, Insufficient Evidence and Manual Review Standard (synthetic training document)

## 5.1 Insufficient evidence
If the transaction record cannot be found or mandatory evidence is unavailable, the case is classified as insufficient
evidence and placed in the Manual Review queue. Analysts must request the missing record rather than infer it.

## 5.2 Contradictory signals
Where device, location or authentication signals contradict each other, the contradiction must be shown to the
reviewer, confidence reduced, and the case placed in Manual Review unless independent high-severity anomalies support
fraud routing.

## 5.3 Customer-authorised activity patterns
Where strong authentication succeeded on a trusted device in the home location and the customer has repeated similar
claims later confirmed as authorised, the activity may be genuine. Such cases go to Manual Review for reviewer
confirmation; they are never closed automatically.

## 5.4 Reviewer disposition
Every recommendation requires a human reviewer disposition with a written rationale before a case is routed or closed.
