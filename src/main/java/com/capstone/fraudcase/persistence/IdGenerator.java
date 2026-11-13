package com.capstone.fraudcase.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Year;

/** Human-readable, database-sequence backed identifiers (CASE-2026-0042, EV-101, TR-118 ...). */
@Component
public class IdGenerator {

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public IdGenerator(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public String caseId() {
        return "CASE-%d-%04d".formatted(Year.now(clock).getValue(), next("case_seq"));
    }

    public String evidenceId() {
        return "EV-" + next("evidence_seq");
    }

    public String traceId() {
        return "TR-" + next("trace_seq");
    }

    public String recommendationId() {
        return "REC-" + next("recommendation_seq");
    }

    public String reviewId() {
        return "RV-" + next("review_seq");
    }

    public long auditSequence() {
        return next("audit_seq");
    }

    private long next(String sequence) {
        Long v = jdbc.queryForObject("SELECT NEXT VALUE FOR " + sequence, Long.class);
        if (v == null) {
            throw new IllegalStateException("Sequence " + sequence + " returned no value");
        }
        return v;
    }
}
