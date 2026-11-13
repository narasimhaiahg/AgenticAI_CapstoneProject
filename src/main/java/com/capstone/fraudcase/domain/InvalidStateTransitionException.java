package com.capstone.fraudcase.domain;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(CaseStatus from, CaseStatus to) {
        super("Transition " + from + " -> " + to + " is not allowed");
    }
}
