package com.capstone.fraudcase.agent.common;

public record PolicyCitationRef(String documentId, String version, String section, String chunkId, Double score) {
}
