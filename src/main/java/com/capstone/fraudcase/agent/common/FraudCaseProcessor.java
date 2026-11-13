package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.domain.ProcessorMode;

/** Framework-neutral processor contract implemented by the rules, Spring AI and LangChain4j adapters. */
public interface FraudCaseProcessor {

    ProcessorMode mode();

    /** Whether the processor can currently be used (e.g. a model is configured). Rules mode is always available. */
    default boolean isAvailable() {
        return true;
    }

    default String availabilityDetail() {
        return isAvailable() ? "available" : "unavailable";
    }

    /** Returns a validated, advisory recommendation. Must never perform an action. */
    FraudCaseRecommendation analyze(FraudCaseContext context);

    /** Answers a case-scoped follow-up question from recorded evidence only. */
    FollowUpAnswer answerFollowUp(FollowUpContext context);
}
