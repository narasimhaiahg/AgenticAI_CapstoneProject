package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.domain.CardPossession;
import com.capstone.fraudcase.domain.IntakeChannel;
import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.TransactionChannel;
import com.capstone.fraudcase.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

/** Validated, masked intake facts. The only customer data a processor ever sees. */
public record CaseFacts(
        String caseId,
        Instant reportedAt,
        IntakeChannel intakeChannel,
        String analystId,
        String maskedCustomerRef,
        String homeRegion,
        String transactionRef,
        BigDecimal amount,
        String currency,
        OffsetDateTime occurredAt,
        TransactionStatus transactionStatus,
        TransactionChannel transactionChannel,
        String maskedMerchant,
        IssueType issueType,
        String customerStatement,
        CardPossession cardPossession,
        boolean merchantRecognised) {
}
