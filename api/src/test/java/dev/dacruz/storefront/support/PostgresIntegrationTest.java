package dev.dacruz.storefront.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Every integration test runs against a real Postgres 17 with the real Flyway
 * migrations. H2 would pass while hiding the things worth testing here: the
 * check constraints, the unique index on (cart_id, product_id), and the
 * bpchar-versus-varchar mismatch that ddl-auto=validate caught on the first run.
 *
 * SINGLETON CONTAINER, DELIBERATELY. The obvious spelling of this class uses
 * {@code @Testcontainers} with a {@code @Container} field, and it fails in a way
 * that looks like something else entirely: JUnit stops the container when the
 * first test class finishes, so every later class inherits a dead port and dies
 * on a 30 second Hikari timeout rather than a connection error. Measured
 * 2026-08-20: 13 of 37 tests failed that way while the first class passed.
 * Starting it here and never stopping it means one container for the whole JVM,
 * which Docker reaps when the run ends.
 */
@SpringBootTest
public abstract class PostgresIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("storefront.cors.allowed-origin", () -> "http://localhost:5173");
    }
}
