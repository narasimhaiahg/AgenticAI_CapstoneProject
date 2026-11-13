package com.capstone.fraudcase.tools;

import java.math.BigDecimal;
import java.time.Instant;

/** Binds tool execution to a single case so a model can never query other customers or transactions. */
public record CaseToolScope(String caseId, int runNumber, String correlationId, String maskedCustomerRef,
                            String transactionRef, String maskedMerchant, Instant reportedAt,
                            BigDecimal amount, String currency, Instant occurredAt, String channel) {
}
