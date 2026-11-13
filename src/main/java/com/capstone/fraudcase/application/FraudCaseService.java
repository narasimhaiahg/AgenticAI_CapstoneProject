package com.capstone.fraudcase.application;

import com.capstone.fraudcase.agent.common.PromptInjectionDetector;
import com.capstone.fraudcase.config.CapstoneProperties;
import com.capstone.fraudcase.domain.ActorType;
import com.capstone.fraudcase.domain.CardPossession;
import com.capstone.fraudcase.domain.CaseStatus;
import com.capstone.fraudcase.domain.IntakeChannel;
import com.capstone.fraudcase.domain.InvalidStateTransitionException;
import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.MaskingRules;
import com.capstone.fraudcase.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Case intake, state transitions and read access. State changes are always audited. */
@Service
public class FraudCaseService {

    public static final int MAX_STATEMENT_LENGTH = 1000;

    private final FraudCaseRepository cases;
    private final TransactionClaimRepository claims;
    private final IdGenerator ids;
    private final AuditWriter audit;
    private final CapstoneProperties props;
    private final Clock clock;

    public FraudCaseService(FraudCaseRepository cases, TransactionClaimRepository claims, IdGenerator ids, AuditWriter audit,
                            CapstoneProperties props, Clock clock) {
        this.cases = cases;
        this.claims = claims;
        this.ids = ids;
        this.audit = audit;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public FraudCaseEntity create(IntakeCommand cmd, String analystId) {
        validate(cmd);
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        String caseId = ids.caseId();
        FraudCaseEntity c = new FraudCaseEntity();
        c.setCaseId(caseId);
        c.setMaskedCustomerRef(cmd.maskedCustomerRef().trim());
        c.setHomeRegion(cmd.homeRegion().trim());
        c.setStatus(CaseStatus.NEW);
        c.setIntakeChannel(cmd.intakeChannel() == null ? IntakeChannel.PHONE : cmd.intakeChannel());
        c.setReportedAt(now);
        c.setCreatedBy(analystId);
        c.setSyntheticConfirmed(cmd.syntheticDataConfirmed());
        c.setReviewRequired(cmd.reviewRequired());
        c.setCurrentRun(0);
        c.setUpdatedAt(now);
        cases.save(c);

        String original = cmd.customerStatement();
        List<String> injection = PromptInjectionDetector.detect(original);
        TransactionClaimEntity t = new TransactionClaimEntity();
        t.setClaimId("CLM-" + caseId.substring(5));
        t.setCaseId(caseId);
        t.setTransactionRef(cmd.transactionRef().trim());
        t.setAmount(cmd.amount());
        t.setCurrency(cmd.currency().trim());
        t.setOccurredAt(cmd.occurredAt());
        t.setTransactionStatus(cmd.status());
        t.setTransactionChannel(cmd.transactionChannel());
        t.setMaskedMerchant(cmd.maskedMerchant().trim());
        t.setIssueType(cmd.issueType());
        t.setStatementOriginal(original);
        t.setCustomerStatement(normalise(original));
        t.setCardPossession(cmd.cardPossession() == null ? CardPossession.UNKNOWN : cmd.cardPossession());
        t.setMerchantRecognised(cmd.merchantRecognised() != null ? cmd.merchantRecognised() : cmd.issueType() != IssueType.MERCHANT_NOT_RECOGNISED);
        t.setInjectionIndicators(injection);
        claims.save(t);

        audit.append(caseId, "CASE_CREATED", ActorType.USER, analystId, null,
                "Case created via " + c.getIntakeChannel() + " for issue " + cmd.issueType(),
                Map.of("transactionRef", t.getTransactionRef(), "issueType", cmd.issueType().name(), "statementLength", original.length()));
        if (!injection.isEmpty()) {
            audit.append(caseId, "INJECTION_FLAGGED", ActorType.SYSTEM, "intake-validator", null,
                    "Instruction-like content detected in customer statement: " + String.join(", ", injection), Map.of("indicators", injection));
        }
        return c;
    }

    @Transactional
    public FraudCaseEntity transition(String caseId, CaseStatus target, ActorType actorType, String actorId, String correlationId, String reason) {
        FraudCaseEntity c = get(caseId);
        CaseStatus from = c.getStatus();
        if (!from.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(from, target);
        }
        c.setStatus(target);
        c.setUpdatedAt(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
        cases.save(c);
        audit.append(caseId, "STATE_CHANGED", actorType, actorId, correlationId, from + " -> " + target + (reason == null ? "" : " (" + reason + ")"),
                Map.of("from", from.name(), "to", target.name()));
        return c;
    }

    @Transactional(readOnly = true)
    public FraudCaseEntity get(String caseId) {
        return cases.findById(caseId).orElseThrow(() -> new CaseNotFoundException(caseId));
    }

    @Transactional(readOnly = true)
    public TransactionClaimEntity claim(String caseId) {
        return claims.findByCaseId(caseId).orElseThrow(() -> new CaseNotFoundException(caseId));
    }

    @Transactional(readOnly = true)
    public List<FraudCaseEntity> list() {
        return cases.findAllByOrderByReportedAtDesc();
    }

    @Transactional
    public void incrementRun(FraudCaseEntity c) {
        c.setCurrentRun(c.getCurrentRun() + 1);
        cases.save(c);
    }

    @Transactional
    public void save(FraudCaseEntity c) {
        cases.save(c);
    }

    void validate(IntakeCommand cmd) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!cmd.syntheticDataConfirmed()) errors.put("syntheticDataConfirmed", "Confirm that only synthetic training data is used");
        if (!cmd.reviewRequired() || !props.isHumanReviewRequired()) errors.put("reviewRequired", "Human review is mandatory");
        check(errors, "maskedCustomerRef", cmd.maskedCustomerRef(), MaskingRules.MASKED_CUSTOMER_REF, "Use a masked reference such as CUST-••••-4821");
        check(errors, "transactionRef", cmd.transactionRef(), MaskingRules.TRANSACTION_REF, "Use a transaction reference such as TXN-2026-784102");
        check(errors, "maskedMerchant", cmd.maskedMerchant(), MaskingRules.MASKED_MERCHANT, "Merchant must be masked, e.g. NORTHSTAR DIGITAL ••••73");
        check(errors, "homeRegion", cmd.homeRegion(), "^[A-Z]{2}(-[A-Z0-9]{1,3})?$", "Use an ISO region code such as IN-KA");
        if (cmd.amount() == null || cmd.amount().signum() <= 0) errors.put("amount", "Amount must be positive");
        else if (cmd.amount().scale() > 2 || cmd.amount().precision() - cmd.amount().scale() > 13) errors.put("amount", "Amount has too many digits");
        if (cmd.currency() == null || !cmd.currency().matches("^[A-Z]{3}$") || !isIsoCurrency(cmd.currency())) errors.put("currency", "Use an ISO 4217 currency code");
        if (cmd.occurredAt() == null) errors.put("occurredAt", "Transaction time is required");
        else if (cmd.occurredAt().toInstant().isAfter(clock.instant().plusSeconds(300))) errors.put("occurredAt", "Transaction time cannot be in the future");
        if (cmd.status() == null) errors.put("status", "Transaction status is required");
        if (cmd.transactionChannel() == null) errors.put("channel", "Transaction channel is required");
        if (cmd.issueType() == null) errors.put("issueType", "Issue type is required");
        String s = cmd.customerStatement();
        if (s == null || s.isBlank()) errors.put("customerStatement", "Customer statement is required");
        else if (s.length() > MAX_STATEMENT_LENGTH) errors.put("customerStatement", "Statement must be at most " + MAX_STATEMENT_LENGTH + " characters");
        else if (MaskingRules.containsMarkup(s)) errors.put("customerStatement", "Markup or scripts are not allowed in the statement");
        else if (MaskingRules.containsUnmaskedNumber(s)) errors.put("customerStatement", "Remove card or account numbers from the statement");
        for (var e : Map.of("maskedCustomerRef", nz(cmd.maskedCustomerRef()), "maskedMerchant", nz(cmd.maskedMerchant()), "transactionRef", nz(cmd.transactionRef())).entrySet()) {
            if (MaskingRules.containsUnmaskedNumber(e.getValue())) errors.put(e.getKey(), "Unmasked card or account numbers are not allowed");
        }
        if (!errors.isEmpty()) {
            throw new DomainValidationException(errors);
        }
    }

    private static void check(Map<String, String> errors, String field, String value, String pattern, String message) {
        if (value == null || !value.trim().matches(pattern)) {
            errors.put(field, message);
        }
    }

    private static boolean isIsoCurrency(String code) {
        try {
            Currency.getInstance(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    static String normalise(String statement) {
        return statement.replaceAll("[\\p{Cntrl}&&[^\\n]]", " ").replaceAll("\\s+", " ").trim();
    }
}
