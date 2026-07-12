package com.prateek.ProjectExpenseManagement.logging;

import com.prateek.ProjectExpenseManagement.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Logs exactly one structured line per HTTP request (method, path, status,
 * duration, correlation id, and the authenticated user if any) rather than
 * scattering ad-hoc log statements through the service layer.
 *
 * Also propagates/generates a correlation id (X-Request-Id) so a single
 * request can be traced across log lines and back to the client that made it.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http.access");

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_HTTP_METHOD = "httpMethod";
    private static final String MDC_PATH = "path";
    private static final String MDC_STATUS = "status";
    private static final String MDC_DURATION_MS = "durationMs";
    private static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestId = resolveRequestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long startNanos = System.nanoTime();

        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_HTTP_METHOD, request.getMethod());
        MDC.put(MDC_PATH, request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

            MDC.put(MDC_STATUS, String.valueOf(response.getStatus()));
            MDC.put(MDC_DURATION_MS, String.valueOf(durationMs));
            resolveUserId(request).ifPresent(userId -> MDC.put(MDC_USER_ID, userId));

            log.info("{} {} -> {} ({} ms)",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);

            // Always clear - MDC is thread-local and threads are pooled/reused,
            // so a leftover value here would leak into an unrelated request.
            MDC.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        return (incoming != null && !incoming.isBlank()) ? incoming : UUID.randomUUID().toString();
    }

    private java.util.Optional<String> resolveUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return java.util.Optional.of(user.userId().toString());
        }
        return java.util.Optional.empty();
    }
}
