package com.prateek.ProjectExpenseManagement.repository;

import com.prateek.ProjectExpenseManagement.domain.SplitAllocation;
import com.prateek.ProjectExpenseManagement.domain.SplitType;
import com.prateek.ProjectExpenseManagement.dto.CreateExpenseRequest;
import com.prateek.ProjectExpenseManagement.dto.ExpenseDetailResponse;
import com.prateek.ProjectExpenseManagement.dto.ExpenseParticipantResponse;
import com.prateek.ProjectExpenseManagement.dto.ExpenseSummaryResponse;
import com.prateek.ProjectExpenseManagement.dto.UpdateExpenseRequest;
import com.prateek.ProjectExpenseManagement.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class ExpenseRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ExpenseRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ExpenseDetailResponse findExpenseById(UUID expenseId) {
        String sql = """
                SELECT e.id, e.group_id, e.paid_by_user_id, payer.full_name AS paid_by_name,
                       e.title, e.description, e.total_amount, e.currency_code, e.split_type,
                       e.expense_date, e.created_by, e.created_at
                FROM expense e
                JOIN app_user payer ON payer.id = e.paid_by_user_id
                WHERE e.id = :expenseId
                """;

        List<ExpenseDetailResponse> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("expenseId", expenseId),
                (rs, rowNum) -> new ExpenseDetailResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("group_id") == null ? null : UUID.fromString(rs.getString("group_id")),
                        UUID.fromString(rs.getString("paid_by_user_id")),
                        rs.getString("paid_by_name"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("currency_code"),
                        SplitType.valueOf(rs.getString("split_type")),
                        rs.getObject("expense_date", LocalDate.class),
                        UUID.fromString(rs.getString("created_by")),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                        findParticipants(expenseId)
                ));

        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Expense not found: " + expenseId);
        }
        return results.get(0);
    }

    public List<ExpenseParticipantResponse> findParticipants(UUID expenseId) {
        String sql = """
                SELECT ep.user_id, u.full_name, ep.owed_amount, ep.percentage_value, ep.exact_amount_input
                FROM expense_participant ep
                JOIN app_user u ON u.id = ep.user_id
                WHERE ep.expense_id = :expenseId
                ORDER BY u.full_name
                """;

        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("expenseId", expenseId),
                (rs, rowNum) -> new ExpenseParticipantResponse(
                        UUID.fromString(rs.getString("user_id")),
                        rs.getString("full_name"),
                        rs.getBigDecimal("owed_amount"),
                        rs.getBigDecimal("percentage_value"),
                        rs.getBigDecimal("exact_amount_input")
                ));
    }

    public List<ExpenseSummaryResponse> findExpensesByGroupId(UUID groupId) {
        String sql = """
                SELECT e.id, e.title, e.total_amount, e.currency_code, e.split_type,
                       e.paid_by_user_id, payer.full_name AS paid_by_name, e.expense_date
                FROM expense e
                JOIN app_user payer ON payer.id = e.paid_by_user_id
                WHERE e.group_id = :groupId
                ORDER BY e.expense_date DESC, e.created_at DESC
                """;

        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("groupId", groupId),
                (rs, rowNum) -> new ExpenseSummaryResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("title"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("currency_code"),
                        SplitType.valueOf(rs.getString("split_type")),
                        UUID.fromString(rs.getString("paid_by_user_id")),
                        rs.getString("paid_by_name"),
                        rs.getObject("expense_date", LocalDate.class)
                ));
    }

    public UUID insertExpense(CreateExpenseRequest request) {
        UUID expenseId = UUID.randomUUID();

        String sql = """
                INSERT INTO expense (
                    id, group_id, paid_by_user_id, title, description, total_amount, currency_code,
                    split_type, expense_date, created_by, created_at
                ) VALUES (
                    :id, :groupId, :paidByUserId, :title, :description, :totalAmount, :currencyCode,
                    :splitType, :expenseDate, :createdBy, NOW()
                )
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", expenseId)
                .addValue("groupId", request.getGroupId(), Types.OTHER)
                .addValue("paidByUserId", request.getPaidByUserId())
                .addValue("title", request.getTitle())
                .addValue("description", request.getDescription())
                .addValue("totalAmount", request.getTotalAmount())
                .addValue("currencyCode", request.getCurrencyCode().toUpperCase())
                .addValue("splitType", request.getSplitType().name())
                .addValue("expenseDate", request.getExpenseDate())
                .addValue("createdBy", request.getCreatedByUserId());

        jdbcTemplate.update(sql, params);
        return expenseId;
    }

    public void updateExpense(UUID expenseId, UpdateExpenseRequest request) {
        String sql = """
                UPDATE expense
                SET paid_by_user_id = :paidByUserId,
                    title = :title,
                    description = :description,
                    total_amount = :totalAmount,
                    currency_code = :currencyCode,
                    split_type = :splitType,
                    expense_date = :expenseDate
                WHERE id = :id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", expenseId)
                .addValue("paidByUserId", request.getPaidByUserId())
                .addValue("title", request.getTitle())
                .addValue("description", request.getDescription())
                .addValue("totalAmount", request.getTotalAmount())
                .addValue("currencyCode", request.getCurrencyCode().toUpperCase())
                .addValue("splitType", request.getSplitType().name())
                .addValue("expenseDate", request.getExpenseDate());

        jdbcTemplate.update(sql, params);
    }

    public void deleteParticipants(UUID expenseId) {
        String sql = "DELETE FROM expense_participant WHERE expense_id = :expenseId";
        jdbcTemplate.update(sql, new MapSqlParameterSource("expenseId", expenseId));
    }

    public void deleteExpense(UUID expenseId) {
        // expense_participant rows cascade on delete (fk_expense_participant_expense).
        String sql = "DELETE FROM expense WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", expenseId));
    }

    public void batchInsertParticipants(UUID expenseId, List<SplitAllocation> allocations) {
        String sql = """
                INSERT INTO expense_participant (
                    expense_id, user_id, owed_amount, percentage_value, exact_amount_input, created_at
                ) VALUES (
                    :expenseId, :userId, :owedAmount, :percentageValue, :exactAmountInput, NOW()
                )
                """;

        SqlParameterBatchBuilder.batchUpdate(jdbcTemplate, sql, allocations, allocation ->
                new MapSqlParameterSource()
                        .addValue("expenseId", expenseId)
                        .addValue("userId", allocation.getUserId())
                        .addValue("owedAmount", allocation.getOwedAmount())
                        .addValue("percentageValue", allocation.getPercentageValue())
                        .addValue("exactAmountInput", allocation.getExactAmountInput())
        );
    }
}
