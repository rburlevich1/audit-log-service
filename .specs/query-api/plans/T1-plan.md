# T1 Plan — Verify Query API Spec Consistency

Task reference: implements `.specs/query-api/tasks.md` § T1.

## Dependencies

None.

## 01 Problem

The Query API specs are now the source of truth for downstream work. This task
checks that `requirements.md`, `design.md`, `tasks.md`, and the saved plans all
agree before implementation starts.

## 02 Context

- The feature is a read-only query endpoint over append-only `audit_events`.
- Fixed decisions now include configurable page sizes with defaults `50`/`200`,
  `occurredAt DESC, id DESC`, `limit > configured max` as `400`, omitted
  last-page `nextCursor`, numeric database id, scalar `actor`/`resource`,
  ISO-8601 UTC instants, no max time-range span, and no auth changes.
- This is a documentation-only verification task.

## 03 Constraints

- Do not edit `AGENTS.md`.
- Do not implement source code.
- Do not re-open resolved decisions without updating `requirements.md` first.
- Keep this task to one documentation-only PR.

## 04 Minimum Expected Changes

- Review `.specs/query-api/requirements.md`.
- Review `.specs/query-api/design.md`.
- Review `.specs/query-api/tasks.md`.
- Review `.specs/query-api/plans/T*-plan.md`.
- Fix only documentation inconsistencies found during the review.

## 05 Verification Method

- `requirements.md` has `Open Questions: None`.
- `requirements.md` and `design.md` agree on every fixed decision.
- `tasks.md` and all plan files match the fixed decisions.
- `./gradlew test` may be run as a no-code regression check.

## 06 Integration With Existing Code

No code integration. This task gates implementation by ensuring the specs and
plans are internally consistent.

## 07 Principles

- Spec first, code second.
- Determinism first.
- Keep changes documentation-only.
- If a new gap is found, put it in `requirements.md` before changing design or
  plans.

## Blockers / Open Questions

None expected. If a new product decision is discovered, stop and update
`requirements.md` first.

## Out of Scope

- Code changes.
- Database migrations.
- Test implementation.
