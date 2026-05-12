# Homework 2

## Статус

- **Спека:** готово
- **План:** готово
- **Реализация:** готово

---

## AGENTS.md — что добавил после Занятия 2

В разделе **Specification workflow**:

```markdown
- Feature specifications live under `.specs/<feature>/`.
- Write specifications in English.
- Use EARS-style acceptance criteria.
- Before writing a specification, ask 5–7 clarifying questions to remove
  ambiguity.
- The specification is the source of truth. If during implementation a gap or
  ambiguity surfaces in `requirements.md`, `design.md`, `tasks.md`, or any
  per-task plan, update the spec first and only then update the code.
- If an agent in plan mode (or any LLM-driven planning step) tries to fill a
  gap by inventing a decision, record (append) the invented decision in
  `.specs/<feature>/_delta.md` and resolve whether it belongs in the spec
  before any code is written. Do not silently let invented decisions enter
  the codebase.
```

---

## Главная дельта «спека ↔ план»

> <одна-две фразы: что агент додумал, что я в итоге дописал в спеку>

Агент попробовал решить открытые вопросы.

---

## Где SDD сэкономит время / силы, где может оказаться overhead

> На вашем **реальном** проекте.

Может быть полезен при написании больших фич, в разработке которых участвует не один человек. Спецификация всех синхронизирует, помогает разбить требования на подзадачи.

Может быть излишен для мелких задач. Документирование может занимать много времени. Особенно неудобно, если требования часто меняются.

---

## Главные вопросы к Занятию 3

Нет вопросов
