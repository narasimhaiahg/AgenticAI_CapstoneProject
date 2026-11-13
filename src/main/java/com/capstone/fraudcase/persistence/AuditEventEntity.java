package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.domain.ActorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

/** Append-only audit record. Updates and deletes are rejected at the entity level. */
@Entity
@Immutable
@Table(name = "audit_event")
public class AuditEventEntity {

    @Id
    private String eventId;
    private long sequenceNo;
    private String caseId;
    private String eventType;
    @Enumerated(EnumType.STRING)
    private ActorType actorType;
    private String actorId;
    private String correlationId;
    private String summary;
    private String payloadHash;
    private String previousHash;
    private OffsetDateTime recordedAt;

    protected AuditEventEntity() {
    }

    public AuditEventEntity(String eventId, long sequenceNo, String caseId, String eventType, ActorType actorType, String actorId,
                            String correlationId, String summary, String payloadHash, String previousHash, OffsetDateTime recordedAt) {
        this.eventId = eventId;
        this.sequenceNo = sequenceNo;
        this.caseId = caseId;
        this.eventType = eventType;
        this.actorType = actorType;
        this.actorId = actorId;
        this.correlationId = correlationId;
        this.summary = summary;
        this.payloadHash = payloadHash;
        this.previousHash = previousHash;
        this.recordedAt = recordedAt;
    }

    @PreUpdate
    @PreRemove
    void rejectMutation() {
        throw new UnsupportedOperationException("Audit events are append-only");
    }

    public String getEventId() { return eventId; }
    public long getSequenceNo() { return sequenceNo; }
    public String getCaseId() { return caseId; }
    public String getEventType() { return eventType; }
    public ActorType getActorType() { return actorType; }
    public String getActorId() { return actorId; }
    public String getCorrelationId() { return correlationId; }
    public String getSummary() { return summary; }
    public String getPayloadHash() { return payloadHash; }
    public String getPreviousHash() { return previousHash; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
}
