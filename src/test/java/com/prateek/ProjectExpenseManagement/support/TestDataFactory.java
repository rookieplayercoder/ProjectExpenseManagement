package com.prateek.ProjectExpenseManagement.support;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Seeds users/groups directly via JDBC rather than through the HTTP layer.
 * Keeps fixture setup fast and decoupled from the create-user/create-group
 * endpoints under test elsewhere, so a bug in those endpoints doesn't cause
 * unrelated tests (e.g. expense creation) to fail for the wrong reason.
 */
@Component
public class TestDataFactory {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TestDataFactory(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID createUser(String fullName, String email) {
        UUID userId = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO app_user (id, email, full_name, is_active, created_at, updated_at)
                VALUES (:id, :email, :fullName, TRUE, NOW(), NOW())
                """,
                new MapSqlParameterSource()
                        .addValue("id", userId)
                        .addValue("email", email)
                        .addValue("fullName", fullName));

        return userId;
    }

    public UUID createGroup(String groupName, UUID createdByUserId, List<UUID> memberUserIds) {
        UUID groupId = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO expense_group (id, group_name, created_by, is_active, created_at, updated_at)
                VALUES (:id, :groupName, :createdBy, TRUE, NOW(), NOW())
                """,
                new MapSqlParameterSource()
                        .addValue("id", groupId)
                        .addValue("groupName", groupName)
                        .addValue("createdBy", createdByUserId));

        addMember(groupId, createdByUserId);
        for (UUID memberId : memberUserIds) {
            if (!memberId.equals(createdByUserId)) {
                addMember(groupId, memberId);
            }
        }

        return groupId;
    }

    public void addMember(UUID groupId, UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO expense_group_member (group_id, user_id, is_active, joined_at)
                VALUES (:groupId, :userId, TRUE, NOW())
                ON CONFLICT DO NOTHING
                """,
                new MapSqlParameterSource()
                        .addValue("groupId", groupId)
                        .addValue("userId", userId));
    }
}
