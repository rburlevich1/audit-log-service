# T2 Plan — Add Database Index Migration

Task reference: implements `.specs/query-api/tasks.md` § T2.

## Dependencies

T1.

## 01 Problem

The Query API needs deterministic newest-first reads and keyset pagination over
large `audit_events` data. Existing single-column indexes do not cover filters
plus `ORDER BY event_timestamp DESC, id DESC`. This task adds the composite
indexes required by the design.

## 02 Context

- PostgreSQL schema is managed by Flyway.
- Existing V1 indexes on `actor`, `resource`, and `event_timestamp` remain for
  now.
- Cleanup of legacy indexes is deferred to T7 and verified locally with
  Testcontainers because this repository has no staging environment.

## 03 Constraints

- Add indexes only; do not drop existing indexes in this task.
- Index order is fixed: `event_timestamp DESC, id DESC`.
- No controller/service/repository changes.
- One safe migration PR.

## 04 Minimum Expected Changes

- Add a Flyway migration with:
  - `(event_timestamp desc, id desc)`
  - `(actor, event_timestamp desc, id desc)`
  - `(resource, event_timestamp desc, id desc)`
  - `(actor, resource, event_timestamp desc, id desc)`
- Add a Testcontainers-backed migration test that checks the four composite
  indexes exist via `pg_indexes`.

## 05 Verification Method

- Testcontainers boots PostgreSQL and runs all Flyway migrations.
- The four composite indexes are present.
- Legacy single-column indexes are still present.
- `./gradlew test` passes.

## 06 Integration With Existing Code

Infrastructure only: one Flyway migration and one migration test. No API,
domain, repository, or entity behavior changes.

## 07 Principles

- Determinism first: indexes match the query sort.
- Preserve append-only behavior.
- Avoid destructive migration work until T7.

## Blockers / Open Questions

None.

## Out of Scope

- Dropping legacy indexes.
- Query implementation.
- Performance tuning beyond the required indexes.
