package com.capstone.fraudcase.agent.common;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Post-processes model follow-up answers: blocks action claims and extracts only references that exist in the case. */
public final class FollowUpGuard {

    public static final String BLOCKED = "The generated answer was withheld because it described an automated action. "
            + "No action has been taken; only a human reviewer can act on this case.";

    private static final Pattern EVIDENCE_REF = Pattern.compile("\\bEV-\\d+\\b");
    private static final Pattern CHUNK_REF = Pattern.compile("\\b[a-z]{2,6}-\\d{2,4}(?:-\\d+)?\\b");

    private FollowUpGuard() {
    }

    public static String safeAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return "No answer could be generated from the recorded case material.";
        }
        return ActionClaimGuard.findClaims(answer).isEmpty() ? answer.trim() : BLOCKED;
    }

    public static List<String> evidenceRefs(String answer, FraudCaseContext ctx) {
        return refs(EVIDENCE_REF, answer, id -> ctx != null && ctx.evidence(id).isPresent());
    }

    public static List<String> chunkRefs(String answer, FraudCaseContext ctx) {
        return refs(CHUNK_REF, answer, id -> ctx != null && ctx.policy().passages().stream().anyMatch(p -> p.chunkId().equals(id)));
    }

    private static List<String> refs(Pattern p, String text, java.util.function.Predicate<String> exists) {
        Set<String> out = new LinkedHashSet<>();
        if (text != null) {
            Matcher m = p.matcher(text);
            while (m.find()) {
                if (exists.test(m.group())) {
                    out.add(m.group());
                }
            }
        }
        return List.copyOf(out);
    }
}
