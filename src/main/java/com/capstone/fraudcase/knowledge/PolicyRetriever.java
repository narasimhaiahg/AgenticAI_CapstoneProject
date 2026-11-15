package com.capstone.fraudcase.knowledge;

import com.capstone.fraudcase.agent.common.PolicyPassage;
import com.capstone.fraudcase.agent.common.PolicyRetrievalResult;

import java.util.List;

/** Thresholded top-K retrieval over searchable (active, effective, non-quarantined) policy chunks. Abstains on no match. */
public class PolicyRetriever {

    private final EmbeddingModelPort embeddings;
    private final InMemoryPolicyVectorStore store;
    private final int topK;
    private final double threshold;

    public PolicyRetriever(EmbeddingModelPort embeddings, InMemoryPolicyVectorStore store, int topK, double threshold) {
        this.embeddings = embeddings;
        this.store = store;
        this.topK = topK;
        this.threshold = threshold;
    }

    public PolicyRetrievalResult retrieve(String query) {
        float[] q = embeddings.embed(query);
        List<PolicyPassage> passages = store.search(q, topK, threshold).stream()
                .map(s -> new PolicyPassage(s.chunk().documentId(), s.chunk().title(), s.chunk().version(),
                        s.chunk().effectiveDate(), s.chunk().section(), s.chunk().sectionTitle(), s.chunk().chunkId(),
                        s.score(), s.chunk().text(), s.chunk().appliesTo()))
                .toList();
        return new PolicyRetrievalResult(query, topK, threshold, passages, store.bestScore(q));
    }

    public int topK() {
        return topK;
    }

    public double threshold() {
        return threshold;
    }
}
