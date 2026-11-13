package com.capstone.fraudcase;

import com.capstone.fraudcase.agent.common.*;
import com.capstone.fraudcase.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

public final class TestFixtures {
    private TestFixtures() {}

    public static CaseFacts facts(IssueType issueType, TransactionChannel channel, boolean merchantRecognised) {
        return new CaseFacts("CASE-2026-0001", Instant.parse("2026-11-13T10:00:00Z"), IntakeChannel.WEB_CHAT,
                "analyst-1", "CUST-••••-4821", "IN-KA", "TXN-2026-784102", new BigDecimal("48750.00"), "INR",
                OffsetDateTime.parse("2026-11-06T08:42:00+05:30"), TransactionStatus.POSTED, channel,
                "NORTHSTAR DIGITAL ••••73", issueType, "I do not recognise this purchase.",
                CardPossession.IN_POSSESSION, merchantRecognised);
    }

    public static EvidenceView ev(String id, EvidenceCategory cat, String type, EvidenceSeverity sev, String summary) {
        return new EvidenceView(id, cat, type, sev, summary, Provenance.TOOL_RESULT, "TR-1",
                Instant.parse("2026-11-13T10:01:00Z"));
    }

    public static PolicyPassage passage(String chunkId, double score) {
        return new PolicyPassage("POL-CNP-017", "CNP fraud policy", "3.2",
                java.time.LocalDate.parse("2026-01-01"), "3.1", "Section", chunkId, score,
                "Unauthorised card-not-present transactions route to fraud investigation.",
                List.of("UNAUTHORISED_TRANSACTION"));
    }

    public static FraudCaseContext ctx(CaseFacts facts, List<EvidenceView> evidence,
                                     List<PolicyPassage> passages) {
        return new FraudCaseContext(facts.caseId(), "corr-1", 1, facts, evidence, List.of(),
                List.of(), List.of(),
                new PolicyRetrievalResult("query", 4, 0.18, passages,
                        passages.stream().mapToDouble(PolicyPassage::score).max().orElse(0)),
                List.of(), List.of());
    }
}
