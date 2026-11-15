package com.capstone.fraudcase.config;

import com.capstone.fraudcase.agent.common.PromptRenderer;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.agent.langchain4j.LangChain4jFraudCaseAgent;
import com.capstone.fraudcase.agent.springai.SpringAiFraudCaseAgent;
import com.capstone.fraudcase.tools.EvidenceToolkit;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Builds the optional AI adapters. Models are only constructed when an API key is configured (or a ChatModel bean is
 * supplied, e.g. a test stub); otherwise the adapters report unavailable and the orchestrator falls back safely.
 * Provider, credentials, model, temperature and timeout all come from configuration.
 */
@Configuration
public class AiAdapterConfig {

    @Bean
    SpringAiFraudCaseAgent springAiFraudCaseAgent(ObjectProvider<org.springframework.ai.chat.model.ChatModel> provided,
                                                  CapstoneProperties props, PromptRenderer prompts, RecommendationValidator validator,
                                                  EvidenceToolkit toolkit, ObjectProvider<ObservationRegistry> observations) {
        CapstoneProperties.Ai ai = props.getAi();
        org.springframework.ai.chat.model.ChatModel model = provided.getIfAvailable();
        String detail = model != null ? "injected ChatModel" : ai.getModel() + " @ " + ai.getBaseUrl();
        if (model == null && ai.isConfigured()) {
            Duration timeout = Duration.ofMillis(ai.getTimeoutMs());
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout).build());
            requestFactory.setReadTimeout(timeout);
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(ai.getBaseUrl())
                    .apiKey(ai.getApiKey())
                    .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                    .build();
            model = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder().model(ai.getModel()).temperature(ai.getTemperature()).build())
                    .toolCallingManager(ToolCallingManager.builder().build())
                    .retryTemplate(RetryTemplate.builder().maxAttempts(1).build())
                    .observationRegistry(observations.getIfAvailable(() -> ObservationRegistry.NOOP))
                    .build();
        }
        return new SpringAiFraudCaseAgent(model, prompts, validator, toolkit, props.getMemory().getMaxMessages(), detail);
    }

    @Bean
    LangChain4jFraudCaseAgent langChain4jFraudCaseAgent(ObjectProvider<dev.langchain4j.model.chat.ChatModel> provided,
                                                        CapstoneProperties props, PromptRenderer prompts,
                                                        RecommendationValidator validator, EvidenceToolkit toolkit) {
        CapstoneProperties.Ai ai = props.getAi();
        dev.langchain4j.model.chat.ChatModel model = provided.getIfAvailable();
        String detail = model != null ? "injected ChatModel" : ai.getModel() + " @ " + ai.getBaseUrl();
        if (model == null && ai.isConfigured()) {
            model = dev.langchain4j.model.openai.OpenAiChatModel.builder()
                    .baseUrl(stripTrailingSlash(ai.getBaseUrl()) + "/v1")
                    .apiKey(ai.getApiKey())
                    .modelName(ai.getModel())
                    .temperature(ai.getTemperature())
                    .timeout(Duration.ofMillis(ai.getTimeoutMs()))
                    .maxRetries(0)
                    .build();
        }
        return new LangChain4jFraudCaseAgent(model, prompts, validator, toolkit, props.getMemory().getMaxMessages(), detail);
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
