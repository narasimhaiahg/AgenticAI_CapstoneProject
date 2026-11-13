package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.domain.*;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "fraud_case")
public class FraudCaseEntity {

    @Id
    private String caseId;
    private String maskedCustomerRef;
    private String homeRegion;
    @Enumerated(EnumType.STRING)
    private CaseStatus status;
    @Enumerated(EnumType.STRING)
    private IntakeChannel intakeChannel;
    private OffsetDateTime reportedAt;
    private String createdBy;
    private boolean syntheticConfirmed;
    private boolean reviewRequired;
    private int currentRun;
    @Enumerated(EnumType.STRING)
    private Route assignedRoute;
    private OffsetDateTime updatedAt;
    @Version @Column(name = "row_version")
    private long rowVersion;

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getMaskedCustomerRef() { return maskedCustomerRef; }
    public void setMaskedCustomerRef(String maskedCustomerRef) { this.maskedCustomerRef = maskedCustomerRef; }
    public String getHomeRegion() { return homeRegion; }
    public void setHomeRegion(String homeRegion) { this.homeRegion = homeRegion; }
    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }
    public IntakeChannel getIntakeChannel() { return intakeChannel; }
    public void setIntakeChannel(IntakeChannel intakeChannel) { this.intakeChannel = intakeChannel; }
    public OffsetDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(OffsetDateTime reportedAt) { this.reportedAt = reportedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public boolean isSyntheticConfirmed() { return syntheticConfirmed; }
    public void setSyntheticConfirmed(boolean syntheticConfirmed) { this.syntheticConfirmed = syntheticConfirmed; }
    public boolean isReviewRequired() { return reviewRequired; }
    public void setReviewRequired(boolean reviewRequired) { this.reviewRequired = reviewRequired; }
    public int getCurrentRun() { return currentRun; }
    public void setCurrentRun(int currentRun) { this.currentRun = currentRun; }
    public Route getAssignedRoute() { return assignedRoute; }
    public void setAssignedRoute(Route assignedRoute) { this.assignedRoute = assignedRoute; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public long getRowVersion() { return rowVersion; }
}
