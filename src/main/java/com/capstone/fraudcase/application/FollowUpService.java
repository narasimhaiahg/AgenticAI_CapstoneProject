package com.capstone.fraudcase.application;

import com.capstone.fraudcase.agent.common.ConversationTurn;
import com.capstone.fraudcase.agent.common.FollowUpAnswer;
import com.capstone.fraudcase.agent.common.FollowUpContext;
import com.capstone.fraudcase.agent.common.FraudCaseContext;
import com.capstone.fraudcase.agent.common.FraudCaseRecommendation;
import com.capstone.fraudcase.domain.ActorType;
import com.capstone.fraudcase.domain.MaskingRules;
import com.capstone.fraudcase.domain.ProcessorMode;
import com.capstone.fraudcase.persistence.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Case-scoped follow-up questions. History and context are loaded for the requested case only; answers are
 * informational and never change the recommendation. Model failure falls back to the deterministic answerer.
 */
@Service
public class FollowUpService {

    public static final int MAX_QUESTION_LENGTH = 500;

    private final FraudCaseService cases;
    private final CaseContextLoader loader;
    private final CaseMemoryService memory;
    private final ProcessorRegistry processors;
    private final RecommendationRepository recommendations;
    private final PolicyCitationRepository citations;
    private final AuditWriter audit;

    public FollowUpService(FraudCaseService cases, CaseContextLoader loader, CaseMemoryService memory, ProcessorRegistry processors,
                           RecommendationRepository recommendations, PolicyCitationRepository citations, AuditWriter audit) {
        this.cases = cases;
        this.loader = loader;
        this.memory = memory;
        this.processors = processors;
        this.recommendations = recommendations;
        this.citations = citations;
        this.audit = audit;
    }

    public record FollowUpResult(String caseId, FollowUpAnswer answer, String note) {
    }

    public FollowUpResult ask(String caseId, String question, String actorId, String modeOverride) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) throw DomainValidationException.of("question", "Enter a question");
        if (q.length() > MAX_QUESTION_LENGTH) throw DomainValidationException.of("question", "Question must be at most " + MAX_QUESTION_LENGTH + " characters");
        if (MaskingRules.containsMarkup(q)) throw DomainValidationException.of("question", "Markup is not allowed");
        if (MaskingRules.containsUnmaskedNumber(q)) throw DomainValidationException.of("question", "Remove card or account numbers from the question");

        FraudCaseEntity c = cases.get(caseId);
        TransactionClaimEntity claim = cases.claim(caseId);
        List<ConversationTurn> history = memory.history(caseId);
        FraudCaseContext ctx = loader.load(c, claim, history);
        FraudCaseRecommendation latest = recommendations.findFirstByCaseIdOrderByCreatedAtDescRecommendationIdDesc(caseId)
                .map(r -> CaseContextLoader.recommendation(r, citations.findByCaseIdAndRunNumberOrderByScoreDesc(caseId, r.getRunNumber())
                        .stream().filter(PolicyCitationEntity::isCited).toList()))
                .orElse(null);
        FollowUpContext fctx = new FollowUpContext(caseId, q, ctx, latest, history);

        ProcessorMode mode = processors.resolveMode(modeOverride);
        String note = null;
        FollowUpAnswer answer;
        try {
            answer = processors.get(mode).answerFollowUp(fctx);
        } catch (RuntimeException e) {
            note = "Model unavailable; answered by deterministic rules from recorded evidence.";
            answer = processors.rules().answerFollowUp(fctx);
        }
        memory.append(caseId, ConversationTurn.USER, q, null);
        memory.append(caseId, ConversationTurn.ASSISTANT, answer.answer(), answer.mode().key());
        audit.append(caseId, "FOLLOW_UP_ANSWERED", ActorType.USER, actorId, null,
                "Follow-up answered in " + answer.mode().key() + " mode; refs " + answer.referencedEvidenceIds(),
                Map.of("question", q, "answer", answer.answer()));
        return new FollowUpResult(caseId, answer, note);
    }
}
