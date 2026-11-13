package com.capstone.fraudcase.agent.common;

import java.util.List;

/** A case-scoped follow-up question. History only ever contains turns of the same case. */
public record FollowUpContext(
        String caseId,
        String question,
        FraudCaseContext caseContext,
        FraudCaseRecommendation latestRecommendation,
        List<ConversationTurn> history) {

    public FollowUpContext {
        history = List.copyOf(history);
    }
}
