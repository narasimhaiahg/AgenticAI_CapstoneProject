package com.capstone.fraudcase.agent.springai;

import com.capstone.fraudcase.agent.common.ModelToolSession;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Spring AI tool registration of the read-only, case-scoped evidence tools. */
public class SpringAiEvidenceTools {

    private final ModelToolSession session;

    public SpringAiEvidenceTools(ModelToolSession session) {
        this.session = session;
    }

    @Tool(name = "getTransactionDetails", description = "Read-only: transaction state, channel and authentication outcome for this case's transaction.")
    public Object getTransactionDetails(@ToolParam(description = "transactionRef of this case, e.g. TXN-2026-784102") String transactionRef) {
        return session.transaction(transactionRef);
    }

    @Tool(name = "getDeviceSignals", description = "Read-only: device novelty and trust indicators for this case.")
    public Object getDeviceSignals(@ToolParam(description = "masked customer reference of this case") String maskedCustomerRef,
                                   @ToolParam(description = "transactionRef of this case") String transactionRef) {
        return session.device(maskedCustomerRef, transactionRef);
    }

    @Tool(name = "compareRecentLocations", description = "Read-only: compare transaction location with recent trusted locations.")
    public Object compareRecentLocations(@ToolParam(description = "masked customer reference of this case") String maskedCustomerRef,
                                         @ToolParam(description = "transactionRef of this case") String transactionRef) {
        return session.location(maskedCustomerRef, transactionRef);
    }

    @Tool(name = "getMerchantRiskProfile", description = "Read-only: training merchant risk band, blocklist match and category.")
    public Object getMerchantRiskProfile(@ToolParam(description = "masked merchant text of this case") String maskedMerchant) {
        return session.merchant(maskedMerchant);
    }

    @Tool(name = "getPriorDisputes", description = "Read-only: summary of earlier disputes for this case's masked customer.")
    public Object getPriorDisputes(@ToolParam(description = "masked customer reference of this case") String maskedCustomerRef) {
        return session.priorDisputes(maskedCustomerRef);
    }
}
