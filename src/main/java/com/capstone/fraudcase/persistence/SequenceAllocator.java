package com.capstone.fraudcase.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Allocates counter values from the row-locked {@code id_sequence} table. Each allocation runs in its own
 * REQUIRES_NEW transaction so the row lock is released on inner commit — the caller's transaction never holds
 * the counter row, which keeps concurrent appends from stalling on each other or forking the audit chain.
 * Synchronized so the audit sequence is allocated in append order within this JVM.
 */
@Component
public class SequenceAllocator {

    private final JdbcTemplate jdbc;

    public SequenceAllocator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized long next(String sequence) {
        Long v = jdbc.queryForObject("SELECT next_val FROM id_sequence WHERE name = ? FOR UPDATE",
                Long.class, sequence);
        if (v == null) {
            throw new IllegalStateException("Sequence " + sequence + " returned no value");
        }
        jdbc.update("UPDATE id_sequence SET next_val = ? WHERE name = ?", v + 1, sequence);
        return v;
    }
}
