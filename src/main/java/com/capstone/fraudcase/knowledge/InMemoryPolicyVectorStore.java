package com.capstone.fraudcase.knowledge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Minimal cosine-similarity vector store over policy chunks. */
public class InMemoryPolicyVectorStore {

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public void replaceAll(List<Entry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
    }

    public List<Scored> search(float[] query, int topK, double threshold) {
        List<Scored> scored = new ArrayList<>();
        for (Entry e : entries) {
            if (!e.chunk().searchable()) {
                continue;
            }
            double s = cosine(query, e.vector());
            if (s >= threshold) {
                scored.add(new Scored(e.chunk(), s));
            }
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        return scored.size() > topK ? scored.subList(0, topK) : scored;
    }

    public double bestScore(float[] query) {
        return entries.stream().filter(e -> e.chunk().searchable())
                .mapToDouble(e -> cosine(query, e.vector())).max().orElse(0.0);
    }

    public int size() {
        return entries.size();
    }

    /** Cosine similarity for vectors already L2-normalised by the embedding model (the norm may include out-of-vocabulary mass). */
    static double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return Math.max(0.0, Math.min(1.0, dot));
    }

    public record Entry(PolicyChunk chunk, float[] vector) {
    }

    public record Scored(PolicyChunk chunk, double score) {
    }
}
