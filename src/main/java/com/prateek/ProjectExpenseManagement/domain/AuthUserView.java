package com.prateek.ProjectExpenseManagement.domain;

import java.util.UUID;

/**
 * Minimal view of app_user needed for authentication.
 * Deliberately excludes fields unrelated to login/token issuance.
 */
public record AuthUserView(
        UUID id,
        String email,
        String fullName,
        String passwordHash,
        String role,
        boolean active
) {
}
