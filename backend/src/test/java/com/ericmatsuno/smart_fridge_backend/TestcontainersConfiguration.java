package com.ericmatsuno.smart_fridge_backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Shared test setup.
 *
 * <p>Postgres: Testcontainers starts a brand-new Postgres 18 in Docker for the test run.
 * {@code @ServiceConnection} points Spring's datasource at it automatically. So tests
 * never touch your real fridge_db, and Flyway builds the schema from V1 every time,
 * which also proves the migrations work on an empty database.
 *
 * <p>Clock: "today" is frozen at {@link #TODAY} so date-based assertions are stable.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final LocalDate TODAY = LocalDate.of(2026, 3, 1);

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18");
    }

    @Bean
    @Primary
    Clock fixedTestClock() {
        ZoneId zone = ZoneId.systemDefault();
        return Clock.fixed(TODAY.atStartOfDay(zone).toInstant(), zone);
    }
}
