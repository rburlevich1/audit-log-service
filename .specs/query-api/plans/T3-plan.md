# T3 Plan — Add Query API Contract and Validation

Task reference: implements `.specs/query-api/tasks.md` § T3.

## Dependencies

T1.

## 01 Problem

Clients need the full `GET /audit-events` request surface and predictable error
codes before pagination is implemented. This task adds parameter binding and
validation without cursor decoding.

## 02 Context

- Existing endpoint already supports basic search by some filters.
- Final cursor behavior arrives in T5.
- This task keeps cursor honest by rejecting non-null cursor until signed cursor
  decoding exists.

## 03 Constraints

- Read-only endpoint only.
- No pagination or cursor decoding in this task.
- Bind `cursor` as an optional raw string and reject non-null cursor with `400`.
- Validate `limit` against `[1, configured max]` where the default max is
  `200`.
- Keep one safe API/service validation PR.

## 04 Minimum Expected Changes

- Update controller parameters: `actor`, `resource`, `from`, `to`, `cursor`,
  `limit`.
- Add query value object for bound filters and limit.
- Add service validation for:
  - `from > to` -> `422`
  - non-null `cursor` -> `400` until T5
  - `limit < 1` or `limit > configured max` -> `400`
- Add controller advice for parse errors and typed query exceptions.
- Return an `items` wrapper with omitted `nextCursor` until pagination.

## 05 Verification Method

- Integration tests cover:
  - `200` for empty and filtered queries
  - malformed timestamp -> `400`
  - non-numeric `limit` -> `400`
  - `limit < 1` / `limit > configured max` -> `400`
  - non-null `cursor` -> `400`
  - `from > to` -> `422`
- `./gradlew test` passes.

## 06 Integration With Existing Code

- Controller binds HTTP parameters and forwards a query object.
- Service owns semantic validation.
- Repository remains read-only and continues using existing search behavior.

## 07 Principles

- Preserve read-only and append-only invariants.
- Keep cursor decoding out until T5.
- Do not change `POST /audit-events`.

## Blockers / Open Questions

None.

## Out of Scope

- Signed cursor support.
- Keyset pagination.
- Response DTO hardening beyond the temporary wrapper.
