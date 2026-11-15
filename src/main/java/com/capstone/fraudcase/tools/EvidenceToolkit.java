package com.capstone.fraudcase.tools;

import com.capstone.fraudcase.domain.MaskingRules;
import com.capstone.fraudcase.tools.ToolResults.CaseSla;
import com.capstone.fraudcase.tools.ToolResults.DeviceSignals;
import com.capstone.fraudcase.tools.ToolResults.LocationComparison;
import com.capstone.fraudcase.tools.ToolResults.MerchantRiskProfile;
import com.capstone.fraudcase.tools.ToolResults.PriorDisputes;
import com.capstone.fraudcase.tools.ToolResults.TransactionDetails;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Framework-neutral, read-only evidence tools backed by deterministic synthetic fixtures.
 * Every call is validated, scoped to one case, time-limited and traced by {@link ToolExecutor}.
 */
public class EvidenceToolkit {

    public static final String ORIGIN_PLAN = "APPLICATION_PLAN";
    public static final String ORIGIN_MODEL = "MODEL_REQUESTED";

    private static final Set<String> ROUTES = Set.of("FRAUD_INVESTIGATION", "DISPUTE_OPERATIONS", "PAYMENT_OPERATIONS", "MANUAL_REVIEW");
    private static final Set<String> RISKS = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private final SyntheticFixtureRepository fixtures;
    private final ToolExecutor executor;
    private final long simulatedSlowMs;

    public EvidenceToolkit(SyntheticFixtureRepository fixtures, ToolExecutor executor, long simulatedSlowMs) {
        this.fixtures = fixtures;
        this.executor = executor;
        this.simulatedSlowMs = simulatedSlowMs;
    }

    public ToolResult<TransactionDetails> getTransactionDetails(CaseToolScope scope, String origin, String transactionRef) {
        return executor.execute(scope, ToolNames.TRANSACTION, origin, Map.of("transactionRef", String.valueOf(transactionRef)), () -> {
            requireMatch(transactionRef, scope.transactionRef(), MaskingRules.TRANSACTION_REF, "transactionRef");
            JsonNode n = fixture(transactionRef, "transaction");
            if (n == null) {
                String[] auth = {"SUCCESS_3DS", "FAILED", "NOT_ATTEMPTED", "UNAVAILABLE"};
                return new TransactionDetails(true, "POSTED", scope.occurredAt(), scope.amount(), scope.currency(),
                        scope.channel(), auth[fixtures.stableInt(transactionRef, "auth", auth.length)], scope.maskedMerchant(),
                        "FP-" + fixtures.stableHex(transactionRef, "fingerprint", 10), null);
            }
            return new TransactionDetails(true, text(n, "status", "POSTED"),
                    n.hasNonNull("timestamp") ? Instant.parse(n.get("timestamp").asText()) : scope.occurredAt(),
                    n.hasNonNull("amount") ? new BigDecimal(n.get("amount").asText()) : scope.amount(),
                    text(n, "currency", scope.currency()), text(n, "channel", scope.channel()),
                    text(n, "authOutcome", "UNAVAILABLE"), text(n, "maskedMerchant", scope.maskedMerchant()),
                    text(n, "fingerprint", "FP-" + fixtures.stableHex(transactionRef, "fingerprint", 10)),
                    text(n, "duplicateOfRef", null));
        }, t -> "%s %s %s %s; auth=%s%s".formatted(t.status(), t.amount().toPlainString(), t.currency(), t.channel(),
                t.authOutcome(), t.duplicateOfRef() == null ? "" : "; duplicate of " + t.duplicateOfRef()));
    }

    public ToolResult<DeviceSignals> getDeviceSignals(CaseToolScope scope, String origin, String maskedCustomerRef, String transactionRef) {
        return executor.execute(scope, ToolNames.DEVICE, origin,
                Map.of("maskedCustomerRef", String.valueOf(maskedCustomerRef), "transactionRef", String.valueOf(transactionRef)), () -> {
                    requireMatch(maskedCustomerRef, scope.maskedCustomerRef(), MaskingRules.MASKED_CUSTOMER_REF, "maskedCustomerRef");
                    requireMatch(transactionRef, scope.transactionRef(), MaskingRules.TRANSACTION_REF, "transactionRef");
                    JsonNode n = fixture(transactionRef, "device");
                    if (n == null) {
                        boolean known = fixtures.stableInt(maskedCustomerRef + transactionRef, "device", 100) < 60;
                        int days = known ? 90 + fixtures.stableInt(transactionRef, "deviceAge", 600) : 0;
                        return new DeviceSignals(known, scope.occurredAt().minus(Duration.ofDays(days)).minus(Duration.ofMinutes(known ? 0 : 7)),
                                known ? "HIGH" : "LOW", List.of(known ? "Device previously used for trusted activity" : "Device first observed on this transaction"));
                    }
                    Instant firstSeen = n.hasNonNull("firstSeenAt") ? Instant.parse(n.get("firstSeenAt").asText()) : scope.occurredAt();
                    return new DeviceSignals(n.path("deviceKnown").asBoolean(), firstSeen, text(n, "trustLevel", "UNKNOWN"), list(n));
                }, d -> "deviceKnown=%s trust=%s firstSeen=%s".formatted(d.deviceKnown(), d.trustLevel(), d.firstSeenAt()));
    }

    public ToolResult<LocationComparison> compareRecentLocations(CaseToolScope scope, String origin, String maskedCustomerRef, String transactionRef) {
        return executor.execute(scope, ToolNames.LOCATION, origin,
                Map.of("maskedCustomerRef", String.valueOf(maskedCustomerRef), "transactionRef", String.valueOf(transactionRef)), () -> {
                    requireMatch(maskedCustomerRef, scope.maskedCustomerRef(), MaskingRules.MASKED_CUSTOMER_REF, "maskedCustomerRef");
                    requireMatch(transactionRef, scope.transactionRef(), MaskingRules.TRANSACTION_REF, "transactionRef");
                    JsonNode n = fixture(transactionRef, "location");
                    if (n == null) {
                        int km = fixtures.stableInt(maskedCustomerRef + transactionRef, "distance", 2500);
                        int minutes = 30 + fixtures.stableInt(transactionRef, "timeDelta", 600);
                        String level = km > 1000 ? "HIGH" : km > 300 ? "MEDIUM" : "LOW";
                        return new LocationComparison(km, minutes, level, List.of("Distance from last trusted location " + km + " km"));
                    }
                    return new LocationComparison(n.path("distanceKm").asInt(), n.path("timeDeltaMinutes").asInt(),
                            text(n, "anomalyLevel", "LOW"), list(n));
                }, l -> "%d km from last trusted location in %d min; anomaly=%s".formatted(l.distanceKm(), l.timeDeltaMinutes(), l.anomalyLevel()));
    }

    public ToolResult<MerchantRiskProfile> getMerchantRiskProfile(CaseToolScope scope, String origin, String maskedMerchant) {
        return executor.execute(scope, ToolNames.MERCHANT, origin, Map.of("maskedMerchant", String.valueOf(maskedMerchant)), () -> {
            requireMatch(maskedMerchant, scope.maskedMerchant(), MaskingRules.MASKED_MERCHANT, "maskedMerchant");
            JsonNode n = fixture(scope.transactionRef(), "merchant");
            if (n == null) {
                String[] bands = {"LOW", "LOW", "MEDIUM", "HIGH"};
                String[] categories = {"DIGITAL_GOODS", "ELECTRONICS", "TRAVEL", "SUBSCRIPTION", "GROCERY"};
                String band = bands[fixtures.stableInt(maskedMerchant, "merchantRisk", bands.length)];
                return new MerchantRiskProfile(band, false, categories[fixtures.stableInt(maskedMerchant, "category", categories.length)],
                        List.of("Training risk band " + band));
            }
            return new MerchantRiskProfile(text(n, "riskBand", "LOW"), n.path("blocklistMatch").asBoolean(), text(n, "category", "UNKNOWN"), list(n));
        }, m -> "riskBand=%s blocklist=%s category=%s".formatted(m.riskBand(), m.blocklistMatch(), m.category()));
    }

    public ToolResult<PriorDisputes> getPriorDisputes(CaseToolScope scope, String origin, String maskedCustomerRef) {
        return executor.execute(scope, ToolNames.PRIOR_DISPUTES, origin, Map.of("maskedCustomerRef", String.valueOf(maskedCustomerRef)), () -> {
            requireMatch(maskedCustomerRef, scope.maskedCustomerRef(), MaskingRules.MASKED_CUSTOMER_REF, "maskedCustomerRef");
            JsonNode n = fixture(scope.transactionRef(), "priorDisputes");
            if (n == null) {
                int count = fixtures.stableInt(maskedCustomerRef, "disputes", 3);
                return new PriorDisputes(count, Math.min(count, fixtures.stableInt(maskedCustomerRef, "similar", 2)), 365,
                        List.of(count + " earlier dispute(s) in 365 days"));
            }
            return new PriorDisputes(n.path("count").asInt(), n.path("similarCount").asInt(), n.path("lookbackDays").asInt(365), list(n));
        }, p -> "%d earlier dispute(s), %d similar, lookback %d days".formatted(p.count(), p.similarCount(), p.lookbackDays()));
    }

    public ToolResult<CaseSla> calculateCaseSla(CaseToolScope scope, String origin, String route, String riskLevel) {
        return executor.execute(scope, ToolNames.SLA, origin, Map.of("route", String.valueOf(route), "riskLevel", String.valueOf(riskLevel)), () -> {
            if (!ROUTES.contains(route) || !RISKS.contains(riskLevel)) {
                throw new IllegalArgumentException("unknown route or risk level");
            }
            int hours = switch (route) {
                case "FRAUD_INVESTIGATION" -> switch (riskLevel) { case "CRITICAL" -> 2; case "HIGH" -> 4; case "MEDIUM" -> 24; default -> 48; };
                case "PAYMENT_OPERATIONS" -> "LOW".equals(riskLevel) ? 48 : 24;
                case "DISPUTE_OPERATIONS" -> "HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel) ? 48 : 120;
                default -> switch (riskLevel) { case "CRITICAL" -> 4; case "HIGH" -> 8; case "MEDIUM" -> 24; default -> 72; };
            };
            String priority = switch (riskLevel) { case "CRITICAL" -> "P1"; case "HIGH" -> "P2"; case "MEDIUM" -> "P3"; default -> "P4"; };
            return new CaseSla(route, priority, scope.reportedAt().plus(Duration.ofHours(hours)),
                    "Training target: " + hours + "h from report for " + route + " at " + riskLevel + " risk");
        }, s -> "queue=%s priority=%s target=%s".formatted(s.queue(), s.priority(), s.targetAt()));
    }

    /** Returns the fixture node, or null for hash-derived values. Handles NOT_FOUND / SLOW / ERROR markers. */
    private JsonNode fixture(String transactionRef, String section) {
        JsonNode n = fixtures.scenario(transactionRef, section).orElse(null);
        if (n != null && n.isTextual()) {
            switch (n.asText()) {
                case "NOT_FOUND" -> throw new ToolNotFoundException(section + " not found");
                case "SLOW" -> {
                    sleep(simulatedSlowMs);
                    return null;
                }
                case "ERROR" -> throw new IllegalStateException("synthetic source error");
                default -> {
                    return null;
                }
            }
        }
        return n;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted");
        }
    }

    private static void requireMatch(String value, String scoped, String pattern, String field) {
        if (value == null || !value.matches(pattern)) {
            throw new IllegalArgumentException(field + " is not a valid masked reference");
        }
        if (!value.equals(scoped)) {
            throw new IllegalArgumentException(field + " is outside the scope of this case");
        }
    }

    private static String text(JsonNode n, String field, String fallback) {
        return n.hasNonNull(field) ? n.get(field).asText() : fallback;
    }

    private static List<String> list(JsonNode n) {
        List<String> out = new ArrayList<>();
        n.path("observations").forEach(o -> out.add(o.asText()));
        return out;
    }
}
