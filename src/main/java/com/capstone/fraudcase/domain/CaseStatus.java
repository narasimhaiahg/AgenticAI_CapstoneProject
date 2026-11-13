package com.capstone.fraudcase.domain;

import java.util.EnumSet;
import java.util.Set;

/** Case lifecycle. Transitions are enforced by the application layer, never by model output. */
public enum CaseStatus {
    NEW, ENRICHING, INCOMPLETE, READY_FOR_REVIEW, UNDER_REVIEW, ROUTED, CLOSED;

    public Set<CaseStatus> allowedNext() {
        return switch (this) {
            case NEW -> EnumSet.of(ENRICHING);
            case ENRICHING -> EnumSet.of(READY_FOR_REVIEW, INCOMPLETE);
            case INCOMPLETE -> EnumSet.of(ENRICHING, CLOSED);
            case READY_FOR_REVIEW -> EnumSet.of(UNDER_REVIEW, ENRICHING);
            // INCOMPLETE is a documented extension: "request information" sends the case back for enrichment.
            case UNDER_REVIEW -> EnumSet.of(ROUTED, CLOSED, INCOMPLETE);
            case ROUTED -> EnumSet.of(CLOSED);
            case CLOSED -> EnumSet.noneOf(CaseStatus.class);
        };
    }

    public boolean canTransitionTo(CaseStatus next) {
        return allowedNext().contains(next);
    }
}
