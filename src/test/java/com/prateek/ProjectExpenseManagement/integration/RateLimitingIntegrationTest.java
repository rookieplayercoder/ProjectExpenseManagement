package com.prateek.ProjectExpenseManagement.integration;

import com.prateek.ProjectExpenseManagement.support.AbstractIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uses a much tighter rate-limit override than the rest of the suite (which
 * disables limiting entirely - see AbstractIntegrationTestBase) so the 429
 * behavior can actually be exercised without needing hundreds of requests.
 */
@TestPropertySource(properties = {
        "rate-limit.login.max-requests=3",
        "rate-limit.login.window-seconds=60"
})
class RateLimitingIntegrationTest extends AbstractIntegrationTestBase {

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
    void exceedingLoginLimitReturns429WithRetryAfter() throws Exception {
        String loginBody = """
                {
                  "email": "does.not.exist@test.com",
                  "password": "irrelevant-wrong-password"
                }
                """;

        // First 3 requests are allowed through to the real handler (and fail
        // with 401 - wrong credentials - since the rate limiter's job is only
        // to gate volume, not to judge correctness).
        for (int i = 0; i < 3; i++) {
            securedMockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody))
                    .andExpect(status().isUnauthorized());
        }

        // 4th request within the window is rejected before it ever reaches
        // the login logic.
        securedMockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"));
    }
}
