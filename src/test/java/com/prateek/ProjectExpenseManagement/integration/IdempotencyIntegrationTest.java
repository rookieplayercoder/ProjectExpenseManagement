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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the Idempotency-Key header actually prevents duplicate side effects
 * on POST /expenses and POST /settlements, rather than just exercising the
 * "happy path" once. Covers: same key returns the same resource id, the
 * balance ledger isn't double-applied, and concurrent requests racing on the
 * same key only let one of them through.
 */
class IdempotencyIntegrationTest extends AbstractIntegrationTestBase {

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void repeatedExpenseRequestWithSameKeyIsNotAppliedTwice() throws Exception {
        UUID payer = testDataFactory.createUser("Priya Payer", "priya.idem@test.com");
        UUID debtor = testDataFactory.createUser("Dev Debtor", "dev.idem@test.com");
        UUID groupId = testDataFactory.createGroup("Idempotency Expense Group", payer, List.of(debtor));

        String requestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "title": "Groceries",
                  "totalAmount": 80.00,
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

        String idempotencyKey = "expense-key-" + UUID.randomUUID();

        MvcResult firstResult = mockMvc.perform(post("/api/v1/expenses")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult secondResult = mockMvc.perform(post("/api/v1/expenses")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        UUID firstExpenseId = extractId(firstResult, "expenseId");
        UUID secondExpenseId = extractId(secondResult, "expenseId");

        // Same key -> same resource, second call is a no-op replay
        assertThat(secondExpenseId).isEqualTo(firstExpenseId);

        Integer expenseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expense WHERE id = :id",
                new MapSqlParameterSource("id", firstExpenseId), Integer.class);
        assertThat(expenseCount).isEqualTo(1);

        // The balance must reflect ONE 80.00 expense split in half (40.00),
        // not two (which would be 80.00)
        List<Map<String, Object>> balanceRows = jdbcTemplate.queryForList(
                "SELECT net_amount FROM user_balance WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId));
        assertThat(balanceRows).hasSize(1);
        assertThat((BigDecimal) balanceRows.get(0).get("net_amount")).isEqualByComparingTo("40.00");
    }

    @Test
    void repeatedSettlementRequestWithSameKeyIsNotAppliedTwice() throws Exception {
        UUID payer = testDataFactory.createUser("Sana Payer", "sana.idem@test.com");
        UUID debtor = testDataFactory.createUser("Tariq Debtor", "tariq.idem@test.com");
        UUID groupId = testDataFactory.createGroup("Idempotency Settlement Group", payer, List.of(debtor));

        String expenseRequestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "title": "Utilities",
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

        // Debtor owes 50.00; settle it partially (20.00) twice with the same key
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

        String idempotencyKey = "settlement-key-" + UUID.randomUUID();

        MvcResult firstResult = mockMvc.perform(post("/api/v1/settlements")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settleRequestBody))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult secondResult = mockMvc.perform(post("/api/v1/settlements")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settleRequestBody))
                .andExpect(status().isCreated())
                .andReturn();

        UUID firstSettlementId = extractId(firstResult, "settlementId");
        UUID secondSettlementId = extractId(secondResult, "settlementId");
        assertThat(secondSettlementId).isEqualTo(firstSettlementId);

        Integer settlementCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId), Integer.class);
        assertThat(settlementCount).isEqualTo(1);

        // One 20.00 settlement against a 50.00 debt -> 30.00 remaining, not 10.00
        List<Map<String, Object>> balanceRows = jdbcTemplate.queryForList(
                "SELECT net_amount FROM user_balance WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId));
        assertThat(balanceRows).hasSize(1);
        assertThat((BigDecimal) balanceRows.get(0).get("net_amount")).isEqualByComparingTo("30.00");
    }

    @Test
    void concurrentRequestsWithSameKeyOnlyCreateOneExpense() throws Exception {
        UUID payer = testDataFactory.createUser("Concurrent Payer", "payer.concurrent@test.com");
        UUID debtor = testDataFactory.createUser("Concurrent Debtor", "debtor.concurrent@test.com");
        UUID groupId = testDataFactory.createGroup("Concurrency Group", payer, List.of(debtor));

        String requestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "title": "Concurrent Expense",
                  "totalAmount": 60.00,
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

        String idempotencyKey = "concurrent-key-" + UUID.randomUUID();
        int concurrentRequests = 5;

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        try {
            List<Callable<Integer>> tasks = IntStream.range(0, concurrentRequests)
                    .<Callable<Integer>>mapToObj(i -> () -> mockMvc.perform(post("/api/v1/expenses")
                                    .header("Idempotency-Key", idempotencyKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody))
                            .andReturn().getResponse().getStatus())
                    .toList();

            List<Future<Integer>> futures = executor.invokeAll(tasks);
            for (Future<Integer> future : futures) {
                // Either the request created the expense (201) or hit the
                // "still processing" retry response (400) - never a 500,
                // and crucially, never more than one row ends up persisted.
                assertThat(future.get()).isIn(201, 400);
            }
        } finally {
            executor.shutdown();
        }

        Integer expenseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expense WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId), Integer.class);
        assertThat(expenseCount).isEqualTo(1);
    }

    private UUID extractId(MvcResult result, String fieldName) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(json.get(fieldName).asText());
    }
}
