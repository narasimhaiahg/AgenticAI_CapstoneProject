package com.capstone.fraudcase.tools;

import com.capstone.fraudcase.domain.ToolStatus;

import java.time.Instant;

/**
 * Envelope returned by every tool: provenance (toolResultId, observedAt, origin) plus either typed data or a
 * structured unavailable outcome. Provider or stack details are never included.
 */
public record ToolResult<T>(String toolResultId, String toolName, ToolStatus status, Instant observedAt, String origin,
                            long durationMs, String summary, T data) {

    public boolean ok() {
        return status == ToolStatus.SUCCESS && data != null;
    }
}
