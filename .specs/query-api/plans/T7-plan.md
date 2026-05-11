# T7 Plan — Drop Legacy Single-Column Indexes

Task reference: implements `.specs/query-api/tasks.md` § T7.

## Dependencies

T2, T5.

## 01 Problem

After composite indexes prove they cover the Query API read path, the legacy
single-column indexes only add write amplification to audit event ingestion.
This follow-up task removes them safely.

## 02 Context

- T2 adds four composite indexes.
- T5 makes the full query path live.
- T7 is operational follow-up, outside initial feature implementation.

## 03 Constraints

- Do not merge until staging has at least one week of evidence.
- PR description must include `EXPLAIN ANALYZE` for canonical queries.
- Prefer `DROP INDEX CONCURRENTLY` when Flyway can run the migration
  non-transactionally; otherwise use an approved low-traffic maintenance window.
- No source code changes except migration test updates.

## 04 Minimum Expected Changes

- Add Flyway migration dropping:
  - `idx_audit_events_actor`
  - `idx_audit_events_resource`
  - `idx_audit_events_timestamp`
- Use `IF EXISTS`.
- Update index migration test to expect only the four composite indexes.

## 05 Verification Method

- Staging `EXPLAIN ANALYZE` shows composite indexes are used for:
  - no-filter
  - actor-only
  - resource-only
  - actor+resource
  - time-range-only
- Testcontainers migration test proves legacy indexes are gone and composites
  remain.
- Query and ingest integration tests remain green.
- `./gradlew test` passes.

## 06 Integration With Existing Code

Infrastructure only: one migration and test updates. Controllers, services,
repositories, entities, and DTOs are untouched.

## 07 Principles

- Remove write amplification only after evidence.
- Do not risk data loss; index drops affect access paths, not table data.
- Defer if staging evidence is missing or ambiguous.

## Blockers / Open Questions

- Blocked on staging evidence and an approved migration execution strategy.

## Out of Scope

- Adding new indexes.
- Query behavior changes.
- Vacuum/statistics tuning.
