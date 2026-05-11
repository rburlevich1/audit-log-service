# T5 Plan — Implement Signed Keyset Pagination

Task reference: implements `.specs/query-api/tasks.md` § T5.

## Dependencies

T1 (tiebreaker direction, `limit > max` handling, `nextCursor`
representation, optional max time-range cap), T2 (composite indexes
exist so keyset queries are fast), T3 (endpoint surface and validation
layer), T4 (response DTO is the contract the new behavior fills in).

## 01 Problem

Analysts must page through large result sets without loss or
duplication, and the system must reject tampered cursors with `400` —
both per `requirements.md`. Today the endpoint runs a single query and
returns everything, and cursor handling is a hard-coded `400`
placeholder. This task implements keyset pagination over the
deterministic sort and ships HMAC-signed cursors in the same PR.
Pagination and signing cannot be split: shipping unsigned cursors would
violate the "tampered → 400" AC because the server could not detect
tampering.

## 02 Context

- `audit_events` is append-only; rows never shift, which is what
  makes keyset pagination correct over inserts.
- Java 21 / Spring Boot 3 with built-in `javax.crypto.Mac` and
  `java.util.Base64.getUrlEncoder()` — no new dependencies.
- PostgreSQL via Flyway (composite indexes from T2 in place);
  Testcontainers for integration tests.
- Existing controller, service, repository layering and the
  `@RestControllerAdvice` introduced in T3 are reused.
- `audit.query.cursor-secret` config slot is introduced here, bound
  from environment variable `AUDIT_QUERY_CURSOR_SECRET`.

## 03 Constraints

- Read-only Query API.
- Append-only events; cursor predicate assumes rows do not shift.
- Deterministic ordering with a unique tiebreaker is a hard
  requirement — cursor correctness depends on it.
- Spec is the source of truth: cursor payload, filter fingerprint,
  and HMAC choices follow `design.md` §2.
- One safe PR: keyset + signing together because shipping unsigned
  would violate `requirements.md` ("tampered → 400").
- Tests required for: round-trip cursor, malformed cursor `400`,
  tampered cursor `400`, filter-mismatch `422`, multi-page walk,
  same-timestamp tiebreaker, last-page behavior.
- No unrelated refactors.
- Tamper detection must use a stable algorithm
  (`design.md` §2 specifies HMAC-SHA256).

## 04 Minimum Expected Changes

- Create
  `src/main/java/com/example/audit/event/cursor/CursorPayload.java`,
  `FilterFingerprint.java`, `CursorCodec.java`,
  `MalformedCursorException.java`, `TamperedCursorException.java`,
  `CursorFilterMismatchException.java`.
- Create
  `src/main/java/com/example/audit/event/AuditQueryProperties.java`
  bound via `@ConfigurationProperties("audit.query")` with
  `cursor-secret`, `default-page-size`, `max-page-size`.
- Modify `src/main/resources/application.yml` to wire
  `audit.query.*` from environment variables; fail-fast if the
  secret is unset at boot.
- Modify
  `src/main/java/com/example/audit/event/AuditEventQuery.java`
  (introduced in T3) to carry the decoded cursor payload + effective
  limit.
- Modify
  `src/main/java/com/example/audit/event/AuditEventRepository.java`
  with a keyset-aware query method
  (JPQL or `Specification` + sort + `LIMIT :limit + 1`).
- Modify
  `src/main/java/com/example/audit/event/AuditEventService.java`
  to decode and verify cursors, compute the filter fingerprint, run
  the keyset query, and re-encode `nextCursor`.
- Modify
  `src/main/java/com/example/audit/event/AuditEventController.java`
  to pass `cursor` and `limit` through (still bound as raw types).
- Modify
  `src/main/java/com/example/audit/event/AuditApiExceptionHandler.java`
  to map the three new cursor exceptions; delete
  `CursorNotSupportedYetException` and its handler.
- Modify
  `src/main/java/com/example/audit/event/AuditEventMapper.java`
  to accept the encoded `nextCursor` string.
- Add unit tests in
  `src/test/java/com/example/audit/event/cursor/`:
  `CursorCodecTest.java`, `FilterFingerprintTest.java`.
- Extend
  `src/test/java/com/example/audit/AuditEventControllerTest.java`
  with end-to-end pagination, tampering, and filter-mismatch cases.

## 05 Verification Method

- DoD bullets from `tasks.md` § T5 mapped to unit tests
  (`CursorCodecTest`, `FilterFingerprintTest`) and Testcontainers
  integration tests (`AuditEventControllerTest`).
- Multi-page walk asserts: no duplicates, no skipped rows, page
  count matches expected.
- Same-timestamp seed asserts the tiebreaker ordering matches T1.
- Tampered-cursor test flips a byte in the HMAC tag and asserts
  `400`.
- Filter-mismatch test reuses a cursor under a different `actor`
  filter and asserts `422`.
- `./gradlew test` passes (with the cursor secret supplied via
  env or a test-scoped property).
- Spec consistency: cursor wire format matches
  `<base64url(payload)>.<base64url(hmac)>` per `design.md` §2.

## 06 Integration With Existing Code

- API / controller layer: `AuditEventController` still does not
  decode cursors; it forwards the opaque string to the service.
- Service / domain layer: `AuditEventService` owns cursor decoding,
  filter fingerprinting, and `nextCursor` re-encoding. Typed
  exceptions are raised and mapped by the existing
  `@RestControllerAdvice` from T3.
- Repository / infrastructure layer: `AuditEventRepository` gains
  a keyset method; the cursor predicate lives here (in either
  JPQL/native SQL or a `Specification` builder), not in the service.
- No JPA entities are returned from the controller — T4's
  `AuditEventPageResponse` carries the wire format; the mapper
  inserts the encoded `nextCursor`.
- ArchUnit rules from T4 (no entity returns) and the existing
  rules (no controller → repository) continue to hold.

## 07 Principles

- Determinism first: cursor correctness rests on the total order
  established by T1's tiebreaker and the composite indexes from T2.
- Preserve append-only and read-only invariants — the keyset
  predicate assumes rows never shift.
- Missing requirement → blocker: `limit > max` behavior, `nextCursor`
  last-page representation, max time-range cap (if any) are T1
  decisions that must be resolved before this task starts.
- One safe PR; keep new code under the new
  `event/cursor/` package to keep the diff scannable.
- Do not edit `AGENTS.md`.
- HMAC tag verification uses `MessageDigest.isEqual` (constant-time
  compare) to avoid timing leakage.
- Secret never logged; treat as sensitive config.

## Blockers / Open Questions

- Blocked on T1 for: tiebreaker direction (drives the cursor
  predicate variant), `limit > max` behavior (drives the validation
  branch), `nextCursor` last-page representation (drives the mapper),
  optional max time-range cap (drives an additional `422` case in
  the service).
- Production secret management for `AUDIT_QUERY_CURSOR_SECRET` is a
  follow-up for ops; the test harness uses a fixed test value.
- Secret rotation strategy is not in scope here; flag in PR
  description so security has a clear hand-off.

## Out of Scope

- Authentication and authorization for the endpoint.
- Multi-secret support for graceful key rotation.
- Rate limiting or quota enforcement.
- Caching of result pages.
- Changes to `POST /audit-events`.
- Dropping legacy indexes (T7).
