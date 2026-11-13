package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.tools.ToolTraceSink;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class JpaToolTraceSink implements ToolTraceSink {

    private final ToolTraceRepository repository;
    private final IdGenerator ids;

    public JpaToolTraceSink(ToolTraceRepository repository, IdGenerator ids) {
        this.repository = repository;
        this.ids = ids;
    }

    @Override
    public String nextTraceId() {
        return ids.traceId();
    }

    @Override
    public void record(TraceRecord t) {
        ToolTraceEntity e = new ToolTraceEntity();
        e.setTraceId(t.traceId());
        e.setCaseId(t.caseId());
        e.setRunNumber(t.runNumber());
        e.setCorrelationId(t.correlationId());
        e.setToolName(t.toolName());
        e.setRequestHash(t.requestHash());
        e.setStatus(t.status());
        e.setDurationMs(t.durationMs());
        e.setResultSummary(truncate(t.resultSummary(), 500));
        e.setResultJson(truncate(t.redactedResultJson(), 4000));
        e.setOrigin(t.origin());
        e.setStartedAt(t.startedAt().atOffset(ZoneOffset.UTC));
        repository.save(e);
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
