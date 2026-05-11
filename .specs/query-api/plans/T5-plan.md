# T5 Plan — Implement Signed Keyset Pagination

Task reference: implements `.specs/query-api/tasks.md` § T5.

## Dependencies

T1, T2, T3, T4.

## 01 Problem

Analysts need to page through large result sets without loss or duplication.
The endpoint must also detect cursor tampering. This task ships keyset
pagination and signed cursors together.

## 02 Context

- Sort order is fixed: `occurredAt DESC, id DESC`.
- Default page size is `50`; maximum is `200`.
- `limit > configured max` returns `400`; the default max is `200`.
- Cursor payload carries `(occurredAt, database id, filter-fingerprint)`.
- Cursor signing uses HMAC-SHA256 with `audit.query.cursor-secret`, bound from
  `AUDIT_QUERY_CURSOR_SECRET`.

## 03 Constraints

- Do not use offset pagination.
- Do not ship unsigned public cursors.
- Application fails fast if cursor secret is blank.
- Use constant-time HMAC comparison.
- Never log the cursor secret.
- Secret rotation is out of scope.

## 04 Minimum Expected Changes

- Add cursor payload, fingerprint, and codec classes.
- Add `AuditQueryProperties` for `cursor-secret`, `default-page-size=50`, and
  `max-page-size=200`.
- Update service to:
  - decode and verify cursor
  - compare filter fingerprint
  - apply effective limit
  - call keyset repository query
  - encode `nextCursor` from the last returned row
- Add repository keyset query using `(event_timestamp, id) < (:t, :i)` and
  `ORDER BY event_timestamp DESC, id DESC`.
- Map malformed/tampered cursor to `400`; filter mismatch to `422`.

## 05 Verification Method

- Unit tests cover cursor encode/decode, HMAC verification, constant-time
  compare path, and fingerprint canonicalization.
- Integration tests cover:
  - multi-page walk without duplicates/skips
  - same-timestamp `id DESC` tiebreaking
  - malformed cursor -> `400`
  - tampered cursor -> `400`
  - cursor reused with different filters -> `422`
  - last page omits `nextCursor`
  - `limit > configured max` -> `400`
- `./gradlew test` passes with test cursor secret configured.

## 06 Integration With Existing Code

- Controller continues treating cursor as opaque.
- Service owns cursor decode/encode and page assembly.
- Repository owns SQL/JPA cursor predicate.
- Controller returns `AuditEventPageResponse` from T4.

## 07 Principles

- Determinism first.
- Preserve append-only assumptions.
- Keep signing simple and dependency-free.
- Do not leak persistence concerns into API DTOs.

## Blockers / Open Questions

None.

## Out of Scope

- Secret rotation.
- Authentication/authorization.
- Dropping legacy indexes.
