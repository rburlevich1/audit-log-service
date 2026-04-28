# Audit Log Service

Audit Log Service is a Spring Boot service for ingesting and searching immutable audit events.
It stores events in PostgreSQL, manages schema changes with Flyway, and exposes a small HTTP API for creating and querying audit records.

The current implementation supports:

- Creating audit events with server-assigned timestamps.
- Searching audit events by actor, resource, and timestamp range.
- Storing arbitrary JSON context data in PostgreSQL `jsonb`.
- Enforcing append-only behavior at the HTTP API level by exposing no update or delete routes.
- Running a scheduled retention job that calculates and logs the archival cutoff.
- Integration testing against PostgreSQL through Testcontainers.

## Technology Stack

- Java 21
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA
- Spring Validation
- PostgreSQL 16
- Flyway
- Gradle Kotlin DSL
- Testcontainers
- Docker
- ArchUnit

## Domain Model

Audit events are stored in the `audit_events` table.

| Field | Required | Source | Description |
| --- | --- | --- | --- |
| `timestamp` | Yes | Server | Time when the event was accepted by the service. Callers cannot set this value. |
| `actor` | Yes | Request | User, service account, or system component that initiated the action. |
| `action` | No | Request | Action that occurred, such as `resource.updated` or `user.login`. |
| `resource` | No | Request | Resource affected by the action, such as `project:42` or `invoice:777`. |
| `outcome` | No | Request | Result of the action. If present, must be `success`, `denied`, or `error`. |
| `context` | No | Request | Arbitrary JSON object or value with additional event details. Stored as `jsonb`. |

The database also has an internal `id` primary key. The service does not include `id` in API responses.

## HTTP API

### Create an Audit Event

```http
POST /audit-events
Content-Type: application/json
```

Example request:

```json
{
  "actor": "user:123",
  "action": "resource.updated",
  "resource": "project:42",
  "outcome": "success",
  "context": {
    "ip": "127.0.0.1",
    "requestId": "req-abc123"
  }
}
```

Example response:

```http
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "timestamp": "2026-04-28T10:15:30.123456Z",
  "actor": "user:123",
  "action": "resource.updated",
  "resource": "project:42",
  "outcome": "success",
  "context": {
    "ip": "127.0.0.1",
    "requestId": "req-abc123"
  }
}
```

Validation rules:

- `actor` is required and must not be blank.
- `outcome` is optional, but when supplied it must be one of `success`, `denied`, or `error`.
- `timestamp` is ignored if supplied by the caller. The service always assigns the current server time.
- Unknown JSON fields are ignored.

Example invalid request:

```json
{
  "action": "user.login",
  "resource": "session:1",
  "outcome": "denied"
}
```

This returns `400 Bad Request` because `actor` is missing.

### Search Audit Events

```http
GET /audit-events
```

Supported query parameters:

| Parameter | Required | Matching behavior |
| --- | --- | --- |
| `actor` | No | Exact match. Blank values are ignored. |
| `resource` | No | Exact match. Blank values are ignored. |
| `from` | No | Inclusive lower bound for `timestamp`. ISO-8601 date-time. |
| `to` | No | Inclusive upper bound for `timestamp`. ISO-8601 date-time. |

Example:

```http
GET /audit-events?actor=user:123&resource=project:42
```

Example with time range:

```http
GET /audit-events?from=2026-04-01T00:00:00Z&to=2026-04-28T23:59:59Z
```

Example response:

```json
[
  {
    "timestamp": "2026-04-28T10:15:30.123456Z",
    "actor": "user:123",
    "action": "resource.updated",
    "resource": "project:42",
    "outcome": "success",
    "context": {
      "ip": "127.0.0.1"
    }
  }
]
```

The current implementation does not define explicit sorting or pagination.

## Append-Only Behavior

The service is designed as an append-only audit log.

Current enforcement:

- There are no `PUT`, `PATCH`, or `DELETE` endpoints for audit events.
- Entity fields are mapped with `updatable = false`.
- Tests assert that update and delete requests are rejected with `404 Not Found` or `405 Method Not Allowed`.

Events can be created and read, but not changed through the exposed API.

## Persistence

Flyway creates the schema in `src/main/resources/db/migration/V1__create_audit_events.sql`.

Table:

```sql
create table audit_events (
    id bigserial primary key,
    event_timestamp timestamptz not null,
    actor text not null,
    action text,
    resource text,
    outcome text check (outcome in ('success', 'denied', 'error')),
    context jsonb
);
```

Indexes:

- `idx_audit_events_actor` on `actor`
- `idx_audit_events_resource` on `resource`
- `idx_audit_events_timestamp` on `event_timestamp`

Spring Boot is configured with `spring.jpa.hibernate.ddl-auto=validate`, so Hibernate validates the mapped schema instead of creating or changing it.

## Retention Job

Scheduling is enabled in `AuditApplication`.

The retention job runs on the configured cron schedule and calculates:

```text
current UTC time - audit.retention.days
```

It currently logs that audit events older than the cutoff are ready for archival.
It does not currently move, delete, or archive records.

Configuration:

| Property | Environment variable | Default |
| --- | --- | --- |
| `audit.retention.days` | `AUDIT_RETENTION_DAYS` | `90` |
| `audit.retention.cron` | `AUDIT_RETENTION_CRON` | `0 0 2 * * *` |

## Configuration

Application configuration is in `src/main/resources/application.yml`.

| Property | Environment variable | Default |
| --- | --- | --- |
| `spring.datasource.url` | `DATABASE_URL` | `jdbc:postgresql://localhost:5432/audit_log` |
| `spring.datasource.username` | `DATABASE_USERNAME` | `audit` |
| `spring.datasource.password` | `DATABASE_PASSWORD` | `audit` |

Flyway is enabled by default.

## Running Locally

### Option 1: Use the helper script

The helper script starts a PostgreSQL 16 container on host networking, exports the required datasource variables, and starts the Spring Boot app.

```bash
./run-app.sh
```

Defaults used by the script:

| Variable | Default |
| --- | --- |
| `POSTGRES_CONTAINER` | `audit-service-postgres-run` |
| `POSTGRES_PORT` | `55433` |
| `POSTGRES_DB` | `audit_log` |
| `POSTGRES_USER` | `audit` |
| `POSTGRES_PASSWORD` | `audit` |

The application starts on the standard Spring Boot port, `8080`, unless overridden through Spring configuration.

### Option 2: Use Docker Compose for PostgreSQL

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Then run the application:

```bash
./gradlew bootRun
```

The Compose database listens on `localhost:5432` and matches the default datasource configuration:

- Database: `audit_log`
- Username: `audit`
- Password: `audit`

## Example cURL Commands

Create an event:

```bash
curl -i -X POST http://localhost:8080/audit-events \
  -H 'Content-Type: application/json' \
  -d '{
    "actor": "user:123",
    "action": "resource.updated",
    "resource": "project:42",
    "outcome": "success",
    "context": {
      "ip": "127.0.0.1"
    }
  }'
```

Search by actor and resource:

```bash
curl -i 'http://localhost:8080/audit-events?actor=user:123&resource=project:42'
```

Search by time range:

```bash
curl -i 'http://localhost:8080/audit-events?from=2026-04-01T00:00:00Z&to=2026-04-28T23:59:59Z'
```

## Testing

Run the full test suite:

```bash
./gradlew test
```

The tests include:

- Endpoint integration tests using a real PostgreSQL container.
- Validation checks for required `actor`.
- Invariant checks that caller-supplied `timestamp` is ignored.
- Invariant checks that update and delete routes are not available.
- Append-only behavior checks that stored events are unchanged after rejected mutation attempts.
- ArchUnit tests for package and layer boundaries.

Testcontainers is configured with Ryuk disabled through:

- `tasks.withType<Test> { environment("TESTCONTAINERS_RYUK_DISABLED", "true") }`
- `src/test/resources/testcontainers.properties`

The PostgreSQL test container uses host networking and an available local port selected at test startup.

## Architecture

Main packages:

- `com.example.audit.event`: audit event entity, request DTO, repository, service, and REST controller.
- `com.example.audit.retention`: scheduled retention cutoff job.

Layering rules are enforced by ArchUnit:

- Controllers must not access repositories directly.
- Repositories should only be accessed by services.
- Services must not depend on controllers.
- The `event` package must not depend on the `retention` package.
- The `retention` package must not depend on the `event` package.

## Current Limitations

The following items are not implemented in the current codebase:

- Actual archival or deletion of events after the retention cutoff.
- Tamper-evidence through hash chaining.
- Pagination or explicit sorting for search results.
- Authentication or authorization.
- Filtering by `action`, `outcome`, or `context`.
- OpenAPI or Swagger documentation.
