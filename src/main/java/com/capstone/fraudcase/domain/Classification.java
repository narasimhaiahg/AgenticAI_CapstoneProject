package com.capstone.fraudcase.domain;

public enum Classification {
    UNAUTHORISED_TRANSACTION(Route.FRAUD_INVESTIGATION),
    MERCHANT_DISPUTE(Route.DISPUTE_OPERATIONS),
    DUPLICATE_PROCESSING(Route.PAYMENT_OPERATIONS),
    SUSPICIOUS_TRANSACTION(Route.FRAUD_INVESTIGATION),
    INSUFFICIENT_EVIDENCE(Route.MANUAL_REVIEW),
    GENUINE_TRANSACTION(Route.MANUAL_REVIEW);

    private final Route typicalRoute;

    Classification(Route typicalRoute) { this.typicalRoute = typicalRoute; }

    public Route typicalRoute() { return typicalRoute; }
}
