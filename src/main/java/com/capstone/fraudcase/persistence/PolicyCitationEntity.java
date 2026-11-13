package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.domain.*;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "policy_citation")
public class PolicyCitationEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String caseId;
    private int runNumber;
    private String recommendationId;
    private String documentId;
    private String title;
    private String version;
    private LocalDate effectiveDate;
    private String section;
    private String sectionTitle;
    private String chunkId;
    private double score;
    private String passage;
    private boolean cited;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public int getRunNumber() { return runNumber; }
    public void setRunNumber(int runNumber) { this.runNumber = runNumber; }
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getSectionTitle() { return sectionTitle; }
    public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }
    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public String getPassage() { return passage; }
    public void setPassage(String passage) { this.passage = passage; }
    public boolean isCited() { return cited; }
    public void setCited(boolean cited) { this.cited = cited; }
}
