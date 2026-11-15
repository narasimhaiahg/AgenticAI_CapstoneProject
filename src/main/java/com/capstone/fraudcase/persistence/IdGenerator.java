package com.capstone.fraudcase.persistence;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Year;

/** Human-readable, database-sequence backed identifiers (CASE-2026-0042, EV-101, TR-118 ...). */
@Component
public class IdGenerator {

    private final SequenceAllocator sequences;
    private final Clock clock;

    public IdGenerator(SequenceAllocator sequences, Clock clock) {
        this.sequences = sequences;
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

    // Delegates to SequenceAllocator so the counter row lock is taken and released inside
    // its own REQUIRES_NEW transaction — self-invocation of a @Transactional method here
    // would bypass the proxy and leave the SELECT FOR UPDATE lock unmanaged.
    private long next(String sequence) {
        return sequences.next(sequence);
    }
}
