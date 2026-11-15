package com.capstone.fraudcase.agent.langchain4j;

import com.capstone.fraudcase.agent.common.ModelToolSession;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/** LangChain4j @Tool registration of the same read-only, case-scoped evidence tools. */
public class LangChain4jEvidenceTools {

    private final ModelToolSession session;

    public LangChain4jEvidenceTools(ModelToolSession session) {
        this.session = session;
    }

    @Tool(name = "getTransactionDetails", value = "Read-only: transaction state, channel and authentication outcome for this case's transaction.")
    public Object getTransactionDetails(@P("transactionRef of this case, e.g. TXN-2026-784102") String transactionRef) {
        return session.transaction(transactionRef);
    }

    @Tool(name = "getDeviceSignals", value = "Read-only: device novelty and trust indicators for this case.")
    public Object getDeviceSignals(@P("masked customer reference of this case") String maskedCustomerRef,
                                   @P("transactionRef of this case") String transactionRef) {
        return session.device(maskedCustomerRef, transactionRef);
    }

    @Tool(name = "compareRecentLocations", value = "Read-only: compare transaction location with recent trusted locations.")
    public Object compareRecentLocations(@P("masked customer reference of this case") String maskedCustomerRef,
                                         @P("transactionRef of this case") String transactionRef) {
        return session.location(maskedCustomerRef, transactionRef);
    }

    @Tool(name = "getMerchantRiskProfile", value = "Read-only: training merchant risk band, blocklist match and category.")
    public Object getMerchantRiskProfile(@P("masked merchant text of this case") String maskedMerchant) {
        return session.merchant(maskedMerchant);
    }

    @Tool(name = "getPriorDisputes", value = "Read-only: summary of earlier disputes for this case's masked customer.")
    public Object getPriorDisputes(@P("masked customer reference of this case") String maskedCustomerRef) {
        return session.priorDisputes(maskedCustomerRef);
    }
}
