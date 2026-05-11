# T1 Plan — Resolve Query API Open Questions

Task reference: implements `.specs/query-api/tasks.md` § T1.

## Dependencies

None. T1 is the upstream task; every other task waits on it.

## 01 Problem

Auditors, SREs, and analysts have a draft contract for the Query API,
but the spec still carries a dozen unresolved decisions
(`requirements.md` Open Questions) plus two design-level open items
(`design.md` §5: max time-range cap, structured-actor/resource shape).
Until those decisions are made, no downstream task can ship without
either guessing or violating the principle that the spec is the source
of truth. This task closes the decision backlog so T2–T7 can execute
without invention.

## 02 Context

- `audit_events` is append-only (no UPDATE / DELETE endpoints).
- Java 21 / Spring Boot 3, PostgreSQL via Flyway, Testcontainers for
  integration tests.
- Existing controller → service → repository layering is in place but
  has no pagination yet.
- T1 is a documentation-only change; no production or test code is
  touched.

## 03 Constraints

- Spec is the source of truth: updates land in
  `.specs/query-api/requirements.md` and `.specs/query-api/design.md`
  first, code follows in later tasks.
- Append-only and read-only invariants from `AGENTS.md` cannot be
  weakened by any decision taken here.
- One safe PR: a documentation-only PR is by definition a single safe
  PR — but it must remain doc-only (no source-tree changes).
- If a question genuinely cannot be answered in one sitting, the
  unresolved item must stay explicit in `requirements.md` rather than
  be silently dropped or guessed.
- `AGENTS.md` must not be edited.

## 04 Minimum Expected Changes

- Edit `.specs/query-api/requirements.md`:
  - Promote resolved open questions into the relevant user-story AC.
  - Reduce the Open Questions section to genuinely deferred items
    only (or remove it entirely if none remain).
- Edit `.specs/query-api/design.md`:
  - Replace `<event-id>` placeholder, `400 placeholder` notes, and
    the `id asc/desc` conditional with concrete values.
  - Remove now-decided items from §5 design open questions.
  - Update §1 status table, §3 tiebreaker direction, §4 index
    direction, and §7 alignment closing paragraph to match.
- Edit `.specs/query-api/tasks.md` only if a decision adds new work
  (structured `actor`/`resource` shape or non-numeric public `id`):
  insert the new task ahead of T3 and renumber.
- No changes under `src/`.

## 05 Verification Method

- Every DoD bullet in `tasks.md` § T1 maps to a concrete diff hunk in
  this PR (decision walkthrough lives in the PR description).
- `requirements.md` and `design.md` are mutually consistent after
  edits (cross-check field names, status codes, `nextCursor` shape,
  tiebreaker direction).
- `./gradlew test` still passes (no code changed — used as a
  regression-not-introduced check, not a feature check).
- Markdown link / anchor check on the three edited spec files.

## 06 Integration With Existing Code

- No code integration. T1 is the gate for everything downstream:
  - T2 reads the resolved tiebreaker direction.
  - T3 reads the timestamp-format and limit-handling decisions.
  - T4 reads the field-shape and `nextCursor`-on-last-page decisions.
  - T5 reads `limit > max` behavior and max time-range cap (if any).
  - T6 reads tiebreaker direction for determinism assertions.
- Layer boundaries (controller / service / repository) are
  untouched.

## 07 Principles

- Determinism first: every decision must preserve a total ordering on
  the result set.
- Preserve append-only and read-only invariants — reject any decision
  that would loosen them.
- Missing requirement → explicit deferral, not invention.
- One safe PR.
- No code changes in this planning task.
- Do not edit `AGENTS.md`.
- This plan does not itself depend on any earlier T1 decision; it is
  the decision-making task.

## Blockers / Open Questions

- The decisions themselves: the team must agree on each open question
  before the PR can merge. The PR description records each
  decision and the rationale.
- If structured `actor{id,type}` / `resource{id,type}` wins, an
  additional task (storage migration or derivation rule) must be
  inserted before T3 and reflected in this PR's `tasks.md` edit.
- If a non-numeric public `id` (e.g. ULID) wins, an additional task
  for generation / persistence / mapping must be inserted before T3.

## Out of Scope

- Any source code changes.
- Reshaping `POST /audit-events`.
- Authentication / authorization for the endpoint (left as an open
  question by design until the team commits a model).
