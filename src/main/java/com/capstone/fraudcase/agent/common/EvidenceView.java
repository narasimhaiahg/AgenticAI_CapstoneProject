package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.domain.EvidenceCategory;
import com.capstone.fraudcase.domain.EvidenceSeverity;
import com.capstone.fraudcase.domain.Provenance;

import java.time.Instant;

/** One material fact with its provenance (intake record or tool trace). */
public record EvidenceView(
        String evidenceId,
        EvidenceCategory category,
        String type,
        EvidenceSeverity severity,
        String summary,
        Provenance provenance,
        String sourceRef,
        Instant observedAt) {

    public boolean isHigh() {
        return severity == EvidenceSeverity.HIGH;
    }
}
