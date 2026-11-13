package com.capstone.fraudcase.application;

import com.capstone.fraudcase.agent.common.CaseFacts;
import com.capstone.fraudcase.agent.common.EvidenceView;
import com.capstone.fraudcase.agent.common.FraudCaseContext;
import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.agent.common.FraudCaseRecommendation;
import com.capstone.fraudcase.agent.common.PolicyCitationRef;
import com.capstone.fraudcase.agent.common.PolicyPassage;
import com.capstone.fraudcase.agent.common.PolicyRetrievalResult;
import com.capstone.fraudcase.agent.common.ProcessorUnavailableException;
import com.capstone.fraudcase.agent.common.RecommendationValidationException;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.application.EvidenceAssembler.AssembledEvidence;
import com.capstone.fraudcase.application.EvidenceAssembler.CollectedResults;
import com.capstone.fraudcase.application.InvestigationPlanner.PlannedStep;
import com.capstone.fraudcase.config.CapstoneProperties;
import com.capstone.fraudcase.domain.ActorType;
import com.capstone.fraudcase.domain.CaseStatus;
import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.ProcessorMode;
import com.capstone.fraudcase.domain.RiskLevel;
import com.capstone.fraudcase.domain.Route;
import com.capstone.fraudcase.knowledge.PolicyQueryBuilder;
import com.capstone.fraudcase.knowledge.PolicyRetriever;
import com.capstone.fraudcase.persistence.*;
import com.capstone.fraudcase.tools.CaseToolScope;
import com.capstone.fraudcase.tools.EvidenceToolkit;
import com.capstone.fraudcase.tools.ToolExecutor;
import com.capstone.fraudcase.tools.ToolNames;
import com.capstone.fraudcase.tools.ToolResult;
import com.capstone.fraudcase.tools.ToolResults.CaseSla;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bounded plan → act → retrieve → assess → recommend loop. The application owns tool selection, state transitions and
 * persistence; the processor only interprets collected evidence. Any failure ends in a safe, reviewable state.
 */
@Service
public class InvestigationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(InvestigationOrchestrator.class);

    private final FraudCaseService cases;
    private final InvestigationPlanner planner;
    private final EvidenceToolkit toolkit;
    private final ToolExecutor executor;
    private final EvidenceAssembler assembler;
    private final PolicyRetriever retriever;
    private final ProcessorRegistry processors;
    private final RecommendationValidator validator;
    private final CaseMemoryService memory;
    private final EvidenceItemRepository evidenceRepo;
    private final PolicyCitationRepository citationRepo;
    private final RecommendationRepository recommendationRepo;
    private final ToolTraceRepository traceRepo;
    private final IdGenerator ids;
    private final AuditWriter audit;
    private final CapstoneProperties props;
    private final MeterRegistry meters;
    private final Clock clock;

    public InvestigationOrchestrator(FraudCaseService cases, InvestigationPlanner planner, EvidenceToolkit toolkit, ToolExecutor executor,
                                     EvidenceAssembler assembler, PolicyRetriever retriever, ProcessorRegistry processors,
                                     RecommendationValidator validator, CaseMemoryService memory, EvidenceItemRepository evidenceRepo,
                                     PolicyCitationRepository citationRepo, RecommendationRepository recommendationRepo, ToolTraceRepository traceRepo, IdGenerator ids,
                                     AuditWriter audit, CapstoneProperties props, MeterRegistry meters, Clock clock) {
        this.cases = cases;
        this.planner = planner;
        this.toolkit = toolkit;
        this.executor = executor;
        this.assembler = assembler;
        this.retriever = retriever;
        this.processors = processors;
        this.validator = validator;
        this.memory = memory;
        this.evidenceRepo = evidenceRepo;
        this.citationRepo = citationRepo;
        this.recommendationRepo = recommendationRepo;
        this.traceRepo = traceRepo;
        this.ids = ids;
        this.audit = audit;
        this.props = props;
        this.meters = meters;
        this.clock = clock;
    }

    public record InvestigationResult(String caseId, int runNumber, String correlationId, CaseStatus status,
                                      List<PlannedStep> plan, String recommendationId, ProcessorMode requestedMode,
                                      ProcessorMode mode, String fallbackReason) {
    }

    public InvestigationResult investigate(String caseId, String actorId, String modeOverride) {
        ProcessorMode requested = processors.resolveMode(modeOverride);
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        try {
            FraudCaseEntity c = cases.transition(caseId, CaseStatus.ENRICHING, ActorType.USER, actorId, correlationId, "investigation started");
            cases.incrementRun(c);
            int run = c.getCurrentRun();
            TransactionClaimEntity claim = cases.claim(caseId);
            CaseFacts facts = CaseContextLoader.facts(c, claim);
            audit.append(caseId, "INVESTIGATION_STARTED", ActorType.USER, actorId, correlationId,
                    "Run " + run + " requested in " + requested.key() + " mode", Map.of("run", run, "mode", requested.key()));
            try {
                return runInvestigation(c, claim, facts, run, correlationId, requested);
            } catch (RuntimeException e) {
                log.error("Investigation failed case={} correlationId={} error={}", caseId, correlationId, e.getClass().getSimpleName());
                audit.append(caseId, "INVESTIGATION_FAILED", ActorType.SYSTEM, "orchestrator", correlationId,
                        "Investigation failed safely (" + e.getClass().getSimpleName() + "); case set to INCOMPLETE", Map.of("run", run));
                cases.transition(caseId, CaseStatus.INCOMPLETE, ActorType.SYSTEM, "orchestrator", correlationId, "investigation failed");
                return new InvestigationResult(caseId, run, correlationId, CaseStatus.INCOMPLETE, List.of(), null, requested, null,
                        "Investigation failed; manual review required");
            }
        } finally {
            MDC.remove("correlationId");
        }
    }

    private InvestigationResult runInvestigation(FraudCaseEntity c, TransactionClaimEntity claim, CaseFacts facts, int run,
                                                 String correlationId, ProcessorMode requested) {
        String caseId = c.getCaseId();
        List<PlannedStep> plan = planner.plan(facts);
        audit.append(caseId, "PLAN_CREATED", ActorType.SYSTEM, "planner", correlationId,
                "Bounded plan: " + String.join(" > ", plan.stream().map(PlannedStep::toolName).toList()), Map.of("steps", plan.size()));

        CaseToolScope scope = new CaseToolScope(caseId, run, correlationId, facts.maskedCustomerRef(), facts.transactionRef(),
                facts.maskedMerchant(), facts.reportedAt(), facts.amount(), facts.currency(), facts.occurredAt().toInstant(),
                facts.transactionChannel().name());
        Set<String> planned = new HashSet<>(plan.stream().map(PlannedStep::toolName).toList());
        String o = EvidenceToolkit.ORIGIN_PLAN;
        CollectedResults results = new CollectedResults(
                planned.contains(ToolNames.TRANSACTION) ? toolkit.getTransactionDetails(scope, o, facts.transactionRef()) : null,
                planned.contains(ToolNames.DEVICE) ? toolkit.getDeviceSignals(scope, o, facts.maskedCustomerRef(), facts.transactionRef()) : null,
                planned.contains(ToolNames.LOCATION) ? toolkit.compareRecentLocations(scope, o, facts.maskedCustomerRef(), facts.transactionRef()) : null,
                planned.contains(ToolNames.MERCHANT) ? toolkit.getMerchantRiskProfile(scope, o, facts.maskedMerchant()) : null,
                planned.contains(ToolNames.PRIOR_DISPUTES) ? toolkit.getPriorDisputes(scope, o, facts.maskedCustomerRef()) : null);

        AssembledEvidence assembled = assembler.assemble(facts, claim.getClaimId(),
                claim.getInjectionIndicators() == null ? List.of() : claim.getInjectionIndicators(), results);
        assembled.items().forEach(v -> evidenceRepo.save(toEntity(caseId, run, v)));

        String query = PolicyQueryBuilder.build(facts, assembled.retrievalIndicators());
        ToolResult<PolicyRetrievalResult> retrieval = executor.execute(scope, ToolNames.POLICY, o, Map.of("query", query),
                () -> retriever.retrieve(query),
                r -> r.abstained() ? "Abstained: no passage above threshold %.2f (best %.3f)".formatted(r.threshold(), r.bestScore())
                        : r.passages().size() + " passage(s) above threshold %.2f; best %.3f".formatted(r.threshold(), r.bestScore()));
        PolicyRetrievalResult policy = retrieval.ok() ? retrieval.data()
                : PolicyRetrievalResult.empty(query, retriever.topK(), retriever.threshold());
        List<String> missing = new ArrayList<>(assembled.missingInformation());
        if (!retrieval.ok()) {
            missing.add("policy retrieval (" + retrieval.status() + ")");
        }
        List<PolicyCitationEntity> retrieved = policy.passages().stream().map(p -> citationEntity(caseId, run, p)).toList();
        citationRepo.saveAll(retrieved);
        audit.append(caseId, policy.abstained() ? "POLICY_ABSTAINED" : "POLICY_RETRIEVED", ActorType.TOOL, ToolNames.POLICY, correlationId,
                policy.abstained() ? "No policy passage met threshold " + policy.threshold()
                        : "Retrieved " + String.join(", ", policy.passages().stream().map(p -> p.documentId() + "/" + p.chunkId()).toList()),
                Map.of("query", query, "count", policy.passages().size()));

        List<EvidenceView> evidence = assembled.items();
        var traces = executorTraces(caseId, run);
        FraudCaseContext ctx = new FraudCaseContext(caseId, correlationId, run, facts, evidence, traces, assembled.contradictions(),
                missing, policy, memory.history(caseId),
                claim.getInjectionIndicators() == null ? List.of() : claim.getInjectionIndicators());

        ProcessorMode used = requested;
        String fallbackReason = null;
        FraudCaseRecommendation rec;
        FraudCaseProcessor processor = processors.get(requested);
        long t0 = System.nanoTime();
        try {
            rec = processor.analyze(ctx);
        } catch (RecommendationValidationException e) {
            fallbackReason = "Recommendation rejected by validation: " + String.join("; ", e.violations());
            audit.append(caseId, "RECOMMENDATION_REJECTED", ActorType.PROCESSOR, requested.key(), correlationId,
                    truncate(fallbackReason, 450), Map.of("violations", e.violations()));
            meters.counter("capstone.recommendation.invalid", "mode", requested.key()).increment();
            rec = fallback(ctx, requested);
            used = rec.mode();
        } catch (ProcessorUnavailableException e) {
            fallbackReason = e.getMessage();
            audit.append(caseId, "PROCESSOR_UNAVAILABLE", ActorType.PROCESSOR, requested.key(), correlationId, e.getMessage(), Map.of());
            rec = fallback(ctx, requested);
            used = rec.mode();
        } catch (RuntimeException e) {
            fallbackReason = "Processor error (" + e.getClass().getSimpleName() + ")";
            audit.append(caseId, "PROCESSOR_UNAVAILABLE", ActorType.PROCESSOR, requested.key(), correlationId, fallbackReason, Map.of());
            rec = fallback(ctx, requested);
            used = rec.mode();
        } finally {
            meters.timer("capstone.processor.duration", "mode", requested.key()).record(java.time.Duration.ofNanos(System.nanoTime() - t0));
        }
        if (fallbackReason != null) {
            audit.append(caseId, "PROCESSOR_FALLBACK", ActorType.SYSTEM, "orchestrator", correlationId,
                    "Fell back from " + requested.key() + " to " + (used == null ? "safe manual review" : used.key()), Map.of());
        }

        ToolResult<CaseSla> sla = toolkit.calculateCaseSla(scope, o, rec.recommendedRoute().name(), rec.riskLevel().name());
        String recId = ids.recommendationId();
        RecommendationEntity entity = recommendationEntity(recId, ctx, rec, requested, fallbackReason, policy, sla);
        recommendationRepo.save(entity);
        Set<String> citedChunks = new HashSet<>(rec.policyCitations().stream().map(PolicyCitationRef::chunkId).toList());
        retrieved.forEach(p -> {
            p.setRecommendationId(recId);
            p.setCited(citedChunks.contains(p.getChunkId()));
        });
        citationRepo.saveAll(retrieved);
        audit.append(caseId, "RECOMMENDATION_VALIDATED", ActorType.PROCESSOR, rec.mode().key(), correlationId,
                recId + ": " + rec.classification() + " -> " + rec.recommendedRoute() + " (risk " + rec.riskLevel() + ", confidence "
                        + rec.confidence() + "); human review required",
                Map.of("recommendationId", recId, "rationale", rec.rationale()));

        CaseStatus target = rec.classification() == Classification.INSUFFICIENT_EVIDENCE ? CaseStatus.INCOMPLETE : CaseStatus.READY_FOR_REVIEW;
        cases.transition(caseId, target, ActorType.SYSTEM, "orchestrator", correlationId, "recommendation " + recId);
        return new InvestigationResult(caseId, run, correlationId, target, plan, recId, requested, rec.mode(), fallbackReason);
    }

    private FraudCaseRecommendation fallback(FraudCaseContext ctx, ProcessorMode requested) {
        if (props.getAi().isFallbackToRules() || requested == ProcessorMode.RULES) {
            try {
                return processors.rules().analyze(ctx);
            } catch (RuntimeException e) {
                log.error("Rules processor failed for case {}", ctx.caseId());
            }
        }
        return safeManualReview(ctx);
    }

    /** Last-resort recommendation that asserts nothing beyond the evidence gap. */
    FraudCaseRecommendation safeManualReview(FraudCaseContext ctx) {
        List<String> missing = new ArrayList<>(ctx.missingInformation());
        missing.add("validated processor recommendation");
        FraudCaseRecommendation rec = new FraudCaseRecommendation(ctx.caseId(), Classification.INSUFFICIENT_EVIDENCE, RiskLevel.MEDIUM,
                Route.MANUAL_REVIEW, 0.1, true, List.of(), List.of(), missing, ctx.contradictions(), List.of("SAFE-MANUAL-REVIEW"),
                List.of(), "No validated recommendation could be produced; route to manual review. No automated action taken.",
                ProcessorMode.RULES, Map.of());
        return validator.validate(rec, ctx);
    }

    private List<com.capstone.fraudcase.agent.common.ToolOutcome> executorTraces(String caseId, int run) {
        return traceRepo.findByCaseIdAndRunNumberOrderByStartedAtAscTraceIdAsc(caseId, run)
                .stream().map(CaseContextLoader::outcome).toList();
    }

    private EvidenceItemEntity toEntity(String caseId, int run, EvidenceView v) {
        EvidenceItemEntity e = new EvidenceItemEntity();
        e.setEvidenceId(v.evidenceId());
        e.setCaseId(caseId);
        e.setRunNumber(run);
        e.setCategory(v.category());
        e.setType(v.type());
        e.setSeverity(v.severity());
        e.setValueSummary(v.summary());
        e.setProvenance(v.provenance());
        e.setSourceRef(v.sourceRef());
        e.setObservedAt(v.observedAt().atOffset(ZoneOffset.UTC));
        return e;
    }

    private PolicyCitationEntity citationEntity(String caseId, int run, PolicyPassage p) {
        PolicyCitationEntity e = new PolicyCitationEntity();
        e.setCaseId(caseId);
        e.setRunNumber(run);
        e.setDocumentId(p.documentId());
        e.setTitle(p.title());
        e.setVersion(p.version());
        e.setEffectiveDate(p.effectiveDate());
        e.setSection(p.section());
        e.setSectionTitle(p.sectionTitle());
        e.setChunkId(p.chunkId());
        e.setScore(Math.round(p.score() * 1000.0) / 1000.0);
        e.setPassage(truncate(p.text(), 3990));
        e.setCited(false);
        return e;
    }

    private RecommendationEntity recommendationEntity(String id, FraudCaseContext ctx, FraudCaseRecommendation rec, ProcessorMode requested,
                                                      String fallbackReason, PolicyRetrievalResult policy, ToolResult<CaseSla> sla) {
        RecommendationEntity e = new RecommendationEntity();
        e.setRecommendationId(id);
        e.setCaseId(ctx.caseId());
        e.setRunNumber(ctx.runNumber());
        e.setCorrelationId(ctx.correlationId());
        e.setMode(rec.mode());
        e.setRequestedMode(requested);
        e.setFallbackReason(fallbackReason == null ? null : truncate(fallbackReason, 495));
        e.setClassification(rec.classification());
        e.setRoute(rec.recommendedRoute());
        e.setRiskLevel(rec.riskLevel());
        e.setConfidence(rec.confidence());
        e.setHumanReviewRequired(rec.humanReviewRequired());
        e.setRationale(rec.rationale());
        e.setEvidenceIds(rec.evidenceIds());
        e.setMissingInformation(rec.missingInformation());
        e.setContradictions(rec.contradictions());
        e.setRuleIds(rec.ruleIds());
        e.setToolTraceIds(rec.toolTraceIds());
        e.setInputFacts(rec.inputFacts());
        if (sla.ok()) {
            e.setPriority(sla.data().priority());
            e.setSlaTargetAt(sla.data().targetAt().atOffset(ZoneOffset.UTC));
        }
        e.setSlaTraceId(sla.toolResultId());
        e.setRetrievalQuery(truncate(policy.query(), 995));
        e.setRetrievalBestScore(Math.round(policy.bestScore() * 1000.0) / 1000.0);
        e.setRetrievalAbstained(policy.abstained());
        e.setCreatedAt(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
        return e;
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
