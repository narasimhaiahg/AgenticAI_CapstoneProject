package com.capstone.fraudcase.web;

import com.capstone.fraudcase.api.CaseQueryService;
import com.capstone.fraudcase.application.*;
import com.capstone.fraudcase.domain.*;
import com.capstone.fraudcase.knowledge.PolicyIngestionService;
import com.capstone.fraudcase.persistence.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Thymeleaf screens: intake, investigation, evidence, recommendation, citations, review, follow-up and audit. */
@Controller
public class UiController {

    private final FraudCaseService cases;
    private final InvestigationOrchestrator orchestrator;
    private final InvestigationPlanner planner;
    private final ReviewService reviews;
    private final FollowUpService followUps;
    private final CaseMemoryService memory;
    private final CaseQueryService queries;
    private final ProcessorRegistry processors;
    private final PolicyIngestionService policies;
    private final AuditWriter auditWriter;
    private final String sampleCasesJson;

    public UiController(FraudCaseService cases, InvestigationOrchestrator orchestrator, InvestigationPlanner planner, ReviewService reviews,
                        FollowUpService followUps, CaseMemoryService memory, CaseQueryService queries, ProcessorRegistry processors,
                        PolicyIngestionService policies, AuditWriter auditWriter) throws IOException {
        this.cases = cases;
        this.orchestrator = orchestrator;
        this.planner = planner;
        this.reviews = reviews;
        this.followUps = followUps;
        this.memory = memory;
        this.queries = queries;
        this.processors = processors;
        this.policies = policies;
        this.auditWriter = auditWriter;
        try (var in = new ClassPathResource("fixtures/sample-cases.json").getInputStream()) {
            this.sampleCasesJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @ModelAttribute
    void common(Model model, Authentication auth) {
        model.addAttribute("configuredMode", processors.configuredMode().key());
        model.addAttribute("modes", processors.all().values().stream()
                .sorted(Comparator.comparing(p -> p.mode().ordinal()))
                .map(p -> Map.of("key", p.mode().key(), "available", p.isAvailable(), "detail", p.availabilityDetail())).toList());
        if (auth != null) {
            model.addAttribute("roles", roles(auth));
        }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/ui/cases";
    }

    @GetMapping("/ui/cases")
    public String list(Model model) {
        List<FraudCaseEntity> all = cases.list();
        Map<String, RecommendationEntity> latest = new HashMap<>();
        all.forEach(c -> queries.latestRecommendation(c.getCaseId()).ifPresent(r -> latest.put(c.getCaseId(), r)));
        model.addAttribute("cases", all);
        model.addAttribute("latest", latest);
        return "cases/list";
    }

    @GetMapping("/ui/cases/new")
    public String newCase(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new IntakeForm());
        }
        addIntakeOptions(model);
        return "cases/intake";
    }

    @PostMapping("/ui/cases")
    public String create(@ModelAttribute("form") IntakeForm form, @RequestParam(defaultValue = "create") String action,
                         Authentication auth, Model model, RedirectAttributes redirect) {
        Map<String, String> errors = new LinkedHashMap<>();
        BigDecimal amount = null;
        OffsetDateTime occurredAt = null;
        try {
            amount = new BigDecimal(form.getAmount() == null ? "" : form.getAmount().trim());
        } catch (NumberFormatException e) {
            errors.put("amount", "Enter a numeric amount");
        }
        try {
            occurredAt = OffsetDateTime.parse(form.getOccurredAt() == null ? "" : form.getOccurredAt().trim());
        } catch (DateTimeParseException e) {
            errors.put("occurredAt", "Use ISO-8601 with offset, e.g. 2026-11-06T08:42:00+05:30");
        }
        FraudCaseEntity created = null;
        if (errors.isEmpty()) {
            try {
                created = cases.create(new IntakeCommand(form.getMaskedCustomerRef(), form.getHomeRegion(), form.getTransactionRef(), amount,
                        form.getCurrency(), occurredAt, form.getStatus(), form.getTransactionChannel(), form.getMaskedMerchant(),
                        form.getIssueType(), form.getCustomerStatement(), form.getCardPossession(), form.isMerchantRecognised(),
                        form.getIntakeChannel(), form.isSyntheticDataConfirmed(), form.isReviewRequired()), auth.getName());
            } catch (DomainValidationException e) {
                errors.putAll(e.fieldErrors());
            }
        }
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            addIntakeOptions(model);
            return "cases/intake";
        }
        if ("start".equals(action)) {
            if (!roles(auth).contains("ROLE_INVESTIGATOR") && !roles(auth).contains("ROLE_LEAD")) {
                redirect.addFlashAttribute("notice", "Case created. An investigator must start the investigation.");
                return "redirect:/ui/cases/" + created.getCaseId() + "/investigation";
            }
            var r = orchestrator.investigate(created.getCaseId(), auth.getName(), null);
            redirect.addFlashAttribute("notice", investigationNotice(r));
        } else {
            redirect.addFlashAttribute("notice", "Case " + created.getCaseId() + " created in state NEW.");
        }
        return "redirect:/ui/cases/" + created.getCaseId() + "/investigation";
    }

    @GetMapping("/ui/cases/{caseId}")
    public String caseHome(@PathVariable String caseId) {
        cases.get(caseId);
        return "redirect:/ui/cases/" + caseId + "/investigation";
    }

    @GetMapping("/ui/cases/{caseId}/investigation")
    public String investigation(@PathVariable String caseId, Model model) {
        caseModel(caseId, model, "investigation");
        TransactionClaimEntity claim = cases.claim(caseId);
        model.addAttribute("plan", planner.plan(CaseContextLoader.facts(cases.get(caseId), claim)));
        model.addAttribute("traces", queries.traces(caseId));
        return "cases/investigation";
    }

    @PostMapping("/ui/cases/{caseId}/investigate")
    public String investigate(@PathVariable String caseId, @RequestParam(required = false) String mode, Authentication auth,
                              RedirectAttributes redirect) {
        try {
            var r = orchestrator.investigate(caseId, auth.getName(), mode == null || mode.isBlank() ? null : mode);
            redirect.addFlashAttribute("notice", investigationNotice(r));
        } catch (InvalidStateTransitionException | DomainValidationException e) {
            redirect.addFlashAttribute("error", "Investigation cannot start: " + safeMessage(e));
        }
        return "redirect:/ui/cases/" + caseId + "/investigation";
    }

    @GetMapping("/ui/cases/{caseId}/evidence")
    public String evidence(@PathVariable String caseId, Model model) {
        caseModel(caseId, model, "evidence");
        List<EvidenceItemEntity> items = queries.evidence(caseId);
        model.addAttribute("claims", items.stream().filter(e -> e.getProvenance() == Provenance.USER_INTAKE).toList());
        model.addAttribute("observed", items.stream().filter(e -> e.getProvenance() == Provenance.TOOL_RESULT).toList());
        model.addAttribute("system", items.stream().filter(e -> e.getProvenance() == Provenance.SYSTEM).toList());
        model.addAttribute("traceById", queries.traces(caseId).stream().collect(Collectors.toMap(ToolTraceEntity::getTraceId, Function.identity())));
        model.addAttribute("claim", cases.claim(caseId));
        queries.latestRecommendation(caseId).ifPresent(r -> model.addAttribute("rec", r));
        return "cases/evidence";
    }

    @GetMapping("/ui/cases/{caseId}/recommendation")
    public String recommendation(@PathVariable String caseId, Model model) {
        caseModel(caseId, model, "recommendation");
        queries.latestRecommendation(caseId).ifPresent(r -> {
            model.addAttribute("rec", r);
            model.addAttribute("cited", queries.cited(r));
            model.addAttribute("evidenceById", queries.evidence(caseId).stream()
                    .collect(Collectors.toMap(EvidenceItemEntity::getEvidenceId, Function.identity())));
        });
        model.addAttribute("history", queries.recommendationHistory(caseId));
        return "cases/recommendation";
    }

    @GetMapping("/ui/cases/{caseId}/citations")
    public String citations(@PathVariable String caseId, Model model) {
        caseModel(caseId, model, "citations");
        model.addAttribute("citations", queries.citations(caseId));
        queries.latestRecommendation(caseId).ifPresent(r -> model.addAttribute("rec", r));
        model.addAttribute("unavailableChunks", policies.chunks().stream().filter(c -> !c.searchable()).toList());
        return "cases/citations";
    }

    @GetMapping("/ui/cases/{caseId}/review")
    public String review(@PathVariable String caseId, Model model, Authentication auth) {
        FraudCaseEntity c = caseModel(caseId, model, "review");
        queries.latestRecommendation(caseId).ifPresent(r -> model.addAttribute("rec", r));
        model.addAttribute("dispositions", ReviewService.allowedDispositions(c.getStatus(), roles(auth)));
        model.addAttribute("routes", Route.values());
        model.addAttribute("history", reviews.history(caseId));
        model.addAttribute("canReview", roles(auth).contains("ROLE_REVIEWER") || roles(auth).contains("ROLE_LEAD"));
        return "cases/review";
    }

    @PostMapping("/ui/cases/{caseId}/review/start")
    public String startReview(@PathVariable String caseId, Authentication auth, RedirectAttributes redirect) {
        try {
            reviews.startReview(caseId, auth.getName());
            redirect.addFlashAttribute("notice", "Review started. The case is now UNDER_REVIEW.");
        } catch (InvalidStateTransitionException e) {
            redirect.addFlashAttribute("error", safeMessage(e));
        }
        return "redirect:/ui/cases/" + caseId + "/review";
    }

    @PostMapping("/ui/cases/{caseId}/review")
    public String submitReview(@PathVariable String caseId, @RequestParam(required = false) Disposition disposition,
                               @RequestParam(required = false) Route route, @RequestParam(required = false) String reason,
                               @RequestParam(defaultValue = "false") boolean missingInformationAcknowledged,
                               Authentication auth, Model model, RedirectAttributes redirect) {
        try {
            var r = reviews.submit(caseId, auth.getName(), roles(auth), new ReviewCommand(disposition, route, reason, missingInformationAcknowledged));
            redirect.addFlashAttribute("notice", "Disposition " + r.getDisposition() + " recorded (" + r.getReviewId() + "); case is now " + r.getToStatus() + ".");
            return "redirect:/ui/cases/" + caseId + "/review";
        } catch (DomainValidationException e) {
            model.addAttribute("errors", e.fieldErrors());
        } catch (AccessDeniedException | InvalidStateTransitionException e) {
            model.addAttribute("errors", Map.of("disposition", safeMessage(e)));
        }
        model.addAttribute("submitted", Map.of("disposition", disposition == null ? "" : disposition.name(),
                "route", route == null ? "" : route.name(), "reason", reason == null ? "" : reason));
        return review(caseId, model, auth);
    }

    @GetMapping("/ui/cases/{caseId}/conversation")
    public String conversation(@PathVariable String caseId, Model model) {
        caseModel(caseId, model, "conversation");
        model.addAttribute("messages", memory.messages(caseId));
        return "cases/conversation";
    }

    @PostMapping("/ui/cases/{caseId}/conversation")
    public String ask(@PathVariable String caseId, @RequestParam String question, Authentication auth, RedirectAttributes redirect) {
        try {
            var r = followUps.ask(caseId, question, auth.getName(), null);
            if (r.note() != null) {
                redirect.addFlashAttribute("notice", r.note());
            }
        } catch (DomainValidationException e) {
            redirect.addFlashAttribute("error", String.join(" ", e.fieldErrors().values()));
        }
        return "redirect:/ui/cases/" + caseId + "/conversation";
    }

    @GetMapping("/ui/cases/{caseId}/audit")
    public String audit(@PathVariable String caseId, Model model) {
        caseModel(caseId, model, "audit");
        model.addAttribute("events", queries.audit(caseId));
        model.addAttribute("brokenLinks", auditWriter.brokenLinks());
        return "cases/audit";
    }

    @GetMapping("/ui/policies")
    public String policies(Model model) {
        model.addAttribute("documents", policies.documents());
        model.addAttribute("chunks", policies.chunks());
        model.addAttribute("rejected", policies.rejectedSources());
        model.addAttribute("embeddingModel", policies.embeddingModelName());
        return "policies";
    }

    @ExceptionHandler(CaseNotFoundException.class)
    public String notFound(Model model) {
        model.addAttribute("message", "Case not found.");
        return "error/case-not-found";
    }

    private FraudCaseEntity caseModel(String caseId, Model model, String tab) {
        FraudCaseEntity c = cases.get(caseId);
        model.addAttribute("c", c);
        model.addAttribute("t", cases.claim(caseId));
        model.addAttribute("tab", tab);
        model.addAttribute("canInvestigate", c.getStatus() == CaseStatus.NEW || c.getStatus().canTransitionTo(CaseStatus.ENRICHING));
        return c;
    }

    private void addIntakeOptions(Model model) {
        model.addAttribute("issueTypes", IssueType.values());
        model.addAttribute("txStatuses", TransactionStatus.values());
        model.addAttribute("txChannels", TransactionChannel.values());
        model.addAttribute("possessions", CardPossession.values());
        model.addAttribute("intakeChannels", IntakeChannel.values());
        model.addAttribute("sampleCasesJson", sampleCasesJson);
    }

    private static String investigationNotice(InvestigationOrchestrator.InvestigationResult r) {
        String base = "Investigation run " + r.runNumber() + " finished: case is " + r.status()
                + (r.recommendationId() == null ? "" : ", recommendation " + r.recommendationId() + " (" + r.mode().key() + " mode)") + ".";
        return r.fallbackReason() == null ? base : base + " Fallback applied: " + r.fallbackReason();
    }

    private static String safeMessage(Exception e) {
        if (e instanceof DomainValidationException d) {
            return String.join(" ", d.fieldErrors().values());
        }
        return e.getMessage() == null ? "Action not allowed" : e.getMessage();
    }

    static Set<String> roles(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
}
