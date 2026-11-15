package com.capstone.fraudcase.domain;

/** Independent evidence source family; used to count independently sourced anomalies. */
public enum EvidenceCategory { CUSTOMER_CLAIM, TRANSACTION, AUTHENTICATION, DEVICE, LOCATION, MERCHANT, HISTORY, DUPLICATE, SAFETY }
