# Query API Tasks

## T1 — Verify Query API Spec Consistency

Refs:
- `requirements.md` — Fixed Decisions, User Stories With AC
- `design.md` — API Contract, Sort & Determinism, Validation Rules and Edge Cases

Dependencies: none.

Scope: one documentation-only PR.

DoD:
- `requirements.md` has no unresolved Query API open questions.
- `requirements.md` and `design.md` agree on page size, sort order, tiebreaker,
  `limit > max`, timestamp format, last-page `nextCursor`, public `id`,
  actor/resource shape, max time range, and auth scope.
- `tasks.md` and plan files match the fixed spec decisions.

## T2 — Add Database Index Migration

Refs:
- `design.md` — Indexes, Sort & Determinism

Dependencies: T1.

Scope: one Flyway migration PR.

DoD:
- Migration adds composite indexes for no-filter, actor, resource, and
  actor+resource query paths.
- Index order matches `occurredAt DESC, id DESC`.
- Existing single-column indexes are not dropped in this task.
- Testcontainers integration test boots PostgreSQL, runs all Flyway
  migrations, and asserts the four composite indexes exist via `pg_indexes`.
- `./gradlew test` passes.

## T3 — Add Query API Contract and Validation

Refs:
- `requirements.md` — Problem, User Stories With AC
- `design.md` — API Contract, Validation Rules and Edge Cases, Layer Integration

Dependencies: T1.

Scope: one API/service validation PR. No pagination, no cursor decoding.

DoD:
- `GET /audit-events` accepts `actor`, `resource`, `from`, `to`, `cursor`, and
  `limit`. `cursor` is bound as an optional raw string. At this task's stage
  any non-null `cursor` is rejected with `400 Bad Request` ("cursor not yet
  supported"); signed cursor decoding lands in T5.
- Empty filters are accepted.
- Malformed timestamps, non-numeric `limit`, `limit < 1`, and
  `limit > configured max` (default `200`) return `400`.
- `from > to` returns `422`.
- Response wraps results in `items` and omitted page-level `nextCursor` at this
  stage — no pagination yet.
- Controller, service, and repository responsibilities follow `design.md`.
- Integration tests cover `200`, `400` (parse failures), and `422`
  (`from > to`).
- `./gradlew test` passes.

## T4 — Add Response DTO, Mapping, and Contract Tests

Refs:
- `requirements.md` — Problem, User Stories With AC, Fixed Decisions
- `design.md` — API Contract, Layer Integration

Dependencies: T1, T3.

Scope: one response-contract PR. Locks the public response shape before any
public pagination behavior is introduced.

DoD:
- Response item shape uses numeric database `id`, scalar `actor`/`resource`,
  `event_timestamp` → `occurredAt`, and `context` → `payload`.
- Response uses a dedicated `AuditEventPageResponse { items, nextCursor }` DTO,
  not the JPA entity. ArchUnit or equivalent asserts the query endpoint does
  not return entity types.
- Last-page `nextCursor` is omitted using matching Jackson configuration such
  as `@JsonInclude(NON_NULL)`.
- Integration tests assert the response contract for non-empty and empty
  result sets (last-page behavior under real pagination is covered in T5).
- `./gradlew test` passes.

## T5 — Implement Signed Keyset Pagination

Refs:
- `requirements.md` — Analyst, SRE / Security Analyst
- `design.md` — Pagination Strategy, Sort & Determinism, Validation Rules and Edge Cases, Layer Integration

Dependencies: T1, T2, T3, T4.

Scope: one pagination/cursor PR. Pagination and signed cursor ship together
because shipping unsigned cursor would violate the "tampered → 400" AC in
`requirements.md`.

DoD:
- Results sort by `occurredAt DESC, id DESC`.
- Repository query uses keyset pagination, not offset pagination.
- Cursor payload carries `(occurredAt, database id, filter-fingerprint)`.
- Filter fingerprint is computed per `design.md` §2 (SHA-256 over canonical
  `(actor, resource, from, to)` tuple, unit-separator delimited, first
  16 bytes base64url-encoded).
- Cursor is signed with HMAC-SHA256 using `audit.query.cursor-secret`, bound
  from `AUDIT_QUERY_CURSOR_SECRET`.
- Wire form is `<base64url(payload)>.<base64url(hmac)>`.
- Structurally malformed cursors (bad base64, missing fields, wrong field
  types) return `400`.
- Tampered cursor (HMAC verification failure) returns `400`.
- Valid cursor whose filter fingerprint does not match the current query
  returns `422`.
- Fetching a next page has no duplicate or skipped events, including the
  same-`occurredAt` tiebreaker case.
- `nextCursor` follows the last-page representation locked in T4.
- Unit tests cover signing, verification, fingerprint canonicalization, and
  the `occurredAt DESC, id DESC` cursor predicate.
- Integration tests cover multi-page results, same-timestamp tiebreaking,
  cursor continuation, malformed-cursor `400`, tampered-cursor `400`,
  filter-mismatch `422`, and last-page `nextCursor` shape.
- `./gradlew test` passes.

## T6 — Verify Read-Only and Append-Only Invariants

Refs:
- `requirements.md` — Out of Scope, SRE / Security Analyst
- `design.md` — AGENTS.md Alignment
- `AGENTS.md` — Invariants

Dependencies: T3, T4, T5.

Scope: one invariant-test PR.

DoD:
- Query API introduces no update or delete route.
- Query requests do not mutate stored audit events.
- Tests assert append-only/read-only behavior for the query path.
- Tests assert deterministic ordering for list results.
- `./gradlew test` passes.

## T7 — Follow-Up: Drop Legacy Single-Column Indexes

Refs:
- `design.md` — Indexes (write-amplification note)

Dependencies: T2, T5.

Scope: follow-up operational Flyway migration PR, outside the initial feature
implementation path.

DoD:
- Trigger condition is met: at least one week in staging with `EXPLAIN ANALYZE`
  on representative production queries (no-filter, actor-only, resource-only,
  actor+resource, time-range-only) showing the four composite indexes from T2
  are used; legacy single-column indexes are not chosen for the read path.
- Migration drops `idx_audit_events_actor`, `idx_audit_events_resource`, and
  `idx_audit_events_timestamp`.
- Migration uses `DROP INDEX CONCURRENTLY` when Flyway can run it
  non-transactionally; otherwise it is scheduled as regular `DROP INDEX` during
  an approved low-traffic maintenance window.
- Existing integration tests for query and ingest paths remain green against
  the reduced index set.
- `./gradlew test` passes.
