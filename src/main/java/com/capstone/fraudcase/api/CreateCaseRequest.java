package com.capstone.fraudcase.api;

import com.capstone.fraudcase.application.IntakeCommand;
import com.capstone.fraudcase.domain.CardPossession;
import com.capstone.fraudcase.domain.IntakeChannel;
import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.MaskingRules;
import com.capstone.fraudcase.domain.TransactionChannel;
import com.capstone.fraudcase.domain.TransactionStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Intake payload. {@code channel} is the transaction channel (as in the starter-pack sample). */
public record CreateCaseRequest(
        @NotBlank @Pattern(regexp = MaskingRules.MASKED_CUSTOMER_REF, message = "must be a masked reference such as CUST-••••-4821") String maskedCustomerRef,
        @Pattern(regexp = "^[A-Z]{2}(-[A-Z0-9]{1,3})?$", message = "must be an ISO region code such as IN-KA") String homeRegion,
        @NotBlank @Pattern(regexp = MaskingRules.TRANSACTION_REF, message = "must look like TXN-2026-784102") String transactionRef,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be an ISO 4217 code") String currency,
        @NotNull OffsetDateTime occurredAt,
        @NotNull TransactionStatus status,
        @NotNull TransactionChannel channel,
        @NotBlank @Pattern(regexp = MaskingRules.MASKED_MERCHANT, message = "must be masked, e.g. NORTHSTAR DIGITAL ••••73") String maskedMerchant,
        @NotNull IssueType issueType,
        @NotBlank @Size(max = 1000) String customerStatement,
        CardPossession cardPossession,
        Boolean merchantRecognised,
        IntakeChannel intakeChannel,
        @AssertTrue(message = "synthetic data must be confirmed") boolean syntheticDataConfirmed,
        @AssertTrue(message = "human review is mandatory") boolean reviewRequired) {

    public IntakeCommand toCommand() {
        return new IntakeCommand(maskedCustomerRef, homeRegion == null ? "IN-KA" : homeRegion, transactionRef, amount, currency, occurredAt,
                status, channel, maskedMerchant, issueType, customerStatement, cardPossession, merchantRecognised, intakeChannel,
                syntheticDataConfirmed, reviewRequired);
    }
}
