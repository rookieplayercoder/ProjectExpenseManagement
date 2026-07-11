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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpenseCreationExactSplitIntegrationTest extends AbstractIntegrationTestBase {

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void rejectsExactSplitWhenAmountsDoNotSumToTotal() throws Exception {
        UUID payer = testDataFactory.createUser("Dave Payer", "dave.exact@test.com");
        UUID participant2 = testDataFactory.createUser("Eve Participant", "eve.exact@test.com");
        UUID groupId = testDataFactory.createGroup("Exact Split Mismatch Group", payer, List.of(participant2));

        // 60 + 30 = 90, but totalAmount claims 100 -> should be rejected before any write happens
        String requestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "title": "Groceries",
                  "totalAmount": 100.00,
                  "currencyCode": "USD",
                  "splitType": "EXACT",
                  "expenseDate": "2026-07-10",
                  "createdByUserId": "%s",
                  "participants": [
                    {"userId": "%s", "exactAmount": 60.00},
                    {"userId": "%s", "exactAmount": 30.00}
                  ]
                }
                """.formatted(groupId, payer, payer, payer, participant2);

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BUSINESS_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Sum of exact amounts")));

        // Nothing should have been persisted - the whole request is one transaction
        Integer expenseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expense WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId), Integer.class);
        assertThat(expenseCount).isZero();
    }

    @Test
    void acceptsExactSplitWhenAmountsSumToTotal() throws Exception {
        UUID payer = testDataFactory.createUser("Frank Payer", "frank.exact@test.com");
        UUID participant2 = testDataFactory.createUser("Grace Participant", "grace.exact@test.com");
        UUID groupId = testDataFactory.createGroup("Exact Split Valid Group", payer, List.of(participant2));

        // 60 + 40 = 100, matches totalAmount exactly
        String requestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "title": "Groceries",
                  "totalAmount": 100.00,
                  "currencyCode": "USD",
                  "splitType": "EXACT",
                  "expenseDate": "2026-07-10",
                  "createdByUserId": "%s",
                  "participants": [
                    {"userId": "%s", "exactAmount": 60.00},
                    {"userId": "%s", "exactAmount": 40.00}
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

        assertThat(participant2Owed).isEqualByComparingTo("40.00");
    }
}
