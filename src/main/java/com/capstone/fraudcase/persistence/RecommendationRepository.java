package com.capstone.fraudcase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<RecommendationEntity, String> {
    Optional<RecommendationEntity> findFirstByCaseIdOrderByCreatedAtDescRecommendationIdDesc(String caseId);

    List<RecommendationEntity> findByCaseIdOrderByCreatedAtAsc(String caseId);
}
