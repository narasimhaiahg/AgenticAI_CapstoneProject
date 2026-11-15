package com.capstone.fraudcase.agent.common;

import java.util.List;
import java.util.Optional;

/** Outcome of a thresholded policy search. {@code abstained} means nothing met the threshold. */
public record PolicyRetrievalResult(String query, int topK, double threshold, List<PolicyPassage> passages, double bestScore) {

    public PolicyRetrievalResult {
        passages = List.copyOf(passages);
    }

    public boolean abstained() {
        return passages.isEmpty();
    }

    public Optional<PolicyPassage> findChunk(String documentId, String version, String chunkId) {
        return passages.stream()
                .filter(p -> p.documentId().equals(documentId) && p.chunkId().equals(chunkId)
                        && (version == null || p.version().equals(version)))
                .findFirst();
    }

    public static PolicyRetrievalResult empty(String query, int topK, double threshold) {
        return new PolicyRetrievalResult(query, topK, threshold, List.of(), 0.0);
    }
}
