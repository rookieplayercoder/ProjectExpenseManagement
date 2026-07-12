package com.prateek.ProjectExpenseManagement.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.prateek.ProjectExpenseManagement.support.AbstractIntegrationTestBase;
import com.prateek.ProjectExpenseManagement.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpenseCreationPercentageSplitIntegrationTest extends AbstractIntegrationTestBase {

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void rejectsPercentageSplitWhenPercentagesDoNotSumToOneHundred() throws Exception {
        UUID payer = testDataFactory.createUser("Henry Payer", "henry.pct@test.com");
        UUID participant2 = testDataFactory.createUser("Iris Participant", "iris.pct@test.com");
        UUID groupId = testDataFactory.createGroup("Percentage Mismatch Group", payer, List.of(participant2));

        // 60 + 30 = 90, not 100 -> should be rejected before any write happens
        String requestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "title": "Utilities",
                  "totalAmount": 100.00,
                  "currencyCode": "USD",
                  "splitType": "PERCENTAGE",
                  "expenseDate": "2026-07-10",
                  "createdByUserId": "%s",
                  "participants": [
                    {"userId": "%s", "percentage": 60.00},
                    {"userId": "%s", "percentage": 30.00}
                  ]
                }
                """.formatted(groupId, payer, payer, payer, participant2);

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BUSINESS_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Sum of all percentages must be exactly 100")));

        Integer expenseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expense WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId), Integer.class);
        assertThat(expenseCount).isZero();
    }

    @Test
    void rejectsPercentageSplitWithNegativePercentage() throws Exception {
        UUID payer = testDataFactory.createUser("Jack Payer", "jack.pct@test.com");
        UUID participant2 = testDataFactory.createUser("Kelly Participant", "kelly.pct@test.com");
        UUID groupId = testDataFactory.createGroup("Percentage Negative Group", payer, List.of(participant2));

        String requestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "title": "Utilities",
                  "totalAmount": 100.00,
                  "currencyCode": "USD",
                  "splitType": "PERCENTAGE",
                  "expenseDate": "2026-07-10",
                  "createdByUserId": "%s",
                  "participants": [
                    {"userId": "%s", "percentage": 110.00},
                    {"userId": "%s", "percentage": -10.00}
                  ]
                }
                """.formatted(groupId, payer, payer, payer, participant2);

        // ParticipantShareRequest.percentage now carries @DecimalMin(0.00)/@DecimalMax(100.00),
        // so a negative (or >100) percentage is now rejected by Bean Validation before the
        // request ever reaches PercentageSplitStrategy - it fails fast at the DTO boundary
        // instead of the old "Percentage cannot be negative" business-layer message. Confirming
        // the outer contract (400, REQUEST_VALIDATION_ERROR) rather than the exact validation
        // message text, since that wording is Hibernate Validator's default interpolation and
        // not something this codebase owns.
        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REQUEST_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("percentage")));
    }

    @Test
    void acceptsPercentageSplitWhenPercentagesSumToOneHundred() throws Exception {
        UUID payer = testDataFactory.createUser("Liam Payer", "liam.pct@test.com");
        UUID participant2 = testDataFactory.createUser("Mia Participant", "mia.pct@test.com");
        UUID groupId = testDataFactory.createGroup("Percentage Valid Group", payer, List.of(participant2));

        // 70% / 30% of 100.00 -> 70.00 / 30.00, no remainder-cent distribution needed
        String requestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "title": "Utilities",
                  "totalAmount": 100.00,
                  "currencyCode": "USD",
                  "splitType": "PERCENTAGE",
                  "expenseDate": "2026-07-10",
                  "createdByUserId": "%s",
                  "participants": [
                    {"userId": "%s", "percentage": 70.00},
                    {"userId": "%s", "percentage": 30.00}
                  ]
                }
                """.formatted(groupId, payer, payer, payer, participant2);

        var result = mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID expenseId = UUID.fromString(responseJson.get("expenseId").asText());

        BigDecimal participant2Owed = jdbcTemplate.queryForObject(
                "SELECT owed_amount FROM expense_participant WHERE expense_id = :expenseId AND user_id = :userId",
                new MapSqlParameterSource()
                        .addValue("expenseId", expenseId)
                        .addValue("userId", participant2),
                BigDecimal.class);

        assertThat(participant2Owed).isEqualByComparingTo("30.00");
    }
}