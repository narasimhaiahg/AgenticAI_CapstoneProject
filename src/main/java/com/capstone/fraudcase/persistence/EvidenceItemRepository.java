package com.capstone.fraudcase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenceItemRepository extends JpaRepository<EvidenceItemEntity, String> {
    List<EvidenceItemEntity> findByCaseIdAndRunNumberOrderByEvidenceIdAsc(String caseId, int runNumber);
}
