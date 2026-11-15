package com.capstone.fraudcase.agent.common;

public class ProcessorUnavailableException extends RuntimeException {
    public ProcessorUnavailableException(String message) {
        super(message);
    }

    public ProcessorUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
