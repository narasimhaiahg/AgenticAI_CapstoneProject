package com.capstone.fraudcase.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "capstone")
public class CapstoneProperties {

    /** rules | spring-ai | langchain4j */
    private String processorMode = "rules";
    private boolean humanReviewRequired = true;
    private boolean allowFinancialActions = false;
    private boolean allowCustomerContact = false;
    /** Allows a per-investigation processor override (e.g. run rules mode in the demo without restart). */
    private boolean allowModeOverride = true;
    private final Retrieval retrieval = new Retrieval();
    private final Memory memory = new Memory();
    private final Tools tools = new Tools();
    private final Ai ai = new Ai();
    private final Security security = new Security();

    public String getProcessorMode() { return processorMode; }
    public void setProcessorMode(String processorMode) { this.processorMode = processorMode; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public void setHumanReviewRequired(boolean v) { this.humanReviewRequired = v; }
    public boolean isAllowFinancialActions() { return allowFinancialActions; }
    public void setAllowFinancialActions(boolean v) { this.allowFinancialActions = v; }
    public boolean isAllowCustomerContact() { return allowCustomerContact; }
    public void setAllowCustomerContact(boolean v) { this.allowCustomerContact = v; }
    public boolean isAllowModeOverride() { return allowModeOverride; }
    public void setAllowModeOverride(boolean v) { this.allowModeOverride = v; }
    public Retrieval getRetrieval() { return retrieval; }
    public Memory getMemory() { return memory; }
    public Tools getTools() { return tools; }
    public Ai getAi() { return ai; }
    public Security getSecurity() { return security; }

    public static class Retrieval {
        private int topK = 4;
        private double similarityThreshold = 0.18;
        private String policyLocation = "classpath*:policies/*.md";
        private int maxChunkChars = 1200;

        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double v) { this.similarityThreshold = v; }
        public String getPolicyLocation() { return policyLocation; }
        public void setPolicyLocation(String v) { this.policyLocation = v; }
        public int getMaxChunkChars() { return maxChunkChars; }
        public void setMaxChunkChars(int v) { this.maxChunkChars = v; }
    }

    public static class Memory {
        private int maxMessages = 12;

        public int getMaxMessages() { return maxMessages; }
        public void setMaxMessages(int maxMessages) { this.maxMessages = maxMessages; }
    }

    public static class Tools {
        private long timeoutMs = 2500;
        private String deterministicSeed = "fraud-training-v1";
        /** How long a fixture marked SLOW sleeps; must exceed timeoutMs to demonstrate timeouts. */
        private long simulatedSlowMs = 4000;

        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
        public String getDeterministicSeed() { return deterministicSeed; }
        public void setDeterministicSeed(String v) { this.deterministicSeed = v; }
        public long getSimulatedSlowMs() { return simulatedSlowMs; }
        public void setSimulatedSlowMs(long v) { this.simulatedSlowMs = v; }
    }

    public static class Ai {
        /** OpenAI-compatible endpoint (OpenAI, Azure OpenAI proxy, Ollama /v1, LM Studio ...). */
        private String baseUrl = "https://api.openai.com";
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private double temperature = 0.0;
        private long timeoutMs = 20000;
        /** On model failure or invalid output, fall back to rules mode (otherwise a safe manual-review result). */
        private boolean fallbackToRules = true;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
        public boolean isFallbackToRules() { return fallbackToRules; }
        public void setFallbackToRules(boolean v) { this.fallbackToRules = v; }

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank() && baseUrl != null && !baseUrl.isBlank();
        }
    }

    public static class Security {
        /** Password for the synthetic training users. If blank a random one is generated and logged at startup. */
        private String demoPassword = "";
        private List<DemoUser> users = new ArrayList<>();

        public String getDemoPassword() { return demoPassword; }
        public void setDemoPassword(String v) { this.demoPassword = v; }
        public List<DemoUser> getUsers() { return users; }
        public void setUsers(List<DemoUser> users) { this.users = users; }
    }

    public static class DemoUser {
        private String username;
        private List<String> roles = new ArrayList<>();

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }

    public String guardrailSummary() {
        return "- human-review-required: " + humanReviewRequired
                + "\n- allow-financial-actions: " + allowFinancialActions + " (no card block, refund, credit or fund movement)"
                + "\n- allow-customer-contact: " + allowCustomerContact
                + "\n- tools are read-only, allow-listed and scoped to this case";
    }
}
