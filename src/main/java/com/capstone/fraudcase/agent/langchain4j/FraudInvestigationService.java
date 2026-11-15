package com.capstone.fraudcase.agent.langchain4j;

import com.capstone.fraudcase.agent.common.RawRecommendation;
import dev.langchain4j.service.UserMessage;

/** LangChain4j AI Service for analysis. The system message is supplied by the shared PromptRenderer. */
public interface FraudInvestigationService {
    RawRecommendation analyze(@UserMessage String casePrompt);
}
