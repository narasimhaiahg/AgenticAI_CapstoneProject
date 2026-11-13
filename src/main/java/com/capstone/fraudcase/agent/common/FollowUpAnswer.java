package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.domain.ProcessorMode;

import java.util.List;

public record FollowUpAnswer(String answer, List<String> referencedEvidenceIds, List<String> referencedChunkIds, ProcessorMode mode) {
    public FollowUpAnswer {
        referencedEvidenceIds = List.copyOf(referencedEvidenceIds);
        referencedChunkIds = List.copyOf(referencedChunkIds);
    }
}
