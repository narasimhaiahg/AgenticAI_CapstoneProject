package com.capstone.fraudcase;

import com.capstone.fraudcase.agent.common.*;
import com.capstone.fraudcase.agent.rules.RulesFraudCaseAgent;
import com.capstone.fraudcase.domain.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RulesAgentTest {
    private final RulesFraudCaseAgent agent = new RulesFraudCaseAgent(new RecommendationValidator());

    @Test
    void missingTransactionIsInsufficientEvidence() {
        var c = TestFixtures.ctx(TestFixtures.facts(IssueType.OTHER, TransactionChannel.CARD_NOT_PRESENT, false),
                List.of(TestFixtures.ev("EV-1", EvidenceCategory.TRANSACTION, EvidenceTypes.TRANSACTION_NOT_FOUND, EvidenceSeverity.HIGH, "not found")),
                List.of());
        var r = agent.analyze(c);
        assertEquals(Classification.INSUFFICIENT_EVIDENCE, r.classification());
        assertEquals(Route.MANUAL_REVIEW, r.recommendedRoute());
        assertTrue(r.humanReviewRequired());
        assertTrue(r.ruleIds().contains("R-01"));
        assertFalse(r.evidenceIds().isEmpty());
    }

    @Test
    void duplicateFingerprintRoutesToPaymentOps() {
        var c = TestFixtures.ctx(TestFixtures.facts(IssueType.DUPLICATE_CHARGE, TransactionChannel.CARD_PRESENT, true),
                List.of(TestFixtures.ev("EV-T", EvidenceCategory.TRANSACTION, EvidenceTypes.TRANSACTION_RECORD, EvidenceSeverity.INFO, "record"),
                        TestFixtures.ev("EV-1", EvidenceCategory.DUPLICATE, EvidenceTypes.DUPLICATE_FINGERPRINT, EvidenceSeverity.HIGH, "dup")),
                List.of(TestFixtures.passage("pay-41", 0.5)));
        var r = agent.analyze(c);
        assertEquals(Classification.DUPLICATE_PROCESSING, r.classification());
        assertEquals(Route.PAYMENT_OPERATIONS, r.recommendedRoute());
        assertTrue(r.ruleIds().contains("R-02"));
    }

    @Test
    void twoHighAnomaliesUnrecognisedMerchantIsUnauthorised() {
        var c = TestFixtures.ctx(TestFixtures.facts(IssueType.MERCHANT_NOT_RECOGNISED, TransactionChannel.CARD_NOT_PRESENT, false),
                List.of(TestFixtures.ev("EV-T", EvidenceCategory.TRANSACTION, EvidenceTypes.TRANSACTION_RECORD, EvidenceSeverity.INFO, "record"),
                        TestFixtures.ev("EV-1", EvidenceCategory.DEVICE, EvidenceTypes.DEVICE_NEW, EvidenceSeverity.HIGH, "new device"),
                        TestFixtures.ev("EV-2", EvidenceCategory.LOCATION, EvidenceTypes.LOCATION_ANOMALY, EvidenceSeverity.HIGH, "far away")),
                List.of(TestFixtures.passage("cnp-32", 0.4)));
        var r = agent.analyze(c);
        assertEquals(Classification.UNAUTHORISED_TRANSACTION, r.classification());
        assertEquals(Route.FRAUD_INVESTIGATION, r.recommendedRoute());
        assertTrue(r.ruleIds().contains("R-03"));
        assertFalse(r.policyCitations().isEmpty());
    }

    @Test
    void sparseEvidenceDefaultsToInsufficientAndHumanReview() {
        var c = TestFixtures.ctx(TestFixtures.facts(IssueType.OTHER, TransactionChannel.ATM, true), List.of(), List.of());
        var r = agent.analyze(c);
        assertEquals(Classification.INSUFFICIENT_EVIDENCE, r.classification());
        assertEquals(Route.MANUAL_REVIEW, r.recommendedRoute());
        assertTrue(r.humanReviewRequired());
    }

    @Test
    void abstentionForcesManualReviewAndGap() {
        var c = TestFixtures.ctx(TestFixtures.facts(IssueType.MERCHANT_NOT_RECOGNISED, TransactionChannel.CARD_NOT_PRESENT, false),
                List.of(TestFixtures.ev("EV-T", EvidenceCategory.TRANSACTION, EvidenceTypes.TRANSACTION_RECORD, EvidenceSeverity.INFO, "record"),TestFixtures.ev("EV-1", EvidenceCategory.DEVICE, EvidenceTypes.DEVICE_NEW, EvidenceSeverity.HIGH, "new")),
                List.of());
        var r = agent.analyze(c);
        assertEquals(Route.MANUAL_REVIEW, r.recommendedRoute());
        assertTrue(r.missingInformation().stream().anyMatch(m -> m.contains("policy")));
    }
}
