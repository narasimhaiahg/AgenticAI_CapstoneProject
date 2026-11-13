package com.capstone.fraudcase.agent.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

/** LangChain4j AI Service for case-scoped follow-ups; memory is keyed by caseId. */
public interface FraudFollowUpService {
    String answer(@MemoryId String caseId, @UserMessage String prompt);
}
