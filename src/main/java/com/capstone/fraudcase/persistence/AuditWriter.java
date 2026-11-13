package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.domain.ActorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Append-only, hash-chained audit trail. Payloads are never stored: only a SHA-256 hash of the canonical payload and a
 * short non-sensitive summary. Each event's hash covers the previous event's hash so tampering is detectable.
 */
@Component
public class AuditWriter {

    public static final String GENESIS = "0".repeat(64);

    private final AuditEventRepository repository;
    private final IdGenerator ids;
    private final Clock clock;
    private final ObjectMapper canonical = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true).findAndRegisterModules();

    public AuditWriter(AuditEventRepository repository, IdGenerator ids, Clock clock) {
        this.repository = repository;
        this.ids = ids;
        this.clock = clock;
    }

    @Transactional
    public synchronized AuditEventEntity append(String caseId, String eventType, ActorType actorType, String actorId,
                                                String correlationId, String summary, Map<String, ?> payload) {
        String previous = repository.findTopByOrderBySequenceNoDesc().map(AuditEventEntity::getPayloadHash).orElse(GENESIS);
        long seq = ids.auditSequence();
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        String safeSummary = summary.length() > 480 ? summary.substring(0, 480) : summary;
        String hash = hash(previous, seq, caseId, eventType, actorType, actorId, now, safeSummary, payloadDigest(payload));
        return repository.save(new AuditEventEntity("AE-" + seq, seq, caseId, eventType, actorType, actorId, correlationId,
                safeSummary, hash, previous, now));
    }

    /** Recomputes the chain link of every event; returns the IDs of events whose previous-hash link is broken. */
    public List<String> brokenLinks() {
        List<AuditEventEntity> all = repository.findAllByOrderBySequenceNoAsc();
        String expectedPrevious = GENESIS;
        java.util.ArrayList<String> broken = new java.util.ArrayList<>();
        for (AuditEventEntity e : all) {
            if (!e.getPreviousHash().equals(expectedPrevious)) {
                broken.add(e.getEventId());
            }
            expectedPrevious = e.getPayloadHash();
        }
        return broken;
    }

    private String payloadDigest(Map<String, ?> payload) {
        try {
            return sha256(canonical.writeValueAsString(payload == null ? Map.of() : new TreeMap<>(payload)));
        } catch (Exception e) {
            return sha256(String.valueOf(payload));
        }
    }

    private static String hash(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            sb.append(p).append('|');
        }
        return sha256(sb.toString());
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
