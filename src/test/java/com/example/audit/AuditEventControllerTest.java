package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditEventControllerTest {
    private static final int postgresPort = findAvailablePort();

    @Container
    static final HostNetworkPostgresContainer postgres = new HostNetworkPostgresContainer()
            .withNetworkMode("host")
            .withCommand("postgres", "-c", "port=" + postgresPort)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void createsAndSearchesAuditEvents() {
        ResponseEntity<Map<String, Object>> created = post(Map.of(
                "actor", "user:123",
                "action", "resource.updated",
                "resource", "project:42",
                "outcome", "success",
                "context", Map.of("ip", "127.0.0.1")
        ));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).containsEntry("actor", "user:123");
        assertThat(created.getBody()).containsEntry("resource", "project:42");
        assertThat(created.getBody()).containsKey("timestamp");

        ResponseEntity<java.util.List<Map<String, Object>>> found = restTemplate.exchange(
                baseUrl() + "/audit-events?actor=user:123&resource=project:42",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody()).hasSize(1);
        assertThat(found.getBody().getFirst()).containsEntry("actor", "user:123");
    }

    @Test
    void rejectsMissingActor() {
        ResponseEntity<Map<String, Object>> response = post(Map.of(
                "action", "user.login",
                "resource", "session:1",
                "outcome", "denied"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ignoresCallerSuppliedTimestamp() {
        String callerTimestamp = "2000-01-01T00:00:00Z";

        ResponseEntity<Map<String, Object>> response = post(Map.of(
                "timestamp", callerTimestamp,
                "actor", "service:billing",
                "action", "invoice.created",
                "resource", "invoice:777",
                "outcome", "success"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("timestamp")).isNotEqualTo(callerTimestamp);
        assertThat(Instant.parse((String) response.getBody().get("timestamp"))).isAfter(Instant.parse(callerTimestamp));
    }

    @Test
    void hasNoUpdateOrDeleteRoute() {
        ResponseEntity<String> put = restTemplate.exchange(
                baseUrl() + "/audit-events",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                String.class
        );
        ResponseEntity<String> delete = restTemplate.exchange(
                baseUrl() + "/audit-events",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class
        );

        assertThat(put.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(delete.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void existingEventsCannotBeMutated() {
        ResponseEntity<Map<String, Object>> created = post(Map.of(
                "actor", "immutable:user",
                "action", "resource.created",
                "resource", "immutable:resource",
                "outcome", "success",
                "context", Map.of("version", 1)
        ));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<java.util.List<Map<String, Object>>> beforeMutation = get(
                "/audit-events?actor=immutable:user&resource=immutable:resource"
        );
        assertThat(beforeMutation.getBody()).hasSize(1);
        Map<String, Object> storedEvent = beforeMutation.getBody().getFirst();

        Map<String, Object> mutation = Map.of(
                "actor", "immutable:user",
                "action", "resource.deleted",
                "resource", "immutable:resource",
                "outcome", "error",
                "context", Map.of("version", 2)
        );

        ResponseEntity<String> put = restTemplate.exchange(
                baseUrl() + "/audit-events",
                HttpMethod.PUT,
                new HttpEntity<>(mutation),
                String.class
        );
        ResponseEntity<String> delete = restTemplate.exchange(
                baseUrl() + "/audit-events",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class
        );

        assertThat(put.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(delete.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.METHOD_NOT_ALLOWED);

        ResponseEntity<java.util.List<Map<String, Object>>> found = get(
                "/audit-events?actor=immutable:user&resource=immutable:resource"
        );

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody()).hasSize(1);
        assertThat(found.getBody().getFirst()).isEqualTo(storedEvent);
    }

    private ResponseEntity<Map<String, Object>> post(Map<String, Object> request) {
        return restTemplate.exchange(
                baseUrl() + "/audit-events",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {
                }
        );
    }

    private ResponseEntity<java.util.List<Map<String, Object>>> get(String path) {
        return restTemplate.exchange(
                baseUrl() + path,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {
                }
        );
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private static int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not find an available PostgreSQL test port", exception);
        }
    }

    private static final class HostNetworkPostgresContainer extends PostgreSQLContainer<HostNetworkPostgresContainer> {
        private HostNetworkPostgresContainer() {
            super("postgres:16");
            setExposedPorts(List.of());
        }

        @Override
        public String getJdbcUrl() {
            return "jdbc:postgresql://127.0.0.1:" + postgresPort + "/" + getDatabaseName() + "?sslmode=disable";
        }
    }
}
