package com.prateek.ProjectExpenseManagement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prateek.ProjectExpenseManagement.dto.ApiErrorResponse;
import com.prateek.ProjectExpenseManagement.logging.RequestLoggingFilter;
import com.prateek.ProjectExpenseManagement.ratelimit.RateLimitingFilter;
import com.prateek.ProjectExpenseManagement.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RequestLoggingFilter requestLoggingFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RequestLoggingFilter requestLoggingFilter,
            RateLimitingFilter rateLimitingFilter,
            ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.requestLoggingFilter = requestLoggingFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.objectMapper = objectMapper;
    }

    // Allows the Vite dev server (frontend/) to call this API from the browser.
    // Without this, the browser blocks every request with a CORS error before
    // it even reaches Spring Security - the JWT/rate-limit filters never see it.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // Without registering this explicitly, Spring Security falls back to
    // Http403ForbiddenEntryPoint for any request that fails .authenticated() -
    // i.e. a request with no token at all comes back as 403 Forbidden instead
    // of 401 Unauthorized. 401 is the correct code here: the caller hasn't
    // authenticated at all, as opposed to being authenticated but disallowed
    // (which is what 403 should mean, and which this API doesn't use yet since
    // there's no per-resource authorization beyond "must have a valid token").
    // Body matches the same ApiErrorResponse shape GlobalExceptionHandler uses
    // elsewhere, since this path never reaches GlobalExceptionHandler - it's
    // handled entirely inside the Spring Security filter chain.
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiErrorResponse body = new ApiErrorResponse("UNAUTHORIZED", "Authentication is required to access this resource");
            objectMapper.writeValue(response.getWriter(), body);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless JWT API - no cookies/session, so CSRF protection isn't applicable.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()))
                .authorizeHttpRequests(auth -> auth
                        // Registration and login must be reachable without a token.
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/users").permitAll()
                        // Static frontend assets.
                        .requestMatchers("/", "/index.html", "/js/**", "/favicon.ico").permitAll()
                        // Container/orchestrator health checks - no sensitive
                        // data is exposed here (show-details=never).
                        .requestMatchers("/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                // Order: logging (see everything) -> rate limiting (block abuse
                // early, but the attempt is still logged) -> JWT auth.
                .addFilterBefore(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}