package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.domain.*;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "transaction_claim")
public class TransactionClaimEntity {

    @Id
    private String claimId;
    private String caseId;
    private String transactionRef;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime occurredAt;
    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;
    @Enumerated(EnumType.STRING)
    private TransactionChannel transactionChannel;
    private String maskedMerchant;
    @Enumerated(EnumType.STRING)
    private IssueType issueType;
    private String customerStatement;
    private String statementOriginal;
    @Enumerated(EnumType.STRING)
    private CardPossession cardPossession;
    private boolean merchantRecognised;
    @Convert(converter = StringListConverter.class)
    private List<String> injectionIndicators;

    public String getClaimId() { return claimId; }
    public void setClaimId(String claimId) { this.claimId = claimId; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
    public TransactionStatus getTransactionStatus() { return transactionStatus; }
    public void setTransactionStatus(TransactionStatus transactionStatus) { this.transactionStatus = transactionStatus; }
    public TransactionChannel getTransactionChannel() { return transactionChannel; }
    public void setTransactionChannel(TransactionChannel transactionChannel) { this.transactionChannel = transactionChannel; }
    public String getMaskedMerchant() { return maskedMerchant; }
    public void setMaskedMerchant(String maskedMerchant) { this.maskedMerchant = maskedMerchant; }
    public IssueType getIssueType() { return issueType; }
    public void setIssueType(IssueType issueType) { this.issueType = issueType; }
    public String getCustomerStatement() { return customerStatement; }
    public void setCustomerStatement(String customerStatement) { this.customerStatement = customerStatement; }
    public String getStatementOriginal() { return statementOriginal; }
    public void setStatementOriginal(String statementOriginal) { this.statementOriginal = statementOriginal; }
    public CardPossession getCardPossession() { return cardPossession; }
    public void setCardPossession(CardPossession cardPossession) { this.cardPossession = cardPossession; }
    public boolean isMerchantRecognised() { return merchantRecognised; }
    public void setMerchantRecognised(boolean merchantRecognised) { this.merchantRecognised = merchantRecognised; }
    public List<String> getInjectionIndicators() { return injectionIndicators; }
    public void setInjectionIndicators(List<String> injectionIndicators) { this.injectionIndicators = injectionIndicators; }
}
