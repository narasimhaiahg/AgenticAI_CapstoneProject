package com.capstone.fraudcase.persistence;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** Deliberately exposes no update or delete operations. */
public interface AuditEventRepository extends Repository<AuditEventEntity, String> {

    AuditEventEntity save(AuditEventEntity event);

    List<AuditEventEntity> findByCaseIdOrderBySequenceNoAsc(String caseId);

    List<AuditEventEntity> findAllByOrderBySequenceNoAsc();

    Optional<AuditEventEntity> findTopByOrderBySequenceNoDesc();

    long count();
}
