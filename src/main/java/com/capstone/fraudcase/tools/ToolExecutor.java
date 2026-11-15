package com.capstone.fraudcase.tools;

import com.capstone.fraudcase.domain.ToolStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Runs allow-listed tools with a timeout, converts every failure into a structured outcome and records a redacted
 * trace (request hash, status, duration, summary). Only hashes and summaries are logged.
 */
public class ToolExecutor implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final ToolTraceSink sink;
    private final ObjectMapper mapper;
    private final MeterRegistry meters;
    private final Clock clock;
    private final long timeoutMs;
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "evidence-tool");
        t.setDaemon(true);
        return t;
    });

    public ToolExecutor(ToolTraceSink sink, ObjectMapper mapper, MeterRegistry meters, Clock clock, long timeoutMs) {
        this.sink = sink;
        this.mapper = mapper;
        this.meters = meters;
        this.clock = clock;
        this.timeoutMs = timeoutMs;
    }

    public <T> ToolResult<T> execute(CaseToolScope scope, String toolName, String origin, Map<String, ?> args,
                                     Supplier<T> action, Function<T, String> summarizer) {
        String traceId = sink.nextTraceId();
        Instant started = clock.instant();
        long t0 = System.nanoTime();
        String requestHash = hash(toolName, args);
        ToolStatus status;
        T data = null;
        String summary;
        if (!ToolNames.isAllowed(toolName)) {
            status = ToolStatus.REJECTED;
            summary = "Tool is not on the approved allow-list";
        } else {
            Future<T> future = pool.submit(action::get);
            try {
                data = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                status = ToolStatus.SUCCESS;
                summary = summarizer.apply(data);
            } catch (TimeoutException e) {
                future.cancel(true);
                status = ToolStatus.TIMEOUT;
                summary = "No response within " + timeoutMs + " ms; evidence unavailable";
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof ToolNotFoundException) {
                    status = ToolStatus.NOT_FOUND;
                    summary = "Record not found in synthetic source";
                } else if (cause instanceof IllegalArgumentException) {
                    status = ToolStatus.REJECTED;
                    summary = "Arguments rejected: " + safe(cause.getMessage());
                } else {
                    status = ToolStatus.UNAVAILABLE;
                    summary = "Source unavailable; evidence not collected";
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                status = ToolStatus.UNAVAILABLE;
                summary = "Interrupted; evidence not collected";
            }
        }
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        String json = toJson(data);
        sink.record(new ToolTraceSink.TraceRecord(traceId, scope.caseId(), scope.runNumber(), scope.correlationId(),
                toolName, requestHash, status, durationMs, summary, json, origin, started));
        if (meters != null) {
            Timer.builder("capstone.tool.duration").tag("tool", toolName).tag("status", status.name())
                    .register(meters).record(durationMs, TimeUnit.MILLISECONDS);
        }
        log.info("tool={} status={} durationMs={} trace={} requestHash={}", toolName, status, durationMs, traceId, requestHash);
        return new ToolResult<>(traceId, toolName, status, started, origin, durationMs, summary, data);
    }

    private String toJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }

    private static String safe(String message) {
        if (message == null) {
            return "invalid argument";
        }
        return message.length() > 120 ? message.substring(0, 120) : message;
    }

    static String hash(String toolName, Map<String, ?> args) {
        try {
            String canonical = toolName + new TreeMap<>(args == null ? Map.of() : args);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        pool.shutdownNow();
    }
}
