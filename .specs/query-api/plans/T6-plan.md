# T6 Plan — Verify Read-Only and Append-Only Invariants

Task reference: implements `.specs/query-api/tasks.md` § T6.

## Dependencies

T3, T4, T5.

## 01 Problem

The Query API must stay read-only, append-only, and deterministic. This task
adds tests that lock those invariants down.

## 02 Context

- `AGENTS.md` forbids UPDATE/DELETE endpoints for audit events.
- Query endpoint is `GET /audit-events`.
- Pagination and deterministic ordering are live after T5.

## 03 Constraints

- Tests target audit event endpoints, not every possible controller in the app.
- No production code changes unless a test exposes a real bug.
- One invariant-test PR.

## 04 Minimum Expected Changes

- Add or update architecture tests to ensure `AuditEventController` does not
  expose PUT/PATCH/DELETE routes for `/audit-events`.
- Add integration test proving GET queries do not change stored audit rows.
- Add deterministic ordering test for repeated list queries and paginated walks.

## 05 Verification Method

- PUT/PATCH/DELETE against `/audit-events` return `404` or `405`.
- Row count and row snapshot are unchanged before/after representative GETs.
- Repeated identical queries return the same ordered items.
- Same-timestamp rows follow `id DESC`.
- `./gradlew test` passes.

## 06 Integration With Existing Code

- API layer is verified through HTTP integration tests.
- Database snapshot can use `JdbcTemplate` in tests.
- Existing architecture rules remain green.

## 07 Principles

- Test the invariants explicitly.
- Keep assertions scoped to audit events.
- Do not loosen failing tests to make the suite pass.

## Blockers / Open Questions

None.

## Out of Scope

- Performance testing.
- Authentication/authorization.
- Mutation testing.
