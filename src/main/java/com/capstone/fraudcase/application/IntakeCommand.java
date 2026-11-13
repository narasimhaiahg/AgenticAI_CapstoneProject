package com.capstone.fraudcase.application;

import com.capstone.fraudcase.domain.CardPossession;
import com.capstone.fraudcase.domain.IntakeChannel;
import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.TransactionChannel;
import com.capstone.fraudcase.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record IntakeCommand(String maskedCustomerRef, String homeRegion, String transactionRef, BigDecimal amount,
                            String currency, OffsetDateTime occurredAt, TransactionStatus status,
                            TransactionChannel transactionChannel, String maskedMerchant, IssueType issueType,
                            String customerStatement, CardPossession cardPossession, Boolean merchantRecognised,
                            IntakeChannel intakeChannel, boolean syntheticDataConfirmed, boolean reviewRequired) {
}
