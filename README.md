# Audit Log Service

Audit Log Service is a Spring Boot service for ingesting and searching immutable audit events.
It stores events in PostgreSQL, manages schema changes with Flyway, and exposes a small HTTP API for creating and querying audit records.

The current implementation supports:

- Creating audit events with server-assigned timestamps.
- Querying audit events by actor, resource, and inclusive `from`/`to` time range.
- Keyset (cursor) pagination over query results, with HMAC-signed cursors and constant-time signature verification.
- Deterministic newest-first ordering by `occurredAt` with `id` as the deterministic tiebreaker.
- Storing arbitrary JSON payload data in PostgreSQL `jsonb`.
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
| `id` | Auto | Database | `bigserial` primary key, surfaced as a numeric `id` in Query API responses. |
| `event_timestamp` | Yes | Server | Time when the event was accepted by the service. Callers cannot set this value. Exposed as `occurredAt` on query responses and as `timestamp` on the ingest response. |
| `actor` | Yes | Request | User, service account, or system component that initiated the action. |
| `action` | No | Request | Action that occurred, such as `resource.updated` or `user.login`. |
| `resource` | No | Request | Resource affected by the action, such as `project:42` or `invoice:777`. |
| `outcome` | No | Request | Result of the action. If present, must be `success`, `denied`, or `error`. |
| `context` | No | Request | Arbitrary JSON object or value with additional event details. Stored as `jsonb`. Exposed as `payload` on query responses. |

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

### Query Audit Events

```http
GET /audit-events
```

Read-only endpoint. All filter parameters are optional and AND together.

| Parameter | Type | Notes |
| --- | --- | --- |
| `actor` | string | Exact match. Blank values are ignored. |
| `resource` | string | Exact match. Blank values are ignored. |
| `from` | ISO-8601 instant | Inclusive lower bound on `occurredAt`. |
| `to` | ISO-8601 instant | Inclusive upper bound on `occurredAt`. |
| `cursor` | opaque string | Continues a prior page; must come from an earlier response under identical filters. |
| `limit` | integer | Page size. Defaults to 50; must be in `[1, 200]`. |

Example request:

```http
GET /audit-events?actor=u_42&resource=order/9f3b&from=2026-04-01T00:00:00Z&to=2026-05-01T00:00:00Z&limit=50
```

Example response:

```json
{
  "items": [
    {
      "id": 12345,
      "occurredAt": "2026-04-17T11:02:14Z",
      "actor": "u_42",
      "resource": "order/9f3b",
      "action": "order.refunded",
      "outcome": "success",
      "payload": {}
    }
  ],
  "nextCursor": "ZXlKdmNtUmxja0o1SWpwN0lt....abc"
}
```

`nextCursor` is present only when more results exist. On the last page it is omitted from the response.

Sort order is fixed: `occurredAt DESC` with `id DESC` as the deterministic tiebreaker. Callers cannot select a different order.

#### Cursor Format

Cursors are opaque to the client. The server encodes them as `<base64url(payload)>.<base64url(hmac)>` where:

- The payload carries `(occurredAt-epoch-millis, id, filter-fingerprint)`.
- The filter fingerprint is a SHA-256 of the canonical `(actor, resource, from, to)` tuple, joined with the ASCII unit separator (`\x1f`), truncated to 16 bytes and base64url-encoded.
- The signature is HMAC-SHA256 of the payload under the server's `audit.query.cursor-secret`, verified with a constant-time comparison.

The cursor secret must be supplied via the `AUDIT_QUERY_CURSOR_SECRET` environment variable. The application fails fast at startup if the secret is blank.

#### Status Codes

| Code | When |
| --- | --- |
| `200 OK` | Query succeeded. Empty `items` is still `200`. |
| `400 Bad Request` | Unparseable `from`/`to`, non-numeric `limit`, `limit < 1`, `limit > 200`, malformed cursor shape, or invalid cursor signature. |
| `422 Unprocessable Entity` | `from > to`; valid cursor used with different filters than it was issued for. |

### Append-Only Behavior

The service is designed as an append-only audit log.

Current enforcement:

- There are no `PUT`, `PATCH`, or `DELETE` endpoints for audit events.
- Entity fields are mapped with `updatable = false`.
- ArchUnit asserts at build time that `AuditEventController` declares no `@PutMapping`, `@PatchMapping`, or `@DeleteMapping`.
- Integration tests assert that `PUT`, `PATCH`, and `DELETE` requests are rejected with `404 Not Found` or `405 Method Not Allowed`.
- Invariant tests snapshot the table before and after representative GET queries and assert byte-for-byte equality.

Events can be created and read, but not changed through the exposed API.

## Persistence

Flyway creates the schema in `src/main/resources/db/migration/`.

Initial table (V1):

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

Composite indexes added by V2 to support the Query API filter combinations plus the deterministic sort and the keyset cursor predicate:

```sql
create index idx_audit_events_ts_id
    on audit_events (event_timestamp desc, id desc);
create index idx_audit_events_actor_ts_id
    on audit_events (actor, event_timestamp desc, id desc);
create index idx_audit_events_resource_ts_id
    on audit_events (resource, event_timestamp desc, id desc);
create index idx_audit_events_actor_resource_ts_id
    on audit_events (actor, resource, event_timestamp desc, id desc);
```

Spring Boot is configured with `spring.jpa.hibernate.ddl-auto=validate`, so Hibernate validates the mapped schema instead of creating or changing it.

## Retention Job

Scheduling is enabled in `AuditApplication`.

The retention job runs on the configured cron schedule and calculates:

```text
current UTC time - audit.retention.days
```

It currently logs that audit events older than the cutoff are ready for archival.
It does not currently move, delete, or archive records.

## Configuration

Application configuration is in `src/main/resources/application.yml`.

| Property | Environment variable | Default |
| --- | --- | --- |
| `spring.datasource.url` | `DATABASE_URL` | `jdbc:postgresql://localhost:5432/audit_log` |
| `spring.datasource.username` | `DATABASE_USERNAME` | `audit` |
| `spring.datasource.password` | `DATABASE_PASSWORD` | `audit` |
| `audit.retention.days` | `AUDIT_RETENTION_DAYS` | `90` |
| `audit.retention.cron` | `AUDIT_RETENTION_CRON` | `0 0 2 * * *` |
| `audit.query.cursor-secret` | `AUDIT_QUERY_CURSOR_SECRET` | _required, no default_ |
| `audit.query.default-page-size` | `AUDIT_QUERY_DEFAULT_PAGE_SIZE` | `50` |
| `audit.query.max-page-size` | `AUDIT_QUERY_MAX_PAGE_SIZE` | `200` |

Flyway is enabled by default. The application refuses to start if `audit.query.cursor-secret` is blank.

## Running Locally

### Option 1: Use the helper script

The helper script starts a PostgreSQL 16 container on host networking, exports the required datasource variables, and starts the Spring Boot app.

```bash
AUDIT_QUERY_CURSOR_SECRET=dev-only-secret ./run-app.sh
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

Then run the application with the required cursor secret:

```bash
AUDIT_QUERY_CURSOR_SECRET=dev-only-secret ./gradlew bootRun
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

Walk a paginated result set:

```bash
curl -i 'http://localhost:8080/audit-events?actor=user:123&limit=50'
# response includes "nextCursor": "..." when more results exist
curl -i 'http://localhost:8080/audit-events?actor=user:123&limit=50&cursor=<nextCursor-from-previous-response>'
```

## Testing

Run the full test suite:

```bash
AUDIT_QUERY_CURSOR_SECRET=test-secret ./gradlew test
```

The tests include:

- Endpoint integration tests using a real PostgreSQL container.
- Validation checks for required `actor` on ingest.
- Invariant checks that caller-supplied `timestamp` is ignored on ingest.
- Invariant checks that update and delete routes are not available, at both runtime (`PUT`, `PATCH` via a JDK `HttpClient`-backed `RestTemplate`, `DELETE`) and build time (ArchUnit reflection on the controller).
- Append-only checks that table contents are unchanged before and after representative GET queries (full row snapshot via `JdbcTemplate`).
- Deterministic-ordering checks (`occurredAt DESC`, `id DESC` tiebreaker), including same-timestamp rows and reproducible paginated walks.
- Cursor unit tests covering encode/decode round-trip, tampered signature, wrong secret, missing or empty signature segment, and non-base64 payload.
- Filter-fingerprint unit tests covering null-tuple stability, delimiter-collision avoidance, and base64url output shape.
- ArchUnit tests for package and layer boundaries.

Testcontainers is configured with Ryuk disabled through:

- `tasks.withType<Test> { environment("TESTCONTAINERS_RYUK_DISABLED", "true") }`
- `src/test/resources/testcontainers.properties`

The PostgreSQL test container uses host networking and an available local port selected at test startup. The integration test classes register `audit.query.cursor-secret` via `@DynamicPropertySource` so the application context can start under test.

## Architecture

Main packages:

- `com.example.audit.event`: audit event entity, request DTO, response DTO and mapper, repository (with a custom keyset query fragment), service, REST controller, and exception advice.
- `com.example.audit.event.cursor`: cursor payload, filter fingerprint, HMAC codec, and the three typed cursor exceptions.
- `com.example.audit.retention`: scheduled retention cutoff job.

Layering rules are enforced by ArchUnit:

- Controllers must not access repositories directly.
- Repositories should only be accessed by services.
- Services must not depend on controllers.
- The query endpoint must not return JPA entity types — controller return type is `AuditEventPageResponse`, never `AuditEvent`.
- The `event` package must not depend on the `retention` package.
- The `retention` package must not depend on the `event` package.

## Specifications

Feature specifications live under `.specs/<feature>/`. The Query API spec is in `.specs/query-api/` and contains the requirements, design, per-task plans, and evaluation reports for the current implementation.

## Current Limitations

The following items are not implemented in the current codebase:

- Actual archival or deletion of events after the retention cutoff.
- Tamper-evidence through hash chaining.
- Authentication or authorization.
- Filtering by `action`, `outcome`, or `payload` contents.
- Caller-selectable sort order.
- OpenAPI or Swagger documentation.
- Cursor-signing secret rotation.
