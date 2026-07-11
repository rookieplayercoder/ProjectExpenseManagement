package com.prateek.ProjectExpenseManagement.repository;

import com.prateek.ProjectExpenseManagement.dto.SettleBalanceRequest;
import com.prateek.ProjectExpenseManagement.dto.SettlementSummaryResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class SettlementRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SettlementRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SettlementSummaryResponse> findByGroupId(UUID groupId) {
        String sql = """
                SELECT s.id, s.paid_by_user_id, payer.full_name AS paid_by_name,
                       s.paid_to_user_id, payee.full_name AS paid_to_name,
                       s.amount, s.currency_code, s.settlement_date, s.note
                FROM settlement s
                JOIN app_user payer ON payer.id = s.paid_by_user_id
                JOIN app_user payee ON payee.id = s.paid_to_user_id
                WHERE s.group_id = :groupId
                ORDER BY s.settlement_date DESC, s.created_at DESC
                """;

        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("groupId", groupId),
                (rs, rowNum) -> new SettlementSummaryResponse(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("paid_by_user_id")),
                        rs.getString("paid_by_name"),
                        UUID.fromString(rs.getString("paid_to_user_id")),
                        rs.getString("paid_to_name"),
                        rs.getBigDecimal("amount"),
                        rs.getString("currency_code"),
                        rs.getObject("settlement_date", LocalDate.class),
                        rs.getString("note")
                ));
    }

    public UUID insertSettlement(SettleBalanceRequest request) {
        UUID settlementId = UUID.randomUUID();

        String sql = """
                INSERT INTO settlement (
                    id, group_id, paid_by_user_id, paid_to_user_id, amount, currency_code,
                    settlement_date, note, created_by, created_at
                ) VALUES (
                    :id, :groupId, :paidByUserId, :paidToUserId, :amount, :currencyCode,
                    :settlementDate, :note, :createdBy, NOW()
                )
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", settlementId)
                .addValue("groupId", request.getGroupId(), Types.OTHER)
                .addValue("paidByUserId", request.getPaidByUserId())
                .addValue("paidToUserId", request.getPaidToUserId())
                .addValue("amount", request.getAmount())
                .addValue("currencyCode", request.getCurrencyCode().toUpperCase())
                .addValue("settlementDate", request.getSettlementDate())
                .addValue("note", request.getNote())
                .addValue("createdBy", request.getCreatedByUserId());

        jdbcTemplate.update(sql, params);
        return settlementId;
    }
}
