package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.domain.*;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "evidence_item")
public class EvidenceItemEntity {

    @Id
    private String evidenceId;
    private String caseId;
    private int runNumber;
    @Enumerated(EnumType.STRING)
    private EvidenceCategory category;
    @Column(name = "evidence_type")
    private String type;
    @Enumerated(EnumType.STRING)
    private EvidenceSeverity severity;
    private String valueSummary;
    @Enumerated(EnumType.STRING)
    private Provenance provenance;
    private String sourceRef;
    private OffsetDateTime observedAt;

    public String getEvidenceId() { return evidenceId; }
    public void setEvidenceId(String evidenceId) { this.evidenceId = evidenceId; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public int getRunNumber() { return runNumber; }
    public void setRunNumber(int runNumber) { this.runNumber = runNumber; }
    public EvidenceCategory getCategory() { return category; }
    public void setCategory(EvidenceCategory category) { this.category = category; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public EvidenceSeverity getSeverity() { return severity; }
    public void setSeverity(EvidenceSeverity severity) { this.severity = severity; }
    public String getValueSummary() { return valueSummary; }
    public void setValueSummary(String valueSummary) { this.valueSummary = valueSummary; }
    public Provenance getProvenance() { return provenance; }
    public void setProvenance(Provenance provenance) { this.provenance = provenance; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }
    public OffsetDateTime getObservedAt() { return observedAt; }
    public void setObservedAt(OffsetDateTime observedAt) { this.observedAt = observedAt; }
}
