package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.domain.*;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "recommendation")
public class RecommendationEntity {

    @Id
    private String recommendationId;
    private String caseId;
    private int runNumber;
    private String correlationId;
    @Enumerated(EnumType.STRING)
    private ProcessorMode mode;
    @Enumerated(EnumType.STRING)
    private ProcessorMode requestedMode;
    private String fallbackReason;
    @Enumerated(EnumType.STRING)
    private Classification classification;
    @Enumerated(EnumType.STRING)
    private Route route;
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;
    private double confidence;
    private boolean humanReviewRequired;
    private String rationale;
    @Convert(converter = StringListConverter.class)
    private List<String> evidenceIds;
    @Convert(converter = StringListConverter.class)
    private List<String> missingInformation;
    @Convert(converter = StringListConverter.class)
    private List<String> contradictions;
    @Convert(converter = StringListConverter.class)
    private List<String> ruleIds;
    @Convert(converter = StringListConverter.class)
    private List<String> toolTraceIds;
    @Convert(converter = StringMapConverter.class)
    private Map<String, String> inputFacts;
    private String priority;
    private OffsetDateTime slaTargetAt;
    private String slaTraceId;
    private String retrievalQuery;
    private Double retrievalBestScore;
    private boolean retrievalAbstained;
    private OffsetDateTime createdAt;

    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public int getRunNumber() { return runNumber; }
    public void setRunNumber(int runNumber) { this.runNumber = runNumber; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public ProcessorMode getMode() { return mode; }
    public void setMode(ProcessorMode mode) { this.mode = mode; }
    public ProcessorMode getRequestedMode() { return requestedMode; }
    public void setRequestedMode(ProcessorMode requestedMode) { this.requestedMode = requestedMode; }
    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }
    public Classification getClassification() { return classification; }
    public void setClassification(Classification classification) { this.classification = classification; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public void setHumanReviewRequired(boolean humanReviewRequired) { this.humanReviewRequired = humanReviewRequired; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public List<String> getEvidenceIds() { return evidenceIds; }
    public void setEvidenceIds(List<String> evidenceIds) { this.evidenceIds = evidenceIds; }
    public List<String> getMissingInformation() { return missingInformation; }
    public void setMissingInformation(List<String> missingInformation) { this.missingInformation = missingInformation; }
    public List<String> getContradictions() { return contradictions; }
    public void setContradictions(List<String> contradictions) { this.contradictions = contradictions; }
    public List<String> getRuleIds() { return ruleIds; }
    public void setRuleIds(List<String> ruleIds) { this.ruleIds = ruleIds; }
    public List<String> getToolTraceIds() { return toolTraceIds; }
    public void setToolTraceIds(List<String> toolTraceIds) { this.toolTraceIds = toolTraceIds; }
    public Map<String, String> getInputFacts() { return inputFacts; }
    public void setInputFacts(Map<String, String> inputFacts) { this.inputFacts = inputFacts; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public OffsetDateTime getSlaTargetAt() { return slaTargetAt; }
    public void setSlaTargetAt(OffsetDateTime slaTargetAt) { this.slaTargetAt = slaTargetAt; }
    public String getSlaTraceId() { return slaTraceId; }
    public void setSlaTraceId(String slaTraceId) { this.slaTraceId = slaTraceId; }
    public String getRetrievalQuery() { return retrievalQuery; }
    public void setRetrievalQuery(String retrievalQuery) { this.retrievalQuery = retrievalQuery; }
    public Double getRetrievalBestScore() { return retrievalBestScore; }
    public void setRetrievalBestScore(Double retrievalBestScore) { this.retrievalBestScore = retrievalBestScore; }
    public boolean isRetrievalAbstained() { return retrievalAbstained; }
    public void setRetrievalAbstained(boolean retrievalAbstained) { this.retrievalAbstained = retrievalAbstained; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
