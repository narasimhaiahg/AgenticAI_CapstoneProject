package com.capstone.fraudcase;

import com.capstone.fraudcase.agent.common.*;
import com.capstone.fraudcase.agent.rules.RulesFraudCaseAgent;
import com.capstone.fraudcase.domain.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Contract guarantees every FraudCaseProcessor implementation must honour. */
class ProcessorContractTest {
    private final FraudCaseProcessor rules = new RulesFraudCaseAgent(new RecommendationValidator());
    private final FraudCaseContext ctx = TestFixtures.ctx(
            TestFixtures.facts(IssueType.MERCHANT_NOT_RECOGNISED, TransactionChannel.CARD_NOT_PRESENT, false),
            List.of(TestFixtures.ev("EV-1", EvidenceCategory.DEVICE, EvidenceTypes.DEVICE_NEW, EvidenceSeverity.HIGH, "new")),
            List.of(TestFixtures.passage("cnp-32", 0.4)));

    @Test
    void rulesModeAlwaysAvailable() {
        assertTrue(rules.isAvailable());
        assertEquals(ProcessorMode.RULES, rules.mode());
    }

    @Test
    void recommendationContract() {
        var r = rules.analyze(ctx);
        assertTrue(r.humanReviewRequired(), "human review is mandatory");
        assertTrue(r.confidence() >= 0 && r.confidence() <= 1);
        assertNotNull(r.rationale());
        r.evidenceIds().forEach(id ->
                assertTrue(ctx.evidence().stream().anyMatch(e -> e.evidenceId().equals(id)),
                        "unknown evidence id " + id));
        r.policyCitations().forEach(c ->
                assertTrue(ctx.policy().passages().stream().anyMatch(p -> p.chunkId().equals(c.chunkId())),
                        "citation not retrieved " + c.chunkId()));
        assertFalse(r.ruleIds().isEmpty());
        assertFalse(r.inputFacts().isEmpty());
    }

    @Test
    void followUpIsCaseScopedAndAdvisory() {
        var a = rules.answerFollowUp(new FollowUpContext(ctx.caseId(), "which evidence supports this?", ctx, rules.analyze(ctx), List.of()));
        assertNotNull(a.answer());
    }
}
