# Delta Review по планам Query API

Цель: проверить каждый сохранённый план из `.specs/query-api/plans/` против
`requirements.md`, `design.md` и `tasks.md`.

Формат по каждому плану:
- Что план добавил сверх спеки, и куда это должно было попасть.
- Что противоречит спеке.
- Что в спеке осталось открытым или недостаточно зафиксированным.

## T1 — Resolve Query API Open Questions

### Сверх спеки

- План добавляет процесс PR: "decision walkthrough lives in the PR
  description", "Markdown link / anchor check", "reviewer acceptance".
  Это не продуктовые требования и не дизайн. В `requirements.md` или
  `design.md` переносить не нужно; это уместно только в `tasks.md` / плане.
- План говорит, что если команда не договорилась, вопрос нужно оставить явным
  или вынести в follow-up T1.x. Это процессная дисциплина. В
  `requirements.md` можно оставить только сам открытый вопрос, не процесс
  принятия решения.

### Противоречия

- Явных противоречий нет. План соответствует роли T1 как документационного
  gate перед реализацией.

### Пробелы в спеках

- `requirements.md` всё ещё содержит открытые решения, которые T1 обязан
  закрыть: page size, tiebreaker direction, `limit > max`, last-page
  `nextCursor`, public id, actor/resource shape, timestamp format, max range,
  auth.
- `design.md` зависит от этих решений и содержит условные ветки. Это нормально
  до выполнения T1, но реализацию начинать нельзя.

## T2 — Add Database Index Migration

### Сверх спеки

- План выбирает конкретное имя миграции:
  `V2__add_query_api_composite_indexes.sql`. Это реализационная деталь; в
  `design.md` не нужно, если проект не фиксирует номера миграций заранее.
- План выбирает конкретный тестовый файл:
  `AuditEventIndexMigrationTest.java`, и способ проверки через `pg_indexes`.
  Это не требование и не дизайн API; остаётся в плане/задаче.
- План говорит, что если появится public ULID, cursor/index всё равно может
  использовать внутренний `bigserial`. Это уже соответствует `design.md`, но
  если T1 выберет ULID, в `design.md` стоит явно закрепить: public id и
  internal cursor id — разные понятия.

### Противоречия

- Явных противоречий нет.

### Пробелы в спеках

- Направление `id asc/desc` всё ещё зависит от T1. До этого нельзя писать
  окончательный SQL индексов.
- `design.md` задаёт набор индексов, но не фиксирует имя Flyway-миграции. Это
  нормально.

## T3 — Add Query API Contract and Validation

### Сверх спеки

- План вводит временное поведение: до T5 любой non-null `cursor` возвращает
  `400 Bad Request` как "cursor not yet supported". В финальном
  `requirements.md` такого состояния нет: там cursor должен работать. Это
  допустимо только как поэтапная реализация, но это должно оставаться в
  `tasks.md`/плане, не в финальных требованиях.
- План предлагает конкретные классы:
  `AuditEventQuery`, `InvalidQueryException`,
  `CursorNotSupportedYetException`, `AuditApiExceptionHandler`. Это детали
  дизайна реализации. Если команда хочет закрепить исключения/слои, это место
  для `design.md`; иначе достаточно плана.
- План говорит о placeholder response wrapper `{items, nextCursor: null}`.
  Это промежуточная реализация; в `requirements.md` попадать не должно.

### Противоречия

- Потенциальное противоречие с финальным `requirements.md`: требование говорит,
  что `cursor` продолжает страницу, а T3 временно возвращает `400` на любой
  cursor. Это снимается только тем, что T3 явно scoped как "No pagination, no
  cursor decoding", а T5 обязателен для выполнения финального AC.
- План говорит, что "deterministic ordering is still required from the
  underlying query even without pagination". В текущем T3 DoD это не проверяется.
  Если T3 должен возвращать список, лучше либо добавить сортировку уже в T3, либо
  явно оставить deterministic ordering на T5/T6. Финальная спека требует
  детерминированный list endpoint.

### Пробелы в спеках

- `requirements.md` не описывает промежуточное состояние "cursor not yet
  supported". Это нормально, если PR T3 не считается финальной поставкой
  feature.
- Нужно закрыть T1-решения по timestamp format и limit behavior до полноценной
  реализации validation.

## T4 — Add Response DTO, Mapping, and Contract Tests

### Сверх спеки

- План вводит конкретные DTO/mapper имена:
  `AuditEventResponse`, `AuditEventPageResponse`, `AuditEventMapper`.
  Это реализационный дизайн; если имена важны, их место в `design.md`, но обычно
  достаточно плана.
- План предлагает ArchUnit-правило, запрещающее controller methods возвращать
  JPA entity. Это сильная архитектурная гарантия. Её можно добавить в
  `design.md` в Layer Integration, если команда хочет закрепить её как
  обязательную, а не просто как тестовую идею.
- План решает, что mapper вызывается на controller boundary, а service может
  продолжать возвращать `List<AuditEvent>`. В `design.md` ранее говорится о
  domain `Page<AuditEvent>` и boundary encoding. Если это принципиально, нужно
  уточнить в `design.md`, где именно происходит mapping и page assembly.

### Противоречия

- Лёгкое расхождение с `design.md`: дизайн говорит, что service возвращает
  domain `Page<AuditEvent>` с typed `Cursor`, а план допускает, что service
  возвращает `List<AuditEvent>` и controller собирает response. Для T4 до
  пагинации это допустимо, но к T5 нужно привести к одному подходу.

### Пробелы в спеках

- Response shape всё ещё зависит от T1: numeric/opaque id,
  scalar/structured actor/resource, last-page `nextCursor`.
- `requirements.md` сейчас помечает response item как illustrative. До T1 это
  правильно, но контрактные тесты T4 нельзя писать окончательно.

## T5 — Implement Signed Keyset Pagination

### Сверх спеки

- План вводит конкретный конфиг `audit.query.cursor-secret` и env var
  `AUDIT_QUERY_CURSOR_SECRET`. Это не в `requirements.md`. Место — `design.md`,
  потому что это часть operational/config contract.
- План вводит конкретные классы в пакете `event/cursor`: `CursorPayload`,
  `FilterFingerprint`, `CursorCodec` и exception classes. Это детали
  реализации; не обязательно переносить в спеки.
- План выбирает fail-fast boot behavior, если cursor secret не задан. Это важно
  для эксплуатации. Если так и должно быть, это нужно добавить в `design.md`
  рядом с HMAC/secret management.
- План требует `MessageDigest.isEqual` для constant-time compare и "secret never
  logged". Это security design. Лучше добавить в `design.md`, если это
  обязательная гарантия.
- План предлагает ручной smoke walk через curl. Это не спек; остаётся в плане
  или PR checklist.

### Противоречия

- Явных противоречий нет. План правильно объединяет pagination и signed cursor,
  чтобы не нарушить AC "tampered cursor -> 400".
- Есть риск: если T1 выберет `limit > max = clamp`, а план всё равно заведёт
  error branch, это будет противоречием. Сейчас план корректно помечает это как
  блокер T1.

### Пробелы в спеках

- `design.md` говорит HMAC-SHA256, но не фиксирует secret config name,
  fail-fast behavior, rotation/non-rotation policy. План добавил это сверх
  дизайна.
- `requirements.md` не фиксирует default/max page size и `limit > max`.
- `requirements.md` не фиксирует last-page `nextCursor`, без этого тесты T5 не
  могут быть окончательными.

## T6 — Verify Read-Only and Append-Only Invariants

### Сверх спеки

- План вводит конкретные тесты:
  `AuditEventReadOnlyInvariantTest`, `AuditEventDeterminismTest`.
  Это реализационные детали тестирования, не требования.
- План предлагает ArchUnit-правило, запрещающее `@PutMapping`, `@PatchMapping`,
  `@DeleteMapping` во всём `com.example.audit`. Это шире, чем Query API. Если
  команда действительно хочет глобальный запрет write endpoints, это нужно
  зафиксировать в `AGENTS.md` или `design.md`. Если запрет только для
  `/audit-events`, правило должно быть уже.
- План предлагает проверять row snapshot byte-for-byte после GET. Это сильная
  тестовая стратегия, но не продуктовый контракт.

### Противоречия

- Возможное противоречие с будущими endpoint'ами: глобальный запрет на
  `@PutMapping`/`@PatchMapping`/`@DeleteMapping` во всём приложении может
  запретить легитимные write endpoints в других доменах. `AGENTS.md` запрещает
  UPDATE/DELETE для audit events, а не обязательно для всего сервиса. Лучше
  сузить план до `AuditEventController` / `/audit-events`.

### Пробелы в спеках

- `AGENTS.md` фиксирует no UPDATE/DELETE endpoints для audit events, но
  `design.md` не уточняет, должен ли тест быть глобальным или только для
  Query/AuditEvent API.
- Точная реакция Spring на PUT/PATCH/DELETE (`404` vs `405`) остаётся
  тестовой деталью, не требованием.

## T7 — Drop Legacy Single-Column Indexes

### Сверх спеки

- План предлагает конкретное имя миграции:
  `V3__drop_legacy_single_column_indexes.sql`. Это реализационная деталь.
- План предлагает `DROP INDEX IF EXISTS`; это deployment/design detail. Если
  важно для миграционной политики, можно добавить в `design.md`.
- План обсуждает `DROP INDEX CONCURRENTLY` vs обычный `DROP INDEX`. Это важная
  operational decision. Если база production-sized, это должно попасть в
  `design.md` или отдельный operational note, иначе PR может выбрать небезопасный
  вариант.
- План требует вставить `EXPLAIN ANALYZE` outputs в PR description. Это
  процессный gate, уже отражён в `tasks.md`; в `requirements.md` не нужен.

### Противоречия

- Явных противоречий нет. План корректно помечает T7 как follow-up outside
  initial feature path.
- Потенциальный риск: Flyway и `DROP INDEX CONCURRENTLY` плохо сочетаются с
  транзакционными миграциями PostgreSQL. Если будет выбран concurrent drop,
  это нужно отдельно спроектировать в `design.md`/операционном плане.

### Пробелы в спеках

- `design.md` говорит "cleanup migration later", но не решает
  `DROP INDEX` vs `DROP INDEX CONCURRENTLY`, lock window, rollback strategy.
- `tasks.md` задаёт staging gate, но не говорит, кто и где хранит результаты
  `EXPLAIN ANALYZE` кроме PR description.

## Общие выводы

- Планы в целом качественные и полезные как implementation guides, но местами
  они добавляют решения, которых нет в `requirements.md`/`design.md`.
- Самые важные решения, которые стоит перенести в `design.md`, если команда с
  ними согласна:
  - имя/источник cursor secret и fail-fast behavior;
  - constant-time HMAC compare и запрет логирования секрета;
  - точное место response mapping/page assembly между controller и service;
  - scope ArchUnit-запрета write endpoints: весь app или только audit events;
  - operational strategy для `DROP INDEX` vs `DROP INDEX CONCURRENTLY`.
- Самые важные решения, которые должны быть закрыты в `requirements.md` через
  T1:
  - page sizes;
  - tiebreaker direction;
  - `limit > max`;
  - last-page `nextCursor`;
  - public id;
  - actor/resource response shape;
  - timestamp format;
  - max time range;
  - auth model.
- Главный временный конфликт — T3 rejecting cursor while final requirements
  expect cursor pagination — допустим только как staged delivery. Финальное
  соответствие появляется после T5.

## Дополнение после исправлений

Статус: проблемы из предыдущей ревизии перенесены в `requirements.md`,
`design.md`, `tasks.md` и планы. Старый разбор выше сохранён как история
решений; этот раздел фиксирует, что было изменено после него.

### Что было исправлено в `requirements.md`

- Закрыты открытые вопросы: `Open Questions` теперь содержит `None`.
- Зафиксированы page size values: configurable defaults `50` и `200`.
- Зафиксирован deterministic sort: `occurredAt DESC, id DESC`.
- Зафиксировано поведение `limit > configured max` (default `200`):
  `400 Bad Request`.
- Зафиксировано поведение последней страницы: `nextCursor` omitted.
- Зафиксирована форма response item:
  - `id` — numeric database id;
  - `actor` / `resource` — scalar strings;
  - `occurredAt` / `payload` — API names for storage timestamp/context.
- Зафиксирован timestamp format: ISO-8601 UTC instant.
- Зафиксировано отсутствие max time-range span.
- Filtering by `outcome`, `action`, `payload` and auth changes вынесены в
  `Out of Scope`.

### Что было исправлено в `design.md`

- Убраны условные ветки по `id ASC` / `id DESC`; дизайн теперь использует
  только `id DESC`.
- Убраны placeholder/open notes для public id, actor/resource shape,
  `nextCursor`, `limit > max`, max range.
- Status code table согласована с требованиями:
  - parse/contract errors -> `400`;
  - `from > to` and valid cursor with different filters -> `422`.
- Cursor secret закреплён как `audit.query.cursor-secret`, bound from
  `AUDIT_QUERY_CURSOR_SECRET`.
- Добавлены fail-fast behavior для пустого cursor secret, constant-time HMAC
  compare, запрет логирования секрета, secret rotation out of scope.
- Уточнено место page assembly/mapping:
  - service owns domain page and cursor encoding;
  - controller maps to response DTO.
- Scope invariant tests уточнён для `/audit-events`, а не для всего приложения.
- Для legacy index cleanup добавлена operational strategy:
  prefer `DROP INDEX CONCURRENTLY` where Flyway can run non-transactionally;
  otherwise use an approved low-traffic maintenance window.

### Что было исправлено в `tasks.md`

- T1 больше не "resolve open questions"; теперь это consistency check.
- T3 валидирует `limit < 1` и `limit > configured max` как `400`.
- T4 больше не зависит от unresolved response shape.
- T5 больше не зависит от configurable tiebreaker direction; uses
  `occurredAt DESC, id DESC`.
- T6 scoped to audit event invariants.
- T7 остаётся operational follow-up.

### Что было исправлено в планах

- Все T1-T7 планы переписаны под закрытые решения.
- Убраны ссылки на unresolved T1 decisions как blockers.
- T5 фиксирует signed keyset pagination в одном PR, без unsigned public cursor
  intermediate state.
- T6 не предлагает глобальный запрет `PUT/PATCH/DELETE` для всего приложения;
  invariant checks scoped to audit events.
- T7 explicitly carries the operational drop-index strategy.

### Оставшиеся намеренные ограничения

- Auth changes остаются out of scope.
- Secret rotation остаётся out of scope.
- Legacy index drop остаётся follow-up после staging evidence.
- T3 всё ещё временно rejects non-null cursor до T5. Это staged delivery, не
  финальное состояние feature.
