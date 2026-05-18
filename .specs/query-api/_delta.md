# Query API Delta

This file records decisions made during planning or spec-fix work before those
decisions enter the source specification.

## 2026-05-18 — Spec Self-Eval Weak Finding Fixes

Context: `spec-self-eval` reported weak coverage in the Query API spec. The
source specification needed updates to remove ambiguity and align with the
current implementation.

### Decisions

- `from > to` is a user-visible validation contract and belongs in
  `requirements.md`, not only `design.md`.
  - Resolution: add an acceptance criterion requiring
    `422 Unprocessable Entity` when both `from` and `to` are supplied and
    `from` is after `to`.
- Malformed `from` or `to` values should return `400 Bad Request`.
  - Rationale: the current implementation binds `from` and `to` as
    `java.time.Instant` query parameters and maps Spring type-mismatch
    failures to `400`.
  - Resolution: add an acceptance criterion requiring `400 Bad Request` when
    `from` or `to` cannot be parsed as an ISO-8601 instant.
- T3 may temporarily reject any non-null `cursor` before T5 adds signed cursor
  pagination.
  - Resolution: clarify in `tasks.md` that T3 is an internal implementation
    slice, not the complete externally releasable Query API contract; the final
    Query API contract is complete only after T5 adds signed cursor pagination.

### Correction Note

These decisions were recorded after the source spec edits instead of before
them. That ordering was a process error. Future planning decisions for this
feature should be recorded here before editing `requirements.md`, `design.md`,
`tasks.md`, or task plans.
