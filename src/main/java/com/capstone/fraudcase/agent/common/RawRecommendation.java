package com.capstone.fraudcase.agent.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Untrusted, loosely typed model output. Enum-like fields stay strings so unknown values can be
 * rejected explicitly by {@link RecommendationValidator} instead of failing inside a framework.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawRecommendation(
        String caseId,
        String classification,
        String riskLevel,
        String recommendedRoute,
        Double confidence,
        Boolean humanReviewRequired,
        List<String> evidenceIds,
        List<RawCitation> policyCitations,
        List<String> missingInformation,
        List<String> contradictions,
        List<String> ruleIds,
        List<String> toolTraceIds,
        String rationale) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawCitation(String documentId, String version, String section, String chunkId, Double score) {
    }
}
