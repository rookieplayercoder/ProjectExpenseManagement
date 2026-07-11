package com.prateek.ProjectExpenseManagement.security;

import java.util.UUID;

/**
 * The principal stored in the SecurityContext for JWT-authenticated requests.
 * Controllers can pull this out via @AuthenticationPrincipal.
 */
public record AuthenticatedUser(UUID userId, String email, String role) {
}
