package com.capstone.fraudcase.application;

import com.capstone.fraudcase.agent.common.CaseFacts;
import com.capstone.fraudcase.agent.common.EvidenceView;
import com.capstone.fraudcase.domain.EvidenceCategory;
import com.capstone.fraudcase.domain.EvidenceSeverity;
import com.capstone.fraudcase.domain.Provenance;
import com.capstone.fraudcase.domain.ToolStatus;
import com.capstone.fraudcase.knowledge.PolicyQueryBuilder;
import com.capstone.fraudcase.persistence.IdGenerator;
import com.capstone.fraudcase.tools.ToolResult;
import com.capstone.fraudcase.tools.ToolResults.DeviceSignals;
import com.capstone.fraudcase.tools.ToolResults.LocationComparison;
import com.capstone.fraudcase.tools.ToolResults.MerchantRiskProfile;
import com.capstone.fraudcase.tools.ToolResults.PriorDisputes;
import com.capstone.fraudcase.tools.ToolResults.TransactionDetails;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.capstone.fraudcase.agent.common.EvidenceTypes.*;

/**
 * Converts intake claims and tool results into evidence items (one per material fact) with severity, independent
 * source category and provenance; detects contradictions and evidence gaps. Never invents unavailable evidence.
 */
@Component
public class EvidenceAssembler {

    private final IdGenerator ids;

    public EvidenceAssembler(IdGenerator ids) {
        this.ids = ids;
    }

    public record CollectedResults(ToolResult<TransactionDetails> transaction, ToolResult<DeviceSignals> device,
                                   ToolResult<LocationComparison> location, ToolResult<MerchantRiskProfile> merchant,
                                   ToolResult<PriorDisputes> priorDisputes) {
    }

    public record AssembledEvidence(List<EvidenceView> items, List<String> contradictions, List<String> missingInformation,
                                    Set<String> retrievalIndicators) {
    }

    public AssembledEvidence assemble(CaseFacts f, String claimId, List<String> injectionIndicators, CollectedResults r) {
        List<EvidenceView> items = new ArrayList<>();
        List<String> contradictions = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Set<String> indicators = new LinkedHashSet<>();
        String intake = "INTAKE:" + claimId;
        Instant reported = f.reportedAt();

        add(items, EvidenceCategory.CUSTOMER_CLAIM, CUSTOMER_CLAIM, EvidenceSeverity.INFO,
                "Customer reports: " + f.issueType().description().toLowerCase(Locale.ROOT) + " — \"" + abbreviate(f.customerStatement()) + "\"",
                Provenance.USER_INTAKE, intake, reported);
        add(items, EvidenceCategory.CUSTOMER_CLAIM, MERCHANT_RECOGNITION, EvidenceSeverity.INFO,
                "Customer states the merchant " + (f.merchantRecognised() ? "is" : "is NOT") + " recognised", Provenance.USER_INTAKE, intake, reported);
        add(items, EvidenceCategory.CUSTOMER_CLAIM, CARD_POSSESSION, EvidenceSeverity.INFO,
                "Card possession reported as " + f.cardPossession(), Provenance.USER_INTAKE, intake, reported);
        if (!injectionIndicators.isEmpty()) {
            add(items, EvidenceCategory.SAFETY, INJECTION_FLAG, EvidenceSeverity.MEDIUM,
                    "Instruction-like content in customer statement treated as data (" + String.join(", ", injectionIndicators) + ")",
                    Provenance.SYSTEM, intake, reported);
        }

        EvidenceView deviceTrusted = null;
        EvidenceView locationHigh = null;

        if (r.transaction() != null) {
            var t = r.transaction();
            if (t.ok()) {
                TransactionDetails d = t.data();
                add(items, EvidenceCategory.TRANSACTION, TRANSACTION_RECORD, EvidenceSeverity.INFO, "Transaction " + t.summary(),
                        Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
                if (!d.status().equals(f.transactionStatus().name())) {
                    contradictions.add("Intake status " + f.transactionStatus() + " differs from system status " + d.status());
                }
                if (d.amount() != null && d.amount().compareTo(f.amount()) != 0) {
                    contradictions.add("Intake amount " + f.amount().toPlainString() + " differs from system amount " + d.amount().toPlainString());
                }
                if (d.authUnavailable()) {
                    add(items, EvidenceCategory.AUTHENTICATION, AUTH_UNAVAILABLE, EvidenceSeverity.MEDIUM,
                            "Authentication challenge outcome unavailable in source system", Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
                    indicators.add(PolicyQueryBuilder.AUTH_UNAVAILABLE);
                } else {
                    EvidenceSeverity sev = d.authOutcome().startsWith("SUCCESS") ? EvidenceSeverity.LOW
                            : "FAILED".equals(d.authOutcome()) ? EvidenceSeverity.HIGH : EvidenceSeverity.MEDIUM;
                    add(items, EvidenceCategory.AUTHENTICATION, AUTH_OUTCOME, sev, "Authentication outcome " + d.authOutcome(),
                            Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
                }
                if (d.duplicateOfRef() != null) {
                    add(items, EvidenceCategory.DUPLICATE, DUPLICATE_FINGERPRINT, EvidenceSeverity.HIGH,
                            "Fingerprint " + d.fingerprint() + " matches earlier posting " + d.duplicateOfRef(),
                            Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
                    indicators.add(PolicyQueryBuilder.DUPLICATE_FINGERPRINT);
                }
            } else if (t.status() == ToolStatus.NOT_FOUND) {
                add(items, EvidenceCategory.TRANSACTION, TRANSACTION_NOT_FOUND, EvidenceSeverity.HIGH,
                        "Transaction " + f.transactionRef() + " not found in source system", Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
                missing.add("transaction record for " + f.transactionRef());
                indicators.add(PolicyQueryBuilder.TRANSACTION_MISSING);
            } else {
                unavailable(items, missing, EvidenceCategory.TRANSACTION, "transaction record", t);
                indicators.add(PolicyQueryBuilder.TRANSACTION_MISSING);
            }
        }
        if (r.device() != null) {
            var t = r.device();
            if (t.ok()) {
                DeviceSignals d = t.data();
                if (!d.deviceKnown() || "LOW".equals(d.trustLevel())) {
                    add(items, EvidenceCategory.DEVICE, DEVICE_NEW, EvidenceSeverity.HIGH, "New/untrusted device: " + t.summary(),
                            Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
                    indicators.add(PolicyQueryBuilder.NEW_DEVICE);
                } else {
                    deviceTrusted = add(items, EvidenceCategory.DEVICE, DEVICE_TRUSTED,
                            "HIGH".equals(d.trustLevel()) ? EvidenceSeverity.LOW : EvidenceSeverity.MEDIUM,
                            "Known device: " + t.summary(), Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
                }
            } else {
                unavailable(items, missing, EvidenceCategory.DEVICE, "device signals", t);
            }
        }
        if (r.location() != null) {
            var t = r.location();
            if (t.ok()) {
                LocationComparison l = t.data();
                switch (l.anomalyLevel()) {
                    case "HIGH" -> {
                        locationHigh = add(items, EvidenceCategory.LOCATION, LOCATION_ANOMALY, EvidenceSeverity.HIGH, t.summary(),
                                Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
                        indicators.add(PolicyQueryBuilder.LOCATION_ANOMALY);
                    }
                    case "MEDIUM" -> {
                        add(items, EvidenceCategory.LOCATION, LOCATION_ANOMALY, EvidenceSeverity.MEDIUM, t.summary(),
                                Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
                        indicators.add(PolicyQueryBuilder.LOCATION_ANOMALY);
                    }
                    default -> add(items, EvidenceCategory.LOCATION, LOCATION_CONSISTENT, EvidenceSeverity.LOW, t.summary(),
                            Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
                }
            } else {
                unavailable(items, missing, EvidenceCategory.LOCATION, "location comparison", t);
            }
        }
        if (r.merchant() != null) {
            var t = r.merchant();
            if (t.ok()) {
                MerchantRiskProfile m = t.data();
                EvidenceSeverity sev = m.blocklistMatch() || "HIGH".equals(m.riskBand()) ? EvidenceSeverity.HIGH
                        : "MEDIUM".equals(m.riskBand()) ? EvidenceSeverity.MEDIUM : EvidenceSeverity.LOW;
                add(items, EvidenceCategory.MERCHANT, MERCHANT_RISK, sev, "Merchant " + t.summary(), Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
            } else {
                unavailable(items, missing, EvidenceCategory.MERCHANT, "merchant risk profile", t);
            }
        }
        if (r.priorDisputes() != null) {
            var t = r.priorDisputes();
            if (t.ok()) {
                PriorDisputes p = t.data();
                if (p.similarCount() >= 2) {
                    add(items, EvidenceCategory.HISTORY, REPEAT_SIMILAR_CLAIMS, EvidenceSeverity.MEDIUM, t.summary(),
                            Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
                    indicators.add(PolicyQueryBuilder.REPEAT_CLAIMS);
                } else {
                    add(items, EvidenceCategory.HISTORY, PRIOR_DISPUTES, p.count() > 0 ? EvidenceSeverity.LOW : EvidenceSeverity.INFO,
                            t.summary(), Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
                }
            } else {
                unavailable(items, missing, EvidenceCategory.HISTORY, "prior dispute history", t);
            }
        }
        if (deviceTrusted != null && locationHigh != null) {
            contradictions.add("Trusted device (" + deviceTrusted.evidenceId() + ") conflicts with high location anomaly ("
                    + locationHigh.evidenceId() + ")");
        }
        if (!contradictions.isEmpty()) {
            indicators.add(PolicyQueryBuilder.CONTRADICTION);
        }
        return new AssembledEvidence(items, contradictions, missing, indicators);
    }

    private void unavailable(List<EvidenceView> items, List<String> missing, EvidenceCategory category, String label, ToolResult<?> t) {
        add(items, category, TOOL_UNAVAILABLE, EvidenceSeverity.MEDIUM,
                capitalise(label) + " unavailable (" + t.status() + "): " + t.summary(), Provenance.TOOL_RESULT, t.toolResultId(), t.observedAt());
        missing.add(label + " (tool " + t.status().name().toLowerCase(Locale.ROOT) + ")");
    }

    private EvidenceView add(List<EvidenceView> items, EvidenceCategory category, String type, EvidenceSeverity severity,
                             String summary, Provenance provenance, String sourceRef, Instant observedAt) {
        EvidenceView v = new EvidenceView(ids.evidenceId(), category, type, severity,
                summary.length() > 480 ? summary.substring(0, 480) : summary, provenance, sourceRef, observedAt);
        items.add(v);
        return v;
    }

    private static String abbreviate(String s) {
        return s.length() > 160 ? s.substring(0, 157) + "..." : s;
    }

    private static String capitalise(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
