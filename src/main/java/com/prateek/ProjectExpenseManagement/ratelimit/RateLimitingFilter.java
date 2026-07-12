package com.prateek.ProjectExpenseManagement.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prateek.ProjectExpenseManagement.dto.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate-limits the two endpoints reachable without a JWT (login and
 * registration), since those are the ones exposed to credential-stuffing /
 * signup-spam from anyone on the internet - everything else already requires
 * a valid token, which is a much stronger throttle on abuse.
 *
 * Keyed by client IP. Note: if this app runs behind a reverse proxy/load
 * balancer, request.getRemoteAddr() will be the proxy's IP unless the proxy
 * is configured to forward the real client IP (e.g. via X-Forwarded-For and
 * a matching ForwardedHeaderFilter) - that setup is environment-specific and
 * left to deployment configuration.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final InMemoryRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    private final int loginMaxRequests;
    private final long loginWindowMillis;
    private final int registerMaxRequests;
    private final long registerWindowMillis;

    public RateLimitingFilter(
            InMemoryRateLimiter rateLimiter,
            ObjectMapper objectMapper,
            @Value("${rate-limit.login.max-requests:10}") int loginMaxRequests,
            @Value("${rate-limit.login.window-seconds:60}") long loginWindowSeconds,
            @Value("${rate-limit.register.max-requests:5}") int registerMaxRequests,
            @Value("${rate-limit.register.window-seconds:60}") long registerWindowSeconds) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.loginMaxRequests = loginMaxRequests;
        this.loginWindowMillis = loginWindowSeconds * 1000;
        this.registerMaxRequests = registerMaxRequests;
        this.registerWindowMillis = registerWindowSeconds * 1000;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        RateLimitRule rule = ruleFor(request);

        if (rule != null) {
            String key = rule.name() + ":" + clientIp(request);
            boolean allowed = rateLimiter.tryConsume(key, rule.maxRequests(), rule.windowMillis());

            if (!allowed) {
                writeTooManyRequests(response, rule.windowMillis());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule ruleFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();

        if (path.equals("/api/v1/auth/login")) {
            return new RateLimitRule("login", loginMaxRequests, loginWindowMillis);
        }
        if (path.equals("/api/v1/users")) {
            return new RateLimitRule("register", registerMaxRequests, registerWindowMillis);
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, long windowMillis) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(windowMillis / 1000));

        ApiErrorResponse body = new ApiErrorResponse(
                "RATE_LIMIT_EXCEEDED", "Too many requests - please try again later");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private record RateLimitRule(String name, int maxRequests, long windowMillis) {
    }
}
