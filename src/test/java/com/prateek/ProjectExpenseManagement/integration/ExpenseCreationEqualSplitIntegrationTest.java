package com.prateek.ProjectExpenseManagement.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.prateek.ProjectExpenseManagement.support.AbstractIntegrationTestBase;
import com.prateek.ProjectExpenseManagement.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpenseCreationEqualSplitIntegrationTest extends AbstractIntegrationTestBase {

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void createsExpenseAndSplitsEquallyAcrossThreeParticipants() throws Exception {
        UUID payer = testDataFactory.createUser("Alice Payer", "alice.equal@test.com");
        UUID participant2 = testDataFactory.createUser("Bob Participant", "bob.equal@test.com");
        UUID participant3 = testDataFactory.createUser("Carol Participant", "carol.equal@test.com");
        UUID groupId = testDataFactory.createGroup("Equal Split Group", payer, List.of(participant2, participant3));

        String requestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "title": "Dinner",
                  "totalAmount": 100.00,
                  "currencyCode": "USD",
                  "splitType": "EQUAL",
                  "expenseDate": "2026-07-10",
                  "createdByUserId": "%s",
                  "participants": [
                    {"userId": "%s"},
                    {"userId": "%s"},
                    {"userId": "%s"}
                  ]
                }
                """.formatted(groupId, payer, payer, payer, participant2, participant3);

        MvcResult result = mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.expenseId").exists())
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID expenseId = UUID.fromString(responseJson.get("expenseId").asText());

        // The expense row itself was persisted with the correct header fields
        Map<String, Object> expenseRow = jdbcTemplate.queryForMap(
                "SELECT total_amount, currency_code, split_type FROM expense WHERE id = :id",
                new MapSqlParameterSource("id", expenseId));
        assertThat(((BigDecimal) expenseRow.get("total_amount"))).isEqualByComparingTo("100.00");
        assertThat(expenseRow.get("currency_code")).isEqualTo("USD");
        assertThat(expenseRow.get("split_type")).isEqualTo("EQUAL");

        // Three participant allocations exist and reconcile exactly to the total,
        // which is the invariant EqualSplitStrategy enforces via its remainder distribution
        List<BigDecimal> owedAmounts = jdbcTemplate.query(
                "SELECT owed_amount FROM expense_participant WHERE expense_id = :id",
                new MapSqlParameterSource("id", expenseId),
                (rs, rowNum) -> rs.getBigDecimal("owed_amount"));

        assertThat(owedAmounts).hasSize(3);
        BigDecimal sum = owedAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("100.00");
        for (BigDecimal owed : owedAmounts) {
            assertThat(owed).isBetween(new BigDecimal("33.3300"), new BigDecimal("33.3400"));
        }

        // Balance ledger: the payer owes nothing to themselves, so only the two
        // non-payer participants should have a balance row, each owing the payer
        List<Map<String, Object>> balanceRows = jdbcTemplate.queryForList(
                """
                SELECT debtor_user_id::text AS debtor_user_id,
                       creditor_user_id::text AS creditor_user_id,
                       net_amount
                FROM user_balance
                WHERE group_id = :groupId
                """,
                new MapSqlParameterSource("groupId", groupId));

        assertThat(balanceRows).hasSize(2);
        for (Map<String, Object> row : balanceRows) {
            assertThat(row.get("creditor_user_id")).isEqualTo(payer.toString());
            assertThat(row.get("debtor_user_id"))
                    .isIn(participant2.toString(), participant3.toString());
        }
    }
}
