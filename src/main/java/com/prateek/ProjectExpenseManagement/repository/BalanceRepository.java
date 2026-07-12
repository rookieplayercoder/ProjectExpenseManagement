package com.prateek.ProjectExpenseManagement.repository;

import com.prateek.ProjectExpenseManagement.dto.BalanceEntryResponse;
import com.prateek.ProjectExpenseManagement.exception.BusinessValidationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

@Repository
public class BalanceRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BalanceRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BalanceEntryResponse> findBalancesForGroup(UUID groupId) {
        String sql = """
                SELECT ub.debtor_user_id, debtor.full_name AS debtor_name,
                       ub.creditor_user_id, creditor.full_name AS creditor_name,
                       ub.currency_code, ub.net_amount
                FROM user_balance ub
                JOIN app_user debtor ON debtor.id = ub.debtor_user_id
                JOIN app_user creditor ON creditor.id = ub.creditor_user_id
                WHERE ub.group_id = :groupId
                ORDER BY debtor.full_name, creditor.full_name, ub.currency_code
                """;

        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("groupId", groupId),
                (rs, rowNum) -> new BalanceEntryResponse(
                        UUID.fromString(rs.getString("debtor_user_id")),
                        rs.getString("debtor_name"),
                        UUID.fromString(rs.getString("creditor_user_id")),
                        rs.getString("creditor_name"),
                        rs.getString("currency_code"),
                        rs.getBigDecimal("net_amount")
                ));
    }

    public void applyDebt(UUID groupId,
                          UUID debtorUserId,
                          UUID creditorUserId,
                          String currencyCode,
                          BigDecimal deltaAmount,
                          String eventType,
                          UUID eventId) {

        if (debtorUserId.equals(creditorUserId)) {
            return;
        }

        if (deltaAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Delta amount must be greater than zero");
        }

        BalanceRow sameDirection = lockBalanceRow(groupId, debtorUserId, creditorUserId, currencyCode);
        if (sameDirection != null) {
            BigDecimal previous = sameDirection.netAmount();
            BigDecimal updated = previous.add(deltaAmount);

            updateBalanceAmount(sameDirection.id(), updated, eventType, eventId);
            insertHistory(sameDirection.id(), groupId, debtorUserId, creditorUserId, currencyCode,
                    eventType, eventId, previous, deltaAmount, updated);
            return;
        }

        BalanceRow reverseDirection = lockBalanceRow(groupId, creditorUserId, debtorUserId, currencyCode);
        if (reverseDirection != null) {
            BigDecimal previous = reverseDirection.netAmount();
            int compare = previous.compareTo(deltaAmount);

            if (compare > 0) {
                BigDecimal updated = previous.subtract(deltaAmount);
                updateBalanceAmount(reverseDirection.id(), updated, eventType, eventId);
                insertHistory(reverseDirection.id(), groupId, reverseDirection.debtorUserId(),
                        reverseDirection.creditorUserId(), currencyCode,
                        eventType, eventId, previous, deltaAmount.negate(), updated);
            } else if (compare == 0) {
                // insertHistory MUST run before deleteBalance: history.balance_id is a
                // foreign key to user_balance.id, so inserting the history row after the
                // balance is already deleted fails with "Key (balance_id)=... is not
                // present in table user_balance" - the FK has to point at a row that
                // still exists at insert time. ON DELETE SET NULL (see V4 migration)
                // then correctly nulls that reference out once deleteBalance runs.
                insertHistory(reverseDirection.id(), groupId, reverseDirection.debtorUserId(),
                        reverseDirection.creditorUserId(), currencyCode,
                        eventType, eventId, previous, deltaAmount.negate(), BigDecimal.ZERO);
                deleteBalance(reverseDirection.id());
            } else {
                BigDecimal residual = deltaAmount.subtract(previous);
                // Same ordering fix as above: history before delete.
                insertHistory(reverseDirection.id(), groupId, reverseDirection.debtorUserId(),
                        reverseDirection.creditorUserId(), currencyCode,
                        eventType, eventId, previous, deltaAmount.negate(), BigDecimal.ZERO);
                deleteBalance(reverseDirection.id());

                UUID newBalanceId = insertBalance(groupId, debtorUserId, creditorUserId, currencyCode, residual, eventType, eventId);
                insertHistory(newBalanceId, groupId, debtorUserId, creditorUserId, currencyCode,
                        eventType, eventId, BigDecimal.ZERO, residual, residual);
            }
            return;
        }

        UUID newBalanceId = insertBalance(groupId, debtorUserId, creditorUserId, currencyCode, deltaAmount, eventType, eventId);
        insertHistory(newBalanceId, groupId, debtorUserId, creditorUserId, currencyCode,
                eventType, eventId, BigDecimal.ZERO, deltaAmount, deltaAmount);
    }

    private BalanceRow lockBalanceRow(UUID groupId,
                                      UUID debtorUserId,
                                      UUID creditorUserId,
                                      String currencyCode) {
        String sql = """
                SELECT id, group_id, debtor_user_id, creditor_user_id, currency_code, net_amount
                FROM user_balance
                WHERE ((group_id IS NULL AND :groupId IS NULL) OR group_id = :groupId)
                  AND debtor_user_id = :debtorUserId
                  AND creditor_user_id = :creditorUserId
                  AND currency_code = :currencyCode
                FOR UPDATE
                """;

        List<BalanceRow> rows = jdbcTemplate.query(sql,
                new MapSqlParameterSource()
                        .addValue("groupId", groupId, Types.OTHER)
                        .addValue("debtorUserId", debtorUserId)
                        .addValue("creditorUserId", creditorUserId)
                        .addValue("currencyCode", currencyCode.toUpperCase()),
                new BalanceRowMapper());

        return rows.isEmpty() ? null : rows.get(0);
    }

    private UUID insertBalance(UUID groupId,
                               UUID debtorUserId,
                               UUID creditorUserId,
                               String currencyCode,
                               BigDecimal amount,
                               String eventType,
                               UUID eventId) {

        UUID id = UUID.randomUUID();

        String sql = """
                INSERT INTO user_balance (
                    id, group_id, debtor_user_id, creditor_user_id, currency_code,
                    net_amount, last_event_type, last_event_id, updated_at
                ) VALUES (
                    :id, :groupId, :debtorUserId, :creditorUserId, :currencyCode,
                    :netAmount, :eventType, :eventId, NOW()
                )
                """;

        jdbcTemplate.update(sql,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("groupId", groupId, Types.OTHER)
                        .addValue("debtorUserId", debtorUserId)
                        .addValue("creditorUserId", creditorUserId)
                        .addValue("currencyCode", currencyCode.toUpperCase())
                        .addValue("netAmount", amount)
                        .addValue("eventType", eventType)
                        .addValue("eventId", eventId));

        return id;
    }

    private void updateBalanceAmount(UUID balanceId,
                                     BigDecimal updatedAmount,
                                     String eventType,
                                     UUID eventId) {

        String sql = """
                UPDATE user_balance
                SET net_amount = :netAmount,
                    last_event_type = :eventType,
                    last_event_id = :eventId,
                    updated_at = NOW()
                WHERE id = :id
                """;

        jdbcTemplate.update(sql,
                new MapSqlParameterSource()
                        .addValue("id", balanceId)
                        .addValue("netAmount", updatedAmount)
                        .addValue("eventType", eventType)
                        .addValue("eventId", eventId));
    }

    private void deleteBalance(UUID balanceId) {
        String sql = "DELETE FROM user_balance WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", balanceId));
    }

    private void insertHistory(@Nullable UUID balanceId,
                               UUID groupId,
                               UUID debtorUserId,
                               UUID creditorUserId,
                               String currencyCode,
                               String eventType,
                               UUID eventId,
                               BigDecimal previousAmount,
                               BigDecimal deltaAmount,
                               BigDecimal newAmount) {

        String sql = """
                INSERT INTO user_balance_history (
                    id, balance_id, group_id, debtor_user_id, creditor_user_id, currency_code,
                    event_type, event_id, previous_amount, delta_amount, new_amount,
                    event_occurred_at, created_at
                ) VALUES (
                    :id, :balanceId, :groupId, :debtorUserId, :creditorUserId, :currencyCode,
                    :eventType, :eventId, :previousAmount, :deltaAmount, :newAmount,
                    NOW(), NOW()
                )
                """;

        jdbcTemplate.update(sql,
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("balanceId", balanceId, Types.OTHER)
                        .addValue("groupId", groupId, Types.OTHER)
                        .addValue("debtorUserId", debtorUserId)
                        .addValue("creditorUserId", creditorUserId)
                        .addValue("currencyCode", currencyCode.toUpperCase())
                        .addValue("eventType", eventType)
                        .addValue("eventId", eventId)
                        .addValue("previousAmount", previousAmount)
                        .addValue("deltaAmount", deltaAmount)
                        .addValue("newAmount", newAmount));
    }

    private record BalanceRow(
            UUID id,
            UUID groupId,
            UUID debtorUserId,
            UUID creditorUserId,
            String currencyCode,
            BigDecimal netAmount
    ) {
    }

    private static class BalanceRowMapper implements RowMapper<BalanceRow> {
        @Override
        public BalanceRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new BalanceRow(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("group_id") == null ? null : UUID.fromString(rs.getString("group_id")),
                    UUID.fromString(rs.getString("debtor_user_id")),
                    UUID.fromString(rs.getString("creditor_user_id")),
                    rs.getString("currency_code"),
                    rs.getBigDecimal("net_amount")
            );
        }
    }
}