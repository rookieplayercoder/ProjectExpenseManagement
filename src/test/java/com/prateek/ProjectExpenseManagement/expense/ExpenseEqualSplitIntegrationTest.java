package com.prateek.ProjectExpenseManagement.expense;

import com.prateek.ProjectExpenseManagement.domain.SplitType;
import com.prateek.ProjectExpenseManagement.dto.CreateExpenseRequest;
import com.prateek.ProjectExpenseManagement.dto.CreateExpenseResponse;
import com.prateek.ProjectExpenseManagement.dto.ParticipantShareRequest;
import com.prateek.ProjectExpenseManagement.support.AbstractIntegrationTestBase;
import com.prateek.ProjectExpenseManagement.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpenseEqualSplitIntegrationTest extends AbstractIntegrationTestBase {

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void createExpense_equalSplit_distributesRemainderAndUpdatesBalances() throws Exception {
        UUID payer = testDataFactory.createUser("Alice Payer", uniqueEmail("alice"));
        UUID participant1 = testDataFactory.createUser("Bob One", uniqueEmail("bob"));
        UUID participant2 = testDataFactory.createUser("Carol Two", uniqueEmail("carol"));
        UUID groupId = testDataFactory.createGroup("Goa Trip", payer, List.of(participant1, participant2));

        CreateExpenseRequest request = buildEqualSplitRequest(
                groupId, payer, new BigDecimal("100.00"), List.of(payer, participant1, participant2));

        MvcResult result = mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andReturn();

        CreateExpenseResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), CreateExpenseResponse.class);
        UUID expenseId = response.getExpenseId();
        assertThat(expenseId).isNotNull();

        // 100.00 / 3 participants: base share 33.3333, remainder 0.0001 goes to exactly one participant
        List<BigDecimal> owedAmounts = jdbcTemplate.query(
                "SELECT owed_amount FROM expense_participant WHERE expense_id = :id ORDER BY owed_amount",
                new MapSqlParameterSource("id", expenseId),
                (rs, rowNum) -> rs.getBigDecimal("owed_amount"));

        assertThat(owedAmounts).hasSize(3);
        BigDecimal totalAllocated = owedAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalAllocated).isEqualByComparingTo("100.00");
        assertThat(owedAmounts.get(0)).isEqualByComparingTo("33.3333");
        assertThat(owedAmounts.get(1)).isEqualByComparingTo("33.3333");
        assertThat(owedAmounts.get(2)).isEqualByComparingTo("33.3334");

        // Payer's own share never becomes a balance - only the other two participants
        // should owe the payer anything.
        List<UUID> debtors = jdbcTemplate.query(
                """
                SELECT debtor_user_id FROM user_balance
                WHERE group_id = :groupId AND creditor_user_id = :payer
                """,
                new MapSqlParameterSource().addValue("groupId", groupId).addValue("payer", payer),
                (rs, rowNum) -> UUID.fromString(rs.getString("debtor_user_id")));

        assertThat(debtors).containsExactlyInAnyOrder(participant1, participant2);

        BigDecimal participant1OwedAmount = jdbcTemplate.queryForObject(
                "SELECT owed_amount FROM expense_participant WHERE expense_id = :id AND user_id = :userId",
                new MapSqlParameterSource().addValue("id", expenseId).addValue("userId", participant1),
                BigDecimal.class);

        BigDecimal participant1Balance = jdbcTemplate.queryForObject(
                """
                SELECT net_amount FROM user_balance
                WHERE group_id = :groupId AND debtor_user_id = :debtor AND creditor_user_id = :creditor
                """,
                new MapSqlParameterSource()
                        .addValue("groupId", groupId)
                        .addValue("debtor", participant1)
                        .addValue("creditor", payer),
                BigDecimal.class);

        // A participant's balance toward the payer must exactly equal their own allocated
        // share - regardless of which of the three participants absorbed the 0.0001
        // rounding remainder (that assignment depends on UUID sort order inside the
        // strategy, so it isn't something this test should assume).
        assertThat(participant1Balance).isEqualByComparingTo(participant1OwedAmount);
    }

    private CreateExpenseRequest buildEqualSplitRequest(UUID groupId, UUID payer, BigDecimal totalAmount,
                                                         List<UUID> participantIds) {
        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setGroupId(groupId);
        request.setPaidByUserId(payer);
        request.setCreatedByUserId(payer);
        request.setTitle("Dinner");
        request.setDescription("Team dinner in Goa");
        request.setTotalAmount(totalAmount);
        request.setCurrencyCode("USD");
        request.setSplitType(SplitType.EQUAL);
        request.setExpenseDate(LocalDate.now());
        request.setParticipants(participantIds.stream().map(this::shareOf).toList());
        return request;
    }

    private ParticipantShareRequest shareOf(UUID userId) {
        ParticipantShareRequest share = new ParticipantShareRequest();
        share.setUserId(userId);
        return share;
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@test.example.com";
    }
}
