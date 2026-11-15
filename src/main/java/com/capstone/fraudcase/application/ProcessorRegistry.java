package com.capstone.fraudcase.application;

import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.config.CapstoneProperties;
import com.capstone.fraudcase.domain.ProcessorMode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Selects the configured processor; rules mode is always registered and usable. */
@Component
public class ProcessorRegistry {

    private final Map<ProcessorMode, FraudCaseProcessor> processors = new EnumMap<>(ProcessorMode.class);
    private final ProcessorMode configured;
    private final boolean allowOverride;

    public ProcessorRegistry(List<FraudCaseProcessor> all, CapstoneProperties props) {
        all.forEach(p -> processors.put(p.mode(), p));
        if (!processors.containsKey(ProcessorMode.RULES)) {
            throw new IllegalStateException("Rules processor must always be available");
        }
        this.configured = ProcessorMode.fromKey(props.getProcessorMode());
        this.allowOverride = props.isAllowModeOverride();
    }

    public ProcessorMode configuredMode() {
        return configured;
    }

    public ProcessorMode resolveMode(String override) {
        if (override == null || override.isBlank()) {
            return configured;
        }
        if (!allowOverride) {
            throw DomainValidationException.of("mode", "Per-investigation mode override is disabled");
        }
        try {
            return ProcessorMode.fromKey(override);
        } catch (IllegalArgumentException e) {
            throw DomainValidationException.of("mode", "Unknown processor mode");
        }
    }

    public FraudCaseProcessor get(ProcessorMode mode) {
        FraudCaseProcessor p = processors.get(mode);
        return p == null ? rules() : p;
    }

    public FraudCaseProcessor rules() {
        return processors.get(ProcessorMode.RULES);
    }

    public Map<ProcessorMode, FraudCaseProcessor> all() {
        return Map.copyOf(processors);
    }
}
