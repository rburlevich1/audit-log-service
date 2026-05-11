# T4 Plan — Add Response DTO, Mapping, and Contract Tests

Task reference: implements `.specs/query-api/tasks.md` § T4.

## Dependencies

T1, T3.

## 01 Problem

The public response contract must be stable before pagination ships. This task
removes JPA entity exposure from the query response and locks the wire shape.

## 02 Context

- Response item fields are fixed: numeric `id`, `occurredAt`, scalar `actor`,
  scalar `resource`, `action`, `outcome`, `payload`.
- Last-page `nextCursor` is omitted.
- `event_timestamp` maps to `occurredAt`; `context` maps to `payload`.

## 03 Constraints

- No JPA entity return types from the Query API `GET /audit-events` response.
- No pagination behavior in this task.
- No repository changes.
- One response-contract PR.

## 04 Minimum Expected Changes

- Add `AuditEventResponse`.
- Add `AuditEventPageResponse`.
- Add mapper from `AuditEvent` to response DTO.
- Update query controller to return the page DTO.
- Configure omitted `nextCursor` with `@JsonInclude(NON_NULL)` or equivalent.
- Add or update architecture test so the query endpoint return type does not
  expose JPA entities.

## 05 Verification Method

- Integration tests assert non-empty and empty query response JSON.
- Tests verify `nextCursor` is omitted when absent.
- Architecture test fails if the query endpoint returns a JPA entity type.
- `./gradlew test` passes.

## 06 Integration With Existing Code

- Controller maps domain/service output to DTOs.
- Service may still return domain objects until T5 introduces domain page
  assembly.
- Repository remains unchanged.

## 07 Principles

- Keep API DTOs separate from persistence entities.
- Preserve read-only behavior.
- Keep mapping deterministic and side-effect free.

## Blockers / Open Questions

None.

## Out of Scope

- Cursor values.
- Keyset pagination.
- Changes to `POST /audit-events`.
