package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditEventInvariantsTest {
  private static final int postgresPort = findAvailablePort();

  @Container
  static final HostNetworkPostgresContainer postgres =
      new HostNetworkPostgresContainer()
          .withNetworkMode("host")
          .withCommand("postgres", "-c", "port=" + postgresPort)
          .waitingFor(
              Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("audit.query.cursor-secret", () -> "test-secret-do-not-use-in-prod");
  }

  @LocalServerPort int port;

  @Autowired TestRestTemplate restTemplate;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void readQueriesDoNotMutateStoredEvents() {
    String actor = "invariant:read-only";
    for (int i = 0; i < 5; i++) {
      post(Map.of("actor", actor, "action", "evt." + i, "resource", "r:ro", "outcome", "success"));
    }

    long countBefore = countAuditEvents();
    List<Map<String, Object>> snapshotBefore = snapshotAuditEvents();

    issueRepresentativeQueries(actor);

    long countAfter = countAuditEvents();
    List<Map<String, Object>> snapshotAfter = snapshotAuditEvents();

    assertThat(countAfter).isEqualTo(countBefore);
    assertThat(snapshotAfter).isEqualTo(snapshotBefore);
  }

  @Test
  void repeatedIdenticalQueriesReturnIdenticalItems() {
    String actor = "invariant:determinism";
    for (int i = 0; i < 6; i++) {
      post(Map.of("actor", actor, "action", "evt." + i, "resource", "r:det", "outcome", "success"));
    }

    ResponseEntity<Map<String, Object>> first = get("/audit-events?actor=" + actor);
    ResponseEntity<Map<String, Object>> second = get("/audit-events?actor=" + actor);

    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(items(second)).isEqualTo(items(first));
  }

  @Test
  void paginatedWalkIsReproducible() {
    String actor = "invariant:walk";
    for (int i = 0; i < 7; i++) {
      post(
          Map.of("actor", actor, "action", "evt." + i, "resource", "r:walk", "outcome", "success"));
    }

    List<Map<String, Object>> firstWalk = walk("/audit-events?actor=" + actor + "&limit=2");
    List<Map<String, Object>> secondWalk = walk("/audit-events?actor=" + actor + "&limit=2");

    assertThat(firstWalk).hasSize(7);
    assertThat(secondWalk).isEqualTo(firstWalk);
  }

  @Test
  void sameTimestampRowsAreOrderedByIdDescending() {
    String actor = "invariant:tiebreak";
    Instant timestamp = Instant.parse("2026-03-01T09:00:00Z");

    Long id1 = insertEventDirect(actor, "r:tie", timestamp);
    Long id2 = insertEventDirect(actor, "r:tie", timestamp);
    Long id3 = insertEventDirect(actor, "r:tie", timestamp);

    ResponseEntity<Map<String, Object>> response = get("/audit-events?actor=" + actor);
    List<Map<String, Object>> items = items(response);

    assertThat(items).hasSize(3);
    assertThat(((Number) items.get(0).get("id")).longValue()).isEqualTo(id3);
    assertThat(((Number) items.get(1).get("id")).longValue()).isEqualTo(id2);
    assertThat(((Number) items.get(2).get("id")).longValue()).isEqualTo(id1);
  }

  @Test
  void writeRoutesOnAuditEventsAreRejected() {
    Map<String, Object> body =
        Map.of(
            "actor", "invariant:writes",
            "action", "x",
            "resource", "r",
            "outcome", "success");
    ResponseEntity<String> put = exchange(HttpMethod.PUT, body);
    ResponseEntity<String> delete = exchange(HttpMethod.DELETE, null);

    assertThat(put.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(delete.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.METHOD_NOT_ALLOWED);
  }

  private void issueRepresentativeQueries(String actor) {
    get("/audit-events");
    get("/audit-events?actor=" + actor);
    get("/audit-events?resource=r:ro");
    get("/audit-events?actor=" + actor + "&resource=r:ro");
    get("/audit-events?from=2000-01-01T00:00:00Z&to=2100-01-01T00:00:00Z");
    get("/audit-events?actor=" + actor + "&limit=2");
  }

  private long countAuditEvents() {
    Long count = jdbcTemplate.queryForObject("select count(*) from audit_events", Long.class);
    return count == null ? 0L : count;
  }

  private List<Map<String, Object>> snapshotAuditEvents() {
    return jdbcTemplate.query(
        "select id, event_timestamp, actor, action, resource, outcome, context::text as context"
            + " from audit_events order by id",
        (rs, rowNum) -> {
          Map<String, Object> row = new HashMap<>();
          row.put("id", rs.getLong("id"));
          row.put("event_timestamp", rs.getTimestamp("event_timestamp").toInstant().toString());
          row.put("actor", rs.getString("actor"));
          row.put("action", rs.getString("action"));
          row.put("resource", rs.getString("resource"));
          row.put("outcome", rs.getString("outcome"));
          row.put("context", rs.getString("context"));
          return row;
        });
  }

  private Long insertEventDirect(String actor, String resource, Instant timestamp) {
    return jdbcTemplate.queryForObject(
        "insert into audit_events (event_timestamp, actor, action, resource, outcome)"
            + " values (?, ?, ?, ?, ?) returning id",
        Long.class,
        Timestamp.from(timestamp),
        actor,
        "evt.direct",
        resource,
        "success");
  }

  private List<Map<String, Object>> walk(String startPath) {
    List<Map<String, Object>> all = new ArrayList<>();
    String cursor = null;
    int pages = 0;
    while (true) {
      String path = startPath + (cursor == null ? "" : "&cursor=" + cursor);
      ResponseEntity<Map<String, Object>> response = get(path);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      all.addAll(items(response));
      cursor = (String) response.getBody().get("nextCursor");
      pages++;
      assertThat(pages).isLessThan(50);
      if (cursor == null) {
        break;
      }
    }
    return all;
  }

  private ResponseEntity<Map<String, Object>> post(Map<String, Object> request) {
    return restTemplate.exchange(
        baseUrl() + "/audit-events",
        HttpMethod.POST,
        new HttpEntity<>(request),
        new ParameterizedTypeReference<>() {});
  }

  private ResponseEntity<Map<String, Object>> get(String path) {
    return restTemplate.exchange(
        baseUrl() + path, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<>() {});
  }

  private ResponseEntity<String> exchange(HttpMethod method, Map<String, Object> body) {
    HttpEntity<?> entity = body == null ? HttpEntity.EMPTY : new HttpEntity<>(body);
    return restTemplate.exchange(baseUrl() + "/audit-events", method, entity, String.class);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> items(ResponseEntity<Map<String, Object>> response) {
    return (List<Map<String, Object>>) response.getBody().get("items");
  }

  private String baseUrl() {
    return "http://localhost:" + port;
  }

  private static int findAvailablePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Could not find an available PostgreSQL test port", exception);
    }
  }

  private static final class HostNetworkPostgresContainer
      extends PostgreSQLContainer<HostNetworkPostgresContainer> {
    private HostNetworkPostgresContainer() {
      super("postgres:16");
      setExposedPorts(List.of());
    }

    @Override
    public String getJdbcUrl() {
      return "jdbc:postgresql://127.0.0.1:"
          + postgresPort
          + "/"
          + getDatabaseName()
          + "?sslmode=disable";
    }
  }
}
