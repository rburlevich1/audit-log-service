# T4 Plan — Add Response DTO, Mapping, and Contract Tests

Task reference: implements `.specs/query-api/tasks.md` § T4.

## Dependencies

T1 (public `id` shape, scalar vs structured `actor`/`resource`, and
`nextCursor`-on-last-page representation drive the DTO and Jackson
configuration), T3 (the placeholder wrapper from T3 is replaced here).

## 01 Problem

The Query API response shape must be stable on the wire before
pagination ships in T5: otherwise T5's integration tests would
implicitly define the response contract, which is the wrong layer to
own it. This task introduces dedicated DTOs (`AuditEventResponse`,
`AuditEventPageResponse`), a mapper, and ArchUnit guards so the
controller stops exposing JPA entity types. It also locks the
last-page `nextCursor` representation per T1.

## 02 Context

- `audit_events` is append-only; the Query API is read-only.
- Java 21 / Spring Boot 3 with Jackson default configuration.
- Existing entity `AuditEvent` uses storage-layer field names
  (`timestamp` / `context`) that differ from the wire-format names
  (`occurredAt` / `payload`).
- T3 has already established the endpoint surface and validation
  layer; T4 layers the formal response shape on top.

## 03 Constraints

- Read-only Query API.
- Append-only audit events.
- Spec is the source of truth: wire-format names and `nextCursor`
  representation match `design.md` §1 and the T1-resolved decisions.
- One safe PR: DTOs + mapper + controller switch + tests, no
  pagination work.
- No JPA entity leakage into API DTOs — enforced by both code and
  ArchUnit.
- No repository access from controllers — already enforced by
  ArchUnit; preserved here.
- No unrelated refactors.

## 04 Minimum Expected Changes

- Create `src/main/java/com/example/audit/event/AuditEventResponse.java`
  — record whose fields exactly match the T1-resolved wire shape.
- Create
  `src/main/java/com/example/audit/event/AuditEventPageResponse.java`
  — `record AuditEventPageResponse(List<AuditEventResponse> items, String nextCursor)`
  with Jackson configuration that matches T1's `nextCursor`
  representation (`@JsonInclude(NON_NULL)`, explicit null, or
  custom serializer for `""`).
- Create
  `src/main/java/com/example/audit/event/AuditEventMapper.java`
  — pure static mapping from entity to response DTO.
- Modify `src/main/java/com/example/audit/event/AuditEventController.java`
  to return `AuditEventPageResponse` (drop T3's placeholder).
- Modify `src/test/java/com/example/audit/ArchitectureTest.java`
  to add a rule banning controller methods from returning JPA
  `@Entity`-annotated classes.
- Modify
  `src/test/java/com/example/audit/AuditEventControllerTest.java`
  to assert wire-format JSON shape for non-empty and empty results.

## 05 Verification Method

- DoD bullets from `tasks.md` § T4 mapped to ArchUnit assertion +
  Testcontainers integration assertions.
- Integration tests use JSON-path or DTO deserialization to verify
  the exact wire shape: field names, presence/absence of
  `nextCursor` per T1 choice, no entity-only fields leaking
  (e.g. raw `event_timestamp`).
- ArchUnit rule fires at build time if a future change reintroduces
  an entity return type.
- `./gradlew test` passes.
- Spec consistency: response example in `design.md` §1 matches the
  shape asserted by the tests.

## 06 Integration With Existing Code

- API / controller layer: `AuditEventController.search` returns
  `AuditEventPageResponse`. The mapper is invoked at this boundary
  — domain objects do not flow further.
- Service / domain layer: `AuditEventService.search` may continue
  to return `List<AuditEvent>` (the controller maps), or it may
  return a domain `Page<AuditEvent>` — the simpler default is to
  keep the entity in the service and map at the controller.
- Repository / infrastructure layer: unchanged.
- ArchUnit enforces "controllers do not return `@Entity` classes" —
  this complements the existing
  "controllers do not access repositories" rule in
  `ArchitectureTest`.

## 07 Principles

- Determinism first: DTO order matches result order; the mapper is
  a pure transform.
- Preserve append-only and read-only invariants.
- Missing requirement → blocker: the `nextCursor` representation
  on the last page is a T1 decision; do not invent it.
- One safe PR.
- No pagination, no signing, no cursor code here.
- Do not edit `AGENTS.md`.
- ArchUnit rule additions stay narrow; do not introduce broad
  rules that would block unrelated future work.

## Blockers / Open Questions

- Blocked on T1 for: public `id` form (numeric vs ULID), `actor` /
  `resource` shape (scalar vs structured), `nextCursor`
  representation on the last page.
- If T1 inserts an upstream task to migrate to ULID or structured
  `actor`/`resource`, that task ships before T4 and T4 consumes
  its outputs without inventing fields.

## Out of Scope

- Real pagination behavior and `nextCursor` *values* on non-last
  pages (T5 covers this).
- Signed cursors and tamper detection (T5).
- Changes to `POST /audit-events` request/response shape.
- Performance tuning of the mapping path.
- Authentication and authorization.
