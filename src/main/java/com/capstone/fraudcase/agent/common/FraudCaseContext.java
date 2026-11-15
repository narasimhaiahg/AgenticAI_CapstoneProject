package com.capstone.fraudcase.agent.common;

import java.util.List;
import java.util.Optional;

/** Everything a processor may use: validated facts, tool evidence, retrieved policy and case-scoped conversation. */
public record FraudCaseContext(
        String caseId,
        String correlationId,
        int runNumber,
        CaseFacts facts,
        List<EvidenceView> evidence,
        List<ToolOutcome> toolOutcomes,
        List<String> contradictions,
        List<String> missingInformation,
        PolicyRetrievalResult policy,
        List<ConversationTurn> conversation,
        List<String> injectionIndicators) {

    public static final String INVESTIGATION_GOAL =
            "Triage the reported payment dispute: classify it, assess risk, recommend an operational route "
                    + "for a human reviewer, and list missing or contradictory information. Do not take any action.";

    public FraudCaseContext {
        evidence = List.copyOf(evidence);
        toolOutcomes = List.copyOf(toolOutcomes);
        contradictions = List.copyOf(contradictions);
        missingInformation = List.copyOf(missingInformation);
        conversation = List.copyOf(conversation);
        injectionIndicators = List.copyOf(injectionIndicators);
    }

    public Optional<EvidenceView> evidence(String evidenceId) {
        return evidence.stream().filter(e -> e.evidenceId().equals(evidenceId)).findFirst();
    }

    public boolean hasTrace(String traceId) {
        return toolOutcomes.stream().anyMatch(t -> t.traceId().equals(traceId));
    }
}
