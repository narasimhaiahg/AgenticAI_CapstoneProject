-- Fraud investigation capstone schema (synthetic training data only).
-- Portable across MySQL 8 and H2: no vendor sequences; counters live in id_sequence
-- and are claimed under SELECT ... FOR UPDATE by IdGenerator.
CREATE TABLE id_sequence (
    name                  VARCHAR(32)  PRIMARY KEY,
    next_val              BIGINT       NOT NULL
);
INSERT INTO id_sequence (name, next_val) VALUES
    ('case_seq', 42), ('evidence_seq', 101), ('trace_seq', 118),
    ('recommendation_seq', 1), ('review_seq', 1), ('audit_seq', 1);

CREATE TABLE fraud_case (
    case_id               VARCHAR(32)  PRIMARY KEY,
    masked_customer_ref   VARCHAR(32)  NOT NULL,
    home_region           VARCHAR(16)  NOT NULL,
    status                VARCHAR(24)  NOT NULL,
    intake_channel        VARCHAR(24)  NOT NULL,
    reported_at           TIMESTAMP(6) NOT NULL,
    created_by            VARCHAR(64)  NOT NULL,
    synthetic_confirmed   BOOLEAN      NOT NULL,
    review_required       BOOLEAN      NOT NULL,
    current_run           INT          NOT NULL DEFAULT 0,
    assigned_route        VARCHAR(32),
    updated_at            TIMESTAMP(6) NOT NULL,
    row_version           BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE transaction_claim (
    claim_id              VARCHAR(40)  PRIMARY KEY,
    case_id               VARCHAR(32)  NOT NULL REFERENCES fraud_case(case_id),
    transaction_ref       VARCHAR(40)  NOT NULL,
    amount                DECIMAL(15,2) NOT NULL,
    currency              VARCHAR(3)   NOT NULL,
    occurred_at           TIMESTAMP(6) NOT NULL,
    transaction_status    VARCHAR(16)  NOT NULL,
    transaction_channel   VARCHAR(24)  NOT NULL,
    masked_merchant       VARCHAR(64)  NOT NULL,
    issue_type            VARCHAR(32)  NOT NULL,
    customer_statement    VARCHAR(1000) NOT NULL,
    statement_original    VARCHAR(1000) NOT NULL,
    card_possession       VARCHAR(16)  NOT NULL,
    merchant_recognised   BOOLEAN      NOT NULL,
    injection_indicators  VARCHAR(255)
);

CREATE TABLE evidence_item (
    evidence_id           VARCHAR(24)  PRIMARY KEY,
    case_id               VARCHAR(32)  NOT NULL REFERENCES fraud_case(case_id),
    run_number            INT          NOT NULL,
    category              VARCHAR(24)  NOT NULL,
    evidence_type         VARCHAR(48)  NOT NULL,
    severity              VARCHAR(8)   NOT NULL,
    value_summary         VARCHAR(500) NOT NULL,
    provenance            VARCHAR(24)  NOT NULL,
    source_ref            VARCHAR(40)  NOT NULL,
    observed_at           TIMESTAMP(6) NOT NULL
);
CREATE INDEX ix_evidence_case ON evidence_item(case_id, run_number);

CREATE TABLE tool_trace (
    trace_id              VARCHAR(24)  PRIMARY KEY,
    case_id               VARCHAR(32)  NOT NULL REFERENCES fraud_case(case_id),
    run_number            INT          NOT NULL,
    correlation_id        VARCHAR(40)  NOT NULL,
    tool_name             VARCHAR(48)  NOT NULL,
    request_hash          VARCHAR(64)  NOT NULL,
    status                VARCHAR(16)  NOT NULL,
    duration_ms           BIGINT       NOT NULL,
    result_summary        VARCHAR(500) NOT NULL,
    result_json           VARCHAR(4000),
    origin                VARCHAR(24)  NOT NULL,
    started_at            TIMESTAMP(6) NOT NULL
);
CREATE INDEX ix_trace_case ON tool_trace(case_id, run_number);

CREATE TABLE recommendation (
    recommendation_id     VARCHAR(24)  PRIMARY KEY,
    case_id               VARCHAR(32)  NOT NULL REFERENCES fraud_case(case_id),
    run_number            INT          NOT NULL,
    correlation_id        VARCHAR(40)  NOT NULL,
    mode                  VARCHAR(16)  NOT NULL,
    requested_mode        VARCHAR(16)  NOT NULL,
    fallback_reason       VARCHAR(500),
    classification        VARCHAR(32)  NOT NULL,
    route                 VARCHAR(32)  NOT NULL,
    risk_level            VARCHAR(16)  NOT NULL,
    confidence            DOUBLE PRECISION NOT NULL,
    human_review_required BOOLEAN      NOT NULL,
    rationale             VARCHAR(1500) NOT NULL,
    evidence_ids          VARCHAR(2000) NOT NULL,
    missing_information   VARCHAR(2000) NOT NULL,
    contradictions        VARCHAR(2000) NOT NULL,
    rule_ids              VARCHAR(500)  NOT NULL,
    tool_trace_ids        VARCHAR(1000) NOT NULL,
    input_facts           VARCHAR(4000) NOT NULL,
    priority              VARCHAR(4),
    sla_target_at         TIMESTAMP(6),
    sla_trace_id          VARCHAR(24),
    retrieval_query       VARCHAR(1000),
    retrieval_best_score  DOUBLE PRECISION,
    retrieval_abstained   BOOLEAN      NOT NULL,
    created_at            TIMESTAMP(6) NOT NULL
);
CREATE INDEX ix_recommendation_case ON recommendation(case_id, run_number);

CREATE TABLE policy_citation (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id               VARCHAR(32)  NOT NULL REFERENCES fraud_case(case_id),
    run_number            INT          NOT NULL,
    recommendation_id     VARCHAR(24),
    document_id           VARCHAR(32)  NOT NULL,
    title                 VARCHAR(200) NOT NULL,
    version               VARCHAR(16)  NOT NULL,
    effective_date        DATE         NOT NULL,
    section               VARCHAR(16)  NOT NULL,
    section_title         VARCHAR(200) NOT NULL,
    chunk_id              VARCHAR(48)  NOT NULL,
    score                 DOUBLE PRECISION NOT NULL,
    passage               VARCHAR(4000) NOT NULL,
    cited                 BOOLEAN      NOT NULL
);
CREATE INDEX ix_citation_case ON policy_citation(case_id, run_number);

CREATE TABLE review_disposition (
    review_id             VARCHAR(24)  PRIMARY KEY,
    case_id               VARCHAR(32)  NOT NULL REFERENCES fraud_case(case_id),
    recommendation_id     VARCHAR(24),
    reviewer_id           VARCHAR(64)  NOT NULL,
    disposition           VARCHAR(24)  NOT NULL,
    selected_route        VARCHAR(32),
    reason                VARCHAR(2000) NOT NULL,
    missing_info_acknowledged BOOLEAN  NOT NULL,
    from_status           VARCHAR(24)  NOT NULL,
    to_status             VARCHAR(24)  NOT NULL,
    recorded_at           TIMESTAMP(6) NOT NULL
);

CREATE TABLE audit_event (
    event_id              VARCHAR(24)  PRIMARY KEY,
    sequence_no           BIGINT       NOT NULL UNIQUE,
    case_id               VARCHAR(32),
    event_type            VARCHAR(40)  NOT NULL,
    actor_type            VARCHAR(16)  NOT NULL,
    actor_id              VARCHAR(64)  NOT NULL,
    correlation_id        VARCHAR(40),
    summary               VARCHAR(500) NOT NULL,
    payload_hash          VARCHAR(64)  NOT NULL,
    previous_hash         VARCHAR(64)  NOT NULL,
    recorded_at           TIMESTAMP(6) NOT NULL
);
CREATE INDEX ix_audit_case ON audit_event(case_id, sequence_no);

CREATE TABLE case_message (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id               VARCHAR(32)  NOT NULL REFERENCES fraud_case(case_id),
    role                  VARCHAR(16)  NOT NULL,
    message_text          VARCHAR(4000) NOT NULL,
    mode                  VARCHAR(16),
    created_at            TIMESTAMP(6) NOT NULL
);
CREATE INDEX ix_message_case ON case_message(case_id, id);
