package com.capstone.fraudcase.agent.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Flags instruction-like content inside untrusted text (customer statements, policy passages).
 * Flagged text is still treated purely as data; the flag is surfaced to reviewers.
 */
public final class PromptInjectionDetector {

    private static final Map<String, Pattern> INDICATORS = new LinkedHashMap<>();

    static {
        INDICATORS.put("override-instructions", Pattern.compile("\\b(ignore|disregard|forget|override)\\b.{0,40}\\b(instruction|instructions|rules|guardrails|prompt|policy|policies)\\b"));
        INDICATORS.put("role-reassignment", Pattern.compile("\\b(you are now|act as|pretend to be|new role|developer mode|jailbreak)\\b"));
        INDICATORS.put("system-prompt-probe", Pattern.compile("(system prompt|<\\s*/?\\s*system\\s*>|^\\s*(system|assistant)\\s*:)", Pattern.MULTILINE));
        INDICATORS.put("action-request", Pattern.compile("\\b(issue|process|approve|send)\\b.{0,20}\\b(refund|chargeback|credit)\\b|\\b(block|unblock|freeze)\\b.{0,15}\\b(card|account)\\b|\\btransfer\\b.{0,20}\\bfunds\\b"));
        INDICATORS.put("decision-forcing", Pattern.compile("\\b(mark|classify|set)\\b.{0,30}\\b(as\\s+)?(genuine|approved|closed|low risk)\\b|humanreviewrequired\\s*[:=]\\s*false"));
    }

    private PromptInjectionDetector() {
    }

    /**
     * Narrower check for approved policy content: policies legitimately mention refunds or card blocks, so only
     * instruction-override, role-reassignment and system-prompt patterns are flagged.
     */
    public static List<String> detectInstructionOverride(String text) {
        return detect(text).stream()
                .filter(i -> i.equals("override-instructions") || i.equals("role-reassignment") || i.equals("system-prompt-probe"))
                .toList();
    }

    public static List<String> detect(String text) {
        List<String> found = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return found;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        INDICATORS.forEach((name, pattern) -> {
            if (pattern.matcher(lower).find()) {
                found.add(name);
            }
        });
        return found;
    }
}
