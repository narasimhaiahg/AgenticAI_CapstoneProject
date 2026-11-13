package com.capstone.fraudcase.application;

import java.util.Map;

/** Business validation failure with field-level messages safe to show to users. */
public class DomainValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public DomainValidationException(Map<String, String> fieldErrors) {
        super("Validation failed: " + fieldErrors.keySet());
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public static DomainValidationException of(String field, String message) {
        return new DomainValidationException(Map.of(field, message));
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
