package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.ProcessorMode;
import com.capstone.fraudcase.domain.RiskLevel;
import com.capstone.fraudcase.domain.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates every recommendation (model or rules) against the output contract and the case context
 * before it may be persisted or displayed. Citation metadata is normalised to the exact retrieved values.
 */
public class RecommendationValidator {

    public static final int MAX_RATIONALE_LENGTH = 1500;

    private static final Pattern PROCEDURAL_CLAIM = Pattern.compile(
            "\\b(per|under|according to|as required by|in line with)\\s+(the\\s+)?(policy|procedure|section|pol-)"
                    + "|\\b(policy|procedure)\\s+(requires|mandates|states|says)"
                    + "|\\bsection\\s+\\d+(\\.\\d+)*");

    public FraudCaseRecommendation validate(RawRecommendation raw, FraudCaseContext context, ProcessorMode mode) {
        List<String> violations = new ArrayList<>();
        if (raw == null) {
            throw new RecommendationValidationException(List.of("recommendation is missing"));
        }
        Classification classification = parseEnum(Classification.class, raw.classification(), "classification", violations);
        RiskLevel risk = parseEnum(RiskLevel.class, raw.riskLevel(), "riskLevel", violations);
        Route route = parseEnum(Route.class, raw.recommendedRoute(), "recommendedRoute", violations);
        if (raw.confidence() == null) {
            violations.add("confidence is missing");
        }
        if (raw.humanReviewRequired() == null) {
            violations.add("humanReviewRequired is missing");
        }
        List<PolicyCitationRef> citations = new ArrayList<>();
        if (raw.policyCitations() != null) {
            for (RawRecommendation.RawCitation c : raw.policyCitations()) {
                if (c == null || blank(c.documentId()) || blank(c.version()) || blank(c.section()) || blank(c.chunkId())) {
                    violations.add("malformed policy citation: documentId, version, section and chunkId are required");
                    continue;
                }
                citations.add(new PolicyCitationRef(c.documentId(), c.version(), c.section(), c.chunkId(), c.score()));
            }
        }
        if (!violations.isEmpty()) {
            throw new RecommendationValidationException(violations);
        }
        FraudCaseRecommendation typed = new FraudCaseRecommendation(
                raw.caseId(), classification, risk, route, raw.confidence(), raw.humanReviewRequired(),
                nonNull(raw.evidenceIds()), citations, nonNull(raw.missingInformation()), nonNull(raw.contradictions()),
                nonNull(raw.ruleIds()), nonNull(raw.toolTraceIds()), raw.rationale(), mode, Map.of());
        return validate(typed, context);
    }

    public FraudCaseRecommendation validate(FraudCaseRecommendation rec, FraudCaseContext context) {
        List<String> violations = new ArrayList<>();
        if (rec == null) {
            throw new RecommendationValidationException(List.of("recommendation is missing"));
        }
        if (blank(rec.caseId())) {
            violations.add("caseId is missing");
        } else if (!rec.caseId().equals(context.caseId())) {
            violations.add("caseId does not match the case under investigation");
        }
        if (rec.classification() == null) violations.add("classification is missing");
        if (rec.riskLevel() == null) violations.add("riskLevel is missing");
        if (rec.recommendedRoute() == null) violations.add("recommendedRoute is missing");
        if (Double.isNaN(rec.confidence()) || rec.confidence() < 0.0 || rec.confidence() > 1.0) {
            violations.add("confidence must be between 0 and 1");
        }
        if (!rec.humanReviewRequired()) {
            violations.add("humanReviewRequired must be true for every recommendation");
        }
        if (rec.classification() != null && rec.recommendedRoute() != null
                && rec.recommendedRoute() != Route.MANUAL_REVIEW
                && rec.recommendedRoute() != rec.classification().typicalRoute()) {
            violations.add("route " + rec.recommendedRoute() + " is not permitted for classification " + rec.classification());
        }
        for (String id : rec.evidenceIds()) {
            if (context.evidence(id).isEmpty()) {
                violations.add("unknown evidence id " + id);
            }
        }
        if (rec.evidenceIds().isEmpty() && rec.classification() != Classification.INSUFFICIENT_EVIDENCE) {
            violations.add("at least one evidence item is required unless the outcome is INSUFFICIENT_EVIDENCE");
        }
        for (String id : rec.toolTraceIds()) {
            if (!context.hasTrace(id)) {
                violations.add("unknown tool trace id " + id);
            }
        }
        List<PolicyCitationRef> normalised = new ArrayList<>();
        for (PolicyCitationRef c : rec.policyCitations()) {
            var match = context.policy().findChunk(c.documentId(), c.version(), c.chunkId());
            if (match.isEmpty()) {
                violations.add("citation " + c.documentId() + "/" + c.chunkId() + " was not retrieved for this case");
            } else {
                normalised.add(match.get().toCitation());
            }
        }
        if (context.policy().abstained()) {
            if (!rec.policyCitations().isEmpty()) {
                violations.add("policy citations are not allowed when retrieval abstained");
            }
            if (rec.recommendedRoute() != null && rec.recommendedRoute() != Route.MANUAL_REVIEW) {
                violations.add("route must be MANUAL_REVIEW when no policy passage met the threshold");
            }
        }
        if (blank(rec.rationale())) {
            violations.add("rationale is missing");
        } else {
            if (rec.rationale().length() > MAX_RATIONALE_LENGTH) {
                violations.add("rationale exceeds " + MAX_RATIONALE_LENGTH + " characters");
            }
            if (normalised.isEmpty() && PROCEDURAL_CLAIM.matcher(rec.rationale().toLowerCase(Locale.ROOT)).find()) {
                violations.add("rationale makes a procedural claim without a policy citation");
            }
        }
        List<String> claims = new ArrayList<>(ActionClaimGuard.findClaims(rec.rationale()));
        rec.missingInformation().forEach(m -> claims.addAll(ActionClaimGuard.findClaims(m)));
        if (!claims.isEmpty()) {
            violations.add("output claims an automated action was taken: " + String.join(", ", claims));
        }
        if (!violations.isEmpty()) {
            throw new RecommendationValidationException(violations);
        }
        return new FraudCaseRecommendation(rec.caseId(), rec.classification(), rec.riskLevel(), rec.recommendedRoute(),
                rec.confidence(), true, rec.evidenceIds(), normalised, rec.missingInformation(), rec.contradictions(),
                rec.ruleIds(), rec.toolTraceIds(), rec.rationale().trim(), rec.mode(), rec.inputFacts());
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field, List<String> violations) {
        if (blank(value)) {
            violations.add(field + " is missing");
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException e) {
            violations.add("unknown " + field + " value '" + truncate(value) + "'");
            return null;
        }
    }

    private static String truncate(String v) {
        return v.length() > 40 ? v.substring(0, 40) + "…" : v;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static List<String> nonNull(List<String> list) {
        return list == null ? List.of() : list.stream().filter(s -> s != null && !s.isBlank()).toList();
    }
}
