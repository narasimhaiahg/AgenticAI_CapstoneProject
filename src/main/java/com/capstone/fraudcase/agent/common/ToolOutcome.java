package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.domain.ToolStatus;

/** Redacted view of a tool execution, shown to processors and reviewers. */
public record ToolOutcome(String traceId, String toolName, ToolStatus status, long durationMs, String summary, String origin) {
}
