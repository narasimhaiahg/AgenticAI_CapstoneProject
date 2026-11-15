package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.domain.*;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "review_disposition")
public class ReviewDispositionEntity {

    @Id
    private String reviewId;
    private String caseId;
    private String recommendationId;
    private String reviewerId;
    @Enumerated(EnumType.STRING)
    private Disposition disposition;
    @Enumerated(EnumType.STRING)
    private Route selectedRoute;
    private String reason;
    @Column(name = "missing_info_acknowledged")
    private boolean missingInformationAcknowledged;
    @Enumerated(EnumType.STRING)
    private CaseStatus fromStatus;
    @Enumerated(EnumType.STRING)
    private CaseStatus toStatus;
    private OffsetDateTime recordedAt;

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public Disposition getDisposition() { return disposition; }
    public void setDisposition(Disposition disposition) { this.disposition = disposition; }
    public Route getSelectedRoute() { return selectedRoute; }
    public void setSelectedRoute(Route selectedRoute) { this.selectedRoute = selectedRoute; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isMissingInformationAcknowledged() { return missingInformationAcknowledged; }
    public void setMissingInformationAcknowledged(boolean missingInformationAcknowledged) { this.missingInformationAcknowledged = missingInformationAcknowledged; }
    public CaseStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(CaseStatus fromStatus) { this.fromStatus = fromStatus; }
    public CaseStatus getToStatus() { return toStatus; }
    public void setToStatus(CaseStatus toStatus) { this.toStatus = toStatus; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }
}
