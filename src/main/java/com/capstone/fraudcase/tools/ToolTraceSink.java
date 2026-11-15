package com.capstone.fraudcase.tools;

import com.capstone.fraudcase.domain.ToolStatus;

import java.time.Instant;

/** Port implemented by persistence: allocates trace IDs and stores redacted tool traces. */
public interface ToolTraceSink {

    String nextTraceId();

    void record(TraceRecord trace);

    record TraceRecord(String traceId, String caseId, int runNumber, String correlationId, String toolName,
                       String requestHash, ToolStatus status, long durationMs, String resultSummary,
                       String redactedResultJson, String origin, Instant startedAt) {
    }
}
