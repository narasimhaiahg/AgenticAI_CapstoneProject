package com.capstone.fraudcase.domain;

import java.util.Locale;

public enum ProcessorMode {
    RULES("rules"), SPRING_AI("spring-ai"), LANGCHAIN4J("langchain4j");

    private final String key;

    ProcessorMode(String key) { this.key = key; }

    public String key() { return key; }

    public static ProcessorMode fromKey(String value) {
        String v = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (ProcessorMode m : values()) {
            if (m.key.equals(v) || m.name().equalsIgnoreCase(v)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Unknown processor mode: " + value);
    }
}
