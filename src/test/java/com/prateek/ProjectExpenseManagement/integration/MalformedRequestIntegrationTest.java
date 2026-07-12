package com.prateek.ProjectExpenseManagement.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.prateek.ProjectExpenseManagement.support.AbstractIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Malformed client input (bad path variables, unreadable JSON bodies) should
 * come back as a clean 400, not a raw 500 - these tests pin that behavior
 * down explicitly, since it's easy to regress by removing a handler.
 */
class MalformedRequestIntegrationTest extends AbstractIntegrationTestBase {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc securedMockMvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        securedMockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        String email = "malformed.test." + System.nanoTime() + "@test.com";
        String registerBody = """
                {
                  "email": "%s",
                  "fullName": "Malformed Test User",
                  "password": "correct-horse-battery"
                }
                """.formatted(email);

        securedMockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = """
                {
                  "email": "%s",
                  "password": "correct-horse-battery"
                }
                """.formatted(email);

        var loginResult = securedMockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        token = loginJson.get("accessToken").asText();
    }

    @Test
    void nonUuidPathVariableReturns400NotServerError() throws Exception {
        securedMockMvc.perform(get("/api/v1/expenses/not-a-valid-uuid")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REQUEST_VALIDATION_ERROR"));
    }

    @Test
    void malformedJsonBodyReturns400NotServerError() throws Exception {
        securedMockMvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REQUEST_VALIDATION_ERROR"));
    }

    @Test
    void wrongTypeInJsonBodyReturns400NotServerError() throws Exception {
        // totalAmount is a number field - sending a non-numeric string for it
        // should be a clean 400, not a raw deserialization 500.
        String body = """
                {
                  "paidByUserId": "%s",
                  "title": "Bad Amount",
                  "totalAmount": "not-a-number",
                  "currencyCode": "USD",
                  "splitType": "EQUAL",
                  "expenseDate": "2026-07-10",
                  "createdByUserId": "%s",
                  "participants": [{"userId": "%s"}]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        securedMockMvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REQUEST_VALIDATION_ERROR"));
    }
}
