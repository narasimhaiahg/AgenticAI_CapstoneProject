package com.capstone.fraudcase.agent.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Detects text claiming that an autonomous financial, account-control, compliance or contact action happened. */
public final class ActionClaimGuard {

    private static final List<Pattern> CLAIMS = List.of(
            Pattern.compile("\\b(refund|chargeback|credit|reimbursement)s?\\s+(has\\s+been\\s+|was\\s+|is\\s+)?(issued|processed|initiated|granted|applied|completed)"),
            Pattern.compile("\\b(issued|processed|initiated|granted)\\s+(a\\s+|the\\s+)?(refund|chargeback|credit)"),
            Pattern.compile("\\bcard\\s+(has\\s+been\\s+|was\\s+|is\\s+now\\s+)?(blocked|frozen|cancelled|canceled|suspended|hotlisted)"),
            Pattern.compile("\\b(blocked|froze|cancelled|suspended)\\s+(the\\s+)?(card|account)"),
            Pattern.compile("\\b(funds|money|payment)\\s+(has\\s+been\\s+|were\\s+|was\\s+)?(moved|transferred|reversed|returned)"),
            Pattern.compile("\\b(filed|submitted|sent)\\s+(a\\s+|the\\s+)?(sar|str|report)\\s+(to|with)"),
            Pattern.compile("\\b(contacted|called|emailed|messaged|notified)\\s+the\\s+customer"));

    private ActionClaimGuard() {
    }

    public static List<String> findClaims(String text) {
        List<String> found = new ArrayList<>();
        if (text == null) {
            return found;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (Pattern p : CLAIMS) {
            var m = p.matcher(lower);
            if (m.find()) {
                found.add(m.group());
            }
        }
        return found;
    }
}
