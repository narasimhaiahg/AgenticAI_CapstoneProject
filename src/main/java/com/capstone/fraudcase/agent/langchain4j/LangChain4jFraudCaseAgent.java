package com.capstone.fraudcase.agent.langchain4j;

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
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LangChain4j adapter: AI Services with the shared prompt contract, @Tool evidence tools bound to the case, typed
 * {@link RawRecommendation} return and application-side validation. Follow-ups use @MemoryId(caseId) window memory.
 */
public class LangChain4jFraudCaseAgent implements FraudCaseProcessor {

    private final ChatModel chatModel;
    private final PromptRenderer prompts;
    private final RecommendationValidator validator;
    private final EvidenceToolkit toolkit;
    private final Map<Object, ChatMemory> memories = new ConcurrentHashMap<>();
    private final FraudFollowUpService followUpService;
    private final String detail;

    public LangChain4jFraudCaseAgent(ChatModel chatModel, PromptRenderer prompts, RecommendationValidator validator,
                                     EvidenceToolkit toolkit, int maxMessages, String detail) {
        this.chatModel = chatModel;
        this.prompts = prompts;
        this.validator = validator;
        this.toolkit = toolkit;
        this.detail = detail;
        this.followUpService = chatModel == null ? null : AiServices.builder(FraudFollowUpService.class)
                .chatModel(chatModel)
                .systemMessageProvider(memoryId -> prompts.followUpSystemPrompt())
                .chatMemoryProvider(memoryId -> memories.computeIfAbsent(memoryId,
                        id -> MessageWindowChatMemory.builder().id(id).maxMessages(maxMessages).build()))
                .build();
    }

    @Override
    public ProcessorMode mode() {
        return ProcessorMode.LANGCHAIN4J;
    }

    @Override
    public boolean isAvailable() {
        return chatModel != null;
    }

    @Override
    public String availabilityDetail() {
        return isAvailable() ? "LangChain4j AI Service: " + detail : "LangChain4j model not configured (set CAPSTONE_AI_API_KEY)";
    }

    @Override
    public FraudCaseRecommendation analyze(FraudCaseContext context) {
        requireAvailable();
        ModelToolSession tools = new ModelToolSession(context, toolkit);
        FraudInvestigationService service = AiServices.builder(FraudInvestigationService.class)
                .chatModel(chatModel)
                .systemMessageProvider(memoryId -> prompts.systemPrompt())
                .tools(new LangChain4jEvidenceTools(tools))
                .maxSequentialToolsInvocations(ModelToolSession.MAX_MODEL_TOOL_CALLS)
                .build();
        RawRecommendation raw;
        try {
            raw = service.analyze(prompts.renderUserPrompt(context));
        } catch (RuntimeException e) {
            throw unavailableOrInvalid(e);
        }
        return validator.validate(raw, tools.extend(context), ProcessorMode.LANGCHAIN4J);
    }

    @Override
    public FollowUpAnswer answerFollowUp(FollowUpContext context) {
        requireAvailable();
        String answer;
        try {
            answer = followUpService.answer(context.caseId(), prompts.renderFollowUpPrompt(context));
        } catch (RuntimeException e) {
            throw new ProcessorUnavailableException("LangChain4j model call failed", e);
        }
        String safe = FollowUpGuard.safeAnswer(answer);
        return new FollowUpAnswer(safe, FollowUpGuard.evidenceRefs(safe, context.caseContext()),
                FollowUpGuard.chunkRefs(safe, context.caseContext()), ProcessorMode.LANGCHAIN4J);
    }

    /** Exposed for memory isolation tests. */
    public List<dev.langchain4j.data.message.ChatMessage> memoryOf(String caseId) {
        ChatMemory m = memories.get(caseId);
        return m == null ? List.of() : m.messages();
    }

    private void requireAvailable() {
        if (!isAvailable()) {
            throw new ProcessorUnavailableException("LangChain4j model is not configured");
        }
    }

    private static RuntimeException unavailableOrInvalid(RuntimeException e) {
        if (e instanceof RecommendationValidationException) {
            return e;
        }
        String type = e.getClass().getName();
        if (type.contains("Json") || type.contains("OutputParsing") || type.contains("Parse") || e instanceof IllegalArgumentException) {
            return new RecommendationValidationException(List.of("model output could not be parsed into the recommendation schema"));
        }
        return new ProcessorUnavailableException("LangChain4j model call failed", e);
    }
}
