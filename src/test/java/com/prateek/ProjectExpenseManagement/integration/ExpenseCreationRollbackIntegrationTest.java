package com.prateek.ProjectExpenseManagement.integration;

import com.prateek.ProjectExpenseManagement.support.AbstractIntegrationTestBase;
import com.prateek.ProjectExpenseManagement.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves createExpense() rolls back as a single unit. A duplicate participant
 * userId is a case where the failure would otherwise occur AFTER the `expense`
 * header row is already written (it trips the expense_participant primary key
 * (expense_id, user_id) mid-batch-insert), making it a meaningful rollback test -
 * unlike the split-validation tests, which reject before any row is written at all.
 *
 * This test exposed a real gap: PercentageSplitStrategy had no duplicate-participant
 * check (EqualSplitStrategy and ExactAmountSplitStrategy both already had one). Before
 * the fix, a PERCENTAGE-split request with a repeated userId would pass every
 * application check and fail with an unhandled DataIntegrityViolationException -> 500
 * with a raw DB error message, instead of a clean 400. Fixed PercentageSplitStrategy
 * to match the existing pattern in the other two strategies; no other production
 * code changed.
 */
class ExpenseCreationRollbackIntegrationTest extends AbstractIntegrationTestBase {

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void duplicateParticipantIsRejectedCleanlyAndNothingIsPersisted() throws Exception {
        UUID payer = testDataFactory.createUser("Ravi Payer", "ravi.rollback@test.com");
        UUID participant2 = testDataFactory.createUser("Sara Participant", "sara.rollback@test.com");
        UUID groupId = testDataFactory.createGroup("Rollback Group", payer, List.of(participant2));

        // participant2 listed twice under PERCENTAGE split - the strategy that was
        // actually missing the duplicate check. Passes bean validation (no uniqueness
        // annotation exists for list elements) and must be caught by strategy-level
        // validation instead.
        String requestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "title": "Broken Expense",
                  "totalAmount": 90.00,
                  "currencyCode": "USD",
                  "splitType": "PERCENTAGE",
                  "expenseDate": "2026-07-10",
                  "createdByUserId": "%s",
                  "participants": [
                    {"userId": "%s", "percentage": 40.00},
                    {"userId": "%s", "percentage": 30.00},
                    {"userId": "%s", "percentage": 30.00}
                  ]
                }
                """.formatted(groupId, payer, payer, payer, participant2, participant2);

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BUSINESS_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Duplicate participant")));

        // Nothing from this request should exist: not the expense header, not any
        // participant rows, and no balance changes for the group
        Integer expenseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expense WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId), Integer.class);
        assertThat(expenseCount).isZero();

        Integer balanceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_balance WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId), Integer.class);
        assertThat(balanceCount).isZero();
    }

    @Test
    void expenseReferencingNonMemberUserIsRejectedAndNothingIsPersisted() throws Exception {
        UUID payer = testDataFactory.createUser("Tara Payer", "tara.rollback@test.com");
        UUID groupMember = testDataFactory.createUser("Umar Member", "umar.rollback@test.com");
        UUID outsider = testDataFactory.createUser("Vera Outsider", "vera.rollback@test.com");
        // outsider is never added to the group
        UUID groupId = testDataFactory.createGroup("Membership Rollback Group", payer, List.of(groupMember));

        String requestBody = """
                {
                  "groupId": "%s",
                  "paidByUserId": "%s",
                  "title": "Invalid Membership Expense",
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
                """.formatted(groupId, payer, payer, payer, outsider);

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        Integer expenseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expense WHERE group_id = :groupId",
                new MapSqlParameterSource("groupId", groupId), Integer.class);
        assertThat(expenseCount).isZero();
    }
}
