# T2 Plan — Add Database Index Migration

Task reference: implements `.specs/query-api/tasks.md` § T2.

## Dependencies

T1 (specifically the tiebreaker direction; the four composite indexes
must trail with `id asc` or `id desc` to match the chosen sort).

## 01 Problem

The Query API will filter by `actor`, `resource`, and time range, walk
results newest-first with a tiebreaker, and paginate via keyset cursor.
The existing single-column indexes from `V1` cover individual filters
but do not support ordering or efficient keyset seek. Without composite
indexes that pair the filter column with `(event_timestamp, id)`, the
planner must sort and offset, defeating the cursor strategy. This task
adds the four composite indexes required by `design.md` §4.

## 02 Context

- `audit_events` is append-only.
- Java 21 / Spring Boot 3 with Flyway-managed PostgreSQL schema.
- Testcontainers runs migrations against a real PostgreSQL in
  integration tests.
- Legacy single-column indexes from `V1` remain in place — they are
  dropped later in T7 once staging `EXPLAIN ANALYZE` confirms the
  composites cover the read path.

## 03 Constraints

- Append-only schema: this migration adds objects only, no `DROP`.
- Deterministic ordering must be supported at the index level
  (composite trails with `(event_timestamp, id)`).
- One safe PR: one Flyway migration plus the test asserting it
  applied.
- No unrelated refactors — do not touch entity classes or controllers
  here.
- Spec is the source of truth: index column order tracks
  `design.md` §4.

## 04 Minimum Expected Changes

- Create `src/main/resources/db/migration/V2__add_query_api_composite_indexes.sql`
  with four `CREATE INDEX` statements covering:
  - `(event_timestamp, id)` for no-filter + time-range queries
  - `(actor, event_timestamp, id)` for actor-filtered queries
  - `(resource, event_timestamp, id)` for resource-filtered queries
  - `(actor, resource, event_timestamp, id)` for combined queries
  The `event_timestamp` and `id` directions inside each index match
  the T1-resolved tiebreaker direction (e.g.
  `event_timestamp desc, id desc` for the descending case).
- Create `src/test/java/com/example/audit/event/AuditEventIndexMigrationTest.java`
  — `@SpringBootTest` + Testcontainers — that queries
  `pg_indexes WHERE tablename = 'audit_events'` and asserts the four
  new index names exist.

## 05 Verification Method

- DoD bullets from `tasks.md` § T2: migration adds the four
  composites; index direction matches the resolved sort; legacy
  indexes are not dropped; Testcontainers test boots PostgreSQL and
  asserts via `pg_indexes`.
- Integration test (Testcontainers) is the primary verification.
- `./gradlew test` passes.
- Existing `AuditEventControllerTest` and `ArchitectureTest` remain
  green.

## 06 Integration With Existing Code

- Infrastructure layer only: one new Flyway migration file in the
  `db/migration` directory. Naming follows existing convention
  (`V<n>__<snake_case>.sql`).
- No controller, service, repository, or entity changes.
- No JPA entity touches — no risk of leakage into API DTOs.
- New test joins the existing `com.example.audit.event` test
  package; it reuses the Testcontainers harness already wired up by
  `AuditEventControllerTest`.

## 07 Principles

- Determinism first: indexes exist precisely to make the
  deterministic sort cheap to execute.
- Preserve append-only and read-only invariants: this migration adds
  read-side infrastructure only.
- Missing requirement → blocker: tiebreaker direction is a T1
  decision; if unresolved at the time of writing, the migration is
  not written.
- One safe PR.
- No production code changes beyond the migration and its test.
- Do not edit `AGENTS.md`.

## Blockers / Open Questions

- Blocked on T1's tiebreaker direction (`id asc` vs `id desc`).
  Reflect in the SQL once T1 ships.
- If T1 introduces a separate public id column (e.g. ULID stored
  alongside `bigserial`), the composite indexes still use the
  internal `bigserial` id — the cursor strategy in `design.md` §2
  is unchanged. Confirm with the T1 outcome before writing the SQL.

## Out of Scope

- Dropping legacy single-column indexes (deferred to T7).
- Tuning index storage parameters (`fillfactor`, partial indexes,
  etc.).
- Statistics tuning (`ANALYZE`); Postgres handles this automatically
  in practice.
- Any source-code change outside `db/migration` and the new
  Testcontainers test.
