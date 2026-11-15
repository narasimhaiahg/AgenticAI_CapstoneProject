package com.capstone.fraudcase.application;

import com.capstone.fraudcase.agent.common.ConversationTurn;
import com.capstone.fraudcase.config.CapstoneProperties;
import com.capstone.fraudcase.persistence.CaseMessageEntity;
import com.capstone.fraudcase.persistence.CaseMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Case-scoped, bounded conversation memory persisted in the database. Every read and write is keyed by caseId, so
 * one case can never see another case's turns. Digit runs that could be account numbers are masked before storage.
 */
@Service
public class CaseMemoryService {

    private final CaseMessageRepository repository;
    private final int maxMessages;
    private final Clock clock;

    public CaseMemoryService(CaseMessageRepository repository, CapstoneProperties props, Clock clock) {
        this.repository = repository;
        this.maxMessages = props.getMemory().getMaxMessages();
        this.clock = clock;
    }

    @Transactional
    public void append(String caseId, String role, String text, String mode) {
        CaseMessageEntity e = new CaseMessageEntity();
        e.setCaseId(caseId);
        e.setRole(role);
        e.setText(redact(text));
        e.setMode(mode);
        e.setCreatedAt(OffsetDateTime.now(clock));
        repository.save(e);
        List<CaseMessageEntity> all = repository.findByCaseIdOrderByIdAsc(caseId);
        if (all.size() > maxMessages) {
            repository.deleteAll(all.subList(0, all.size() - maxMessages));
        }
    }

    @Transactional(readOnly = true)
    public List<ConversationTurn> history(String caseId) {
        return repository.findByCaseIdOrderByIdAsc(caseId).stream()
                .map(m -> new ConversationTurn(m.getRole(), m.getText(), m.getCreatedAt().toInstant())).toList();
    }

    @Transactional(readOnly = true)
    public List<CaseMessageEntity> messages(String caseId) {
        return repository.findByCaseIdOrderByIdAsc(caseId);
    }

    static String redact(String text) {
        if (text == null) {
            return "";
        }
        String masked = text.replaceAll("(?<!\\d)(?:\\d[ -]?){12,19}(?!\\d)", "[REDACTED-NUMBER]");
        return masked.length() > 3900 ? masked.substring(0, 3900) : masked;
    }
}
