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

class PartialSettlementIntegrationTest extends AbstractIntegrationTestBase {

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void partiallySettlingADebtReducesTheBalanceWithoutFlippingDirection() throws Exception {
        UUID payer = testDataFactory.createUser("Priya Payer", "priya.partial@test.com");
        UUID debtor = testDataFactory.createUser("Quinn Debtor", "quinn.partial@test.com");
        UUID groupId = testDataFactory.createGroup("Partial Settlement Group", payer, List.of(debtor));

        // Priya pays 100.00, split equally two ways -> Quinn owes Priya exactly 50.00
        String expenseRequestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "title": "Trip",
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

        // Quinn pays Priya only 20.00 of the 50.00 owed
        String settleRequestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "paidToUserId": "%s",
                  "amount": 20.00,
                  "currencyCode": "USD",
                  "settlementDate": "2026-07-11",
                  "createdByUserId": "%s"
                }
                """.formatted(groupId, debtor, payer, debtor);

        mockMvc.perform(post("/api/v1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settleRequestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // The balance row survives (not deleted) with the reduced amount, same direction:
        // Quinn still owes Priya, just less. This is applyDebt's compare > 0 branch.
        List<Map<String, Object>> balancesAfterPartialSettlement = jdbcTemplate.queryForList(
                "SELECT debtor_user_id::text AS debtor_user_id, creditor_user_id::text AS creditor_user_id, net_amount " +
                        "FROM user_balance WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId));

        assertThat(balancesAfterPartialSettlement).hasSize(1);
        Map<String, Object> balance = balancesAfterPartialSettlement.get(0);
        assertThat(balance.get("debtor_user_id")).isEqualTo(debtor.toString());
        assertThat(balance.get("creditor_user_id")).isEqualTo(payer.toString());
        assertThat((BigDecimal) balance.get("net_amount")).isEqualByComparingTo("30.00");

        // A second partial payment of the remaining 30.00 should now fully settle it
        String secondSettleRequestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "paidToUserId": "%s",
                  "amount": 30.00,
                  "currencyCode": "USD",
                  "settlementDate": "2026-07-12",
                  "createdByUserId": "%s"
                }
                """.formatted(groupId, debtor, payer, debtor);

        mockMvc.perform(post("/api/v1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondSettleRequestBody))
                .andExpect(status().isCreated());

        List<Map<String, Object>> balancesAfterFullSettlement = jdbcTemplate.queryForList(
                "SELECT * FROM user_balance WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId));
        assertThat(balancesAfterFullSettlement).isEmpty();

        // Both settlements were recorded independently
        Integer settlementCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId), Integer.class);
        assertThat(settlementCount).isEqualTo(2);
    }
}
