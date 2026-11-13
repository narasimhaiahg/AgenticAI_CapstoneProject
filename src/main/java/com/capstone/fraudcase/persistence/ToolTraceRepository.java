package com.capstone.fraudcase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToolTraceRepository extends JpaRepository<ToolTraceEntity, String> {
    List<ToolTraceEntity> findByCaseIdAndRunNumberOrderByStartedAtAscTraceIdAsc(String caseId, int runNumber);
}
