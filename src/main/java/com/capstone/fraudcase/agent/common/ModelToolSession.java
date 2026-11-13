package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.tools.CaseToolScope;
import com.capstone.fraudcase.tools.EvidenceToolkit;
import com.capstone.fraudcase.tools.ToolResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Case-scoped, read-only tool access for a model during one invocation. Calls are bounded, traced with origin
 * MODEL_REQUESTED and collected so the resulting trace IDs can be validated afterwards.
 */
public class ModelToolSession {

    public static final int MAX_MODEL_TOOL_CALLS = 6;

    private final CaseToolScope scope;
    private final EvidenceToolkit toolkit;
    private final List<ToolResult<?>> results = new ArrayList<>();
    private final AtomicInteger calls = new AtomicInteger();

    public ModelToolSession(FraudCaseContext ctx, EvidenceToolkit toolkit) {
        CaseFacts f = ctx.facts();
        this.scope = new CaseToolScope(ctx.caseId(), ctx.runNumber(), ctx.correlationId(), f.maskedCustomerRef(),
                f.transactionRef(), f.maskedMerchant(), f.reportedAt(), f.amount(), f.currency(),
                f.occurredAt().toInstant(), f.transactionChannel().name());
        this.toolkit = toolkit;
    }

    public Object transaction(String transactionRef) {
        return bounded(() -> toolkit.getTransactionDetails(scope, EvidenceToolkit.ORIGIN_MODEL, transactionRef));
    }

    public Object device(String maskedCustomerRef, String transactionRef) {
        return bounded(() -> toolkit.getDeviceSignals(scope, EvidenceToolkit.ORIGIN_MODEL, maskedCustomerRef, transactionRef));
    }

    public Object location(String maskedCustomerRef, String transactionRef) {
        return bounded(() -> toolkit.compareRecentLocations(scope, EvidenceToolkit.ORIGIN_MODEL, maskedCustomerRef, transactionRef));
    }

    public Object merchant(String maskedMerchant) {
        return bounded(() -> toolkit.getMerchantRiskProfile(scope, EvidenceToolkit.ORIGIN_MODEL, maskedMerchant));
    }

    public Object priorDisputes(String maskedCustomerRef) {
        return bounded(() -> toolkit.getPriorDisputes(scope, EvidenceToolkit.ORIGIN_MODEL, maskedCustomerRef));
    }

    private synchronized Object bounded(java.util.function.Supplier<ToolResult<?>> call) {
        if (calls.incrementAndGet() > MAX_MODEL_TOOL_CALLS) {
            return java.util.Map.of("status", "REJECTED", "summary", "Tool call budget for this investigation exhausted");
        }
        ToolResult<?> r = call.get();
        results.add(r);
        return r;
    }

    public synchronized List<ToolOutcome> outcomes() {
        return results.stream().map(r -> new ToolOutcome(r.toolResultId(), r.toolName(), r.status(), r.durationMs(),
                r.summary(), r.origin())).toList();
    }

    /** The original context extended with traces created by model-requested tool calls. */
    public FraudCaseContext extend(FraudCaseContext ctx) {
        List<ToolOutcome> all = new ArrayList<>(ctx.toolOutcomes());
        all.addAll(outcomes());
        return new FraudCaseContext(ctx.caseId(), ctx.correlationId(), ctx.runNumber(), ctx.facts(), ctx.evidence(), all,
                ctx.contradictions(), ctx.missingInformation(), ctx.policy(), ctx.conversation(), ctx.injectionIndicators());
    }
}
