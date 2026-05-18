# Spec Evaluation Checklist

## Requirements

- Requirements are written in English.
- Acceptance criteria use EARS-style phrasing where possible.
- Acceptance criteria are testable and avoid vague terms such as "fast", "simple", "robust", or "user-friendly" unless objectively defined.
- Required inputs, outputs, validation rules, and error cases are explicit.
- Open questions are explicit and are not silently answered in design, tasks, or plans.
- Decisions invented during planning are recorded in `.specs/<feature>/_delta.md` before implementation.

## Traceability

- Design decisions trace back to one or more requirements.
- Tasks trace back to requirements and design sections.
- No implementation decision appears only in `design.md`, `tasks.md`, or a task plan when it changes the requirement contract.
- Dependencies between tasks are explicit.
- Tasks have references and a clear definition of done.

## Architecture And Invariants

- Design choices align with `AGENTS.md` invariants and repository workflow rules.
- Append-only behavior is preserved for audit events: no update or delete endpoint is introduced.
- Server-assigned fields, such as audit event timestamps, are not accepted from callers unless the requirements explicitly change that contract.
- Required domain fields, especially `actor`, are validated.
- List endpoints define deterministic sorting with an explicit tiebreaker when the primary sort key is not unique.
- Pagination strategy is justified when list responses can grow.
- Retention or archival behavior is specified when the feature touches event lifecycle.

## Testing And Verification

- Verification includes the relevant unit tests for pure logic.
- Verification includes integration tests for endpoints and database interactions.
- Invariant tests cover hard rules affected by the feature.
- Test data, boundary cases, and error cases are described enough to implement.
- The task list requires the full suite to pass with the repository's single test command.

## Report Quality

- Each `PASS`, `WEAK`, or `FAIL` finding includes concise evidence.
- `WEAK` is used for partial, indirect, vague, or unproven coverage.
- `FAIL` is used for missing, contradictory, or materially unsafe coverage.
- Recommended next steps are specific and ordered by impact.
