package com.capstone.fraudcase.web;

import com.capstone.fraudcase.domain.CardPossession;
import com.capstone.fraudcase.domain.IntakeChannel;
import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.TransactionChannel;
import com.capstone.fraudcase.domain.TransactionStatus;

/** Mutable form-backing bean for the intake screen; values are validated by FraudCaseService. */
public class IntakeForm {
    private String maskedCustomerRef;
    private String homeRegion = "IN-KA";
    private String transactionRef;
    private String amount;
    private String currency = "INR";
    private String occurredAt;
    private TransactionStatus status = TransactionStatus.POSTED;
    private TransactionChannel transactionChannel = TransactionChannel.CARD_NOT_PRESENT;
    private String maskedMerchant;
    private IssueType issueType = IssueType.MERCHANT_NOT_RECOGNISED;
    private String customerStatement;
    private CardPossession cardPossession = CardPossession.IN_POSSESSION;
    private boolean merchantRecognised;
    private IntakeChannel intakeChannel = IntakeChannel.PHONE;
    private boolean syntheticDataConfirmed;
    private boolean reviewRequired = true;

    public String getMaskedCustomerRef() { return maskedCustomerRef; }
    public void setMaskedCustomerRef(String v) { this.maskedCustomerRef = v; }
    public String getHomeRegion() { return homeRegion; }
    public void setHomeRegion(String v) { this.homeRegion = v; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String v) { this.transactionRef = v; }
    public String getAmount() { return amount; }
    public void setAmount(String v) { this.amount = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String v) { this.occurredAt = v; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus v) { this.status = v; }
    public TransactionChannel getTransactionChannel() { return transactionChannel; }
    public void setTransactionChannel(TransactionChannel v) { this.transactionChannel = v; }
    public String getMaskedMerchant() { return maskedMerchant; }
    public void setMaskedMerchant(String v) { this.maskedMerchant = v; }
    public IssueType getIssueType() { return issueType; }
    public void setIssueType(IssueType v) { this.issueType = v; }
    public String getCustomerStatement() { return customerStatement; }
    public void setCustomerStatement(String v) { this.customerStatement = v; }
    public CardPossession getCardPossession() { return cardPossession; }
    public void setCardPossession(CardPossession v) { this.cardPossession = v; }
    public boolean isMerchantRecognised() { return merchantRecognised; }
    public void setMerchantRecognised(boolean v) { this.merchantRecognised = v; }
    public IntakeChannel getIntakeChannel() { return intakeChannel; }
    public void setIntakeChannel(IntakeChannel v) { this.intakeChannel = v; }
    public boolean isSyntheticDataConfirmed() { return syntheticDataConfirmed; }
    public void setSyntheticDataConfirmed(boolean v) { this.syntheticDataConfirmed = v; }
    public boolean isReviewRequired() { return reviewRequired; }
    public void setReviewRequired(boolean v) { this.reviewRequired = v; }
}
