package com.prateek.ProjectExpenseManagement.repository;

import com.prateek.ProjectExpenseManagement.dto.CreateGroupRequest;
import com.prateek.ProjectExpenseManagement.dto.GroupDetailResponse;
import com.prateek.ProjectExpenseManagement.dto.GroupMemberResponse;
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

    public GroupDetailResponse findGroupDetail(UUID groupId) {
        String headerSql = """
                SELECT id, group_name, description, created_by, created_at
                FROM expense_group
                WHERE id = :groupId AND is_active = TRUE
                """;

        List<GroupDetailResponse> headerResults = jdbcTemplate.query(headerSql,
                new MapSqlParameterSource("groupId", groupId),
                (rs, rowNum) -> new GroupDetailResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("group_name"),
                        rs.getString("description"),
                        UUID.fromString(rs.getString("created_by")),
                        rs.getTimestamp("created_at").toInstant(),
                        findMembers(groupId)
                ));

        if (headerResults.isEmpty()) {
            throw new ResourceNotFoundException("Expense group not found: " + groupId);
        }
        return headerResults.get(0);
    }

    public List<GroupMemberResponse> findMembers(UUID groupId) {
        String sql = """
                SELECT u.id, u.full_name, u.email, m.joined_at
                FROM expense_group_member m
                JOIN app_user u ON u.id = m.user_id
                WHERE m.group_id = :groupId
                  AND m.is_active = TRUE
                  AND u.is_active = TRUE
                ORDER BY u.full_name
                """;

        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("groupId", groupId),
                (rs, rowNum) -> new GroupMemberResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getTimestamp("joined_at").toInstant()
                ));
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
        // ON CONFLICT DO UPDATE rather than a plain INSERT: this method is called both
        // at group creation (no existing rows, so it behaves like a plain insert) and
        // when adding members to an existing group later, where the (group_id, user_id)
        // pair might already exist with is_active = FALSE (someone previously removed
        // and now re-invited). A plain INSERT would violate the composite primary key
        // in that case; this reactivates the row instead.
        String sql = """
                INSERT INTO expense_group_member (group_id, user_id, is_active, joined_at)
                VALUES (:groupId, :userId, TRUE, NOW())
                ON CONFLICT (group_id, user_id) DO UPDATE
                    SET is_active = TRUE, joined_at = NOW()
                """;

        SqlParameterBatchBuilder.batchUpdate(jdbcTemplate, sql, userIds, userId ->
                new MapSqlParameterSource()
                        .addValue("groupId", groupId)
                        .addValue("userId", userId)
        );
    }

    // Soft delete, matching the is_active pattern used everywhere else in this schema
    // (app_user, expense_group). Keeps expense/settlement history involving this member
    // intact and queryable, rather than losing it to an ON DELETE CASCADE.
    public void removeMember(UUID groupId, UUID userId) {
        String sql = """
                UPDATE expense_group_member
                SET is_active = FALSE
                WHERE group_id = :groupId AND user_id = :userId
                """;

        int rowsAffected = jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("groupId", groupId)
                .addValue("userId", userId));

        if (rowsAffected == 0) {
            throw new ResourceNotFoundException("User is not an active member of this group: " + userId);
        }
    }

    // Guards against removing a member while they still owe money or are owed money
    // within this group - doing so would orphan real, unsettled debt in user_balance
    // that no longer has a corresponding active member to settle it.
    public boolean hasOutstandingBalance(UUID groupId, UUID userId) {
        String sql = """
                SELECT COUNT(1) FROM user_balance
                WHERE group_id = :groupId
                  AND (debtor_user_id = :userId OR creditor_user_id = :userId)
                """;

        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
                .addValue("groupId", groupId)
                .addValue("userId", userId), Integer.class);

        return count != null && count > 0;
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
