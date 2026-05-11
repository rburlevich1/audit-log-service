package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class AuditEventMigrationTest {
  private static final int postgresPort = findAvailablePort();

  @Container
  static final HostNetworkPostgresContainer postgres =
      new HostNetworkPostgresContainer()
          .withNetworkMode("host")
          .withCommand("postgres", "-c", "port=" + postgresPort)
          .waitingFor(
              Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));

  @Test
  void migrationsLeaveOnlyCompositeQueryApiIndexes() throws SQLException {
    Flyway.configure()
        .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .load()
        .migrate();

    Map<String, String> indexes = auditEventIndexes();

    assertThat(indexes)
        .containsKeys(
            "idx_audit_events_ts_id",
            "idx_audit_events_actor_ts_id",
            "idx_audit_events_resource_ts_id",
            "idx_audit_events_actor_resource_ts_id");
    assertThat(indexes)
        .doesNotContainKeys(
            "idx_audit_events_actor",
            "idx_audit_events_resource",
            "idx_audit_events_timestamp");

    assertIndexDefinition(indexes, "idx_audit_events_ts_id", "(event_timestamp DESC, id DESC)");
    assertIndexDefinition(
        indexes, "idx_audit_events_actor_ts_id", "(actor, event_timestamp DESC, id DESC)");
    assertIndexDefinition(
        indexes, "idx_audit_events_resource_ts_id", "(resource, event_timestamp DESC, id DESC)");
    assertIndexDefinition(
        indexes,
        "idx_audit_events_actor_resource_ts_id",
        "(actor, resource, event_timestamp DESC, id DESC)");
  }

  private Map<String, String> auditEventIndexes() throws SQLException {
    Map<String, String> indexes = new HashMap<>();
    try (var connection =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var statement =
            connection.prepareStatement(
                """
                select indexname, indexdef
                from pg_indexes
                where schemaname = 'public'
                  and tablename = 'audit_events'
                """);
        var resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        indexes.put(resultSet.getString("indexname"), resultSet.getString("indexdef"));
      }
    }
    return indexes;
  }

  private void assertIndexDefinition(
      Map<String, String> indexes, String indexName, String expectedColumns) {
    assertThat(indexes.get(indexName)).contains(expectedColumns);
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
