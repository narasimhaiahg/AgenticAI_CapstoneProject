-- Content-integrity columns for audit events: lets brokenLinks() recompute each
-- event's hash from stored fields instead of trusting stored hashes. Rows written
-- before this migration have NULL values and are verified by chain links only.
ALTER TABLE audit_event ADD COLUMN content_hash VARCHAR(64);
ALTER TABLE audit_event ADD COLUMN payload_digest VARCHAR(64);
