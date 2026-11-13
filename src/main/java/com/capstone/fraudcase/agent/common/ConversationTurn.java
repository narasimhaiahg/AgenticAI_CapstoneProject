package com.capstone.fraudcase.agent.common;

import java.time.Instant;

public record ConversationTurn(String role, String text, Instant at) {
    public static final String USER = "USER";
    public static final String ASSISTANT = "ASSISTANT";
}
