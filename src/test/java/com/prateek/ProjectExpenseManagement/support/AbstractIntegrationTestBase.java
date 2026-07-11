package com.prateek.ProjectExpenseManagement.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared base for all Testcontainers-backed integration tests.
 *
 * Uses the singleton container pattern: the Postgres container is started once
 * (in a static initializer, not via @Container) and stays up for the lifetime of
 * the JVM, shared across every test class that extends this one. Testcontainers'
 * Ryuk resource reaper cleans it up when the test JVM exits, so we deliberately
 * never call POSTGRES.stop() ourselves - starting/stopping per class would make
 * the suite far slower without adding any real isolation, since Flyway re-runs
 * migrations fresh against the same container for the whole run anyway.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// These tests exercise business logic (expense splitting, settlements, etc.),
// not authentication - JWT/security behavior gets its own test class instead.
@AutoConfigureMockMvc(addFilters = false)
public abstract class AbstractIntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;
}
