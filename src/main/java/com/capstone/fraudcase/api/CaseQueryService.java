package com.capstone.fraudcase.api;

import com.capstone.fraudcase.application.FraudCaseService;
import com.capstone.fraudcase.persistence.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Read-side queries shared by the REST API and the UI. Every query is keyed by caseId and checks existence. */
@Component
public class CaseQueryService {

    private final FraudCaseService cases;
    private final EvidenceItemRepository evidence;
    private final ToolTraceRepository traces;
    private final RecommendationRepository recommendations;
    private final PolicyCitationRepository citations;
    private final AuditEventRepository audit;

    public CaseQueryService(FraudCaseService cases, EvidenceItemRepository evidence, ToolTraceRepository traces,
                            RecommendationRepository recommendations, PolicyCitationRepository citations, AuditEventRepository audit) {
        this.cases = cases;
        this.evidence = evidence;
        this.traces = traces;
        this.recommendations = recommendations;
        this.citations = citations;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<EvidenceItemEntity> evidence(String caseId) {
        FraudCaseEntity c = cases.get(caseId);
        return evidence.findByCaseIdAndRunNumberOrderByEvidenceIdAsc(caseId, c.getCurrentRun());
    }

    @Transactional(readOnly = true)
    public List<ToolTraceEntity> traces(String caseId) {
        FraudCaseEntity c = cases.get(caseId);
        return traces.findByCaseIdAndRunNumberOrderByStartedAtAscTraceIdAsc(caseId, c.getCurrentRun());
    }

    @Transactional(readOnly = true)
    public Optional<RecommendationEntity> latestRecommendation(String caseId) {
        cases.get(caseId);
        return recommendations.findFirstByCaseIdOrderByCreatedAtDescRecommendationIdDesc(caseId);
    }

    @Transactional(readOnly = true)
    public List<RecommendationEntity> recommendationHistory(String caseId) {
        cases.get(caseId);
        return recommendations.findByCaseIdOrderByCreatedAtAsc(caseId);
    }

    @Transactional(readOnly = true)
    public List<PolicyCitationEntity> citations(String caseId) {
        FraudCaseEntity c = cases.get(caseId);
        return citations.findByCaseIdAndRunNumberOrderByScoreDesc(caseId, c.getCurrentRun());
    }

    @Transactional(readOnly = true)
    public List<PolicyCitationEntity> cited(RecommendationEntity r) {
        return citations.findByCaseIdAndRunNumberOrderByScoreDesc(r.getCaseId(), r.getRunNumber()).stream()
                .filter(c -> c.isCited() && r.getRecommendationId().equals(c.getRecommendationId())).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventEntity> audit(String caseId) {
        cases.get(caseId);
        return audit.findByCaseIdOrderBySequenceNoAsc(caseId);
    }
}
