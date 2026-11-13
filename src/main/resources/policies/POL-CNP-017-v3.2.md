---
documentId: POL-CNP-017
title: Card-Not-Present Dispute Handling Procedure
version: 3.2
effectiveDate: 2026-10-01
owner: Payment Fraud Operations
classification: SYNTHETIC_TRAINING
status: ACTIVE
chunkPrefix: cnp
appliesTo: UNAUTHORISED_TRANSACTION, SUSPICIOUS_TRANSACTION, FRAUD_INVESTIGATION
---
# Card-Not-Present Dispute Handling Procedure (synthetic training document)

## 3.1 Scope
This procedure applies to card-not-present (online, in-app and mail order) transactions that a customer reports as not
recognised or not authorised. It does not apply to card-present terminal transactions or bank transfers.

## 3.2 Initial fraud routing
When the customer does not recognise the merchant and at least two independently sourced high-severity anomalies are
present (for example a new device, a location anomaly inconsistent with recent trusted activity, or a high-risk
merchant), the case must be routed to the Fraud Investigation queue for human review. Routing is a recommendation; the
analyst must not block the card, issue a credit or contact the customer from the triage tool.

## 3.3 Authentication evidence
The authentication challenge outcome (for example 3-D Secure result) must be recorded before a final determination.
If the authentication outcome is unavailable, record it as missing information and do not assume either a successful
or failed challenge. A successful strong authentication on a long-standing trusted device weakens an unauthorised-use claim.

## 3.4 Device and location anomalies
A device first observed shortly before the transaction, with no prior trusted sessions, is a high-severity device
anomaly. A transaction location more than 1000 km from the last trusted location within a short time window is a
high-severity location anomaly. Device and location signals are independent sources; contradictory signals (a trusted
device reported far from recent activity) must be shown to the reviewer and lower confidence.
