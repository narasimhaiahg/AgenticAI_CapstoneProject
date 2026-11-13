package com.capstone.fraudcase.knowledge;

import java.time.LocalDate;
import java.util.List;

/** A synthetic policy document with validated metadata. */
public record PolicyDocument(String documentId, String title, String version, LocalDate effectiveDate, String owner,
                             String classification, String declaredStatus, String chunkPrefix, List<String> appliesTo,
                             String sourceName, List<PolicySection> sections) {

    public record PolicySection(String section, String title, String body) {
    }
}
