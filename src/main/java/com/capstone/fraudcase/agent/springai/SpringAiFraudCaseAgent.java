package com.capstone.fraudcase.agent.springai;

import com.capstone.fraudcase.agent.common.FollowUpAnswer;
import com.capstone.fraudcase.agent.common.FollowUpContext;
import com.capstone.fraudcase.agent.common.FollowUpGuard;
import com.capstone.fraudcase.agent.common.FraudCaseContext;
import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.agent.common.FraudCaseRecommendation;
import com.capstone.fraudcase.agent.common.ModelToolSession;
import com.capstone.fraudcase.agent.common.ProcessorUnavailableException;
import com.capstone.fraudcase.agent.common.PromptRenderer;
import com.capstone.fraudcase.agent.common.RawRecommendation;
import com.capstone.fraudcase.agent.common.RecommendationValidationException;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.domain.ProcessorMode;
import com.capstone.fraudcase.tools.EvidenceToolkit;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.template.NoOpTemplateRenderer;

/**
 * Spring AI adapter: ChatClient with the shared prompt contract, case-scoped read-only tools, typed entity mapping
 * into {@link RawRecommendation}, and application-side validation. Follow-ups use case-keyed MessageWindowChatMemory.
 */
public class SpringAiFraudCaseAgent implements FraudCaseProcessor {

    private final ChatClient chatClient;
    private final PromptRenderer prompts;
    private final RecommendationValidator validator;
    private final EvidenceToolkit toolkit;
    private final ChatMemory followUpMemory;
    private final String detail;

    public SpringAiFraudCaseAgent(ChatModel chatModel, PromptRenderer prompts, RecommendationValidator validator,
                                  EvidenceToolkit toolkit, int maxMessages, String detail) {
        this.chatClient = chatModel == null ? null : ChatClient.builder(chatModel).build();
        this.prompts = prompts;
        this.validator = validator;
        this.toolkit = toolkit;
        this.followUpMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository()).maxMessages(maxMessages).build();
        this.detail = detail;
    }

    @Override
    public ProcessorMode mode() {
        return ProcessorMode.SPRING_AI;
    }

    @Override
    public boolean isAvailable() {
        return chatClient != null;
    }

    @Override
    public String availabilityDetail() {
        return isAvailable() ? "Spring AI ChatClient: " + detail : "Spring AI model not configured (set CAPSTONE_AI_API_KEY)";
    }

    @Override
    public FraudCaseRecommendation analyze(FraudCaseContext context) {
        requireAvailable();
        ModelToolSession tools = new ModelToolSession(context, toolkit);
        RawRecommendation raw;
        try {
            raw = chatClient.prompt()
                    .templateRenderer(new NoOpTemplateRenderer())
                    .system(prompts.systemPrompt())
                    .user(prompts.renderUserPrompt(context))
                    .tools(new SpringAiEvidenceTools(tools))
                    .call()
                    .entity(RawRecommendation.class);
        } catch (RuntimeException e) {
            throw unavailableOrInvalid(e);
        }
        return validator.validate(raw, tools.extend(context), ProcessorMode.SPRING_AI);
    }

    @Override
    public FollowUpAnswer answerFollowUp(FollowUpContext context) {
        requireAvailable();
        String answer;
        try {
            answer = chatClient.prompt()
                    .templateRenderer(new NoOpTemplateRenderer())
                    .system(prompts.followUpSystemPrompt())
                    .user(prompts.renderFollowUpPrompt(context))
                    .advisors(MessageChatMemoryAdvisor.builder(followUpMemory).conversationId(context.caseId()).build())
                    .call()
                    .content();
        } catch (RuntimeException e) {
            throw new ProcessorUnavailableException("Spring AI model call failed", e);
        }
        String safe = FollowUpGuard.safeAnswer(answer);
        return new FollowUpAnswer(safe, FollowUpGuard.evidenceRefs(safe, context.caseContext()),
                FollowUpGuard.chunkRefs(safe, context.caseContext()), ProcessorMode.SPRING_AI);
    }

    /** Exposed for memory isolation tests. */
    public ChatMemory followUpMemory() {
        return followUpMemory;
    }

    private void requireAvailable() {
        if (!isAvailable()) {
            throw new ProcessorUnavailableException("Spring AI model is not configured");
        }
    }

    private static RuntimeException unavailableOrInvalid(RuntimeException e) {
        if (e instanceof RecommendationValidationException) {
            return e;
        }
        String type = e.getClass().getSimpleName();
        if (type.contains("Json") || type.contains("Conversion") || type.contains("Parse") || e instanceof IllegalArgumentException) {
            return new RecommendationValidationException(java.util.List.of("model output could not be parsed into the recommendation schema"));
        }
        return new ProcessorUnavailableException("Spring AI model call failed", e);
    }
}
