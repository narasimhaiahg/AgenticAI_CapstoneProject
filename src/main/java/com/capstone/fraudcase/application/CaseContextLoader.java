package com.capstone.fraudcase.application;

import com.capstone.fraudcase.agent.common.CaseFacts;
import com.capstone.fraudcase.agent.common.EvidenceView;
import com.capstone.fraudcase.agent.common.FraudCaseContext;
import com.capstone.fraudcase.agent.common.FraudCaseRecommendation;
import com.capstone.fraudcase.agent.common.PolicyCitationRef;
import com.capstone.fraudcase.agent.common.PolicyPassage;
import com.capstone.fraudcase.agent.common.PolicyRetrievalResult;
import com.capstone.fraudcase.agent.common.ToolOutcome;
import com.capstone.fraudcase.config.CapstoneProperties;
import com.capstone.fraudcase.knowledge.PolicyIngestionService;
import com.capstone.fraudcase.persistence.*;
import org.springframework.stereotype.Component;

import java.util.List;

/** Maps persisted case artefacts back into the framework-neutral processor context. */
@Component
public class CaseContextLoader {

    private final EvidenceItemRepository evidence;
    private final ToolTraceRepository traces;
    private final PolicyCitationRepository citations;
    private final RecommendationRepository recommendations;
    private final PolicyIngestionService policies;
    private final CapstoneProperties props;

    public CaseContextLoader(EvidenceItemRepository evidence, ToolTraceRepository traces, PolicyCitationRepository citations,
                             RecommendationRepository recommendations, PolicyIngestionService policies, CapstoneProperties props) {
        this.evidence = evidence;
        this.traces = traces;
        this.citations = citations;
        this.recommendations = recommendations;
        this.policies = policies;
        this.props = props;
    }

    public static CaseFacts facts(FraudCaseEntity c, TransactionClaimEntity t) {
        return new CaseFacts(c.getCaseId(), c.getReportedAt().toInstant(), c.getIntakeChannel(), c.getCreatedBy(),
                c.getMaskedCustomerRef(), c.getHomeRegion(), t.getTransactionRef(), t.getAmount(), t.getCurrency(),
                t.getOccurredAt(), t.getTransactionStatus(), t.getTransactionChannel(), t.getMaskedMerchant(),
                t.getIssueType(), t.getCustomerStatement(), t.getCardPossession(), t.isMerchantRecognised());
    }

    public static EvidenceView view(EvidenceItemEntity e) {
        return new EvidenceView(e.getEvidenceId(), e.getCategory(), e.getType(), e.getSeverity(), e.getValueSummary(),
                e.getProvenance(), e.getSourceRef(), e.getObservedAt().toInstant());
    }

    public static ToolOutcome outcome(ToolTraceEntity t) {
        return new ToolOutcome(t.getTraceId(), t.getToolName(), t.getStatus(), t.getDurationMs(), t.getResultSummary(), t.getOrigin());
    }

    public PolicyPassage passage(PolicyCitationEntity c) {
        List<String> applies = policies.chunk(c.getChunkId()).map(ch -> ch.appliesTo()).orElse(List.of());
        return new PolicyPassage(c.getDocumentId(), c.getTitle(), c.getVersion(), c.getEffectiveDate(), c.getSection(),
                c.getSectionTitle(), c.getChunkId(), c.getScore(), c.getPassage(), applies);
    }

    /** Rebuilds the latest run's context, or null if the case has not been investigated. */
    public FraudCaseContext load(FraudCaseEntity c, TransactionClaimEntity t, List<com.capstone.fraudcase.agent.common.ConversationTurn> conversation) {
        if (c.getCurrentRun() == 0) {
            return null;
        }
        int run = c.getCurrentRun();
        var rec = recommendations.findFirstByCaseIdOrderByCreatedAtDescRecommendationIdDesc(c.getCaseId());
        List<PolicyPassage> passages = citations.findByCaseIdAndRunNumberOrderByScoreDesc(c.getCaseId(), run).stream().map(this::passage).toList();
        PolicyRetrievalResult policy = new PolicyRetrievalResult(rec.map(RecommendationEntity::getRetrievalQuery).orElse(""),
                props.getRetrieval().getTopK(), props.getRetrieval().getSimilarityThreshold(), passages,
                rec.map(RecommendationEntity::getRetrievalBestScore).orElse(0.0));
        return new FraudCaseContext(c.getCaseId(), rec.map(RecommendationEntity::getCorrelationId).orElse("n/a"), run, facts(c, t),
                evidence.findByCaseIdAndRunNumberOrderByEvidenceIdAsc(c.getCaseId(), run).stream().map(CaseContextLoader::view).toList(),
                traces.findByCaseIdAndRunNumberOrderByStartedAtAscTraceIdAsc(c.getCaseId(), run).stream().map(CaseContextLoader::outcome).toList(),
                rec.map(RecommendationEntity::getContradictions).orElse(List.of()), List.of(), policy, conversation,
                t.getInjectionIndicators() == null ? List.of() : t.getInjectionIndicators());
    }

    public static FraudCaseRecommendation recommendation(RecommendationEntity r, List<PolicyCitationEntity> cited) {
        return new FraudCaseRecommendation(r.getCaseId(), r.getClassification(), r.getRiskLevel(), r.getRoute(), r.getConfidence(),
                r.isHumanReviewRequired(), r.getEvidenceIds(),
                cited.stream().map(c -> new PolicyCitationRef(c.getDocumentId(), c.getVersion(), c.getSection(), c.getChunkId(), c.getScore())).toList(),
                r.getMissingInformation(), r.getContradictions(), r.getRuleIds(), r.getToolTraceIds(), r.getRationale(), r.getMode(), r.getInputFacts());
    }
}
