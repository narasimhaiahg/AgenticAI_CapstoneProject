package com.capstone.fraudcase.knowledge;

import java.time.LocalDate;
import java.util.List;

/** An indexed chunk of a policy document. {@code availability} is ACTIVE, SUPERSEDED, WITHDRAWN, NOT_YET_EFFECTIVE or QUARANTINED. */
public record PolicyChunk(String chunkId, String documentId, String title, String version, LocalDate effectiveDate,
                          String section, String sectionTitle, int chunkIndex, String text, List<String> appliesTo,
                          String availability, List<String> injectionIndicators) {

    public boolean searchable() {
        return "ACTIVE".equals(availability);
    }
}
