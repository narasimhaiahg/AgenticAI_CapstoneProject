package com.capstone.fraudcase.tools;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Typed, read-only tool result payloads. Values are synthetic training fixtures. */
public final class ToolResults {

    private ToolResults() {
    }

    public record TransactionDetails(boolean found, String status, Instant timestamp, BigDecimal amount, String currency,
                                     String channel, String authOutcome, String maskedMerchant, String fingerprint,
                                     String duplicateOfRef) {
        public boolean authUnavailable() {
            return authOutcome == null || "UNAVAILABLE".equals(authOutcome);
        }
    }

    public record DeviceSignals(boolean deviceKnown, Instant firstSeenAt, String trustLevel, List<String> observations) {
    }

    public record LocationComparison(int distanceKm, int timeDeltaMinutes, String anomalyLevel, List<String> observations) {
    }

    public record MerchantRiskProfile(String riskBand, boolean blocklistMatch, String category, List<String> observations) {
    }

    public record PriorDisputes(int count, int similarCount, int lookbackDays, List<String> observations) {
    }

    public record CaseSla(String queue, String priority, Instant targetAt, String explanation) {
    }
}
