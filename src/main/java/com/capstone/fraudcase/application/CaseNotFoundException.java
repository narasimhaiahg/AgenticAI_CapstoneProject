package com.capstone.fraudcase.application;

public class CaseNotFoundException extends RuntimeException {
    public CaseNotFoundException(String caseId) {
        super("Case not found: " + caseId);
    }
}
