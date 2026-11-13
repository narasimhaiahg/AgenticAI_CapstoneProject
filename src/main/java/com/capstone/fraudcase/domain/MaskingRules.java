package com.capstone.fraudcase.domain;

import java.util.regex.Pattern;

/** Masking / tokenisation rules for identifiers. Raw card or account numbers are never accepted. */
public final class MaskingRules {

    public static final String MASKED_CUSTOMER_REF = "^CUST-[•*X]{4}-\\d{4}$";
    public static final String TRANSACTION_REF = "^TXN-[A-Z0-9]{2,8}-[A-Z0-9]{4,12}$";
    public static final String MASKED_MERCHANT = "^[A-Z0-9][A-Z0-9 &.'-]{1,40} [•*]{2,4}\\d{2,4}$";

    private static final Pattern PAN_LIKE = Pattern.compile("(?<!\\d)(?:\\d[ -]?){12,19}(?!\\d)");
    private static final Pattern MARKUP = Pattern.compile("<\\s*/?\\s*[a-zA-Z!][^>]*>|javascript:|on\\w+\\s*=", Pattern.CASE_INSENSITIVE);

    private MaskingRules() {
    }

    /** True if the text contains a 12–19 digit sequence that could be a card or account number. */
    public static boolean containsUnmaskedNumber(String text) {
        return text != null && PAN_LIKE.matcher(text).find();
    }

    public static boolean containsMarkup(String text) {
        return text != null && MARKUP.matcher(text).find();
    }

    /** Display-safe form of a customer reference: only the last four characters are shown. */
    public static String maskForDisplay(String ref) {
        if (ref == null || ref.length() <= 4) {
            return "••••";
        }
        return "••••" + ref.substring(ref.length() - 4);
    }
}
