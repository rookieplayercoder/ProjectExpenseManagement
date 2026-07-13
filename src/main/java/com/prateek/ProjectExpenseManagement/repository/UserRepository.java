package com.prateek.ProjectExpenseManagement.repository;

import com.prateek.ProjectExpenseManagement.domain.AuthUserView;
import com.prateek.ProjectExpenseManagement.dto.CreateUserRequest;
import com.prateek.ProjectExpenseManagement.dto.UserLookupResponse;
import com.prateek.ProjectExpenseManagement.dto.UserProfileResponse;
import com.prateek.ProjectExpenseManagement.exception.BusinessValidationException;
import com.prateek.ProjectExpenseManagement.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserProfileResponse findProfileById(UUID userId) {
        String sql = """
                SELECT id, email, full_name, mobile_number, role, created_at
                FROM app_user
                WHERE id = :userId AND is_active = TRUE
                """;

        List<UserProfileResponse> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> new UserProfileResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("mobile_number"),
                        rs.getString("role"),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant()
                ));

        if (results.isEmpty()) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return results.get(0);
    }

    public UserLookupResponse findLookupByEmail(String email) {
        String sql = """
                SELECT id, full_name, email
                FROM app_user
                WHERE email = :email AND is_active = TRUE
                """;

        List<UserLookupResponse> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("email", email.trim().toLowerCase()),
                (rs, rowNum) -> new UserLookupResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("full_name"),
                        rs.getString("email")
                ));

        if (results.isEmpty()) {
            throw new ResourceNotFoundException("No user found with email: " + email);
        }
        return results.get(0);
    }

    public void assertUsersExist(List<UUID> userIds) {
        String sql = "SELECT id FROM app_user WHERE id IN (:userIds) AND is_active = TRUE";

        List<UUID> existing = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("userIds", userIds),
                (rs, rowNum) -> UUID.fromString(rs.getString("id"))
        );

        if (existing.size() != userIds.size()) {
            throw new ResourceNotFoundException("One or more users do not exist or are inactive");
        }
    }

    public void assertEmailNotInUse(String email) {
        String sql = "SELECT COUNT(1) FROM app_user WHERE email = :email";
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("email", email), Integer.class);

        if (count != null && count > 0) {
            throw new BusinessValidationException("Email is already registered: " + email);
        }
    }

    public UUID insertUser(CreateUserRequest request, String passwordHash) {
        UUID userId = UUID.randomUUID();

        String sql = """
                INSERT INTO app_user (id, email, full_name, mobile_number, password_hash, role, is_active, created_at, updated_at)
                VALUES (:id, :email, :fullName, :mobileNumber, :passwordHash, 'USER', TRUE, NOW(), NOW())
                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", userId)
                .addValue("email", request.getEmail())
                .addValue("fullName", request.getFullName())
                .addValue("mobileNumber", request.getMobileNumber())
                .addValue("passwordHash", passwordHash));

        return userId;
    }

    public Optional<AuthUserView> findByEmail(String email) {
        String sql = """
                SELECT id, email, full_name, password_hash, role, is_active
                FROM app_user
                WHERE email = :email
                """;

        List<AuthUserView> results = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("email", email),
                (rs, rowNum) -> new AuthUserView(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getBoolean("is_active")
                )
        );

        return results.stream().findFirst();
    }
}
