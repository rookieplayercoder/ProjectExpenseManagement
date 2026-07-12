package com.prateek.ProjectExpenseManagement.repository;

import com.prateek.ProjectExpenseManagement.dto.CreateGroupRequest;
import com.prateek.ProjectExpenseManagement.dto.GroupSummaryResponse;
import com.prateek.ProjectExpenseManagement.exception.BusinessValidationException;
import com.prateek.ProjectExpenseManagement.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class GroupRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GroupRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID insertGroup(CreateGroupRequest request) {
        UUID groupId = UUID.randomUUID();

        String sql = """
                INSERT INTO expense_group (id, group_name, description, created_by, is_active, created_at, updated_at)
                VALUES (:id, :groupName, :description, :createdBy, TRUE, NOW(), NOW())
                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", groupId)
                .addValue("groupName", request.getGroupName())
                .addValue("description", request.getDescription())
                .addValue("createdBy", request.getCreatedByUserId()));

        return groupId;
    }

    public void addMembers(UUID groupId, List<UUID> userIds) {
        String sql = """
                INSERT INTO expense_group_member (group_id, user_id, is_active, joined_at)
                VALUES (:groupId, :userId, TRUE, NOW())
                """;

        SqlParameterBatchBuilder.batchUpdate(jdbcTemplate, sql, userIds, userId ->
                new MapSqlParameterSource()
                        .addValue("groupId", groupId)
                        .addValue("userId", userId)
        );
    }

    /**
     * All active groups the given user is an active member of, most recently
     * created first, each annotated with its current active member count.
     */
    public List<GroupSummaryResponse> findGroupsForUser(UUID userId) {
        String sql = """
                SELECT g.id, g.group_name, g.description, g.created_by, g.created_at,
                       (SELECT COUNT(1)
                          FROM expense_group_member m2
                         WHERE m2.group_id = g.id
                           AND m2.is_active = TRUE) AS member_count
                FROM expense_group g
                JOIN expense_group_member m ON m.group_id = g.id
                WHERE m.user_id = :userId
                  AND m.is_active = TRUE
                  AND g.is_active = TRUE
                ORDER BY g.created_at DESC
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> new GroupSummaryResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("group_name"),
                        rs.getString("description"),
                        UUID.fromString(rs.getString("created_by")),
                        rs.getInt("member_count"),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
    }

    public void assertGroupExists(UUID groupId) {
        if (groupId == null) {
            return;
        }

        String sql = "SELECT COUNT(1) FROM expense_group WHERE id = :groupId AND is_active = TRUE";
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("groupId", groupId), Integer.class);

        if (count == null || count == 0) {
            throw new ResourceNotFoundException("Expense group not found: " + groupId);
        }
    }

    public void assertUsersBelongToGroup(UUID groupId, List<UUID> userIds) {
        if (groupId == null) {
            return;
        }

        String sql = """
                SELECT user_id
                FROM expense_group_member
                WHERE group_id = :groupId
                  AND is_active = TRUE
                  AND user_id IN (:userIds)
                """;

        List<UUID> matched = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("groupId", groupId)
                        .addValue("userIds", userIds),
                (rs, rowNum) -> UUID.fromString(rs.getString("user_id"))
        );

        if (matched.size() != userIds.size()) {
            throw new BusinessValidationException("All users must belong to the specified group");
        }
    }
}
