package com.capstone.fraudcase;

import com.capstone.fraudcase.domain.ToolStatus;
import com.capstone.fraudcase.tools.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ToolPipelineTest {
    private final List<ToolTraceSink.TraceRecord> traces = new CopyOnWriteArrayList<>();
    private final ToolTraceSink sink = new ToolTraceSink() {
        private int seq;
        public String nextTraceId() { return "TR-TEST-" + (++seq); }
        public void record(TraceRecord t) { traces.add(t); }
    };
    private final ToolExecutor executor = new ToolExecutor(sink, new ObjectMapper(),
            new SimpleMeterRegistry(), Clock.systemUTC(), 1500);
    private final SyntheticFixtureRepository fixtures =
            new SyntheticFixtureRepository(new ObjectMapper(), "fraud-training-v1");
    private final EvidenceToolkit toolkit = new EvidenceToolkit(fixtures, executor, 4000);

    private CaseToolScope scope(String txn) { return scope(txn, "NORTHSTAR DIGITAL ••••73"); }

    private CaseToolScope scope(String txn, String merchant) {
        return new CaseToolScope("CASE-1", 1, "c1", "CUST-••••-4821", txn, merchant,
                Instant.now(), new BigDecimal("10"), "INR", Instant.now(), "CARD_NOT_PRESENT");
    }

    @Test
    void fixturesAreDeterministic() {
        var a = toolkit.getTransactionDetails(scope("TXN-2026-784102"), "PLAN", "TXN-2026-784102");
        var b = toolkit.getTransactionDetails(scope("TXN-2026-784102"), "PLAN", "TXN-2026-784102");
        assertEquals(ToolStatus.SUCCESS, a.status());
        assertEquals(a.data(), b.data());
    }

    @Test
    void unknownTransactionReturnsNotFound() {
        var r = toolkit.getTransactionDetails(scope("TXN-2026-999404"), "PLAN", "TXN-2026-999404");
        assertEquals(ToolStatus.NOT_FOUND, r.status());
    }

    @Test
    void slowToolTimesOut() {
        var r = toolkit.getMerchantRiskProfile(scope("TXN-2026-118850", "SLOW MERCHANT ••55"), "PLAN", "SLOW MERCHANT ••55");
        assertEquals(ToolStatus.TIMEOUT, r.status());
        assertTrue(traces.stream().anyMatch(t -> t.toolName().equals(ToolNames.MERCHANT) && t.status() == ToolStatus.TIMEOUT));
    }

    @Test
    void disallowedToolRejected() {
        var r = executor.execute(scope("TXN-2026-784102"), "issueRefund", "MODEL", Map.of(), () -> Map.of(), Object::toString);
        assertEquals(ToolStatus.REJECTED, r.status());
    }

    @Test
    void traceRecordedWithRequestHash() {
        toolkit.getDeviceSignals(scope("TXN-2026-784102"), "PLAN", "CUST-••••-4821", "TXN-2026-784102");
        var tr = traces.stream().filter(t -> t.toolName().equals(ToolNames.DEVICE)).findFirst().orElseThrow();
        assertNotNull(tr.requestHash());
        assertTrue(tr.requestHash().matches("[0-9a-f]{16,64}"));
        assertTrue(tr.durationMs() >= 0);
        assertTrue(tr.redactedResultJson() == null || !tr.redactedResultJson().contains("4111"));
    }
}
