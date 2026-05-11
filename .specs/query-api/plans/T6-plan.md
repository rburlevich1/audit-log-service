# T6 Plan — Verify Read-Only and Append-Only Invariants

Task reference: implements `.specs/query-api/tasks.md` § T6.

## Dependencies

T3 (endpoint surface), T4 (response DTO), T5 (real pagination so
determinism is testable).

## 01 Problem

`AGENTS.md` defines hard invariants: append-only events, no UPDATE /
DELETE routes, deterministic list ordering with a tiebreaker. These
properties are easy to break accidentally in future refactors. This
task installs explicit ArchUnit + integration tests so any regression
fails the build instead of slipping into production.

## 02 Context

- `audit_events` is append-only by schema and convention.
- Java 21 / Spring Boot 3 with ArchUnit and JUnit 5 already wired.
- PostgreSQL via Flyway; Testcontainers harness already used by
  `AuditEventControllerTest`.
- T3, T4, T5 are merged; full Query API surface is live.

## 03 Constraints

- Read-only Query API: the test suite must prove this, not just the
  code.
- Append-only audit events: read traffic does not mutate state.
- Deterministic list ordering with a tiebreaker per `design.md` §3.
- Spec is the source of truth: assertions reference `AGENTS.md`
  invariants and `design.md` §7.
- One safe PR: tests-only.
- No production code changes.
- No unrelated refactors.

## 04 Minimum Expected Changes

- Modify `src/test/java/com/example/audit/ArchitectureTest.java` to
  add an ArchUnit rule banning `@PutMapping`, `@PatchMapping`, and
  `@DeleteMapping` anywhere in `com.example.audit`.
- Create
  `src/test/java/com/example/audit/event/AuditEventReadOnlyInvariantTest.java`
  — Testcontainers integration test asserting:
  - Row count and full snapshot of `audit_events` are byte-for-byte
    identical before and after a representative series of
    `GET /audit-events` calls.
  - `PUT`, `PATCH`, `DELETE` on `/audit-events` return a
    framework-default status indicating the route does not exist
    (`404` or `405`; accept whichever Spring emits, documented in a
    short test comment).
- Create
  `src/test/java/com/example/audit/event/AuditEventDeterminismTest.java`
  — Testcontainers integration test asserting:
  - Two consecutive identical queries return identical `items`
    element-for-element.
  - A paginated walk of the entire result set is reproducible across
    repeated runs.
  - Events sharing an `occurredAt` are ordered by the T1-resolved
    tiebreaker direction.

## 05 Verification Method

- DoD bullets from `tasks.md` § T6 each map to one of the new
  ArchUnit or integration assertions.
- Tests are the verification; no separate harness needed.
- `./gradlew test` passes.
- Existing `AuditEventControllerTest`, `ArchitectureTest` rules,
  and T2 index-existence test remain green.

## 06 Integration With Existing Code

- API / controller layer: no code changes — only ArchUnit
  assertions over existing controller annotations.
- Service / domain layer: untouched.
- Repository / infrastructure layer: untouched. Tests query the
  database via `JdbcTemplate` for snapshot capture, not via the
  repository, so they do not depend on repository internals.
- No JPA entity leakage and no controller → repository access is
  introduced. The ArchUnit additions complement the existing rules.

## 07 Principles

- Determinism first: the determinism test directly asserts the
  property that pagination correctness depends on.
- Preserve append-only and read-only invariants — tests fail loudly
  if either weakens.
- Missing requirement → blocker: tiebreaker direction must be
  resolved (it is, by T1 and consumed by T5) before the
  same-timestamp ordering assertion can be written.
- One safe PR.
- No production code in this task.
- Do not edit `AGENTS.md`.
- When framework defaults vary (e.g. `404` vs `405` on a missing
  route), accept the actual status with a documented comment rather
  than forcing a brittle expectation.

## Blockers / Open Questions

- Blocked on T5 — the determinism walk exercises real pagination,
  which only lands in T5.
- The exact status code returned by Spring for an unmapped HTTP
  method on an existing path depends on framework version; verify
  empirically on the first test run rather than guessing.

## Out of Scope

- Performance / load testing.
- Mutation testing of the production code.
- Coverage of `POST /audit-events`'s own invariants (handled by
  earlier tests on the write endpoint).
- Property-based testing.
- Authentication and authorization.
