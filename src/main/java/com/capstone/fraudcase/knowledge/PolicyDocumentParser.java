package com.capstone.fraudcase.knowledge;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses a markdown policy with a front-matter header and "## <section> <title>" headings. */
public class PolicyDocumentParser {

    private static final Pattern HEADING = Pattern.compile("^##\\s+(\\d+(?:\\.\\d+)*)\\s+(.+)$");
    private static final List<String> REQUIRED = List.of("documentId", "title", "version", "effectiveDate", "owner", "classification", "chunkPrefix");

    public PolicyDocument parse(String sourceName, String content) {
        String normalised = content.replace("\r\n", "\n");
        if (!normalised.startsWith("---\n")) {
            throw new PolicyValidationException(sourceName + ": missing metadata header");
        }
        int end = normalised.indexOf("\n---", 4);
        if (end < 0) {
            throw new PolicyValidationException(sourceName + ": unterminated metadata header");
        }
        Map<String, String> meta = new LinkedHashMap<>();
        for (String line : normalised.substring(4, end).split("\n")) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                meta.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        }
        for (String key : REQUIRED) {
            if (meta.getOrDefault(key, "").isBlank()) {
                throw new PolicyValidationException(sourceName + ": missing metadata '" + key + "'");
            }
        }
        if (!"SYNTHETIC_TRAINING".equals(meta.get("classification"))) {
            throw new PolicyValidationException(sourceName + ": only SYNTHETIC_TRAINING policies may be ingested");
        }
        LocalDate effective;
        try {
            effective = LocalDate.parse(meta.get("effectiveDate"));
        } catch (DateTimeParseException e) {
            throw new PolicyValidationException(sourceName + ": invalid effectiveDate");
        }
        List<PolicyDocument.PolicySection> sections = new ArrayList<>();
        String currentSection = null;
        String currentTitle = null;
        StringBuilder body = new StringBuilder();
        for (String line : normalised.substring(end + 4).split("\n")) {
            Matcher m = HEADING.matcher(line.trim());
            if (m.matches()) {
                if (currentSection != null) {
                    sections.add(new PolicyDocument.PolicySection(currentSection, currentTitle, body.toString().trim()));
                }
                currentSection = m.group(1);
                currentTitle = m.group(2).trim();
                body.setLength(0);
            } else if (currentSection != null) {
                body.append(line).append('\n');
            }
        }
        if (currentSection != null) {
            sections.add(new PolicyDocument.PolicySection(currentSection, currentTitle, body.toString().trim()));
        }
        if (sections.isEmpty()) {
            throw new PolicyValidationException(sourceName + ": no numbered sections found");
        }
        List<String> appliesTo = Arrays.stream(meta.getOrDefault("appliesTo", "").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        return new PolicyDocument(meta.get("documentId"), meta.get("title"), meta.get("version"), effective,
                meta.get("owner"), meta.get("classification"), meta.getOrDefault("status", "ACTIVE").toUpperCase(),
                meta.get("chunkPrefix"), appliesTo, sourceName, sections);
    }

    public static class PolicyValidationException extends RuntimeException {
        public PolicyValidationException(String message) {
            super(message);
        }
    }
}
