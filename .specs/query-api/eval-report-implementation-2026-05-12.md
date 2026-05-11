# Query API Implementation vs Requirements — 2026-05-12

Walks every AC, Out-of-Scope item, and Fixed Decision in
`.specs/query-api/requirements.md` against the code on `master`
(T1–T6 merged; T7 pending PR #18; WEAK gaps closed by
`codex/close-weak-test-gaps`).

## User Stories With AC

### Compliance Officer

1. **PASS — actor filter returns only that actor.**
   `AuditEventService.buildSpecification` applies `cb.equal(root.get("actor"), query.actor())` for non-blank actor. Exercised by `AuditEventControllerTest.createsAndSearchesAuditEvents`, the paginated walk, `rejectsCursorIssuedForDifferentFilters`, and the invariants determinism tests.
2. **PASS — resource filter returns only that resource.**
   Same `cb.equal(...)` pattern for `resource`. Exercised by `createsAndSearchesAuditEvents` (actor + resource) and `existingEventsCannotBeMutated`.
3. **PASS — `from`/`to` boundary inclusivity.**
   Service uses `cb.greaterThanOrEqualTo` and `cb.lessThanOrEqualTo`. `AuditEventControllerTest.timeRangeBoundsAreInclusiveOnBothEnds` inserts rows at exactly `from`, exactly `to`, one second before `from`, and one second after `to`, then asserts the boundary rows appear and the off-boundary rows do not.
4. **PASS — no filters → default page size.**
   `AuditEventService.search` applies `properties.getDefaultPageSize()` when `query.limit() == null`. `AuditEventControllerTest.appliesDefaultPageSizeWhenLimitIsOmitted` seeds 51 events under a unique actor, issues `GET /audit-events?actor=...` without `limit`, and asserts exactly 50 items returned with `nextCursor` present.

### SRE / Security Analyst

5. **PASS — sort `occurredAt DESC` with `id DESC` tiebreaker.**
   `AuditEventKeysetQueryImpl` calls `cb.desc(root.get("timestamp")), cb.desc(root.get("id"))`. `AuditEventControllerTest.tiebreaksBySameTimestampUsingIdDescending` and `AuditEventInvariantsTest.sameTimestampRowsAreOrderedByIdDescending` both insert three rows at the same `Instant` and assert id-descending order.
6. **PASS — read does not mutate.**
   `AuditEventInvariantsTest.readQueriesDoNotMutateStoredEvents` snapshots the table via `JdbcTemplate`, runs the full set of representative GETs, and asserts the row count and full snapshot are byte-for-byte identical.

### Analyst (pagination)

7. **PASS — `nextCursor` returned when more results exist.**
   `AuditEventService.search` fetches `limit + 1` rows and builds `nextCursor` from the last returned row when the extra row exists. `paginatesThroughLargeResultSetsWithoutLossOrDuplication` walks 10 events at `limit=3` until the last page.
8. **PASS — `cursor` continues after the prior page deterministically.**
   Keyset predicate `(t < t*) OR (t = t* AND id < i*)` in `buildSpecification` matches the DESC+DESC sort. The multi-page walk asserts no duplicates and no skips across pages.
9. **PASS — malformed or tampered cursor → 400.**
   `MalformedCursorException` and `TamperedCursorException` map to `400`. `CursorCodecTest` covers tampered, wrong-secret, missing signature, empty signature, and non-base64 cases at the unit level; `rejectsMalformedCursor` and `rejectsTamperedCursor` cover the HTTP path.
10. **PASS — valid cursor with different filters → 422.**
    `CursorFilterMismatchException` maps to `422`. `rejectsCursorIssuedForDifferentFilters` reuses a cursor from `actor=mismatch:a` with `actor=mismatch:b` and asserts `422`.
11. **PASS — `limit` compared with the configured maximum.**
    `AuditEventService.validate` checks `query.limit() > properties.getMaxPageSize()` and throws `BadAuditEventQueryException`. `AuditQueryProperties.maxPageSize` defaults to 200.
12. **PASS — `limit` omitted → default page size.**
    Same `appliesDefaultPageSizeWhenLimitIsOmitted` test that covers AC #4 also covers this AC: it omits `limit` and observes exactly 50 items.
13. **PASS — `limit > configured max` → 400.**
    `rejectsLimitOutsideConfiguredRange` asserts `limit=201` returns `400`.
14. **PASS — `limit < 1` → 400.**
    Same test asserts `limit=0` returns `400`.
15. **PASS — `from`/`to` is an ISO-8601 UTC instant.**
    Controller binds with `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)`. `rejectsMalformedTimestamp` covers the failure path; happy-path tests use values like `2026-04-01T00:00:00Z`.

## Out of Scope

- **PASS — no create/update/delete through the Query API.** Only `POST /audit-events` exists (ingest). `AuditEventInvariantsTest.writeRoutesOnAuditEventsAreRejected` exercises `PUT`, `PATCH`, and `DELETE` at runtime; `ArchitectureTest.auditEventControllerHasNoUpdateOrDeleteRoutes` enforces absence of `@PutMapping`/`@PatchMapping`/`@DeleteMapping` at build time.
- **PASS — no archival retrieval.** Not added.
- **PASS — no caller-selectable sort.** Controller exposes no `sort` parameter; service always uses `event_timestamp DESC, id DESC`.
- **PASS — no filtering by `outcome`/`action`/`payload`.** Controller `@RequestParam` list does not include these; service's `buildSpecification` only branches on actor, resource, from, to, and cursor.
- **PASS — no auth changes.** `application.yml` and controller carry no security config.

## Fixed Decisions

- **PASS — default page size 50, configurable.** `AuditQueryProperties.defaultPageSize = 50`; bound via `audit.query.default-page-size`.
- **PASS — max page size 200, configurable.** Same shape via `audit.query.max-page-size`.
- **PASS — no max time-range span.** Service does not enforce a `to - from` cap.
- **PASS — response `id` is the numeric database id.** `AuditEventResponse.id` is `Long`.
- **PASS — `actor`/`resource` scalar.** Both `String` in `AuditEventResponse`.
- **PASS — page response contains only `items` and `nextCursor`.** `AuditEventPageResponse` is a record with exactly those two components; `nextCursor` is `@JsonInclude(NON_NULL)` so the last page omits it on the wire.

## Tally

- **PASS**: 25
- **WEAK**: 0
- **FAIL**: 0

## History

- 2026-05-12 — first pass produced 22 PASS / 3 WEAK (boundary inclusivity, default-page-size with no filter, default-page-size with omitted limit).
- 2026-05-12 — `codex/close-weak-test-gaps` adds `timeRangeBoundsAreInclusiveOnBothEnds` and `appliesDefaultPageSizeWhenLimitIsOmitted`, closing all three WEAKs.
