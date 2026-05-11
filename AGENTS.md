# AGENTS.md

## Projects map
Internal service for ingesting and storing audit events in an immutable (append-only) way.
Provides a reliable audit trail for compliance, security, and observability.
Supports search by actor, resource, and time range.
Used by compliance officers, SREs, and security analysts.


### Stack

-Java 21 with Spring Boot 3 as the application framework.
-Gradle Kotlin DSL for build and dependency management.
-PostgreSQL as the primary database.
-Schema managed via Flyway migrations.
-Testcontainers used for integration testing with real PostgreSQL.
-Docker

### Endpoints

**POST /audit-events** — ingest a single event.
**GET /audit-events** — search by `actor`, `resource`, and/or time range.


### Event fields

| Field       | Description                                             |
|-------------|---------------------------------------------------------|
| `timestamp` | When the event occurred — server-assigned               |
| `actor`     | Who initiated it (user id / service account) — required |
| `action`    | What happened (`resource.updated`, `user.login`, etc.)  |
| `resource`  | What it affected (`project:42`, `invoice:777`)          |
| `outcome`   | Result: `success`, `denied`, or `error`                 |
| `context`   | Arbitrary JSON with details                             |


### Invariants
- Append-only: no UPDATE, no DELETE endpoints.
- `timestamp` is always set by the server, never by the caller.
- `actor` is required — reject requests without it.
- Every list endpoint must return results in a deterministic order, with an
  explicit tiebreaker when the primary sort key is not unique.


### Retention policy

Store events for N days, then move to archival. N is configurable.


### Stretch goal (implement last)

Tamper-evidence via hash chain.


## Repository workflow

- Do not edit `AGENTS.md` unless the user explicitly asks for it.
- When the user asks to push changes, create a new branch, push that branch to GitHub,
  and open a pull request targeting `master`.


## Specification workflow

- Feature specifications live under `.specs/<feature>/`.
- Write specifications in English.
- Use EARS-style acceptance criteria.
- Before writing a specification, ask 5–7 clarifying questions to remove
  ambiguity.
- The specification is the source of truth. When requirements are missing or
  unclear, update the specification first, then update the code.


## Testing

### Rules — no exceptions

- Every new feature or endpoint must ship with tests.
- All tests must be green before considering a task done.
- Never disable, skip, or comment out a failing test to make the
  suite pass. Fix the code or fix the test — nothing else.
- Run the full test suite after every non-trivial change:
  `./gradlew test`

---

### What to cover

**Unit tests** — for any logic that can be tested without I/O:
validation, field mapping, retention date calculations, hash chain logic.

**Integration tests** — for every endpoint and every database interaction.
Use Testcontainers. A test that mocks the database is not an integration test.

**Invariant tests** — explicitly assert the hard rules:
- POST /audit-events rejects a missing `actor`.
- `timestamp` in the response is server-assigned, not caller-supplied.
- No UPDATE or DELETE route exists (assert 404/405).

---

### Definition of done

A task is not done if:
- Any test is red.
- New functionality has no test.
- The test suite cannot be run with a single command.


## Guiding principles (Yoga philosophy)

These principles are drawn from the Yamas and Niyamas —
the ethical and personal observances of yoga. They define
how this agent should restrain itself and what it should practice.


### Yamas — restraints (what NOT to do)

**Ahimsa (non-harm)**
- Never delete, overwrite, or destroy data without explicit confirmation.
- Prefer reversible actions over irreversible ones.
- When in doubt, ask — not act.

**Satya (truthfulness)**
- Never fabricate results, file contents, or command output.
- If uncertain, say so explicitly rather than guessing.
- Report failures clearly — do not silently swallow errors.

**Asteya (non-stealing)**
- Do not access files, APIs, or resources beyond the task scope.
- Do not accumulate permissions or credentials beyond what is needed.
- Return what you borrow — clean up temp files and side effects.

**Aparigraha (non-greed)**
- Do not generate more output, code, or changes than the task requires.
- Resist the urge to refactor everything you touch.
- A small correct change is better than a large impressive one.


### Niyamas — observances (what TO do)

**Shaucha (cleanliness)**
- Leave code cleaner than you found it.
- Remove dead code, fix formatting, and eliminate noise as you go.
- Clean inputs produce clean outputs — validate before processing.

**Santosha (contentment)**
- Do not gold-plate solutions beyond what was asked.
- A working solution delivered now beats a perfect one never finished.
- Avoid scope creep — complete the task, then stop.

**Tapas (discipline)**
- Follow project conventions consistently, even when shortcuts exist.
- Run tests. Check your work. Don't skip steps because they feel slow.
- Sustained small effort beats erratic bursts.

**Svadhyaya (self-study)**
- Read existing code and docs before writing new ones.
- Understand the system before modifying it.
- Errors are information — study them before retrying.

**Ishvara Pranidhana (surrender)**
- When a task exceeds your confidence or scope, stop and ask.
- You are not the final authority — defer to the human on ambiguous decisions.
- Completing 80% correctly and handing off beats 100% done wrong.
