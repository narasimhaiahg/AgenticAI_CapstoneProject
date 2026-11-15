package com.capstone.fraudcase.api;

import com.capstone.fraudcase.application.InvestigationPlanner.PlannedStep;
import com.capstone.fraudcase.domain.*;
import com.capstone.fraudcase.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Response DTOs. Only masked/synthetic values are exposed; raw statements are never returned by list endpoints. */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record CaseResponse(String caseId, CaseStatus status, List<CaseStatus> allowedNextStates, OffsetDateTime reportedAt,
                               IntakeChannel intakeChannel, String createdBy, String maskedCustomerRef, String homeRegion,
                               String transactionRef, BigDecimal amount, String currency, OffsetDateTime occurredAt,
                               TransactionStatus transactionStatus, TransactionChannel channel, String maskedMerchant,
                               IssueType issueType, String customerStatement, CardPossession cardPossession,
                               boolean merchantRecognised, List<String> injectionIndicators, int currentRun, Route assignedRoute,
                               boolean syntheticDataConfirmed, boolean reviewRequired) {
        public static CaseResponse from(FraudCaseEntity c, TransactionClaimEntity t) {
            return new CaseResponse(c.getCaseId(), c.getStatus(), List.copyOf(c.getStatus().allowedNext()), c.getReportedAt(),
                    c.getIntakeChannel(), c.getCreatedBy(), c.getMaskedCustomerRef(), c.getHomeRegion(), t.getTransactionRef(),
                    t.getAmount(), t.getCurrency(), t.getOccurredAt(), t.getTransactionStatus(), t.getTransactionChannel(),
                    t.getMaskedMerchant(), t.getIssueType(), t.getCustomerStatement(), t.getCardPossession(), t.isMerchantRecognised(),
                    t.getInjectionIndicators(), c.getCurrentRun(), c.getAssignedRoute(), c.isSyntheticConfirmed(), c.isReviewRequired());
        }
    }

    public record InvestigationResponse(String caseId, int runNumber, String correlationId, CaseStatus status, List<PlannedStep> plan,
                                        String recommendationId, ProcessorMode requestedMode, ProcessorMode mode, String fallbackReason) {
    }

    public record EvidenceResponse(String evidenceId, int runNumber, EvidenceCategory category, String type, EvidenceSeverity severity,
                                   String valueSummary, Provenance provenance, String sourceRef, OffsetDateTime observedAt) {
        public static EvidenceResponse from(EvidenceItemEntity e) {
            return new EvidenceResponse(e.getEvidenceId(), e.getRunNumber(), e.getCategory(), e.getType(), e.getSeverity(),
                    e.getValueSummary(), e.getProvenance(), e.getSourceRef(), e.getObservedAt());
        }
    }

    public record ToolTraceResponse(String traceId, int runNumber, String correlationId, String toolName, ToolStatus status,
                                    long durationMs, String resultSummary, String redactedResult, String origin, String requestHash,
                                    OffsetDateTime startedAt) {
        public static ToolTraceResponse from(ToolTraceEntity t) {
            return new ToolTraceResponse(t.getTraceId(), t.getRunNumber(), t.getCorrelationId(), t.getToolName(), t.getStatus(),
                    t.getDurationMs(), t.getResultSummary(), t.getResultJson(), t.getOrigin(), t.getRequestHash(), t.getStartedAt());
        }
    }

    public record CitationResponse(String documentId, String title, String version, LocalDate effectiveDate, String section,
                                   String sectionTitle, String chunkId, double score, boolean cited, String recommendationId, String passage) {
        public static CitationResponse from(PolicyCitationEntity c) {
            return new CitationResponse(c.getDocumentId(), c.getTitle(), c.getVersion(), c.getEffectiveDate(), c.getSection(),
                    c.getSectionTitle(), c.getChunkId(), c.getScore(), c.isCited(), c.getRecommendationId(), c.getPassage());
        }
    }

    public record CitationRef(String documentId, String version, String section, String chunkId, double score) {
    }

    public record RecommendationResponse(String recommendationId, String caseId, int runNumber, Classification classification,
                                         RiskLevel riskLevel, Route recommendedRoute, double confidence, boolean humanReviewRequired,
                                         List<String> evidenceIds, List<CitationRef> policyCitations, List<String> missingInformation,
                                         List<String> contradictions, List<String> ruleIds, List<String> toolTraceIds, String rationale,
                                         ProcessorMode mode, ProcessorMode requestedMode, String fallbackReason, Map<String, String> inputFacts,
                                         String priority, OffsetDateTime slaTargetAt, boolean policyAbstained, OffsetDateTime createdAt) {
        public static RecommendationResponse from(RecommendationEntity r, List<PolicyCitationEntity> cited) {
            return new RecommendationResponse(r.getRecommendationId(), r.getCaseId(), r.getRunNumber(), r.getClassification(), r.getRiskLevel(),
                    r.getRoute(), r.getConfidence(), r.isHumanReviewRequired(), r.getEvidenceIds(),
                    cited.stream().map(c -> new CitationRef(c.getDocumentId(), c.getVersion(), c.getSection(), c.getChunkId(), c.getScore())).toList(),
                    r.getMissingInformation(), r.getContradictions(), r.getRuleIds(), r.getToolTraceIds(), r.getRationale(), r.getMode(),
                    r.getRequestedMode(), r.getFallbackReason(), r.getInputFacts(), r.getPriority(), r.getSlaTargetAt(), r.isRetrievalAbstained(),
                    r.getCreatedAt());
        }
    }

    public record MessageRequest(String question, String mode) {
    }

    public record MessageResponse(String caseId, String answer, List<String> referencedEvidenceIds, List<String> referencedChunkIds,
                                  ProcessorMode mode, String note, String disclaimer) {
    }

    public record ReviewRequest(Disposition disposition, Route route, String reason, boolean missingInformationAcknowledged) {
    }

    public record ReviewResponse(String reviewId, String caseId, String recommendationId, String reviewerId, Disposition disposition,
                                 Route selectedRoute, String reason, boolean missingInformationAcknowledged, CaseStatus fromStatus,
                                 CaseStatus toStatus, OffsetDateTime recordedAt) {
        public static ReviewResponse from(ReviewDispositionEntity r) {
            return new ReviewResponse(r.getReviewId(), r.getCaseId(), r.getRecommendationId(), r.getReviewerId(), r.getDisposition(),
                    r.getSelectedRoute(), r.getReason(), r.isMissingInformationAcknowledged(), r.getFromStatus(), r.getToStatus(), r.getRecordedAt());
        }
    }

    public record AuditResponse(String eventId, long sequenceNo, String caseId, String eventType, ActorType actorType, String actorId,
                                String correlationId, String summary, String payloadHash, String previousHash, OffsetDateTime recordedAt) {
        public static AuditResponse from(AuditEventEntity a) {
            return new AuditResponse(a.getEventId(), a.getSequenceNo(), a.getCaseId(), a.getEventType(), a.getActorType(), a.getActorId(),
                    a.getCorrelationId(), a.getSummary(), a.getPayloadHash(), a.getPreviousHash(), a.getRecordedAt());
        }
    }
}
