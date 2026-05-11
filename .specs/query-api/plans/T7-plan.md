# T7 Plan — Drop Legacy Single-Column Indexes (Follow-Up)

Task reference: implements `.specs/query-api/tasks.md` § T7.

## Dependencies

T2 (composite indexes exist), T5 (full read path is live so staging
`EXPLAIN ANALYZE` reflects production usage). T7 is an operational
follow-up, not part of the initial feature implementation path; it
ships once the trigger condition is met.

## 01 Problem

T2 added four composite indexes that subsume the V1 single-column
indexes for the Query API's read paths. Leaving the legacy indexes in
place means every insert into `audit_events` updates seven indexes
instead of four — pure write amplification with no read benefit. This
task drops the three legacy indexes once `EXPLAIN ANALYZE` in staging
confirms the planner reliably chooses the composites for every
canonical query.

## 02 Context

- `audit_events` is append-only and write-heavy in production —
  every additional index taxes the ingest path.
- Java 21 / Spring Boot 3 with Flyway-managed schema.
- PostgreSQL planner stats are stable in staging after a week of
  representative traffic; this is the gating signal.
- Testcontainers integration tests already assert index existence
  (added in T2); they are updated here.

## 03 Constraints

- Append-only schema: this migration is destructive only with
  respect to *indexes*, not data. PostgreSQL `DROP INDEX` is safe
  on a live table but takes a brief lock; the migration must run
  during a low-traffic window or use `DROP INDEX CONCURRENTLY`.
- Spec is the source of truth: index removal matches the
  write-amplification note in `design.md` §4.
- One safe PR: one Flyway migration plus the test update.
- Tests required: existing integration tests must remain green
  against the reduced index set.
- No unrelated refactors.
- Do not merge without staging `EXPLAIN ANALYZE` evidence in the PR
  description (gate, not optional).

## 04 Minimum Expected Changes

- Create
  `src/main/resources/db/migration/V3__drop_legacy_single_column_indexes.sql`
  dropping `idx_audit_events_actor`, `idx_audit_events_resource`,
  and `idx_audit_events_timestamp` (use `DROP INDEX IF EXISTS` so
  the migration is idempotent in environments where they were
  removed manually).
- Modify
  `src/test/java/com/example/audit/event/AuditEventIndexMigrationTest.java`
  (introduced in T2) to drop the three legacy index names from its
  expected set and keep the four composites.
- No production code changes.

## 05 Verification Method

- Pre-merge gate: paste `EXPLAIN ANALYZE` output for each of the
  five canonical queries (no-filter, actor-only, resource-only,
  actor+resource, time-range-only) from staging into the PR
  description, showing the four composite indexes from T2 are
  chosen and the legacy indexes are not.
- DoD bullets from `tasks.md` § T7 each map to the migration plus
  the test update.
- Testcontainers harness runs all migrations including `V3` and
  re-asserts the expected index set.
- Existing `AuditEventControllerTest`,
  `AuditEventReadOnlyInvariantTest`, and
  `AuditEventDeterminismTest` must remain green.
- `./gradlew test` passes.

## 06 Integration With Existing Code

- Infrastructure layer only: one Flyway migration + one test edit.
- Controller, service, repository, entity, and DTO code are
  untouched.
- Index-existence test from T2 becomes the regression guard against
  reintroducing the legacy indexes in a future migration.

## 07 Principles

- Determinism first: removing the legacy indexes must not change
  query results — verified by re-running the determinism test.
- Preserve append-only and read-only invariants — index changes
  affect storage layout only, not data semantics.
- Missing requirement → blocker: if staging telemetry is unavailable
  to prove the trigger condition, defer the PR; do not infer it.
- One safe PR.
- No production code changes.
- Do not edit `AGENTS.md`.
- Prefer `DROP INDEX IF EXISTS` over plain `DROP INDEX` to keep the
  migration idempotent across environments.

## Blockers / Open Questions

- Blocked on the trigger condition: at least one week of staging
  traffic since T2 + `EXPLAIN ANALYZE` confirming the composites
  cover all five canonical queries.
- If `EXPLAIN ANALYZE` shows the planner still picks a legacy index
  for some workload, do not merge T7. Open a separate ticket to
  investigate statistics (`ANALYZE audit_events`) or revisit the
  composite column order in `design.md` §4.
- Whether to use `DROP INDEX CONCURRENTLY` instead of `DROP INDEX`
  is a deployment-environment call; default to non-concurrent if the
  staging window allows a brief lock.

## Out of Scope

- Adding any new index.
- Tuning composite index storage parameters.
- Statistics or vacuum tuning.
- Source-code changes outside the migration and the index-existence
  test.
- Authentication and authorization.
