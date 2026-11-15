package com.capstone.fraudcase.agent.common;

import java.time.LocalDate;
import java.util.List;

/** A retrieved policy chunk with exact citation metadata and similarity score. */
public record PolicyPassage(
        String documentId,
        String title,
        String version,
        LocalDate effectiveDate,
        String section,
        String sectionTitle,
        String chunkId,
        double score,
        String text,
        List<String> appliesTo) {

    public PolicyCitationRef toCitation() {
        return new PolicyCitationRef(documentId, version, section, chunkId, round(score));
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
