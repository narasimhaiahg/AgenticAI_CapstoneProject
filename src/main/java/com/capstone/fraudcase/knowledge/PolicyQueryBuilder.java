package com.capstone.fraudcase.knowledge;

import com.capstone.fraudcase.agent.common.CaseFacts;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builds the retrieval query from structured case type, evidence indicators and the investigation goal.
 * The free-text customer statement is deliberately excluded so it cannot steer retrieval.
 */
public final class PolicyQueryBuilder {

    public static final String NEW_DEVICE = "new-device";
    public static final String LOCATION_ANOMALY = "location-anomaly";
    public static final String AUTH_UNAVAILABLE = "auth-unavailable";
    public static final String DUPLICATE_FINGERPRINT = "duplicate-fingerprint";
    public static final String TRANSACTION_MISSING = "transaction-missing";
    public static final String CONTRADICTION = "contradiction";
    public static final String REPEAT_CLAIMS = "repeat-claims";

    private PolicyQueryBuilder() {
    }

    public static String build(CaseFacts facts, Set<String> indicators) {
        Set<String> terms = new LinkedHashSet<>();
        switch (facts.issueType()) {
            case MERCHANT_NOT_RECOGNISED -> terms.add("customer does not recognise merchant unauthorised transaction initial fraud routing");
            case GOODS_NOT_RECEIVED -> terms.add("merchant dispute goods services not received fulfilment");
            case CANCELLED_NOT_REFUNDED -> terms.add("merchant dispute cancellation refund not received");
            case INCORRECT_AMOUNT -> terms.add("merchant dispute incorrect amount charged");
            case DUPLICATE_CHARGE -> terms.add("duplicate charge processing posting");
            case OTHER -> terms.add("payment issue");
        }
        switch (facts.transactionChannel()) {
            case CARD_NOT_PRESENT -> terms.add("card-not-present online");
            case ATM -> terms.add("atm cash withdrawal dispensed amount");
            case BANK_TRANSFER -> terms.add("bank transfer");
            default -> terms.add("card present terminal");
        }
        if (indicators.contains(NEW_DEVICE)) terms.add("new device");
        if (indicators.contains(LOCATION_ANOMALY)) terms.add("location anomaly");
        if (indicators.contains(AUTH_UNAVAILABLE)) terms.add("authentication outcome unavailable");
        if (indicators.contains(DUPLICATE_FINGERPRINT)) terms.add("matching transaction fingerprint duplicate posting payment operations");
        if (indicators.contains(TRANSACTION_MISSING)) terms.add("insufficient evidence transaction record missing manual review");
        if (indicators.contains(CONTRADICTION)) terms.add("contradictory signals manual review");
        if (indicators.contains(REPEAT_CLAIMS)) terms.add("repeated similar claims customer authorised");
        return String.join(" ", terms);
    }
}
