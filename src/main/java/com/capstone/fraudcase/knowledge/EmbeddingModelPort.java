package com.capstone.fraudcase.knowledge;

import java.util.List;

/** Framework-neutral embedding port. The default implementation is local and deterministic (no network). */
public interface EmbeddingModelPort {

    String name();

    /** Optional corpus fitting step (e.g. IDF statistics) run once at ingestion. */
    default void fit(List<String> corpus) {
    }

    float[] embed(String text);
}
