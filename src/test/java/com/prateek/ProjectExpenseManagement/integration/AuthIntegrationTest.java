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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unlike the other integration tests (which run with security filters
 * disabled to keep them focused on business logic), this class builds its
 * own MockMvc with the real Spring Security filter chain applied, since
 * it's specifically verifying auth behavior.
 */
class AuthIntegrationTest extends AbstractIntegrationTestBase {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc securedMockMvc;

    @BeforeEach
    void setUpSecuredMockMvc() {
        securedMockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void registerThenLoginReturnsAccessToken() throws Exception {
        String email = "auth.flow." + System.nanoTime() + "@test.com";

        String registerBody = """
                {
                  "email": "%s",
                  "fullName": "Auth Flow User",
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

        securedMockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        String email = "auth.wrongpw." + System.nanoTime() + "@test.com";

        String registerBody = """
                {
                  "email": "%s",
                  "fullName": "Auth Flow User",
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
                  "password": "totally-wrong-password"
                }
                """.formatted(email);

        securedMockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsRequestsWithoutToken() throws Exception {
        securedMockMvc.perform(get("/api/v1/expenses/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsRequestWithValidToken() throws Exception {
        String email = "auth.token." + System.nanoTime() + "@test.com";

        String registerBody = """
                {
                  "email": "%s",
                  "fullName": "Auth Flow User",
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
        String token = loginJson.get("accessToken").asText();

        // A random (non-existent) expense id still passes auth and should 404,
        // not 401 - proving the token itself was accepted.
        securedMockMvc.perform(get("/api/v1/expenses/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
