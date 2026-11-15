package com.capstone.fraudcase;

import com.capstone.fraudcase.domain.CaseStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaseLifecycleTest {
    @Test
    void happyPathTransitionsAllowed() {
        assertTrue(CaseStatus.NEW.canTransitionTo(CaseStatus.ENRICHING));
        assertTrue(CaseStatus.ENRICHING.canTransitionTo(CaseStatus.READY_FOR_REVIEW));
        assertTrue(CaseStatus.READY_FOR_REVIEW.canTransitionTo(CaseStatus.UNDER_REVIEW));
        assertTrue(CaseStatus.UNDER_REVIEW.canTransitionTo(CaseStatus.ROUTED));
        assertTrue(CaseStatus.ROUTED.canTransitionTo(CaseStatus.CLOSED));
        assertTrue(CaseStatus.ENRICHING.canTransitionTo(CaseStatus.INCOMPLETE));
        assertTrue(CaseStatus.INCOMPLETE.canTransitionTo(CaseStatus.ENRICHING));
    }

    @Test
    void illegalTransitionsRejected() {
        assertFalse(CaseStatus.NEW.canTransitionTo(CaseStatus.ROUTED));
        assertFalse(CaseStatus.CLOSED.canTransitionTo(CaseStatus.ENRICHING));
        assertFalse(CaseStatus.NEW.canTransitionTo(CaseStatus.CLOSED));

    }
}
