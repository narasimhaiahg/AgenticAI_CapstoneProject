package com.capstone.fraudcase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyCitationRepository extends JpaRepository<PolicyCitationEntity, Long> {
    List<PolicyCitationEntity> findByCaseIdAndRunNumberOrderByScoreDesc(String caseId, int runNumber);
}
