package com.capstone.fraudcase.agent.common;

/** Evidence type vocabulary shared by the evidence assembler and every processor. */
public final class EvidenceTypes {
    public static final String CUSTOMER_CLAIM = "CUSTOMER_CLAIM";
    public static final String MERCHANT_RECOGNITION = "MERCHANT_RECOGNITION";
    public static final String CARD_POSSESSION = "CARD_POSSESSION";
    public static final String TRANSACTION_RECORD = "TRANSACTION_RECORD";
    public static final String TRANSACTION_NOT_FOUND = "TRANSACTION_NOT_FOUND";
    public static final String AUTH_OUTCOME = "AUTH_OUTCOME";
    public static final String AUTH_UNAVAILABLE = "AUTH_UNAVAILABLE";
    public static final String DUPLICATE_FINGERPRINT = "DUPLICATE_FINGERPRINT";
    public static final String DEVICE_NEW = "DEVICE_NEW";
    public static final String DEVICE_TRUSTED = "DEVICE_TRUSTED";
    public static final String LOCATION_ANOMALY = "LOCATION_ANOMALY";
    public static final String LOCATION_CONSISTENT = "LOCATION_CONSISTENT";
    public static final String MERCHANT_RISK = "MERCHANT_RISK";
    public static final String PRIOR_DISPUTES = "PRIOR_DISPUTES";
    public static final String REPEAT_SIMILAR_CLAIMS = "REPEAT_SIMILAR_CLAIMS";
    public static final String TOOL_UNAVAILABLE = "TOOL_UNAVAILABLE";
    public static final String INJECTION_FLAG = "PROMPT_INJECTION_FLAG";

    private EvidenceTypes() {
    }
}
