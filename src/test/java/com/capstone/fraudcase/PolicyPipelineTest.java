package com.capstone.fraudcase;

import com.capstone.fraudcase.knowledge.*;
import com.capstone.fraudcase.agent.common.PolicyRetrievalResult;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolicyPipelineTest {
    private static final String DOC = """
            ---
            documentId: POL-T-001
            title: Test policy
            version: "1.0"
            effectiveDate: 2026-01-01
            declaredStatus: ACTIVE
            owner: risk
            classification: SYNTHETIC_TRAINING
            chunkPrefix: tst
            appliesTo: [UNAUTHORISED_TRANSACTION]
            ---
            # Test
            ## 1.1 Unauthorised handling
            Unauthorised card-not-present transactions route to fraud investigation for review.
            """;

    private static PolicyIngestionService service() {
        return new PolicyIngestionService(new LocalTfIdfEmbeddingModel(), new InMemoryPolicyVectorStore(),
                Clock.fixed(Instant.parse("2026-11-13T00:00:00Z"), ZoneOffset.UTC), 4000);
    }

    @Test
    void validDocumentParses() {
        var doc = new PolicyDocumentParser().parse("p.md", DOC);
        assertEquals("POL-T-001", doc.documentId());
        assertEquals(1, doc.sections().size());
    }

    @Test
    void missingFrontMatterRejected() {
        assertThrows(PolicyDocumentParser.PolicyValidationException.class,
                () -> new PolicyDocumentParser().parse("bad.md", "# nothing here"));
    }

    @Test
    void ingestionAndRetrievalWork() {
        var store = new InMemoryPolicyVectorStore();
        var embeddings = new LocalTfIdfEmbeddingModel();
        var svc = new PolicyIngestionService(embeddings, store,
                Clock.fixed(Instant.parse("2026-11-13T00:00:00Z"), ZoneOffset.UTC), 4000);
        var report = svc.ingestDocuments(List.of(new PolicyDocumentParser().parse("p.md", DOC)), List.of());
        assertEquals(1, report.searchableChunks());
        var retriever = new PolicyRetriever(embeddings, store, 4, 0.10);
        PolicyRetrievalResult r = retriever.retrieve("unauthorised card not present dispute");
        assertFalse(r.abstained());
        assertEquals("POL-T-001", r.passages().get(0).documentId());
    }

    @Test
    void irrelevantQueryAbstains() {
        var store = new InMemoryPolicyVectorStore();
        var embeddings = new LocalTfIdfEmbeddingModel();
        var svc = new PolicyIngestionService(embeddings, store,
                Clock.fixed(Instant.parse("2026-11-13T00:00:00Z"), ZoneOffset.UTC), 4000);
        svc.ingestDocuments(List.of(new PolicyDocumentParser().parse("p.md", DOC)), List.of());
        var retriever = new PolicyRetriever(embeddings, store, 4, 0.18);
        assertTrue(retriever.retrieve("weather forecast for tomorrow").abstained());
    }
}
