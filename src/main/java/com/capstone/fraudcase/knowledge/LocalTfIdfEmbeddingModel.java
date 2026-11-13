package com.capstone.fraudcase.knowledge;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * Deterministic local embedding: normalised unigram + bigram features, TF-IDF weighted and hashed into a fixed
 * dense vector, L2-normalised. Keeps the RAG pipeline runnable offline and reproducible in tests.
 */
public class LocalTfIdfEmbeddingModel implements EmbeddingModelPort {

    private static final int DIMENSIONS = 4096;
    private static final Set<String> STOP = Set.of("a", "an", "the", "and", "or", "of", "to", "in", "on", "for", "is", "are",
            "be", "by", "with", "as", "at", "it", "this", "that", "from", "was", "were", "has", "have", "not", "no", "any",
            "all", "must", "may", "should", "will", "shall", "can", "if", "when", "where", "which", "who", "than", "then",
            "its", "their", "there", "into", "only", "also", "do", "does", "i", "my", "me", "we", "our", "you", "your", "case");
    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("unauthorized", "unauthorised"), Map.entry("recognize", "recognise"), Map.entry("recognized", "recognise"),
            Map.entry("unrecognised", "recognise"), Map.entry("unrecognized", "recognise"), Map.entry("cnp", "card-not-present"),
            Map.entry("cancelation", "cancellation"), Map.entry("canceled", "cancel"), Map.entry("cancelled", "cancel"),
            Map.entry("duplicated", "duplicate"), Map.entry("twice", "duplicate"), Map.entry("delivered", "delivery"),
            Map.entry("arrived", "delivery"), Map.entry("fulfillment", "fulfilment"), Map.entry("3ds", "authentication"),
            Map.entry("otp", "authentication"), Map.entry("geolocation", "location"));

    private final Map<String, Double> idf = new HashMap<>();
    private double defaultIdf = 1.0;

    @Override
    public String name() {
        return "local-tfidf-hash-" + DIMENSIONS;
    }

    @Override
    public synchronized void fit(List<String> corpus) {
        idf.clear();
        Map<String, Integer> df = new HashMap<>();
        for (String doc : corpus) {
            for (String f : new HashSet<>(features(doc))) {
                df.merge(f, 1, Integer::sum);
            }
        }
        int n = Math.max(1, corpus.size());
        df.forEach((f, c) -> idf.put(f, Math.log(1.0 + (double) n / c)));
        defaultIdf = Math.log(1.0 + n);
    }

    @Override
    public float[] embed(String text) {
        Map<String, Integer> tf = new HashMap<>();
        for (String f : features(text)) {
            tf.merge(f, 1, Integer::sum);
        }
        float[] v = new float[DIMENSIONS];
        double[] outOfVocabularyMass = {0.0};
        tf.forEach((f, count) -> {
            Double termIdf = idf.isEmpty() ? Double.valueOf(defaultIdf) : idf.get(f);
            if (termIdf == null) {
                // Out-of-vocabulary terms cannot match any chunk: they only enlarge the norm (lowering similarity)
                // instead of being hashed into dimensions where they would collide with real terms.
                double w = (1.0 + Math.log(count)) * defaultIdf * (f.contains(" ") ? 0.6 : 1.0);
                outOfVocabularyMass[0] += w * w;
                return;
            }
            double weight = (1.0 + Math.log(count)) * termIdf * (f.contains(" ") ? 0.6 : 1.0);
            long h = crc(f);
            int idx = (int) (h % DIMENSIONS);
            v[idx] += (float) (((h >> 20) & 1) == 0 ? weight : -weight);
        });
        double norm = outOfVocabularyMass[0];
        for (float x : v) {
            norm += x * x;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) {
                v[i] = (float) (v[i] / norm);
            }
        }
        return v;
    }

    static List<String> features(String text) {
        List<String> tokens = new ArrayList<>();
        for (String raw : text.toLowerCase(Locale.ROOT).split("[^a-z0-9-]+")) {
            String t = raw.replaceAll("^-+|-+$", "");
            if (t.length() < 2 || STOP.contains(t) || t.chars().allMatch(Character::isDigit)) {
                continue;
            }
            t = SYNONYMS.getOrDefault(t, t);
            tokens.add(stem(t));
        }
        List<String> features = new ArrayList<>(tokens);
        for (int i = 0; i + 1 < tokens.size(); i++) {
            features.add(tokens.get(i) + " " + tokens.get(i + 1));
        }
        return features;
    }

    private static String stem(String t) {
        if (t.length() > 6 && t.endsWith("ing")) return t.substring(0, t.length() - 3);
        if (t.length() > 5 && t.endsWith("ies")) return t.substring(0, t.length() - 3) + "y";
        if (t.length() > 5 && t.endsWith("ed")) return t.substring(0, t.length() - 2);
        if (t.length() > 4 && t.endsWith("s") && !t.endsWith("ss")) return t.substring(0, t.length() - 1);
        return t;
    }

    private static long crc(String f) {
        CRC32 c = new CRC32();
        c.update(f.getBytes(StandardCharsets.UTF_8));
        return c.getValue();
    }
}
