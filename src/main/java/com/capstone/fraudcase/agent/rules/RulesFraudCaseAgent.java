package com.capstone.fraudcase.agent.rules;

import com.capstone.fraudcase.agent.common.ConversationTurn;
import com.capstone.fraudcase.agent.common.EvidenceView;
import com.capstone.fraudcase.agent.common.FollowUpAnswer;
import com.capstone.fraudcase.agent.common.FollowUpContext;
import com.capstone.fraudcase.agent.common.FraudCaseContext;
import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.agent.common.FraudCaseRecommendation;
import com.capstone.fraudcase.agent.common.PolicyCitationRef;
import com.capstone.fraudcase.agent.common.PolicyPassage;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.EvidenceCategory;
import com.capstone.fraudcase.domain.EvidenceSeverity;
import com.capstone.fraudcase.domain.ProcessorMode;
import com.capstone.fraudcase.domain.RiskLevel;
import com.capstone.fraudcase.domain.Route;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.capstone.fraudcase.agent.common.EvidenceTypes.*;

/**
 * Deterministic fallback processor. No model dependency; every result lists the rule IDs that fired and the exact
 * input facts used. Rule catalogue:
 * <ul>
 *   <li>R-01 mandatory transaction evidence missing → INSUFFICIENT_EVIDENCE / MANUAL_REVIEW</li>
 *   <li>R-02 duplicate fingerprint confirmed → DUPLICATE_PROCESSING / PAYMENT_OPERATIONS</li>
 *   <li>R-03 ≥2 independently sourced high-severity anomalies → UNAUTHORISED (merchant denied) or SUSPICIOUS / FRAUD_INVESTIGATION</li>
 *   <li>R-04 merchant recognised + fulfilment/cancellation/amount issue → MERCHANT_DISPUTE / DISPUTE_OPERATIONS</li>
 *   <li>R-05 no policy passage met the threshold → MANUAL_REVIEW, gap recorded, no procedural claim</li>
 *   <li>R-06 contradictory signals without corroboration → SUSPICIOUS_TRANSACTION / MANUAL_REVIEW, confidence reduced</li>
 *   <li>R-07 strong authentication on trusted device at home location → GENUINE_TRANSACTION / MANUAL_REVIEW</li>
 *   <li>R-08 single high-severity anomaly → SUSPICIOUS_TRANSACTION / FRAUD_INVESTIGATION</li>
 *   <li>R-09 no decisive evidence → INSUFFICIENT_EVIDENCE / MANUAL_REVIEW</li>
 *   <li>R-10 contradiction penalty, R-11 unavailable tool penalty, R-12 authentication outcome unavailable</li>
 * </ul>
 */
public class RulesFraudCaseAgent implements FraudCaseProcessor {

    private static final Set<EvidenceCategory> ANOMALY_SOURCES =
            EnumSet.of(EvidenceCategory.DEVICE, EvidenceCategory.LOCATION, EvidenceCategory.MERCHANT, EvidenceCategory.AUTHENTICATION);

    private final RecommendationValidator validator;

    public RulesFraudCaseAgent(RecommendationValidator validator) {
        this.validator = validator;
    }

    @Override
    public ProcessorMode mode() {
        return ProcessorMode.RULES;
    }

    @Override
    public String availabilityDetail() {
        return "deterministic rules (no model dependency)";
    }

    @Override
    public FraudCaseRecommendation analyze(FraudCaseContext ctx) {
        var f = ctx.facts();
        List<String> rules = new ArrayList<>();
        Map<String, String> facts = new LinkedHashMap<>();
        Set<String> used = new LinkedHashSet<>();
        List<String> missing = new ArrayList<>(ctx.missingInformation());
        List<String> contradictions = new ArrayList<>(ctx.contradictions());

        facts.put("issueType", f.issueType().name());
        facts.put("merchantRecognised", String.valueOf(f.merchantRecognised()));
        facts.put("cardPossession", f.cardPossession().name());
        facts.put("transactionChannel", f.transactionChannel().name());

        boolean transactionMissing = !has(ctx, TRANSACTION_RECORD);
        facts.put("transactionRecordFound", String.valueOf(!transactionMissing));
        List<EvidenceView> highAnomalies = ctx.evidence().stream()
                .filter(e -> e.severity() == EvidenceSeverity.HIGH && ANOMALY_SOURCES.contains(e.category())).toList();
        long independentHigh = highAnomalies.stream().map(EvidenceView::category).distinct().count();
        facts.put("independentHighAnomalies", String.valueOf(independentHigh));
        boolean duplicate = has(ctx, DUPLICATE_FINGERPRINT);
        facts.put("duplicateFingerprint", String.valueOf(duplicate));
        boolean policyAbstained = ctx.policy().abstained();
        facts.put("policyPassagesAboveThreshold", String.valueOf(ctx.policy().passages().size()));
        facts.put("contradictions", String.valueOf(contradictions.size()));

        Classification classification;
        RiskLevel risk;
        Route route;
        double confidence;
        String reason;

        if (transactionMissing) {
            rules.add("R-01");
            classification = Classification.INSUFFICIENT_EVIDENCE;
            risk = RiskLevel.MEDIUM;
            route = Route.MANUAL_REVIEW;
            confidence = 0.40;
            ids(ctx, used, TRANSACTION_NOT_FOUND, TOOL_UNAVAILABLE, CUSTOMER_CLAIM);
            addOnce(missing, "transaction record for " + f.transactionRef());
            reason = "The transaction record could not be retrieved, so mandatory evidence is missing.";
        } else if (duplicate) {
            rules.add("R-02");
            classification = Classification.DUPLICATE_PROCESSING;
            risk = RiskLevel.LOW;
            route = Route.PAYMENT_OPERATIONS;
            confidence = 0.90;
            ids(ctx, used, DUPLICATE_FINGERPRINT, TRANSACTION_RECORD, CUSTOMER_CLAIM);
            reason = "A matching transaction fingerprint confirms a duplicate posting.";
        } else if (independentHigh >= 2) {
            rules.add("R-03");
            boolean denied = !f.merchantRecognised() && f.issueType() == com.capstone.fraudcase.domain.IssueType.MERCHANT_NOT_RECOGNISED;
            classification = denied ? Classification.UNAUTHORISED_TRANSACTION : Classification.SUSPICIOUS_TRANSACTION;
            risk = RiskLevel.HIGH;
            route = Route.FRAUD_INVESTIGATION;
            confidence = denied ? 0.86 : 0.70;
            highAnomalies.forEach(e -> used.add(e.evidenceId()));
            ids(ctx, used, CUSTOMER_CLAIM, MERCHANT_RECOGNITION);
            reason = independentHigh + " independently sourced high-severity anomalies ("
                    + highAnomalies.stream().map(e -> e.evidenceId() + " " + e.type()).collect(Collectors.joining(", "))
                    + ")" + (denied ? " support the customer's denial of the merchant." : " were observed although the claim does not establish unauthorised use.");
        } else if (f.merchantRecognised() && f.issueType().isMerchantDisputeType()) {
            rules.add("R-04");
            classification = Classification.MERCHANT_DISPUTE;
            risk = RiskLevel.LOW;
            route = Route.DISPUTE_OPERATIONS;
            confidence = 0.85;
            ids(ctx, used, CUSTOMER_CLAIM, MERCHANT_RECOGNITION, TRANSACTION_RECORD, MERCHANT_RISK);
            reason = "The customer recognises the merchant and disputes " + f.issueType().description().toLowerCase(Locale.ROOT) + ".";
        } else if (!contradictions.isEmpty()) {
            rules.add("R-06");
            classification = Classification.SUSPICIOUS_TRANSACTION;
            risk = RiskLevel.MEDIUM;
            route = Route.MANUAL_REVIEW;
            confidence = 0.50;
            ids(ctx, used, DEVICE_TRUSTED, LOCATION_ANOMALY, AUTH_OUTCOME, CUSTOMER_CLAIM);
            reason = "Device, location or authentication signals contradict each other and are not independently corroborated.";
        } else if (f.issueType() == com.capstone.fraudcase.domain.IssueType.MERCHANT_NOT_RECOGNISED && has(ctx, DEVICE_TRUSTED) && has(ctx, LOCATION_CONSISTENT) && hasAuthSuccess(ctx)) {
            rules.add("R-07");
            classification = Classification.GENUINE_TRANSACTION;
            risk = RiskLevel.LOW;
            route = Route.MANUAL_REVIEW;
            confidence = 0.70;
            ids(ctx, used, AUTH_OUTCOME, DEVICE_TRUSTED, LOCATION_CONSISTENT, REPEAT_SIMILAR_CLAIMS, CUSTOMER_CLAIM);
            reason = "Strong authentication succeeded on a trusted device in the home location, which supports customer-authorised activity; reviewer confirmation is required.";
        } else if (independentHigh == 1) {
            rules.add("R-08");
            classification = Classification.SUSPICIOUS_TRANSACTION;
            risk = RiskLevel.MEDIUM;
            route = Route.FRAUD_INVESTIGATION;
            confidence = 0.60;
            highAnomalies.forEach(e -> used.add(e.evidenceId()));
            ids(ctx, used, CUSTOMER_CLAIM);
            reason = "One high-severity anomaly (" + highAnomalies.get(0).evidenceId() + ") was observed without independent corroboration.";
        } else {
            rules.add("R-09");
            classification = Classification.INSUFFICIENT_EVIDENCE;
            risk = RiskLevel.LOW;
            route = Route.MANUAL_REVIEW;
            confidence = 0.40;
            ids(ctx, used, CUSTOMER_CLAIM, TRANSACTION_RECORD);
            reason = "No decisive evidence pattern was found in the collected signals.";
        }

        if (has(ctx, AUTH_UNAVAILABLE)) {
            rules.add("R-12");
            addOnce(missing, "authentication challenge outcome");
            ids(ctx, used, AUTH_UNAVAILABLE);
        }
        long unavailable = ctx.evidence().stream().filter(e -> TOOL_UNAVAILABLE.equals(e.type())).count();
        if (unavailable > 0) {
            rules.add("R-11");
            confidence -= 0.05 * unavailable;
        }
        if (!contradictions.isEmpty() && !rules.contains("R-06")) {
            rules.add("R-10");
            confidence -= 0.15;
        }
        List<PolicyCitationRef> citations = new ArrayList<>();
        if (policyAbstained) {
            rules.add("R-05");
            route = Route.MANUAL_REVIEW;
            confidence -= 0.20;
            addOnce(missing, "applicable policy passage above the retrieval threshold");
        } else {
            citations = cite(ctx.policy().passages(), classification, route);
        }
        confidence = Math.max(0.05, Math.min(0.95, Math.round(confidence * 100.0) / 100.0));

        StringBuilder rationale = new StringBuilder("Rules ").append(String.join(", ", rules)).append(": ").append(reason);
        if (policyAbstained) {
            rationale.append(" No approved passage met the retrieval threshold, so no procedure is asserted.");
        } else if (!citations.isEmpty()) {
            rationale.append(" Consistent with ").append(citations.stream()
                    .map(c -> c.documentId() + " v" + c.version() + " section " + c.section()).collect(Collectors.joining("; "))).append('.');
        }
        rationale.append(" Recommended for human review; no automated action taken.");

        FraudCaseRecommendation rec = new FraudCaseRecommendation(ctx.caseId(), classification, risk, route, confidence, true,
                List.copyOf(used), citations, missing, contradictions, rules,
                ctx.toolOutcomes().stream().map(t -> t.traceId()).toList(), rationale.toString(), ProcessorMode.RULES, facts);
        return validator.validate(rec, ctx);
    }

    @Override
    public FollowUpAnswer answerFollowUp(FollowUpContext ctx) {
        String q = ctx.question().toLowerCase(Locale.ROOT);
        FraudCaseContext c = ctx.caseContext();
        List<String> evidenceRefs = new ArrayList<>();
        List<String> chunkRefs = new ArrayList<>();
        StringBuilder answer = new StringBuilder();
        if (q.matches(".*\\b(earlier|previous|before|remind|said|told)\\b.*")) {
            List<String> priorUser = ctx.history().stream().filter(t -> ConversationTurn.USER.equals(t.role()))
                    .map(ConversationTurn::text).toList();
            answer.append(priorUser.isEmpty() ? "No earlier questions or notes are recorded for this case. "
                    : "Earlier in this case you noted: " + String.join(" | ", priorUser) + ". ");
        }
        if (c == null) {
            answer.append("This case has not been investigated yet, so no evidence is recorded.");
        } else {
            Map<String, List<String>> topics = Map.of(
                    "device", List.of(DEVICE_NEW, DEVICE_TRUSTED), "location", List.of(LOCATION_ANOMALY, LOCATION_CONSISTENT),
                    "merchant", List.of(MERCHANT_RISK, MERCHANT_RECOGNITION), "auth", List.of(AUTH_OUTCOME, AUTH_UNAVAILABLE),
                    "duplicate", List.of(DUPLICATE_FINGERPRINT), "dispute", List.of(PRIOR_DISPUTES, REPEAT_SIMILAR_CLAIMS),
                    "transaction", List.of(TRANSACTION_RECORD, TRANSACTION_NOT_FOUND));
            topics.forEach((topic, types) -> {
                if (q.contains(topic)) {
                    c.evidence().stream().filter(e -> types.contains(e.type())).forEach(e -> {
                        evidenceRefs.add(e.evidenceId());
                        answer.append(e.evidenceId()).append(" (").append(e.type()).append(", ").append(e.severity()).append("): ")
                                .append(e.summary()).append(". ");
                    });
                }
            });
            if (q.matches(".*\\b(policy|procedure|citation|section)\\b.*")) {
                if (c.policy().abstained()) {
                    answer.append("No policy passage met the retrieval threshold for this case. ");
                } else {
                    c.policy().passages().forEach(p -> {
                        chunkRefs.add(p.chunkId());
                        answer.append(p.documentId()).append(" v").append(p.version()).append(" §").append(p.section())
                                .append(" (").append(p.chunkId()).append(", score ").append("%.2f".formatted(p.score())).append("). ");
                    });
                }
            }
            if (q.matches(".*\\b(why|route|recommend|recommendation|classification|risk)\\b.*") && ctx.latestRecommendation() != null) {
                answer.append(ctx.latestRecommendation().rationale()).append(' ');
            }
            if (q.matches(".*\\b(missing|gap|unknown)\\b.*") && ctx.latestRecommendation() != null) {
                List<String> m = ctx.latestRecommendation().missingInformation();
                answer.append(m.isEmpty() ? "No missing information is recorded. " : "Missing: " + String.join("; ", m) + ". ");
            }
        }
        if (answer.isEmpty()) {
            answer.append("That is not recorded in this case's evidence. Ask about device, location, merchant, authentication, "
                    + "transaction, disputes, policy, missing information or the recommendation.");
        }
        return new FollowUpAnswer(answer.toString().trim(), evidenceRefs, chunkRefs, ProcessorMode.RULES);
    }

    private static List<PolicyCitationRef> cite(List<PolicyPassage> passages, Classification classification, Route route) {
        List<PolicyCitationRef> matched = passages.stream()
                .filter(p -> p.appliesTo().contains(classification.name()) || p.appliesTo().contains(route.name()))
                .limit(3).map(PolicyPassage::toCitation).toList();
        return matched.isEmpty() ? passages.stream().limit(1).map(PolicyPassage::toCitation).toList() : matched;
    }

    private static boolean has(FraudCaseContext ctx, String type) {
        return ctx.evidence().stream().anyMatch(e -> type.equals(e.type()));
    }

    private static boolean hasAuthSuccess(FraudCaseContext ctx) {
        return ctx.evidence().stream().anyMatch(e -> AUTH_OUTCOME.equals(e.type()) && e.summary().contains("SUCCESS"));
    }

    private static void ids(FraudCaseContext ctx, Set<String> used, String... types) {
        List<String> wanted = List.of(types);
        ctx.evidence().stream().filter(e -> wanted.contains(e.type())).forEach(e -> used.add(e.evidenceId()));
    }

    private static void addOnce(List<String> list, String value) {
        if (!list.contains(value)) {
            list.add(value);
        }
    }
}
