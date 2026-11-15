package com.capstone.fraudcase;

import com.capstone.fraudcase.agent.common.*;
import com.capstone.fraudcase.domain.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationValidatorTest {
    private final RecommendationValidator v = new RecommendationValidator();
    private final FraudCaseContext ctx = TestFixtures.ctx(
            TestFixtures.facts(IssueType.MERCHANT_NOT_RECOGNISED, TransactionChannel.CARD_NOT_PRESENT, false),
            List.of(TestFixtures.ev("EV-1", EvidenceCategory.DEVICE, EvidenceTypes.DEVICE_NEW, EvidenceSeverity.HIGH, "x")),
            List.of(TestFixtures.passage("cnp-32", 0.4)));

    private RawRecommendation raw() {
        return new RawRecommendation("CASE-2026-0001", "UNAUTHORISED_TRANSACTION", "HIGH", "FRAUD_INVESTIGATION",
                0.8, true, List.of("EV-1"), List.of(new RawRecommendation.RawCitation("POL-CNP-017", "3.2", "3.1", "cnp-32", 0.4)),
                List.of(), List.of(), List.of(), List.of(), "Evidence indicates unauthorised use.");
    }

    @Test
    void validOutputPasses() {
        var r = v.validate(raw(), ctx, ProcessorMode.RULES);
        assertEquals(Classification.UNAUTHORISED_TRANSACTION, r.classification());
        assertEquals("cnp-32", r.policyCitations().get(0).chunkId());
    }

    @Test
    void confidenceOutOfRangeRejected() {
        var bad = new RawRecommendation("CASE-2026-0001", "UNAUTHORISED_TRANSACTION", "HIGH", "FRAUD_INVESTIGATION",
                1.5, true, List.of("EV-1"), List.of(), List.of(), List.of(), List.of(), List.of(), "r");
        assertThrows(RecommendationValidationException.class, () -> v.validate(bad, ctx, ProcessorMode.RULES));
    }

    @Test
    void missingHumanReviewRejected() {
        var bad = new RawRecommendation("CASE-2026-0001", "UNAUTHORISED_TRANSACTION", "HIGH", "FRAUD_INVESTIGATION",
                0.5, false, List.of("EV-1"), List.of(), List.of(), List.of(), List.of(), List.of(), "r");
        assertThrows(RecommendationValidationException.class, () -> v.validate(bad, ctx, ProcessorMode.RULES));
    }

    @Test
    void citationNotInRetrievedSetRejected() {
        var bad = new RawRecommendation("CASE-2026-0001", "UNAUTHORISED_TRANSACTION", "HIGH", "FRAUD_INVESTIGATION",
                0.8, true, List.of("EV-1"), List.of(new RawRecommendation.RawCitation("POL-X", "9", "1", "made-up", 0.9)),
                List.of(), List.of(), List.of(), List.of(), "r");
        var ex = assertThrows(RecommendationValidationException.class, () -> v.validate(bad, ctx, ProcessorMode.RULES));
        assertTrue(ex.getMessage().toLowerCase().contains("cit"));
    }

    @Test
    void unknownEnumRejected() {
        var bad = new RawRecommendation("CASE-2026-0001", "SOMETHING_ELSE", "HIGH", "FRAUD_INVESTIGATION",
                0.8, true, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "r");
        assertThrows(RecommendationValidationException.class, () -> v.validate(bad, ctx, ProcessorMode.RULES));
    }

    @Test
    void inconsistentRouteRejected() {
        var bad = new RawRecommendation("CASE-2026-0001", "UNAUTHORISED_TRANSACTION", "HIGH", "PAYMENT_OPERATIONS",
                0.8, true, List.of("EV-1"), List.of(), List.of(), List.of(), List.of(), List.of(), "r");
        assertThrows(RecommendationValidationException.class, () -> v.validate(bad, ctx, ProcessorMode.RULES));
    }
}
