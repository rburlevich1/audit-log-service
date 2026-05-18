# Spec Self-Eval: query-api

Date: 2026-05-19
Checklist: .specs/_eval-checklist.md

## Summary

- PASS: 6
- WEAK: 1
- FAIL: 1

## Findings

| Status | Area | Finding | Evidence |
|--------|------|---------|----------|
| PASS | Requirements | Acceptance criteria are testable for filters, inclusive time bounds, deterministic ordering, read-only behavior, cursor errors, limit bounds, and timestamp validation. | `requirements.md:37`, `requirements.md:40`, `requirements.md:50`, `requirements.md:52`, `requirements.md:60`, `requirements.md:64`, `requirements.md:68`, `requirements.md:74` |
| PASS | Pagination | Keyset pagination is justified against offset pagination and tied to append-only behavior, stable sort keys, and `limit + 1` page detection. | `design.md:62`, `design.md:66`, `design.md:76`, `design.md:107`, `design.md:111` |
| WEAK | Tasks | Tasks have references and DoD, but some references point to `requirements.md` sections that are absent in the current file. | `tasks.md:6`, `tasks.md:70`, `tasks.md:130`; `requirements.md:1`, `requirements.md:30`, `requirements.md:45`, `requirements.md:55` |
| PASS | Dependencies | Task dependencies are explicit for every task, including `none` for T1 and prerequisite task lists for T2-T7. | `tasks.md:9`, `tasks.md:25`, `tasks.md:44`, `tasks.md:73`, `tasks.md:96`, `tasks.md:134`, `tasks.md:150` |
| FAIL | Open Questions | Open questions and fixed decisions are not explicit in `requirements.md`; design and plans answer source-of-truth decisions that the current requirements file does not record. | `requirements.md:1`, `requirements.md:30`; `design.md:16`, `design.md:19`, `design.md:21`, `design.md:90`, `design.md:203`, `design.md:272`; `plans/T1-plan.md:41`, `plans/T1-plan.md:61` |
| PASS | Invariants | The design aligns with AGENTS.md invariants by keeping the endpoint read-only, preserving append-only behavior, avoiding update/delete routes, and defining deterministic ordering. | `design.md:5`, `design.md:267`, `design.md:268`, `design.md:269`, `design.md:270` |
| PASS | Sorting | The list endpoint defines deterministic ordering with explicit `occurredAt DESC` primary sort and `id DESC` tiebreaker. | `requirements.md:50`, `design.md:115`, `design.md:117`, `design.md:118` |
| PASS | Verification | Verification includes relevant integration, invariant, architecture, and unit tests, plus the repository-wide `./gradlew test` command. | `tasks.md:34`, `tasks.md:63`, `tasks.md:81`, `tasks.md:120`, `tasks.md:122`, `tasks.md:138`, `tasks.md:143` |

## Details

### PASS

- Requirements are testable. The acceptance criteria specify concrete request parameters, boundary behavior, deterministic ordering, read-only behavior, cursor error semantics, limit validation, and timestamp parsing.
- Pagination strategy is justified. The design explains why keyset pagination is chosen over offset pagination, how cursor keys and filter fingerprints work, and how `limit + 1` detects the next page.
- Dependencies are explicit. Every task lists dependencies, with later work sequenced after the spec, index, contract, response, and pagination tasks that it depends on.
- AGENTS.md invariants are covered in design. The design keeps the query endpoint read-only, introduces no update/delete route, preserves append-only assumptions, and requires deterministic ordering.
- Sorting is deterministic. Requirements and design both require `occurredAt DESC` plus `id DESC`.
- Verification coverage is broad enough for the feature. Tasks require Testcontainers migration checks, endpoint integration tests, DTO/architecture tests, cursor unit tests, pagination integration tests, invariant tests, and `./gradlew test`.

### WEAK

- Task references are partially stale. T1 and T4 cite `requirements.md` "Fixed Decisions", and T6 cites `requirements.md` "Out of Scope", but the current `requirements.md` only has `Problem` and `User Stories With AC` sections. The task DoDs are still actionable, so this is a traceability weakness rather than an execution blocker by itself.

### FAIL

- The source-of-truth requirements file does not explicitly record open questions or several fixed decisions. The design and plans state or rely on decisions for default/max page size, auth being out of scope, cursor secret configuration, secret rotation, and no maximum time span, while `requirements.md` does not contain `Open Questions`, `Fixed Decisions`, or `Out of Scope` sections. `plans/T1-plan.md` also claims `requirements.md` has `Open Questions: None`, which is false for the current file. This violates the checklist rule that open questions be explicit and not silently answered in design or plans.

## Recommended Next Steps

1. Update `requirements.md` to explicitly record `Open Questions`, fixed Query API decisions, and out-of-scope items that are already used by `design.md`, `tasks.md`, and plans.
2. Correct task references that point to absent requirements sections, especially T1, T4, and T6.
3. Re-run this self-eval after the requirements source-of-truth sections and task references are aligned.

---

# Spec Self-Eval: query-api

Date: 2026-05-19
Checklist: `.specs/_eval-checklist.md`

## Summary

- PASS: 8
- WEAK: 0
- FAIL: 0

## Findings

| Status | Area | Finding | Evidence |
|--------|------|---------|----------|
| PASS | Acceptance Criteria | Acceptance criteria are testable for filters, inclusive time bounds, deterministic ordering, read-only behavior, cursor errors, limit bounds, timestamp parsing, and invalid time ranges. | `requirements.md:37`, `requirements.md:40`, `requirements.md:50`, `requirements.md:52`, `requirements.md:60`, `requirements.md:64`, `requirements.md:68`, `requirements.md:74`, `requirements.md:77` |
| PASS | Pagination | The design justifies keyset cursor pagination over offset pagination and explains cursor keys, filter fingerprinting, `limit + 1` page detection, and no-loss/no-duplication behavior. | `design.md:62`, `design.md:66`, `design.md:76`, `design.md:83`, `design.md:107`, `design.md:111` |
| PASS | Tasks | Each task has references, dependencies, scope, and a concrete DoD. | `tasks.md:5`, `tasks.md:9`, `tasks.md:13`, `tasks.md:22`, `tasks.md:25`, `tasks.md:29`, `tasks.md:40`, `tasks.md:44`, `tasks.md:51`, `tasks.md:92`, `tasks.md:96`, `tasks.md:102` |
| PASS | Dependencies | Task dependencies are explicit, including `none` for T1 and prerequisite task lists for T2 through T7. | `tasks.md:9`, `tasks.md:25`, `tasks.md:44`, `tasks.md:73`, `tasks.md:96`, `tasks.md:134`, `tasks.md:150` |
| PASS | Open Questions | Open questions are explicit in the requirements, and fixed decisions used by the design and tasks are recorded in the requirements source of truth. | `requirements.md:80`, `requirements.md:88`, `requirements.md:90`, `requirements.md:92`, `requirements.md:94`, `requirements.md:100`; `design.md:21`, `design.md:43`, `design.md:203` |
| PASS | AGENTS.md Invariants | The design aligns with project invariants by keeping the endpoint read-only, preserving append-only behavior, avoiding update/delete routes, and requiring deterministic ordering. | `requirements.md:52`, `requirements.md:80`; `design.md:5`, `design.md:263`, `design.md:267`, `design.md:268`, `design.md:269`, `design.md:270` |
| PASS | Deterministic Sorting | The list endpoint defines deterministic sorting with `occurredAt DESC` and event `id DESC` as the explicit tiebreaker. | `requirements.md:50`, `requirements.md:51`, `design.md:99`, `design.md:115`, `design.md:117`, `design.md:118` |
| PASS | Verification | Verification includes relevant migration, endpoint integration, response contract, cursor unit, pagination integration, invariant, architecture, and full-suite checks. | `tasks.md:34`, `tasks.md:63`, `tasks.md:81`, `tasks.md:86`, `tasks.md:120`, `tasks.md:122`, `tasks.md:138`, `tasks.md:143`, `tasks.md:165` |

## Details

### PASS

- Acceptance criteria are testable. The requirements define concrete request parameters, inclusive boundary behavior, deterministic ordering, read-only expectations, cursor error status codes, limit bounds, timestamp parsing, and `from > to` behavior.
- Pagination strategy is justified. The design explains why keyset pagination fits append-only audit events, how cursor keys and filter fingerprints work, and how `limit + 1` detects whether another page exists.
- Tasks are actionable and traceable. T1 through T7 include references, dependencies, scope, and DoD entries that map to the requirements and design.
- Dependencies are explicit. Every task lists its prerequisites, with T1 explicitly independent and later implementation tasks sequenced after the needed spec, index, contract, response, and pagination work.
- Open questions are explicit. `requirements.md` states there are no open questions and records the fixed decisions that the design and tasks rely on.
- AGENTS.md invariants are preserved. The feature is read-only, introduces no update/delete behavior, preserves append-only audit event assumptions, and defines deterministic ordering.
- Sorting is deterministic. Requirements and design both require `occurredAt DESC` with event `id DESC` as the tiebreaker.
- Verification coverage is sufficient for the spec. Tasks require Testcontainers migration checks, endpoint integration tests, response contract tests, cursor unit tests, pagination integration tests, invariant tests, architecture checks, and `./gradlew test`.

### WEAK

None.

### FAIL

None.

## Recommended Next Steps

None.
