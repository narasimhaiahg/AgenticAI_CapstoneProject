package com.capstone.fraudcase.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Deterministic synthetic evidence. Named scenarios come from {@code fixtures/scenarios.json}; any other reference
 * derives stable values from a SHA-256 hash of (seed, reference) so identical input always yields identical results.
 */
public class SyntheticFixtureRepository {

    public static final String FIXTURE_RESOURCE = "fixtures/scenarios.json";

    private final JsonNode scenarios;
    private final String seed;

    public SyntheticFixtureRepository(ObjectMapper mapper, String seed) {
        this.seed = seed;
        try (var in = new ClassPathResource(FIXTURE_RESOURCE).getInputStream()) {
            this.scenarios = mapper.readTree(new String(in.readAllBytes(), StandardCharsets.UTF_8)).path("transactions");
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load synthetic fixtures", e);
        }
    }

    /** Fixture node for a section ("transaction", "device", ...) of a named scenario, if present. */
    public Optional<JsonNode> scenario(String transactionRef, String section) {
        JsonNode node = scenarios.path(transactionRef).path(section);
        return node.isMissingNode() || node.isNull() ? Optional.empty() : Optional.of(node);
    }

    public boolean isNamedScenario(String transactionRef) {
        return scenarios.has(transactionRef);
    }

    /** Stable pseudo-random integer in [0, bound) for the given reference and purpose. */
    public int stableInt(String reference, String purpose, int bound) {
        byte[] h = hash(seed + "|" + purpose + "|" + reference);
        int v = ((h[0] & 0xff) << 24) | ((h[1] & 0xff) << 16) | ((h[2] & 0xff) << 8) | (h[3] & 0xff);
        return Math.floorMod(v, bound);
    }

    public String stableHex(String reference, String purpose, int length) {
        byte[] h = hash(seed + "|" + purpose + "|" + reference);
        StringBuilder sb = new StringBuilder();
        for (byte b : h) {
            sb.append(String.format("%02x", b));
        }
        return sb.substring(0, Math.min(length, sb.length()));
    }

    private static byte[] hash(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
