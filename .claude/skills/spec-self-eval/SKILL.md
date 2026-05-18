---
name: spec-self-eval
description: Evaluate, audit, score, self-check, or validate a feature specification in .specs/<feature>/ by reading requirements.md, design.md, and tasks.md, applying the spec evaluation checklist, and writing a PASS / FAIL / WEAK report.
---

# Spec Self-Eval

Use this skill when asked to evaluate a feature spec under `.specs/<feature>/`.

## Inputs

- Feature docs:
  - `.specs/<feature>/requirements.md`
  - `.specs/<feature>/design.md`
  - `.specs/<feature>/tasks.md`
- Checklist:
  - Prefer `.specs/_eval-checklist.md` when present.
  - Otherwise use this skill's `references/_eval-checklist.md`.

Determine `<feature>` from the user's request. If absent, inspect `.specs/`; ask only when multiple feature directories make the target ambiguous.

## Workflow

1. Locate the three required feature files. If any are missing, include a `FAIL` finding and continue with the files that exist.
2. Load the checklist source:
   - repo checklist: `.specs/_eval-checklist.md`
   - bundled fallback: `references/_eval-checklist.md`
3. Evaluate each checklist item against `requirements.md`, `design.md`, and `tasks.md`.
4. Classify each finding:
   - `PASS`: clearly satisfied.
   - `WEAK`: partially satisfied, vague, underspecified, indirect, or not testably stated.
   - `FAIL`: missing, contradicted, unsafe, or materially inconsistent.
5. Cite concise evidence with file references. Use line numbers when practical.
6. Save the report to `.specs/<feature>/eval-report-<YYYY-MM-DD>.md`.
   - If the file does not exist, create it with the report.
   - If the file already exists, append the new check result to the end of the file; do not overwrite existing results.
   - When appending, add a separator line (`---`) before the new report block.

Do not modify the feature spec during evaluation unless the user explicitly asks for fixes. If the evaluation discovers an invented planning or implementation decision that is not in the source spec, flag it; do not silently move it into the spec.

## Report Format

```markdown
# Spec Self-Eval: <feature>

Date: <YYYY-MM-DD>
Checklist: <repo checklist path or bundled checklist path>

## Summary

- PASS: <n>
- WEAK: <n>
- FAIL: <n>

## Findings

| Status | Area | Finding | Evidence |
|--------|------|---------|----------|
| PASS/WEAK/FAIL | <area> | <short finding> | `<file>` |

## Details

### PASS

...

### WEAK

...

### FAIL

...

## Recommended Next Steps

1. ...
2. ...
```

## Output

In `Recommended Next Steps`, list only corrective actions for current `WEAK`
or `FAIL` findings. If there are no `WEAK` or `FAIL` findings, write `None.`
Do not include standing process reminders or repeat already-satisfied workflow
rules.

End by reporting the saved report path and the highest-impact `FAIL` and `WEAK` findings. If there are no `FAIL` or `WEAK` findings, say that clearly and mention any residual review limits.
