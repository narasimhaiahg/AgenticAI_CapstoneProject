package com.capstone.fraudcase.domain;

public enum IssueType {
    MERCHANT_NOT_RECOGNISED("Customer does not recognise the merchant or purchase"),
    GOODS_NOT_RECEIVED("Goods or services not received"),
    CANCELLED_NOT_REFUNDED("Cancelled order or subscription not refunded"),
    INCORRECT_AMOUNT("Charged amount differs from agreed amount"),
    DUPLICATE_CHARGE("Customer reports being charged more than once"),
    OTHER("Other payment issue");

    private final String description;

    IssueType(String description) { this.description = description; }

    public String description() { return description; }

    public boolean isMerchantDisputeType() {
        return this == GOODS_NOT_RECEIVED || this == CANCELLED_NOT_REFUNDED || this == INCORRECT_AMOUNT;
    }
}
