package com.prateek.ProjectExpenseManagement.integration;

import com.prateek.ProjectExpenseManagement.support.AbstractIntegrationTestBase;
import com.prateek.ProjectExpenseManagement.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettlementIntegrationTest extends AbstractIntegrationTestBase {

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void fullySettlingAnExactDebtRemovesTheBalanceRow() throws Exception {
        UUID payer = testDataFactory.createUser("Nora Payer", "nora.settle@test.com");
        UUID debtor = testDataFactory.createUser("Omar Debtor", "omar.settle@test.com");
        UUID groupId = testDataFactory.createGroup("Settlement Group", payer, List.of(debtor));

        // Nora pays 100.00, split equally two ways -> Omar owes Nora exactly 50.00
        String expenseRequestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "title": "Rent",
                  "totalAmount": 100.00,
                  "currencyCode": "USD",
                  "splitType": "EQUAL",
                  "expenseDate": "2026-07-10",
                  "createdByUserId": "%s",
                  "participants": [
                    {"userId": "%s"},
                    {"userId": "%s"}
                  ]
                }
                """.formatted(groupId, payer, payer, payer, debtor);

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseRequestBody))
                .andExpect(status().isCreated());

        // Sanity check: exactly one balance row exists, Omar owes Nora 50.00, before settling
        List<Map<String, Object>> balancesBeforeSettlement = jdbcTemplate.queryForList(
                "SELECT debtor_user_id::text AS debtor_user_id, creditor_user_id::text AS creditor_user_id, net_amount " +
                        "FROM user_balance WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId));
        assertThat(balancesBeforeSettlement).hasSize(1);
        assertThat(balancesBeforeSettlement.get(0).get("debtor_user_id")).isEqualTo(debtor.toString());
        assertThat(balancesBeforeSettlement.get(0).get("creditor_user_id")).isEqualTo(payer.toString());
        assertThat((BigDecimal) balancesBeforeSettlement.get(0).get("net_amount")).isEqualByComparingTo("50.00");

        // Omar pays Nora the full 50.00 he owes
        String settleRequestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "paidToUserId": "%s",
                  "amount": 50.00,
                  "currencyCode": "USD",
                  "settlementDate": "2026-07-11",
                  "createdByUserId": "%s"
                }
                """.formatted(groupId, debtor, payer, debtor);

        mockMvc.perform(post("/api/v1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settleRequestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.settlementId").exists());

        // A fully-paid balance is deleted, not left at zero - that's applyDebt's
        // documented behavior for the compare == 0 branch
        List<Map<String, Object>> balancesAfterSettlement = jdbcTemplate.queryForList(
                "SELECT * FROM user_balance WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId));
        assertThat(balancesAfterSettlement).isEmpty();

        // The settlement itself was recorded
        Integer settlementCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement WHERE group_id = :groupId " +
                        "AND paid_by_user_id = :debtor AND paid_to_user_id = :payer",
                new MapSqlParameterSource()
                        .addValue("groupId", groupId)
                        .addValue("debtor", debtor)
                        .addValue("payer", payer),
                Integer.class);
        assertThat(settlementCount).isEqualTo(1);

        // And the ledger history retains a record of how the balance reached zero
        Integer historyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_balance_history " +
                        "WHERE group_id = :groupId AND event_type = 'SETTLEMENT' AND new_amount = 0",
                new MapSqlParameterSource("groupId", groupId),
                Integer.class);
        assertThat(historyCount).isEqualTo(1);
    }
}
