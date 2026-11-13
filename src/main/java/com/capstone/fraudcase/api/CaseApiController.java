package com.capstone.fraudcase.api;

import com.capstone.fraudcase.api.ApiDtos.*;
import com.capstone.fraudcase.application.FollowUpService;
import com.capstone.fraudcase.application.FraudCaseService;
import com.capstone.fraudcase.application.InvestigationOrchestrator;
import com.capstone.fraudcase.application.ReviewCommand;
import com.capstone.fraudcase.application.ReviewService;
import com.capstone.fraudcase.persistence.FraudCaseEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** REST API for the fraud case lifecycle. Every endpoint is case-scoped and role-protected (see SecurityConfig). */
@RestController
@RequestMapping("/api/cases")
public class CaseApiController {

    private final FraudCaseService cases;
    private final InvestigationOrchestrator orchestrator;
    private final FollowUpService followUps;
    private final ReviewService reviews;
    private final CaseQueryService queries;

    public CaseApiController(FraudCaseService cases, InvestigationOrchestrator orchestrator, FollowUpService followUps,
                             ReviewService reviews, CaseQueryService queries) {
        this.cases = cases;
        this.orchestrator = orchestrator;
        this.followUps = followUps;
        this.reviews = reviews;
        this.queries = queries;
    }

    @PostMapping
    public ResponseEntity<CaseResponse> create(@Valid @RequestBody CreateCaseRequest request, Authentication auth) {
        FraudCaseEntity c = cases.create(request.toCommand(), auth.getName());
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(c.getCaseId()).toUri();
        return ResponseEntity.created(location).body(CaseResponse.from(c, cases.claim(c.getCaseId())));
    }

    @GetMapping
    public List<CaseResponse> list() {
        return cases.list().stream().map(c -> CaseResponse.from(c, cases.claim(c.getCaseId()))).toList();
    }

    @GetMapping("/{caseId}")
    public CaseResponse get(@PathVariable String caseId) {
        return CaseResponse.from(cases.get(caseId), cases.claim(caseId));
    }

    @PostMapping("/{caseId}/investigations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public InvestigationResponse investigate(@PathVariable String caseId, @RequestParam(required = false) String mode, Authentication auth) {
        var r = orchestrator.investigate(caseId, auth.getName(), mode);
        return new InvestigationResponse(r.caseId(), r.runNumber(), r.correlationId(), r.status(), r.plan(), r.recommendationId(),
                r.requestedMode(), r.mode(), r.fallbackReason());
    }

    @GetMapping("/{caseId}/evidence")
    public List<EvidenceResponse> evidence(@PathVariable String caseId) {
        return queries.evidence(caseId).stream().map(EvidenceResponse::from).toList();
    }

    @GetMapping("/{caseId}/tool-traces")
    public List<ToolTraceResponse> toolTraces(@PathVariable String caseId) {
        return queries.traces(caseId).stream().map(ToolTraceResponse::from).toList();
    }

    @GetMapping("/{caseId}/recommendation")
    public ResponseEntity<RecommendationResponse> recommendation(@PathVariable String caseId) {
        return queries.latestRecommendation(caseId)
                .map(r -> ResponseEntity.ok(RecommendationResponse.from(r, queries.cited(r))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{caseId}/citations")
    public List<CitationResponse> citations(@PathVariable String caseId) {
        return queries.citations(caseId).stream().map(CitationResponse::from).toList();
    }

    @PostMapping("/{caseId}/messages")
    public MessageResponse message(@PathVariable String caseId, @RequestBody MessageRequest request, Authentication auth) {
        var r = followUps.ask(caseId, request.question(), auth.getName(), request.mode());
        return new MessageResponse(caseId, r.answer().answer(), r.answer().referencedEvidenceIds(), r.answer().referencedChunkIds(),
                r.answer().mode(), r.note(), "Informational only; the recommendation and disposition are unchanged.");
    }

    @PostMapping("/{caseId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse review(@PathVariable String caseId, @RequestBody ReviewRequest request, Authentication auth) {
        var r = reviews.submit(caseId, auth.getName(), roles(auth),
                new ReviewCommand(request.disposition(), request.route(), request.reason(), request.missingInformationAcknowledged()));
        return ReviewResponse.from(r);
    }

    @GetMapping("/{caseId}/reviews")
    public List<ReviewResponse> reviewHistory(@PathVariable String caseId) {
        cases.get(caseId);
        return reviews.history(caseId).stream().map(ReviewResponse::from).toList();
    }

    @GetMapping("/{caseId}/audit")
    public List<AuditResponse> audit(@PathVariable String caseId) {
        return queries.audit(caseId).stream().map(AuditResponse::from).toList();
    }

    static Set<String> roles(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
}
