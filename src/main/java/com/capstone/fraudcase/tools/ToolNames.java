package com.capstone.fraudcase.tools;

import java.util.List;

/** The approved tool catalogue (allow-list). Anything not listed here cannot be executed. */
public final class ToolNames {
    public static final String TRANSACTION = "getTransactionDetails";
    public static final String DEVICE = "getDeviceSignals";
    public static final String LOCATION = "compareRecentLocations";
    public static final String MERCHANT = "getMerchantRiskProfile";
    public static final String PRIOR_DISPUTES = "getPriorDisputes";
    public static final String SLA = "calculateCaseSla";
    public static final String POLICY = "retrievePolicyEvidence";
    public static final String AUDIT = "saveAuditEvent";

    public static final List<String> ALLOW_LIST = List.of(TRANSACTION, DEVICE, LOCATION, MERCHANT, PRIOR_DISPUTES, SLA, POLICY, AUDIT);

    private ToolNames() {
    }

    public static boolean isAllowed(String name) {
        return ALLOW_LIST.contains(name);
    }
}
