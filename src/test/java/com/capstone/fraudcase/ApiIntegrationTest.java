package com.capstone.fraudcase;

import com.capstone.fraudcase.application.CaseMemoryService;
import com.capstone.fraudcase.persistence.AuditWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "capstone.security.demo-password=test-pass",
        "capstone.db.url=jdbc:h2:mem:itest;DB_CLOSE_DELAY=-1",
        "capstone.ai.api-key=",
        "capstone.ai.base-url=http://localhost:1",
        "spring.datasource.url=jdbc:h2:mem:itest;DB_CLOSE_DELAY=-1"
})
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AuditWriter audit;
    @Autowired CaseMemoryService memory;

    private static String auth(String user) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":test-pass").getBytes());
    }

    private static String caseBody(String txn, String statement) {
        return """
            {"maskedCustomerRef":"CUST-••••-4821","homeRegion":"IN-KA","transactionRef":"%s",
             "amount":100.00,"currency":"INR","occurredAt":"2026-11-06T08:42:00+05:30","status":"POSTED",
             "channel":"CARD_NOT_PRESENT","maskedMerchant":"NORTHSTAR DIGITAL ••••73",
             "issueType":"MERCHANT_NOT_RECOGNISED","customerStatement":"%s","cardPossession":"IN_POSSESSION",
             "merchantRecognised":false,"intakeChannel":"WEB_CHAT","syntheticDataConfirmed":true,"reviewRequired":true}
            """.formatted(txn, statement);
    }

    private String createCase(String txn) throws Exception {
        var res = mvc.perform(post("/api/cases").header("Authorization", auth("agent"))
                        .contentType(MediaType.APPLICATION_JSON).content(caseBody(txn, "I do not recognise this.")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return res.replaceAll(".*\"caseId\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    @Test
    void unauthenticatedRequestRejected() throws Exception {
        mvc.perform(post("/api/cases").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void intakeRequiresConsentFlags() throws Exception {
        mvc.perform(post("/api/cases").header("Authorization", auth("agent"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(caseBody("TXN-2026-784102", "x").replace("\"syntheticDataConfirmed\":true", "\"syntheticDataConfirmed\":false")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void panLikeStatementRejected() throws Exception {
        mvc.perform(post("/api/cases").header("Authorization", auth("agent"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(caseBody("TXN-2026-784102", "my card is 4111111111111111")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fullFlowProducesValidatedRecommendationAndAuditChain() throws Exception {
        String id = createCase("TXN-2026-784102");
        mvc.perform(post("/api/cases/" + id + "/investigations").header("Authorization", auth("investigator")))
                .andExpect(status().isAccepted());
        mvc.perform(get("/api/cases/" + id + "/recommendation").header("Authorization", auth("reviewer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.humanReviewRequired").value(true))
                .andExpect(jsonPath("$.classification").value("UNAUTHORISED_TRANSACTION"))
                .andExpect(jsonPath("$.recommendedRoute").value("FRAUD_INVESTIGATION"));
        assertTrue(audit.brokenLinks().isEmpty(), "audit chain must be intact");
    }

    @Test
    void auditorCannotCreateCase() throws Exception {
        mvc.perform(post("/api/cases").header("Authorization", auth("auditor"))
                        .contentType(MediaType.APPLICATION_JSON).content(caseBody("TXN-2026-784102", "x")))
                .andExpect(status().isForbidden());
    }

    @Test
    void investigationOnlyForInvestigatorOrAbove() throws Exception {
        String id = createCase("TXN-2026-784102");
        mvc.perform(post("/api/cases/" + id + "/investigations").header("Authorization", auth("agent")))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewAcceptRoutesCase() throws Exception {
        String id = createCase("TXN-2026-784102");
        mvc.perform(post("/api/cases/" + id + "/investigations").header("Authorization", auth("investigator")))
                .andExpect(status().isAccepted());
        mvc.perform(post("/api/cases/" + id + "/reviews").header("Authorization", auth("reviewer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"ACCEPT\",\"reason\":\"Evidence supports fraud routing.\",\"missingInformationAcknowledged\":true}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/cases/" + id).header("Authorization", auth("reviewer")))
                .andExpect(jsonPath("$.status").value("ROUTED"));
    }

    @Test
    void memoryIsolatedBetweenCases() throws Exception {
        String a = createCase("TXN-2026-784102");
        String b = createCase("TXN-2026-551230");
        mvc.perform(post("/api/cases/" + a + "/messages").header("Authorization", auth("investigator"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"which evidence supports this?\"}"))
                .andExpect(status().isOk());
        assertTrue(memory.history(b).isEmpty(), "case B must not see case A turns");
        assertFalse(memory.history(a).isEmpty());
    }
}
