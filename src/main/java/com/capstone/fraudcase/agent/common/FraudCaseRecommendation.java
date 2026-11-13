package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.ProcessorMode;
import com.capstone.fraudcase.domain.RiskLevel;
import com.capstone.fraudcase.domain.Route;

import java.util.List;
import java.util.Map;

/** The validated recommendation contract shared by every processor. Advisory only. */
public record FraudCaseRecommendation(
        String caseId,
        Classification classification,
        RiskLevel riskLevel,
        Route recommendedRoute,
        double confidence,
        boolean humanReviewRequired,
        List<String> evidenceIds,
        List<PolicyCitationRef> policyCitations,
        List<String> missingInformation,
        List<String> contradictions,
        List<String> ruleIds,
        List<String> toolTraceIds,
        String rationale,
        ProcessorMode mode,
        Map<String, String> inputFacts) {

    public FraudCaseRecommendation {
        evidenceIds = List.copyOf(evidenceIds);
        policyCitations = List.copyOf(policyCitations);
        missingInformation = List.copyOf(missingInformation);
        contradictions = List.copyOf(contradictions);
        ruleIds = List.copyOf(ruleIds);
        toolTraceIds = List.copyOf(toolTraceIds);
        inputFacts = inputFacts == null ? Map.of() : Map.copyOf(inputFacts);
    }
}
