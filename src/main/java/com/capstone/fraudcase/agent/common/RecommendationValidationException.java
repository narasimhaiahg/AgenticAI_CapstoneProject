package com.capstone.fraudcase.agent.common;

import java.util.List;

public class RecommendationValidationException extends RuntimeException {
    private final List<String> violations;

    public RecommendationValidationException(List<String> violations) {
        super("Recommendation rejected: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return violations;
    }
}
