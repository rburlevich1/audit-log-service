# T3 Plan — Add Query API Contract and Validation

Task reference: implements `.specs/query-api/tasks.md` § T3.

## Dependencies

T1 (timestamp format, default and maximum `limit`, and basic field-shape
decisions feed the parameter binding and validation rules).

## 01 Problem

Auditors and SREs need to call `GET /audit-events` with `actor`,
`resource`, `from`, `to`, `cursor`, and `limit` parameters and receive
predictable responses: `200` on success, `400` on parse failure, `422`
on semantic violation. Today the endpoint takes only `actor`,
`resource`, `from`, `to` and has no exception handling. This task adds
the full parameter surface, parse-level validation, semantic checks for
`from > to`, and rejects any non-null `cursor` with `400` until T5
introduces signed-cursor decoding.

## 02 Context

- `audit_events` is append-only; this endpoint is read-only.
- Java 21 / Spring Boot 3, with `@RestController` and JPA
  `Specification`-based filtering already in place.
- PostgreSQL via Flyway, Testcontainers for integration tests.
- T3 ships ahead of T4 (response DTO) and T5 (pagination), so the
  controller may continue to use a minimal placeholder response
  wrapper (`{items, nextCursor: null}`) that T4 replaces.

## 03 Constraints

- Read-only Query API: no UPDATE / DELETE / PATCH route added.
- Append-only: this task does not interact with writes.
- Deterministic ordering is still required from the underlying query
  even without pagination; existing tiebreaker behavior is preserved
  until T5 hardens it.
- Spec is the source of truth: status codes and validation rules
  match `design.md` §1 and §5 exactly.
- One safe PR: contract + validation + tests in one cohesive change.
- Tests required for `200`, `400`, and `422` cases.
- No unrelated refactors — leave `POST /audit-events` alone.

## 04 Minimum Expected Changes

- Modify `src/main/java/com/example/audit/event/AuditEventController.java`
  to accept `cursor` (`String`, optional, opaque) and `limit`
  (`Integer`, optional).
- Modify `src/main/java/com/example/audit/event/AuditEventService.java`
  to:
  - Reject `from > to` with a typed semantic exception.
  - Reject any non-null `cursor` with a typed "cursor not yet
    supported" exception.
- Create
  `src/main/java/com/example/audit/event/AuditEventQuery.java`
  (small value object carrying filters + limit).
- Create
  `src/main/java/com/example/audit/event/InvalidQueryException.java`
  (mapped to `422`).
- Create
  `src/main/java/com/example/audit/event/CursorNotSupportedYetException.java`
  (mapped to `400`; removed in T5).
- Create
  `src/main/java/com/example/audit/event/AuditApiExceptionHandler.java`
  (`@RestControllerAdvice`) mapping
  `MethodArgumentTypeMismatchException` → `400`,
  `InvalidQueryException` → `422`,
  `CursorNotSupportedYetException` → `400`.
- Extend `src/test/java/com/example/audit/AuditEventControllerTest.java`
  with integration tests for `200` (empty + non-empty), `400`
  (malformed timestamp, non-numeric `limit`, non-null `cursor`),
  and `422` (`from > to`).

## 05 Verification Method

- DoD bullets from `tasks.md` § T3, mapped one-for-one to integration
  tests in `AuditEventControllerTest`.
- Unit-test-level coverage is unnecessary for this thin layer —
  Testcontainers tests give end-to-end signal at low cost.
- `./gradlew test` passes.
- Spec consistency: the response wrapper exposes `items` and
  `nextCursor` as required by `design.md` §1, even though
  `nextCursor` is always `null` at this task's stage.

## 06 Integration With Existing Code

- API / controller layer: `AuditEventController.search` gains two
  parameters; mapper / DTO work is deferred to T4. The controller
  must not access the repository directly (existing ArchUnit rule
  in `ArchitectureTest`).
- Service / domain layer: `AuditEventService.search` accepts the
  new value object, applies the existing `Specification` builders,
  raises typed exceptions for semantic and "not-yet-supported"
  violations. The exception types live in the event package so the
  service does not import controller types.
- Repository / infrastructure layer: unchanged. `JpaSpecificationExecutor`
  continues to serve the query.
- No JPA entity types leak through the new exception classes or
  value objects.

## 07 Principles

- Determinism first: even without pagination, the existing
  underlying ordering is preserved.
- Preserve append-only and read-only invariants.
- Missing requirement → blocker, not invention: do not pick a
  default `limit` here; that value is a T1 decision and the
  controller leaves it unused at this stage if T1 has not landed.
- One safe PR.
- No code outside the listed files.
- Do not edit `AGENTS.md`.
- Cursor decoding is explicitly deferred to T5; T3 does not
  implement HMAC, base64, or filter-fingerprint logic.

## Blockers / Open Questions

- Blocked on T1 for: timestamp format strictness and the
  `default-page-size` / `max-page-size` values (used in T5; not
  required to *bind* `limit` here, but tests should assert any
  T1-resolved range-checking behavior).
- If T1 inserts an upstream task for structured `actor`/`resource`
  or non-numeric `id`, T3 cannot start until that upstream task
  ships, because parameter parsing may change shape.

## Out of Scope

- Cursor encoding / decoding (T5).
- Response DTO and Jackson configuration (T4).
- Pagination and keyset query (T5).
- Performance / index-usage assertions (covered by T2 and T7).
- Authentication and authorization.
