# T7 Plan — Drop Legacy Single-Column Indexes

Task reference: implements `.specs/query-api/tasks.md` § T7.

## Dependencies

T2, T5.

## 01 Problem

After composite indexes prove they cover the Query API read path, the legacy
single-column indexes only add write amplification to audit event ingestion.
This follow-up task removes them.

## 02 Context

- T2 adds four composite indexes that cover every Query API filter and the
  deterministic sort.
- T5 makes the full query path live; the Testcontainers integration tests
  exercise every supported filter combination end-to-end.
- T7 is the cleanup migration that retires the V1 single-column indexes.
- This repository has no staging environment, so verification is local-only
  (Testcontainers integration tests). The original plan's staging-evidence
  gate has been retired in favor of local verification — see `_delta.md`
  "Дополнение при реализации T7" for the rationale.

## 03 Constraints

- Use `DROP INDEX IF EXISTS` so the migration is idempotent across
  environments.
- No source code changes except the migration and the migration test.
- Existing query and ingest integration tests must remain green against the
  reduced index set.
- One operational Flyway migration PR.
- Production deployment cadence and concurrency strategy
  (`DROP INDEX CONCURRENTLY` vs regular `DROP INDEX`, maintenance windows)
  is left to the deployer; not gated in this repository.

## 04 Minimum Expected Changes

- Add Flyway migration `V3__drop_legacy_single_column_indexes.sql` dropping:
  - `idx_audit_events_actor`
  - `idx_audit_events_resource`
  - `idx_audit_events_timestamp`
- Update the migration test to assert the four composite indexes are present
  and the three legacy indexes are absent.

## 05 Verification Method

- Testcontainers migration test proves legacy indexes are gone and the four
  composite indexes from T2 remain.
- Controller, invariants, cursor, and fingerprint test suites all stay green
  against the reduced index set, demonstrating the composites cover every
  Query API read path the tests exercise.
- `./gradlew test` passes.

## 06 Integration With Existing Code

Infrastructure only: one migration and a test update. Controllers, services,
repositories, entities, and DTOs are untouched.

## 07 Principles

- Remove write amplification once the composite indexes are demonstrably
  sufficient for the read path.
- Do not risk data loss; index drops affect access paths, not table data.
- Keep the migration idempotent so it can be re-applied safely.

## Blockers / Open Questions

None. The original staging-evidence gate is retired (see `_delta.md`).

## Out of Scope

- Adding new indexes.
- Query behavior changes.
- Vacuum/statistics tuning.
- Production deployment scheduling and concurrency strategy.
