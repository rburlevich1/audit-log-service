# Spec Self-Eval: query-api

Date: 2026-05-18
Checklist: `.specs/_eval-checklist.md`

## Summary

- PASS: 26
- WEAK: 1
- FAIL: 0

## Findings

| Status | Area | Finding | Evidence |
|--------|------|---------|----------|
| PASS | Requirements language | Requirements, design, and tasks are written in English. | `.specs/query-api/requirements.md:1`, `.specs/query-api/design.md:1`, `.specs/query-api/tasks.md:1` |
| PASS | EARS acceptance criteria | Acceptance criteria use EARS-style `When ... the system shall ...` phrasing where practical. | `.specs/query-api/requirements.md:37`, `.specs/query-api/requirements.md:50`, `.specs/query-api/requirements.md:60` |
| WEAK | Acceptance criteria testability | The `limit` supplied criterion describes internal comparison behavior rather than an observable in-range result. | `.specs/query-api/requirements.md:68` |
| PASS | Inputs, outputs, validation, errors | Query inputs, response shape, validation rules, and status codes are explicit. | `.specs/query-api/design.md:7`, `.specs/query-api/design.md:24`, `.specs/query-api/design.md:46`, `.specs/query-api/design.md:182` |
| PASS | Open questions | Open questions are explicitly closed and prior planning deltas are recorded rather than silently absorbed. | `.specs/query-api/requirements.md:84`, `.specs/query-api/plans/_delta.md:255` |
| PASS | Planning decisions | Invented planning and implementation decisions are recorded in `_delta.md`, with later resolutions called out. | `.specs/query-api/plans/_delta.md:327`, `.specs/query-api/plans/_delta.md:348`, `.specs/query-api/plans/_delta.md:373` |
| PASS | Design traceability | Design sections trace to the Query API requirements for contract, pagination, sorting, validation, and invariants. | `.specs/query-api/requirements.md:30`, `.specs/query-api/design.md:3`, `.specs/query-api/design.md:62`, `.specs/query-api/design.md:266` |
| PASS | Task traceability | Tasks cite requirements and design sections and map implementation slices to spec decisions. | `.specs/query-api/tasks.md:5`, `.specs/query-api/tasks.md:40`, `.specs/query-api/tasks.md:89` |
| PASS | Contract discipline | No implementation decision appears only in design, tasks, or plans in a way that silently changes the requirement contract. | `.specs/query-api/requirements.md:88`, `.specs/query-api/design.md:275`, `.specs/query-api/plans/_delta.md:255` |
| PASS | Dependencies | Task dependencies are explicit. | `.specs/query-api/tasks.md:9`, `.specs/query-api/tasks.md:44`, `.specs/query-api/tasks.md:93`, `.specs/query-api/tasks.md:131` |
| PASS | Task DoD | Tasks have references and clear definitions of done. | `.specs/query-api/tasks.md:5`, `.specs/query-api/tasks.md:13`, `.specs/query-api/tasks.md:29`, `.specs/query-api/tasks.md:99` |
| PASS | AGENTS alignment | Design preserves repository invariants and workflow expectations for a read-only query endpoint. | `.specs/query-api/design.md:266` |
| PASS | Append-only behavior | The feature introduces only `GET /audit-events` and no update or delete behavior. | `.specs/query-api/requirements.md:76`, `.specs/query-api/design.md:270`, `.specs/query-api/tasks.md:135` |
| PASS | Server-assigned fields | The feature does not accept caller-supplied audit timestamps; `occurredAt` is a response mapping from storage. | `.specs/query-api/requirements.md:13`, `.specs/query-api/design.md:31`, `.specs/query-api/design.md:231` |
| PASS | Required actor invariant | The feature does not change event ingestion; query `actor` is an optional exact-match filter, while write validation remains outside this feature. | `.specs/query-api/design.md:9`, `.specs/query-api/requirements.md:76` |
| PASS | Deterministic ordering | List results define `occurredAt DESC` with `id DESC` as an explicit deterministic tiebreaker. | `.specs/query-api/requirements.md:50`, `.specs/query-api/design.md:115` |
| PASS | Pagination strategy | Cursor/keyset pagination is justified, including why offset pagination is rejected and how `limit + 1` detects another page. | `.specs/query-api/design.md:62`, `.specs/query-api/design.md:107` |
| PASS | Retention lifecycle | The feature does not touch event lifecycle or archival retrieval; archival retrieval is explicitly out of scope. | `.specs/query-api/requirements.md:78` |
| PASS | Unit tests | Tasks require unit tests for cursor signing, verification, fingerprint canonicalization, and cursor predicates. | `.specs/query-api/tasks.md:117` |
| PASS | Integration tests | Tasks require Testcontainers/Flyway, endpoint, pagination, invariant, and migration integration tests. | `.specs/query-api/tasks.md:34`, `.specs/query-api/tasks.md:60`, `.specs/query-api/tasks.md:119`, `.specs/query-api/tasks.md:156` |
| PASS | Invariant tests | T6 explicitly covers read-only behavior, no update/delete route, append-only behavior, and deterministic ordering. | `.specs/query-api/tasks.md:124`, `.specs/query-api/tasks.md:135` |
| PASS | Test data and boundaries | Boundary and error cases are described for empty results, malformed timestamps, invalid limits, cursor tampering, filter mismatch, and same-timestamp pagination. | `.specs/query-api/design.md:182`, `.specs/query-api/design.md:194`, `.specs/query-api/tasks.md:119` |
| PASS | Full suite | Each implementation task requires `./gradlew test` to pass. | `.specs/query-api/tasks.md:36`, `.specs/query-api/tasks.md:62`, `.specs/query-api/tasks.md:122`, `.specs/query-api/tasks.md:162` |
| PASS | Evidence quality | This report includes concise file and line evidence for every finding. | `.specs/query-api/eval-report-2026-05-18.md` |
| PASS | WEAK usage | `WEAK` is reserved for partial or indirect coverage. | `.specs/query-api/eval-report-2026-05-18.md` |
| PASS | FAIL usage | No missing, contradictory, or materially unsafe checklist item was found. | `.specs/query-api/eval-report-2026-05-18.md` |
| PASS | Next steps quality | Recommended next steps are specific and ordered by impact. | `.specs/query-api/eval-report-2026-05-18.md` |

## Details

### PASS

- Requirements are written in English and most acceptance criteria follow testable EARS-style `When ... shall ...` phrasing.
- The spec explicitly defines query parameters, response shape, status codes, validation rules, pagination behavior, sort order, and fixed decisions.
- Open questions are closed in `requirements.md`, and prior invented planning or implementation decisions are preserved in `_delta.md` with resolution notes.
- Design and tasks trace back to requirements. Tasks include references, dependencies, scoped PR boundaries, and DoD items.
- AGENTS.md invariants are preserved: the feature is a read-only query endpoint, introduces no update/delete route, preserves append-only audit events, and specifies deterministic list ordering with an explicit tiebreaker.
- Pagination is justified as keyset/cursor pagination. The design explains the offset tradeoff, stable ordering, cursor payload, filter fingerprint, HMAC tamper detection, and `limit + 1` next-page detection.
- Testing coverage is broad enough for the feature: unit tests for pure cursor/fingerprint logic, integration tests for endpoint and database behavior, invariant tests for hard audit rules, boundary cases, and a full `./gradlew test` gate.
- Retention/archival behavior is not affected by this feature, and archival retrieval is explicitly out of scope.

### WEAK

- `requirements.md` contains one acceptance criterion that is less directly observable than the others: "When `limit` is supplied, the system shall compare it with the configured maximum." The adjacent criteria define observable failures for `limit > max`, `limit < 1`, and omitted `limit`, but the successful in-range behavior would be clearer if stated as an output contract, for example: "When `limit` is within `[1, max]`, the system shall return at most `limit` items."

### FAIL

- None.

## Recommended Next Steps

1. Replace the internal `limit` comparison acceptance criterion with an observable in-range behavior.
2. Align the stale T2/design cleanup-trigger wording with the current T7 decision, or explicitly mark the staging language as historical/example-only.

---

# Spec Self-Eval: query-api

Date: 2026-05-18
Checklist: `.specs/_eval-checklist.md`

## Summary

- PASS: 26
- WEAK: 2
- FAIL: 0

## Findings

| Status | Area | Finding | Evidence |
|--------|------|---------|----------|
| PASS | Requirements language | Requirements are written in English. | `.specs/query-api/requirements.md:1` |
| PASS | EARS acceptance criteria | Most acceptance criteria use direct `When ... shall ...` phrasing. | `.specs/query-api/requirements.md:37`, `.specs/query-api/requirements.md:50`, `.specs/query-api/requirements.md:60` |
| WEAK | Testable acceptance criteria | One limit criterion describes internal comparison rather than an observable result; adjacent criteria cover invalid and omitted limits. | `.specs/query-api/requirements.md:68`, `.specs/query-api/requirements.md:70` |
| PASS | Inputs, outputs, validation, errors | Query parameters, response shape, status codes, and validation failures are explicit. | `.specs/query-api/requirements.md:9`, `.specs/query-api/requirements.md:27`, `.specs/query-api/design.md:46`, `.specs/query-api/design.md:182` |
| PASS | Open questions | Open questions are explicitly closed. | `.specs/query-api/requirements.md:84` |
| PASS | Delta recording | Prior planning/implementation decisions are recorded and resolved in `_delta.md`. | `.specs/query-api/plans/_delta.md:255`, `.specs/query-api/plans/_delta.md:327`, `.specs/query-api/plans/_delta.md:348`, `.specs/query-api/plans/_delta.md:373` |
| PASS | Design traceability | Design sections trace to requirement-level choices for API contract, pagination, sorting, validation, and invariants. | `.specs/query-api/design.md:3`, `.specs/query-api/design.md:62`, `.specs/query-api/design.md:115`, `.specs/query-api/design.md:266` |
| PASS | Task traceability | Tasks include refs back to requirements and design sections. | `.specs/query-api/tasks.md:5`, `.specs/query-api/tasks.md:40`, `.specs/query-api/tasks.md:89`, `.specs/query-api/tasks.md:126` |
| PASS | Requirement contract drift | Implementation-level choices in plans are either reflected in design/tasks or documented as historical delta; no unrecorded product contract change was found. | `.specs/query-api/design.md:90`, `.specs/query-api/tasks.md:106`, `.specs/query-api/plans/_delta.md:404` |
| PASS | Task dependencies | Dependencies are explicit for each task. | `.specs/query-api/tasks.md:9`, `.specs/query-api/tasks.md:25`, `.specs/query-api/tasks.md:93`, `.specs/query-api/tasks.md:147` |
| PASS | Task references and DoD | Tasks include scoped references and definition-of-done checks. | `.specs/query-api/tasks.md:13`, `.specs/query-api/tasks.md:29`, `.specs/query-api/tasks.md:99`, `.specs/query-api/tasks.md:152` |
| WEAK | Plan consistency | T7 now retires the staging-evidence gate, but T2's plan still says cleanup is deferred until staging evidence; design also keeps staging as the example trigger. | `.specs/query-api/plans/T2-plan.md:21`, `.specs/query-api/plans/T7-plan.md:22`, `.specs/query-api/design.md:164`, `.specs/query-api/tasks.md:163` |
| PASS | AGENTS.md alignment | Design explicitly maps the Query API to read-only, append-only, and deterministic ordering invariants. | `.specs/query-api/design.md:266` |
| PASS | Append-only behavior | The feature introduces no update/delete API and keeps the read path non-mutating. | `.specs/query-api/requirements.md:52`, `.specs/query-api/requirements.md:76`, `.specs/query-api/design.md:270` |
| PASS | Server-assigned fields | The feature does not accept caller-supplied audit timestamps; `occurredAt` is a response mapping from stored event timestamp. | `.specs/query-api/requirements.md:13`, `.specs/query-api/design.md:31`, `.specs/query-api/design.md:231` |
| PASS | Required actor invariant | The feature does not change ingestion; query `actor` is an optional exact-match filter only. | `.specs/query-api/design.md:9`, `.specs/query-api/requirements.md:76` |
| PASS | Deterministic ordering | Results order by `occurredAt DESC` with `id DESC` as an explicit tiebreaker. | `.specs/query-api/requirements.md:50`, `.specs/query-api/design.md:115` |
| PASS | Pagination strategy | Keyset pagination is justified and `limit + 1` next-page detection is specified. | `.specs/query-api/design.md:62`, `.specs/query-api/design.md:107` |
| PASS | Retention lifecycle | The feature does not touch retention or archival retrieval; archival retrieval is explicitly out of scope. | `.specs/query-api/requirements.md:78` |
| PASS | Unit tests | Tasks require unit tests for cursor signing, verification, fingerprint canonicalization, and cursor predicates. | `.specs/query-api/tasks.md:117` |
| PASS | Integration tests | Tasks require Testcontainers/Flyway, endpoint, pagination, invariant, and migration integration tests. | `.specs/query-api/tasks.md:34`, `.specs/query-api/tasks.md:60`, `.specs/query-api/tasks.md:119`, `.specs/query-api/tasks.md:156` |
| PASS | Invariant tests | T6 covers read-only behavior, no update/delete route, append-only behavior, and deterministic ordering. | `.specs/query-api/tasks.md:124`, `.specs/query-api/tasks.md:135` |
| PASS | Test data and boundaries | Boundary/error cases cover empty results, malformed timestamps, invalid limits, cursor tampering, filter mismatch, and same-timestamp pagination. | `.specs/query-api/design.md:182`, `.specs/query-api/design.md:194`, `.specs/query-api/tasks.md:119` |
| PASS | Full suite | Each implementation task requires `./gradlew test` to pass. | `.specs/query-api/tasks.md:36`, `.specs/query-api/tasks.md:62`, `.specs/query-api/tasks.md:122`, `.specs/query-api/tasks.md:162` |
| PASS | Evidence quality | This report includes concise file and line evidence for every finding. | `.specs/query-api/eval-report-2026-05-18.md` |
| PASS | WEAK usage | `WEAK` is used only for partial, indirect, vague, or stale coverage. | `.specs/query-api/eval-report-2026-05-18.md` |
| PASS | FAIL usage | No missing, contradictory, or materially unsafe checklist item was found. | `.specs/query-api/eval-report-2026-05-18.md` |
| PASS | Next steps quality | Recommended next steps are specific and ordered by impact. | `.specs/query-api/eval-report-2026-05-18.md` |

## Details

### PASS

- The current Query API spec is broadly consistent across requirements, design, tasks, and plans. It defines the read-only endpoint, filters, response shape, status-code split, deterministic ordering, signed keyset cursor behavior, and validation rules.
- AGENTS.md invariants are preserved: the feature adds only a read endpoint, does not introduce update/delete routes, treats stored audit events as append-only, and specifies an explicit sort tiebreaker.
- Testing expectations are strong enough for the feature: pure cursor/fingerprint logic has unit-test coverage, endpoint and database behavior use integration tests, invariant tests cover the hard audit rules, and implementation tasks require `./gradlew test`.
- Prior planning deltas are recorded in `_delta.md`, including the T7 gate relaxation and implementation-time test-client choice.

### WEAK

- `requirements.md` has one acceptance criterion that is not directly observable: "When `limit` is supplied, the system shall compare it with the configured maximum." The intended observable behavior is mostly covered by adjacent criteria, but the successful in-range case would be clearer if stated as "return at most `limit` items" for `1 <= limit <= max`.
- There is residual stale planning language around T7 index cleanup. `tasks.md` and `T7-plan.md` now say local Testcontainers verification replaced the staging-evidence gate, but `T2-plan.md` still says cleanup is deferred until staging evidence and `design.md` still uses staging evidence as the example trigger.

### FAIL

- None.

## Recommended Next Steps

1. Reword the `limit` acceptance criterion into an observable successful in-range behavior.
2. Align the stale T2/design cleanup-trigger wording with the current T7 decision, or explicitly mark the staging language as historical/example-only.

---

# Spec Self-Eval: query-api

Date: 2026-05-18
Checklist: `.specs/_eval-checklist.md`

## Summary

- PASS: 27
- WEAK: 0
- FAIL: 0

## Findings

| Status | Area | Finding | Evidence |
|--------|------|---------|----------|
| PASS | Acceptance criteria testability | The `limit` supplied criterion now states observable in-range behavior: return at most `limit` items. | `.specs/query-api/requirements.md:68` |
| PASS | Index cleanup decision | Active design and plans now state that legacy index cleanup is verified locally with Testcontainers because this repository has no staging environment. | `.specs/query-api/design.md:156`, `.specs/query-api/tasks.md:163`, `.specs/query-api/plans/T2-plan.md:21`, `.specs/query-api/plans/T7-plan.md:21` |
| PASS | Overall checklist | The updated requirements, design, tasks, and plans satisfy the expanded repo checklist with no remaining WEAK or FAIL findings. | `.specs/_eval-checklist.md`, `.specs/query-api/requirements.md`, `.specs/query-api/design.md`, `.specs/query-api/tasks.md` |

## Details

### PASS

- The previous `limit` weakness is fixed by replacing internal comparison wording with an observable successful in-range result.
- The previous T2/design cleanup-trigger weakness is fixed by removing active staging-gate wording and documenting local Testcontainers verification because this repository has no staging environment.
- Historical `_delta.md` mentions of the retired staging gate remain decision history and are followed by the recorded resolution.

### WEAK

- None.

### FAIL

- None.

## Recommended Next Steps

1. None for the specification.

---

# Spec Self-Eval: query-api

Date: 2026-05-18
Checklist: `.specs/_eval-checklist.md`

## Summary

- PASS: 31
- WEAK: 0
- FAIL: 0

## Findings

| Status | Area | Finding | Evidence |
|--------|------|---------|----------|
| PASS | Delta path | Recent planning/spec-fix decisions are now recorded at the required `.specs/<feature>/_delta.md` path. | `.specs/query-api/_delta.md:1`, `AGENTS.md:74` |
| PASS | Corrective transparency | The root delta file explicitly records that the decisions were added after the spec edits and marks that ordering as a process error. | `.specs/query-api/_delta.md:29` |
| PASS | Observable validation contract | Requirements own the `from`/`to` validation contract: malformed timestamps return `400`, and `from > to` returns `422`. | `.specs/query-api/requirements.md:75`, `.specs/query-api/requirements.md:77` |
| PASS | Staged cursor work | T3 is documented as an internal implementation slice, not the complete externally releasable Query API contract. | `.specs/query-api/tasks.md:46` |
| PASS | Overall checklist | The updated requirements, design, tasks, active plans, and root delta file satisfy the expanded repo checklist with no remaining WEAK or FAIL findings. | `.specs/_eval-checklist.md`, `.specs/query-api/_delta.md`, `.specs/query-api/requirements.md`, `.specs/query-api/design.md`, `.specs/query-api/tasks.md` |

## Details

### PASS

- The feature now has `.specs/query-api/_delta.md`, matching the path required by `AGENTS.md`.
- The recent spec-fix decisions are recorded in the root delta file, including the corrective note that they were recorded late.
- The latest validation-contract and staged-cursor weak findings remain fixed in the source specs.

### WEAK

- None.

### FAIL

- None.

## Recommended Next Steps

1. Use `.specs/query-api/_delta.md` for future Query API planning decisions before editing the source spec.

---

# Spec Self-Eval: query-api

Date: 2026-05-18
Checklist: `.specs/_eval-checklist.md`

## Summary

- PASS: 27
- WEAK: 0
- FAIL: 0

## Findings

| Status | Area | Finding | Evidence |
|--------|------|---------|----------|
| PASS | Requirements language | Requirements, design, and tasks are written in English. | `.specs/query-api/requirements.md:1`, `.specs/query-api/design.md:1`, `.specs/query-api/tasks.md:1` |
| PASS | EARS acceptance criteria | Acceptance criteria use EARS-style `When ... the system shall ...` phrasing where practical. | `.specs/query-api/requirements.md:37`, `.specs/query-api/requirements.md:50`, `.specs/query-api/requirements.md:60`, `.specs/query-api/requirements.md:68` |
| PASS | Acceptance criteria testability | The `limit` supplied criterion now states observable in-range behavior: return at most `limit` items. | `.specs/query-api/requirements.md:68` |
| PASS | Inputs, outputs, validation, errors | Query inputs, response shape, validation rules, and status codes are explicit. | `.specs/query-api/design.md:7`, `.specs/query-api/design.md:24`, `.specs/query-api/design.md:46`, `.specs/query-api/design.md:182` |
| PASS | Open questions | Open questions are explicitly closed and prior planning deltas are recorded rather than silently absorbed. | `.specs/query-api/requirements.md:84`, `.specs/query-api/plans/_delta.md:255` |
| PASS | Planning decisions | Invented planning and implementation decisions are recorded in `_delta.md`, with later resolutions called out. | `.specs/query-api/plans/_delta.md:327`, `.specs/query-api/plans/_delta.md:348`, `.specs/query-api/plans/_delta.md:373` |
| PASS | Design traceability | Design sections trace to the Query API requirements for contract, pagination, sorting, validation, and invariants. | `.specs/query-api/requirements.md:30`, `.specs/query-api/design.md:3`, `.specs/query-api/design.md:62`, `.specs/query-api/design.md:266` |
| PASS | Task traceability | Tasks cite requirements and design sections and map implementation slices to spec decisions. | `.specs/query-api/tasks.md:5`, `.specs/query-api/tasks.md:40`, `.specs/query-api/tasks.md:89` |
| PASS | Contract discipline | No implementation decision appears only in design, tasks, or plans in a way that silently changes the requirement contract. | `.specs/query-api/requirements.md:88`, `.specs/query-api/design.md:275`, `.specs/query-api/plans/_delta.md:255` |
| PASS | Dependencies | Task dependencies are explicit. | `.specs/query-api/tasks.md:9`, `.specs/query-api/tasks.md:44`, `.specs/query-api/tasks.md:93`, `.specs/query-api/tasks.md:131` |
| PASS | Task DoD | Tasks have references and clear definitions of done. | `.specs/query-api/tasks.md:5`, `.specs/query-api/tasks.md:13`, `.specs/query-api/tasks.md:29`, `.specs/query-api/tasks.md:99` |
| PASS | AGENTS alignment | Design preserves repository invariants and workflow expectations for a read-only query endpoint. | `.specs/query-api/design.md:266` |
| PASS | Append-only behavior | The feature introduces only `GET /audit-events` and no update or delete behavior. | `.specs/query-api/requirements.md:76`, `.specs/query-api/design.md:270`, `.specs/query-api/tasks.md:135` |
| PASS | Server-assigned fields | The feature does not accept caller-supplied audit timestamps; `occurredAt` is a response mapping from storage. | `.specs/query-api/requirements.md:13`, `.specs/query-api/design.md:31`, `.specs/query-api/design.md:231` |
| PASS | Required actor invariant | The feature does not change event ingestion; query `actor` is an optional exact-match filter, while write validation remains outside this feature. | `.specs/query-api/design.md:9`, `.specs/query-api/requirements.md:76` |
| PASS | Deterministic ordering | List results define `occurredAt DESC` with `id DESC` as an explicit deterministic tiebreaker. | `.specs/query-api/requirements.md:50`, `.specs/query-api/design.md:115` |
| PASS | Pagination strategy | Cursor/keyset pagination is justified, including why offset pagination is rejected and how `limit + 1` detects another page. | `.specs/query-api/design.md:62`, `.specs/query-api/design.md:107` |
| PASS | Index cleanup decision | Active design and plans now state that legacy index cleanup is verified locally with Testcontainers because this repository has no staging environment. | `.specs/query-api/design.md:156`, `.specs/query-api/tasks.md:163`, `.specs/query-api/plans/T2-plan.md:21`, `.specs/query-api/plans/T7-plan.md:21` |
| PASS | Retention lifecycle | The feature does not touch event lifecycle or archival retrieval; archival retrieval is explicitly out of scope. | `.specs/query-api/requirements.md:78` |
| PASS | Unit tests | Tasks require unit tests for cursor signing, verification, fingerprint canonicalization, and cursor predicates. | `.specs/query-api/tasks.md:117` |
| PASS | Integration tests | Tasks require Testcontainers/Flyway, endpoint, pagination, invariant, and migration integration tests. | `.specs/query-api/tasks.md:34`, `.specs/query-api/tasks.md:60`, `.specs/query-api/tasks.md:119`, `.specs/query-api/tasks.md:156` |
| PASS | Invariant tests | T6 explicitly covers read-only behavior, no update/delete route, append-only behavior, and deterministic ordering. | `.specs/query-api/tasks.md:124`, `.specs/query-api/tasks.md:135` |
| PASS | Test data and boundaries | Boundary and error cases are described for empty results, malformed timestamps, invalid limits, cursor tampering, filter mismatch, and same-timestamp pagination. | `.specs/query-api/design.md:182`, `.specs/query-api/design.md:194`, `.specs/query-api/tasks.md:119` |
| PASS | Full suite | Each implementation task requires `./gradlew test` to pass. | `.specs/query-api/tasks.md:36`, `.specs/query-api/tasks.md:62`, `.specs/query-api/tasks.md:122`, `.specs/query-api/tasks.md:162` |
| PASS | Evidence quality | This report includes concise file and line evidence for every finding. | `.specs/query-api/eval-report-2026-05-18.md` |
| PASS | WEAK usage | No partial, indirect, vague, or stale checklist item remains in this evaluation. | `.specs/query-api/eval-report-2026-05-18.md` |
| PASS | FAIL usage | No missing, contradictory, or materially unsafe checklist item was found. | `.specs/query-api/eval-report-2026-05-18.md` |
| PASS | Next steps quality | No spec fixes are recommended by this evaluation. | `.specs/query-api/eval-report-2026-05-18.md` |

## Details

### PASS

- The `limit` acceptance criterion now states the observable successful in-range behavior: for `1 <= limit <= configured maximum`, the system returns at most `limit` items.
- Active index-cleanup wording is aligned with the current T7 decision: this repository has no staging environment, so T7 is verified locally with Testcontainers. Historical `_delta.md` entries remain as decision history and are followed by the recorded resolution.
- Requirements, design, tasks, and current plans remain consistent on the read-only Query API contract, deterministic ordering, signed keyset cursor behavior, validation rules, and verification gates.
- AGENTS.md invariants remain preserved: no update/delete route is introduced, audit events remain append-only, server-assigned timestamps are not accepted from query callers, and list ordering has an explicit tiebreaker.

### WEAK

- None.

### FAIL

- None.

## Recommended Next Steps

1. None for the specification. Continue with implementation or review using `requirements.md`, `design.md`, and `tasks.md` as the current source of truth.

---

# Spec Self-Eval: query-api

Date: 2026-05-18
Checklist: `.specs/_eval-checklist.md`

## Summary

- PASS: 30
- WEAK: 0
- FAIL: 0

## Findings

| Status | Area | Finding | Evidence |
|--------|------|---------|----------|
| PASS | `from > to` requirement | The user-visible inverted time range contract is now an acceptance criterion with `422 Unprocessable Entity`. | `.specs/query-api/requirements.md:77`, `.specs/query-api/design.md:52` |
| PASS | Timestamp parse failure | Requirements now state malformed `from` or `to` timestamps return `400 Bad Request`, matching the controller binding behavior and design status table. | `.specs/query-api/requirements.md:75`, `.specs/query-api/design.md:51`, `src/main/java/com/example/audit/event/AuditEventExceptionHandler.java:39` |
| PASS | T3 staged cursor behavior | T3 now explicitly says temporary non-null cursor rejection is an internal implementation slice, not the complete externally releasable Query API contract. | `.specs/query-api/tasks.md:46`, `.specs/query-api/tasks.md:52`, `.specs/query-api/tasks.md:93` |
| PASS | Overall checklist | The updated requirements, design, tasks, and active plans satisfy the expanded repo checklist with no remaining WEAK or FAIL findings. | `.specs/_eval-checklist.md`, `.specs/query-api/requirements.md`, `.specs/query-api/design.md`, `.specs/query-api/tasks.md` |

## Details

### PASS

- `requirements.md` now promotes the `from > to` validation rule from design-only behavior to source-of-truth acceptance criteria.
- Timestamp parse failures are now explicit in requirements as `400 Bad Request`, matching the current Spring `Instant` query binding and exception handler behavior.
- T3's temporary cursor rejection is documented as a staged implementation slice. The final external Query API contract remains the post-T5 signed cursor behavior.
- The earlier fixed `limit` criterion and local-only T7 cleanup decision remain aligned.

### WEAK

- None.

### FAIL

- None.

## Recommended Next Steps

1. None for the specification.

---

# Spec Self-Eval: query-api

Date: 2026-05-18
Checklist: `.specs/_eval-checklist.md`

## Summary

- PASS: 20
- WEAK: 3
- FAIL: 0

## Findings

| Status | Area | Finding | Evidence |
|--------|------|---------|----------|
| PASS | Requirements | Requirements are written in English and use mostly EARS-style, testable acceptance criteria. | `requirements.md:30-75` |
| PASS | Requirements | Core inputs, response shape, pagination fields, limit bounds, cursor errors, and fixed page-size decisions are explicit. | `requirements.md:9-28`, `requirements.md:60-75`, `requirements.md:88-96` |
| WEAK | Requirements | `from > to` is an observable API error only specified in design, not in requirements. | `requirements.md:40-41`, `design.md:52-57`, `design.md:184-185` |
| WEAK | Requirements | Timestamp parse failures are required to be UTC instants, but the requirements do not state the expected error status; design adds `400`. | `requirements.md:74-75`, `design.md:51`, `design.md:184` |
| PASS | Requirements | Open questions are explicit and not contradicted by unresolved entries in the feature spec. | `requirements.md:84-96` |
| PASS | Traceability | Most design decisions trace to requirements: keyset pagination, cursor mismatch handling, deterministic sort, limits, and response shape. | `requirements.md:50-72`, `design.md:62-129` |
| PASS | Traceability | Tasks include references to requirements/design sections and define DoD for each implementation slice. | `tasks.md:3-166` |
| PASS | Traceability | Dependencies between tasks are explicit. | `tasks.md:9`, `tasks.md:25`, `tasks.md:44`, `tasks.md:70`, `tasks.md:93`, `tasks.md:131`, `tasks.md:147` |
| WEAK | Traceability | T3 can temporarily expose `GET /audit-events` with all query parameters while rejecting any cursor as unsupported, and the task list does not state a release gate preventing that interim contract from shipping. | `tasks.md:46-58`, `requirements.md:60-67` |
| PASS | Architecture | Design aligns with AGENTS.md invariants: read-only query, no update/delete route, append-only data, and deterministic ordering. | `design.md:263-274` |
| PASS | Architecture | Append-only behavior is preserved; creating, updating, and deleting events are out of scope. | `requirements.md:52-53`, `requirements.md:76-82`, `design.md:267-269` |
| PASS | Architecture | Server-assigned timestamps are not accepted from query callers; `occurredAt` is only a response field and filter boundary. | `requirements.md:13-24`, `design.md:9-16`, `design.md:228-229` |
| PASS | Architecture | Required event actor is not weakened by this read-only API; actor is an optional filter and scalar response field. | `requirements.md:37-39`, `requirements.md:94-96`, `design.md:11`, `design.md:43-44` |
| PASS | Architecture | List ordering is deterministic with `occurredAt DESC` and `id DESC` as an explicit tiebreaker. | `requirements.md:50-51`, `design.md:115-129` |
| PASS | Architecture | Pagination strategy is justified and uses keyset pagination for large append-only result sets. | `requirements.md:55-75`, `design.md:62-113` |
| PASS | Architecture | Retention and archival behavior are scoped out because this feature does not alter event lifecycle. | `requirements.md:76-82`, `design.md:203` |
| PASS | Testing | Unit tests are required for cursor signing, verification, fingerprinting, and cursor predicate logic. | `tasks.md:117-118` |
| PASS | Testing | Integration tests are required for endpoint behavior, database migrations, pagination, cursor errors, and response contracts. | `tasks.md:34-36`, `tasks.md:60-62`, `tasks.md:83-85`, `tasks.md:119-122` |
| PASS | Testing | Invariant tests cover read-only/append-only behavior and deterministic ordering. | `tasks.md:124-140` |
| PASS | Testing | Boundary and error cases are detailed enough to implement, including malformed/tampered cursors, filter mismatch, invalid limits, and same-timestamp ties. | `design.md:179-203`, `tasks.md:109-121` |
| PASS | Testing | Each implementation task requires `./gradlew test` to pass. | `tasks.md:36`, `tasks.md:62`, `tasks.md:85`, `tasks.md:122`, `tasks.md:140`, `tasks.md:162` |
| PASS | Report Quality | This report classifies partial/specification-source gaps as `WEAK`, reserves `FAIL` for missing or contradictory coverage, and cites concise evidence. | This report |
| PASS | Report Quality | Recommended next steps are ordered by impact. | This report |

## Details

### PASS

- The core Query API contract is clear and mostly requirement-backed: filters, response item shape, page envelope, cursor continuation, deterministic order, page-size defaults, and max limits are all documented.
- The design preserves project invariants: the endpoint is read-only, introduces no update/delete route, does not accept caller-supplied event timestamps, and defines an explicit ordering tiebreaker.
- Pagination is justified and concrete. The cursor carries sort keys and a filter fingerprint, uses signed keyset pagination, and handles malformed, tampered, and filter-mismatched cursors distinctly.
- The task plan has explicit dependencies, references, per-task DoD, integration coverage, invariant coverage, and the repository-wide `./gradlew test` gate.

### WEAK

- `from > to` is a user-visible validation contract in `design.md` but not in `requirements.md`. Since requirements are the source of truth, this should be promoted to an acceptance criterion.
- Timestamp parse failure behavior is only indirectly covered in requirements. `requirements.md` requires ISO-8601 UTC instants, while `design.md` defines `400`; requirements should state the expected error response.
- T3 intentionally rejects any non-null cursor before T5 adds signed pagination. That may be acceptable as an implementation slice, but the task list does not state that this interim behavior must not be considered the complete Query API contract or externally released as final.

### FAIL

- None.

## Recommended Next Steps

1. Add requirements-level acceptance criteria for `from > to` returning `422` and malformed/non-UTC timestamp values returning `400`.
2. Clarify in `tasks.md` whether T3 is an internal incremental slice and that the final Query API is not complete until T5/T6 are done.
3. Continue implementation only after confirming the requirements remain the source of truth for all observable API behavior.

---

# Spec Self-Eval: query-api

Date: 2026-05-18
Checklist: `.specs/_eval-checklist.md`

## Summary

- PASS: 30
- WEAK: 0
- FAIL: 0

## Findings

| Status | Area | Finding | Evidence |
|--------|------|---------|----------|
| PASS | `from > to` requirement | The inverted time range contract is now a requirements-level acceptance criterion with `422 Unprocessable Entity`. | `.specs/query-api/requirements.md:77`, `.specs/query-api/design.md:52` |
| PASS | Timestamp parse failure | Requirements now state malformed `from` or `to` timestamps return `400 Bad Request`, matching the current implementation's query binding behavior. | `.specs/query-api/requirements.md:75`, `.specs/query-api/design.md:51`, `src/main/java/com/example/audit/event/AuditEventExceptionHandler.java:46` |
| PASS | T3 staged cursor behavior | T3 now says temporary non-null cursor rejection is an internal implementation slice, not the complete externally releasable Query API contract. | `.specs/query-api/tasks.md:46`, `.specs/query-api/tasks.md:52`, `.specs/query-api/tasks.md:93` |
| PASS | Overall checklist | The updated requirements, design, tasks, and active plans satisfy the expanded repo checklist with no remaining WEAK or FAIL findings. | `.specs/_eval-checklist.md`, `.specs/query-api/requirements.md`, `.specs/query-api/design.md`, `.specs/query-api/tasks.md` |

## Details

### PASS

- The three latest WEAK findings are fixed in source specs.
- Requirements now own the observable `from`/`to` validation contract.
- T3 is explicitly documented as an internal incremental slice; T5 remains the point where the final signed cursor contract is complete.

### WEAK

- None.

### FAIL

- None.

## Recommended Next Steps

1. None for the specification.
