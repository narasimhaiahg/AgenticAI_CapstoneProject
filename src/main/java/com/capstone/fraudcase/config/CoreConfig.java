package com.capstone.fraudcase.config;

import com.capstone.fraudcase.agent.common.PromptRenderer;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.agent.rules.RulesFraudCaseAgent;
import com.capstone.fraudcase.knowledge.EmbeddingModelPort;
import com.capstone.fraudcase.knowledge.InMemoryPolicyVectorStore;
import com.capstone.fraudcase.knowledge.LocalTfIdfEmbeddingModel;
import com.capstone.fraudcase.knowledge.PolicyIngestionService;
import com.capstone.fraudcase.knowledge.PolicyRetriever;
import com.capstone.fraudcase.tools.EvidenceToolkit;
import com.capstone.fraudcase.tools.SyntheticFixtureRepository;
import com.capstone.fraudcase.tools.ToolExecutor;
import com.capstone.fraudcase.tools.ToolTraceSink;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Wires the framework-neutral core: tools, knowledge, validation and the rules processor. */
@Configuration
public class CoreConfig {

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RecommendationValidator recommendationValidator() {
        return new RecommendationValidator();
    }

    @Bean
    PromptRenderer promptRenderer(CapstoneProperties props) {
        return new PromptRenderer(props.guardrailSummary());
    }

    @Bean
    SyntheticFixtureRepository syntheticFixtureRepository(ObjectMapper mapper, CapstoneProperties props) {
        return new SyntheticFixtureRepository(mapper, props.getTools().getDeterministicSeed());
    }

    @Bean(destroyMethod = "close")
    ToolExecutor toolExecutor(ToolTraceSink sink, ObjectMapper mapper, MeterRegistry meters, Clock clock, CapstoneProperties props) {
        return new ToolExecutor(sink, mapper, meters, clock, props.getTools().getTimeoutMs());
    }

    @Bean
    EvidenceToolkit evidenceToolkit(SyntheticFixtureRepository fixtures, ToolExecutor executor, CapstoneProperties props) {
        return new EvidenceToolkit(fixtures, executor, props.getTools().getSimulatedSlowMs());
    }

    @Bean
    @ConditionalOnMissingBean
    EmbeddingModelPort embeddingModel() {
        return new LocalTfIdfEmbeddingModel();
    }

    @Bean
    InMemoryPolicyVectorStore policyVectorStore() {
        return new InMemoryPolicyVectorStore();
    }

    @Bean
    PolicyIngestionService policyIngestionService(EmbeddingModelPort embeddings, InMemoryPolicyVectorStore store, Clock clock,
                                                  CapstoneProperties props) {
        PolicyIngestionService service = new PolicyIngestionService(embeddings, store, clock, props.getRetrieval().getMaxChunkChars());
        service.ingest(props.getRetrieval().getPolicyLocation());
        return service;
    }

    @Bean
    PolicyRetriever policyRetriever(EmbeddingModelPort embeddings, InMemoryPolicyVectorStore store, CapstoneProperties props,
                                    PolicyIngestionService ingestion) {
        return new PolicyRetriever(embeddings, store, props.getRetrieval().getTopK(), props.getRetrieval().getSimilarityThreshold());
    }

    @Bean
    RulesFraudCaseAgent rulesFraudCaseAgent(RecommendationValidator validator) {
        return new RulesFraudCaseAgent(validator);
    }
}
