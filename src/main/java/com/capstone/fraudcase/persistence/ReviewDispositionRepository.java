package com.capstone.fraudcase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewDispositionRepository extends JpaRepository<ReviewDispositionEntity, String> {
    List<ReviewDispositionEntity> findByCaseIdOrderByRecordedAtAsc(String caseId);
}
