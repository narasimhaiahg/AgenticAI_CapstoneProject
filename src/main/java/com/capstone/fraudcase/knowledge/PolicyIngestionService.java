package com.capstone.fraudcase.knowledge;

import com.capstone.fraudcase.agent.common.PromptInjectionDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Validates policy metadata, chunks by numbered headings (splitting long sections on paragraph boundaries),
 * resolves version availability, quarantines chunks containing instruction-like content, embeds and indexes.
 */
public class PolicyIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PolicyIngestionService.class);

    private final PolicyDocumentParser parser = new PolicyDocumentParser();
    private final EmbeddingModelPort embeddings;
    private final InMemoryPolicyVectorStore store;
    private final Clock clock;
    private final int maxChunkChars;
    private volatile List<PolicyChunk> chunks = List.of();
    private volatile List<PolicyDocument> documents = List.of();
    private volatile List<String> rejectedSources = List.of();

    public PolicyIngestionService(EmbeddingModelPort embeddings, InMemoryPolicyVectorStore store, Clock clock, int maxChunkChars) {
        this.embeddings = embeddings;
        this.store = store;
        this.clock = clock;
        this.maxChunkChars = maxChunkChars;
    }

    public synchronized IngestionReport ingest(String locationPattern) {
        List<PolicyDocument> docs = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(locationPattern);
            for (Resource r : resources) {
                try (var in = r.getInputStream()) {
                    docs.add(parser.parse(r.getFilename(), new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                } catch (PolicyDocumentParser.PolicyValidationException e) {
                    rejected.add(e.getMessage());
                    log.warn("Policy rejected: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read policies from " + locationPattern, e);
        }
        return ingestDocuments(docs, rejected);
    }

    public synchronized IngestionReport ingestDocuments(List<PolicyDocument> docs, List<String> rejected) {
        LocalDate today = LocalDate.now(clock);
        Map<String, Optional<PolicyDocument>> currentByDoc = docs.stream()
                .filter(d -> "ACTIVE".equals(d.declaredStatus()) && !d.effectiveDate().isAfter(today))
                .collect(Collectors.groupingBy(PolicyDocument::documentId,
                        Collectors.maxBy(Comparator.comparing(PolicyDocument::effectiveDate))));
        List<PolicyChunk> built = new ArrayList<>();
        for (PolicyDocument d : docs) {
            String availability;
            if ("WITHDRAWN".equals(d.declaredStatus())) {
                availability = "WITHDRAWN";
            } else if (d.effectiveDate().isAfter(today)) {
                availability = "NOT_YET_EFFECTIVE";
            } else if (currentByDoc.getOrDefault(d.documentId(), Optional.empty()).map(c -> c == d).orElse(false)) {
                availability = "ACTIVE";
            } else {
                availability = "SUPERSEDED";
            }
            for (PolicyDocument.PolicySection s : d.sections()) {
                List<String> parts = split(s.body());
                for (int i = 0; i < parts.size(); i++) {
                    String chunkId = d.chunkPrefix() + "-" + s.section().replace(".", "")
                            + (parts.size() > 1 ? "-" + (i + 1) : "") + (availability.equals("ACTIVE") ? "" : "-v" + d.version());
                    String text = parts.get(i);
                    List<String> injection = PromptInjectionDetector.detectInstructionOverride(text);
                    String chunkAvailability = availability.equals("ACTIVE") && !injection.isEmpty() ? "QUARANTINED" : availability;
                    built.add(new PolicyChunk(chunkId, d.documentId(), d.title(), d.version(), d.effectiveDate(), s.section(),
                            s.title(), i, text, d.appliesTo(), chunkAvailability, injection));
                }
            }
        }
        List<String> corpus = built.stream().filter(PolicyChunk::searchable).map(PolicyIngestionService::indexText).toList();
        embeddings.fit(corpus);
        store.replaceAll(built.stream().map(c -> new InMemoryPolicyVectorStore.Entry(c, embeddings.embed(indexText(c)))).toList());
        this.chunks = List.copyOf(built);
        this.documents = List.copyOf(docs);
        this.rejectedSources = List.copyOf(rejected);
        IngestionReport report = new IngestionReport(docs.size(), built.size(),
                (int) built.stream().filter(PolicyChunk::searchable).count(), rejected);
        log.info("Policy ingestion: {} documents, {} chunks ({} searchable), {} rejected, embedding={}",
                report.documents(), report.chunks(), report.searchableChunks(), rejected.size(), embeddings.name());
        return report;
    }

    static String indexText(PolicyChunk c) {
        return c.title() + ". Section " + c.section() + " " + c.sectionTitle() + ". " + c.text();
    }

    private List<String> split(String body) {
        if (body.length() <= maxChunkChars) {
            return List.of(body);
        }
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String para : body.split("\n\\s*\n")) {
            if (current.length() > 0 && current.length() + para.length() > maxChunkChars) {
                parts.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(para).append("\n\n");
        }
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }
        return parts;
    }

    public List<PolicyChunk> chunks() {
        return chunks;
    }

    public List<PolicyDocument> documents() {
        return documents;
    }

    public List<String> rejectedSources() {
        return rejectedSources;
    }

    public Optional<PolicyChunk> chunk(String chunkId) {
        return chunks.stream().filter(c -> c.chunkId().equals(chunkId)).findFirst();
    }

    public String embeddingModelName() {
        return embeddings.name();
    }

    public record IngestionReport(int documents, int chunks, int searchableChunks, List<String> rejected) {
    }
}
