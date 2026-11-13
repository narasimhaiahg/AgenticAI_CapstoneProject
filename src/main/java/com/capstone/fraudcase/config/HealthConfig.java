package com.capstone.fraudcase.config;

import com.capstone.fraudcase.application.ProcessorRegistry;
import com.capstone.fraudcase.knowledge.PolicyIngestionService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Health for the vector store and configured processor, without revealing credentials or calling the model. */
@Configuration
public class HealthConfig {

    @Bean
    HealthIndicator policyStore(PolicyIngestionService ingestion) {
        return () -> {
            long searchable = ingestion.chunks().stream().filter(c -> c.searchable()).count();
            Health.Builder b = searchable > 0 ? Health.up() : Health.down();
            return b.withDetail("documents", ingestion.documents().size()).withDetail("searchableChunks", searchable)
                    .withDetail("embeddingModel", ingestion.embeddingModelName()).withDetail("rejectedDocuments", ingestion.rejectedSources().size())
                    .build();
        };
    }

    @Bean
    HealthIndicator processor(ProcessorRegistry registry) {
        return () -> {
            var configured = registry.get(registry.configuredMode());
            Health.Builder b = configured.isAvailable() ? Health.up() : Health.status("DEGRADED");
            return b.withDetail("configuredMode", registry.configuredMode().key())
                    .withDetail("available", configured.isAvailable())
                    .withDetail("fallback", "rules")
                    .build();
        };
    }
}
