package com.capstone.fraudcase.application;

import com.capstone.fraudcase.domain.ActorType;
import com.capstone.fraudcase.domain.CaseStatus;
import com.capstone.fraudcase.domain.Disposition;
import com.capstone.fraudcase.domain.MaskingRules;
import com.capstone.fraudcase.domain.Route;
import com.capstone.fraudcase.persistence.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Records the human disposition. Nothing is routed or closed without an authorised reviewer, a written rationale and
 * (where gaps exist) an explicit acknowledgement of missing information. No financial or customer action is executed.
 */
@Service
public class ReviewService {

    public static final int MIN_REASON_LENGTH = 10;
    public static final String ROLE_LEAD = "ROLE_LEAD";

    private final FraudCaseService cases;
    private final RecommendationRepository recommendations;
    private final ReviewDispositionRepository reviews;
    private final IdGenerator ids;
    private final AuditWriter audit;
    private final Clock clock;

    public ReviewService(FraudCaseService cases, RecommendationRepository recommendations, ReviewDispositionRepository reviews,
                         IdGenerator ids, AuditWriter audit, Clock clock) {
        this.cases = cases;
        this.recommendations = recommendations;
        this.reviews = reviews;
        this.ids = ids;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public FraudCaseEntity startReview(String caseId, String reviewerId) {
        FraudCaseEntity c = cases.transition(caseId, CaseStatus.UNDER_REVIEW, ActorType.REVIEWER, reviewerId, null, "review started");
        audit.append(caseId, "REVIEW_STARTED", ActorType.REVIEWER, reviewerId, null, "Reviewer opened the recommendation", Map.of());
        return c;
    }

    @Transactional
    public ReviewDispositionEntity submit(String caseId, String reviewerId, Set<String> roles, ReviewCommand cmd) {
        FraudCaseEntity c = cases.get(caseId);
        RecommendationEntity rec = recommendations.findFirstByCaseIdOrderByCreatedAtDescRecommendationIdDesc(caseId).orElse(null);
        Map<String, String> errors = new LinkedHashMap<>();
        String reason = cmd.reason() == null ? "" : cmd.reason().trim();
        if (cmd.disposition() == null) errors.put("disposition", "Select a disposition");
        if (reason.length() < MIN_REASON_LENGTH) errors.put("reason", "A reviewer rationale of at least " + MIN_REASON_LENGTH + " characters is required");
        else if (reason.length() > 2000) errors.put("reason", "Rationale must be at most 2000 characters");
        else if (MaskingRules.containsMarkup(reason) || MaskingRules.containsUnmaskedNumber(reason)) errors.put("reason", "Remove markup or account numbers from the rationale");
        if (rec == null) errors.put("disposition", "No recommendation exists yet; run the investigation first");
        else if (!rec.getMissingInformation().isEmpty() && !cmd.missingInformationAcknowledged()) {
            errors.put("missingInformationAcknowledged", "Acknowledge the missing information before submitting");
        }
        if (!errors.isEmpty()) {
            throw new DomainValidationException(errors);
        }
        if (c.getStatus() == CaseStatus.READY_FOR_REVIEW) {
            startReview(caseId, reviewerId);
            c = cases.get(caseId);
        }
        CaseStatus from = c.getStatus();
        Route route = null;
        CaseStatus to;
        switch (cmd.disposition()) {
            case ACCEPT -> {
                requireStatus(from, CaseStatus.UNDER_REVIEW);
                route = rec.getRoute();
                to = CaseStatus.ROUTED;
            }
            case REROUTE -> {
                requireStatus(from, CaseStatus.UNDER_REVIEW);
                if (cmd.route() == null) throw DomainValidationException.of("route", "Select the queue to reroute to");
                if (cmd.route() == rec.getRoute()) throw DomainValidationException.of("route", "Choose a route different from the recommendation, or accept it");
                route = cmd.route();
                to = CaseStatus.ROUTED;
            }
            case ESCALATE -> {
                requireStatus(from, CaseStatus.UNDER_REVIEW);
                route = Route.FRAUD_INVESTIGATION;
                to = CaseStatus.ROUTED;
            }
            case REQUEST_INFORMATION -> {
                requireStatus(from, CaseStatus.UNDER_REVIEW, CaseStatus.INCOMPLETE);
                to = CaseStatus.INCOMPLETE;
            }
            case REJECT_AND_CLOSE -> {
                if (!roles.contains(ROLE_LEAD)) throw new AccessDeniedException("Only a team lead may reject and close a recommendation");
                requireStatus(from, CaseStatus.UNDER_REVIEW, CaseStatus.INCOMPLETE);
                to = CaseStatus.CLOSED;
            }
            case CLOSE -> {
                requireStatus(from, CaseStatus.ROUTED, CaseStatus.INCOMPLETE);
                route = c.getAssignedRoute();
                to = CaseStatus.CLOSED;
            }
            default -> throw DomainValidationException.of("disposition", "Unsupported disposition");
        }
        if (to != from) {
            cases.transition(caseId, to, ActorType.REVIEWER, reviewerId, null, cmd.disposition().name());
        }
        c = cases.get(caseId);
        if (route != null) {
            c.setAssignedRoute(route);
            cases.save(c);
        }
        ReviewDispositionEntity r = new ReviewDispositionEntity();
        r.setReviewId(ids.reviewId());
        r.setCaseId(caseId);
        r.setRecommendationId(rec.getRecommendationId());
        r.setReviewerId(reviewerId);
        r.setDisposition(cmd.disposition());
        r.setSelectedRoute(route);
        r.setReason(reason);
        r.setMissingInformationAcknowledged(cmd.missingInformationAcknowledged());
        r.setFromStatus(from);
        r.setToStatus(to);
        r.setRecordedAt(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
        reviews.save(r);
        audit.append(caseId, "REVIEW_RECORDED", ActorType.REVIEWER, reviewerId, null,
                r.getReviewId() + ": " + cmd.disposition() + (route == null ? "" : " -> " + route) + " on " + rec.getRecommendationId()
                        + " (" + from + " -> " + to + ")",
                Map.of("reason", reason, "recommendationId", rec.getRecommendationId(), "disposition", cmd.disposition().name()));
        return r;
    }

    @Transactional(readOnly = true)
    public List<ReviewDispositionEntity> history(String caseId) {
        return reviews.findByCaseIdOrderByRecordedAtAsc(caseId);
    }

    /** Dispositions offered to a reviewer for the current state and role. */
    public static List<Disposition> allowedDispositions(CaseStatus status, Set<String> roles) {
        boolean lead = roles.contains(ROLE_LEAD);
        return switch (status) {
            case READY_FOR_REVIEW, UNDER_REVIEW -> lead
                    ? List.of(Disposition.ACCEPT, Disposition.REROUTE, Disposition.ESCALATE, Disposition.REQUEST_INFORMATION, Disposition.REJECT_AND_CLOSE)
                    : List.of(Disposition.ACCEPT, Disposition.REROUTE, Disposition.ESCALATE, Disposition.REQUEST_INFORMATION);
            case INCOMPLETE -> lead ? List.of(Disposition.REQUEST_INFORMATION, Disposition.CLOSE, Disposition.REJECT_AND_CLOSE)
                    : List.of(Disposition.REQUEST_INFORMATION, Disposition.CLOSE);
            case ROUTED -> List.of(Disposition.CLOSE);
            default -> List.of();
        };
    }

    private static void requireStatus(CaseStatus actual, CaseStatus... allowed) {
        for (CaseStatus s : allowed) {
            if (s == actual) {
                return;
            }
        }
        throw DomainValidationException.of("disposition", "This disposition is not allowed while the case is " + actual);
    }
}
