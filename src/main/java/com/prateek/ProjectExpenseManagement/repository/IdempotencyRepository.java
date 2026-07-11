package com.prateek.ProjectExpenseManagement.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.UUID;

@Repository
public class IdempotencyRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public IdempotencyRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Attempts to atomically claim an idempotency key for a given request type.
     * Returns true if this call claimed the key (caller should proceed with the operation).
     * Returns false if the key was already claimed by an earlier request (caller should
     * look up the original result via {@link #findReferenceId}).
     *
     * Relies on the unique constraint on (idempotency_key, request_type): concurrent callers
     * racing on the same key will serialize on the underlying index, so at most one insert wins.
     */
    public boolean reserveKey(String idempotencyKey, String requestType) {
        String sql = """
                INSERT INTO idempotency_record (id, idempotency_key, request_type, reference_id, created_at)
                VALUES (:id, :idempotencyKey, :requestType, NULL, NOW())
                ON CONFLICT (idempotency_key, request_type) DO NOTHING
                """;

        int rowsAffected = jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("idempotencyKey", idempotencyKey)
                .addValue("requestType", requestType));

        return rowsAffected == 1;
    }

    public void attachReferenceId(String idempotencyKey, String requestType, UUID referenceId) {
        String sql = """
                UPDATE idempotency_record
                SET reference_id = :referenceId
                WHERE idempotency_key = :idempotencyKey
                  AND request_type = :requestType
                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("referenceId", referenceId, Types.OTHER)
                .addValue("idempotencyKey", idempotencyKey)
                .addValue("requestType", requestType));
    }

    public UUID findReferenceId(String idempotencyKey, String requestType) {
        String sql = """
                SELECT reference_id FROM idempotency_record
                WHERE idempotency_key = :idempotencyKey
                  AND request_type = :requestType
                """;

        List<UUID> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource()
                        .addValue("idempotencyKey", idempotencyKey)
                        .addValue("requestType", requestType),
                (rs, rowNum) -> {
                    String referenceId = rs.getString("reference_id");
                    return referenceId == null ? null : UUID.fromString(referenceId);
                });

        return results.isEmpty() ? null : results.get(0);
    }
}
