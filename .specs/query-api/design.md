# Query API Design

## 1. API Contract

`GET /audit-events` — read-only search over `audit_events`.

### Query parameters

| Name       | Type             | Required | Notes                                                |
|------------|------------------|----------|------------------------------------------------------|
| `actor`    | string           | no       | Exact match.                                         |
| `resource` | string           | no       | Exact match.                                         |
| `from`     | ISO-8601 instant | no       | Inclusive lower bound on `occurredAt`.               |
| `to`       | ISO-8601 instant | no       | Inclusive upper bound on `occurredAt`.               |
| `cursor`   | opaque string    | no       | Must come from an earlier call with identical filters; reuse across different filters returns `422`. |
| `limit`    | integer          | no       | Page size; defaults and maximum are open questions.  |

All filters AND together. Omitting filters returns the most recent page.

### Response (`200 OK`)

```json
{
  "items": [
    {
      "id": "<event-id>",
      "occurredAt": "2026-04-17T11:02:14Z",
      "actor":    "u_42",
      "resource": "order/9f3b",
      "action":   "order.refunded",
      "outcome":  "success",
      "payload":  {}
    }
  ],
  "nextCursor": "..."
}
```

`nextCursor` representation on the last page is an open question (see §5).
The public `id` representation is an open requirement: it may be the numeric
database id or a separate opaque id such as a ULID. The cursor design can use
the database id internally either way.
Structured `actor {id,type}` / `resource {id,type}` is not part of the contract
until the type-discriminator question is resolved in requirements.

### Status codes

| Code | When                                                                                     |
|------|------------------------------------------------------------------------------------------|
| 200  | Query succeeded. Empty `items` is still 200.                                             |
| 400  | Malformed syntax: unparseable timestamp, non-numeric `limit`, malformed cursor shape, invalid cursor signature. |
| 422  | Parseable but semantically invalid: `from > to`; valid cursor used with different filters. `limit > max` listed as `400` placeholder pending open requirement (`400` vs clamp). |

`400` vs `422` rule: if the value cannot be parsed into the declared type, it
is `400`. If it parses but violates a business rule, it is `422`. The
`limit > max` row in both this table and §5 is a placeholder using `400` —
requirements lists only two options (`400` or clamp); the final choice is
open (see requirements.md).
For cursors: malformed shape or failed signature verification is `400`; a valid
cursor whose embedded filter fingerprint does not match the current query is
`422`.

## 2. Pagination Strategy

**Choice: keyset (cursor) pagination, not offset.**

Why keyset fits append-only audit events:
- `audit_events` is insert-only. Older rows do not shift, so a cursor anchored
  on a row remains valid across writes.
- Offset pagination scans and discards `OFFSET` rows on every page — cost
  grows linearly with depth. Audit queries routinely walk deep history.
- Keyset gives stable `O(log n)` seek via an index, regardless of depth.
- Offset is also prone to skip/duplicate when concurrent inserts shift the
  window between page fetches. Keyset is immune because new rows simply land
  on a fresher page than the cursor.

Cursor encoding: the cursor carries the deterministic sort keys of the last
row of the previous page — `(occurredAt, database id)` — plus a fingerprint of
the filters used to create the cursor. The current database identifier is the
`bigserial` primary key. Even if the public response `id` becomes a ULID-style
identifier, the cursor can still use database id internally. Concretely,
base64-url of a compact payload such as
`"<occurredAt-epoch-millis>:<id>:<filter-fingerprint>"`.

Filter fingerprint: SHA-256 over a canonical UTF-8 encoding of the filter
tuple `(actor, resource, from, to)` — each field rendered as its string form
or a fixed sentinel for null, separated by `\x1f` (unit separator) to prevent
collision between, e.g., `actor="a|b"` and `resource="c"` vs `actor="a"` and
`resource="b|c"`. Output the first 16 bytes of the hash base64url-encoded.
Empty filters hash deterministically (all-null tuple → one fixed value).

Tamper detection: HMAC-SHA256 over the payload, server-side secret. The wire
form is `<base64url(payload)>.<base64url(hmac)>`. The service rejects any
cursor whose HMAC does not verify with the current server secret as `400`. A
valid cursor whose filter fingerprint differs from the current query returns
`422`. Alternative mechanisms (signed JWE, sealed box) satisfy the same
requirement; HMAC is chosen for simplicity and zero external dependencies.

Continuation query if the tiebreaker direction is descending, cursor
`(t*, i*)`:

```sql
... WHERE (event_timestamp, id) < (:t*, :i*)
ORDER BY event_timestamp DESC, id DESC
LIMIT :limit + 1
```

If requirements choose `id ASC` as the tiebreaker direction, use an expanded
predicate:

```sql
... WHERE event_timestamp < :t*
   OR (event_timestamp = :t* AND id > :i*)
ORDER BY event_timestamp DESC, id ASC
LIMIT :limit + 1
```

Fetching `limit + 1` lets the service detect whether another page exists
without a second round trip. If the extra row exists, it is dropped from
`items`, and `nextCursor` is built from the last returned item's keys.

No loss, no duplication: every row has a unique `(occurredAt, id)` (id is
unique by itself), and the strict cursor predicate guarantees the next page
starts strictly past the prior page's last returned row.

## 3. Sort & Determinism

- Primary sort: `occurredAt DESC` (newest first; required by requirements).
- Tiebreaker: event database `id`. Direction is an open requirement.

The tiebreaker is required because `occurredAt` is not guaranteed unique —
two events can share a server timestamp. Without a tiebreaker, the row order
within a tie is database-defined and may differ between page fetches, which
breaks keyset pagination: the cursor `(t*, i*)` could exclude a peer row
with the same `t*` or include it twice. A unique secondary key restores a
total order and makes the strict cursor comparison correct.

The database `id` is the natural internal tiebreaker because it is unique,
monotonic per writer, and already indexed as the primary key. If the API later
needs a ULID-style public identifier, update requirements first; the cursor can
still use database id internally.

## 4. Indexes

Goal: cover all single-filter cases and the global no-filter scan with
indexes that also serve the `(event_timestamp, id)` sort and cursor
comparison. Avoid combinatorial blow-up.

Proposed:

```sql
-- global newest-first scan + keyset
create index idx_audit_events_ts_id
  on audit_events (event_timestamp desc, id desc);

-- actor filter + sort/keyset
create index idx_audit_events_actor_ts_id
  on audit_events (actor, event_timestamp desc, id desc);

-- resource filter + sort/keyset
create index idx_audit_events_resource_ts_id
  on audit_events (resource, event_timestamp desc, id desc);

-- actor + resource filter + sort/keyset
create index idx_audit_events_actor_resource_ts_id
  on audit_events (actor, resource, event_timestamp desc, id desc);
```

Do not drop existing single-column indexes in the first migration. The
composite indexes likely subsume them for this endpoint, but removal is a
separate migration decision that should be based on `EXPLAIN` output and write
overhead.

Write-amplification note: during the transition the table carries seven
indexes (three legacy single-column + four new composites), and every INSERT
updates all of them. Schedule the cleanup migration that drops the legacy
indexes as a follow-up task tied to this feature, with a defined trigger
(e.g. "after one week in staging with `EXPLAIN` showing the new composites
cover the read path") — otherwise the temporary seven-index state quietly
becomes permanent and degrades ingest throughput.

The actor+resource composite is included because the example request uses both
filters and bitmap index intersection would not preserve the desired ordering;
it could force a sort before pagination.

Time-range-only queries: served by `idx_audit_events_ts_id` (range scan on
leading column).

Index direction (`desc`) is written for the illustrated
`occurredAt DESC, id DESC` plan. If requirements choose `id ASC`, define the
same indexes with `event_timestamp desc, id asc` instead.

## 5. Validation Rules and Edge Cases

| Rule                              | Result on violation                |
|-----------------------------------|------------------------------------|
| Empty filters allowed             | 200 with most recent page.         |
| `from`, `to` ISO-8601 instant     | 400 on parse failure.              |
| `from <= to` when both present    | 422.                               |
| `limit` is a positive integer     | 400 on parse failure.              |
| `limit` within `[1, max]`         | 400 placeholder (or clamp — open requirement). |
| `cursor` decodes and verifies     | 400 on malformed or tampered.      |
| Cursor filter fingerprint matches | 422 on mismatch.                   |

Edge cases:

- **Empty result page**: `200 OK` with `items: []`. `nextCursor` omitted (or
  `null` / `""` — pending requirements open question).
- **Last (terminal) page**: returned `items` may be shorter than `limit`;
  `nextCursor` representation pending requirements open question. Whichever
  representation wins, the response model needs the matching Jackson setting
  (`@JsonInclude(NON_NULL)` for "omitted", default for explicit `null`,
  custom serializer for `""`) so the wire form does not accidentally drift
  from the chosen semantics.
- **Cursor pointing past the end**: returns `items: []` with no `nextCursor`,
  same as the last-page case.
- **Concurrent inserts during paging**: new rows always have larger
  `(occurredAt, id)` than any in-flight cursor, so they appear only on
  pages fetched *before* their cursor — never as duplicates on later pages.

Open design questions specific to this design (not yet in requirements.md):

- **Maximum time-range span**: a single query covering years of data can
  fan out across many partitions / produce large pages. Cap `to - from` at
  some configurable maximum? Not in requirements.
- **Structured actor/resource shape**: requirements examples show
  `actor{id,type}` / `resource{id,type}`, but storage currently has scalar
  `actor` / `resource`. This design keeps scalar response fields for now. If
  structured objects are required, decide the source of `type` in requirements:
  derive from a prefix convention, or add `actor_type` / `resource_type`
  columns.

## 6. Layer Integration

The existing layering (enforced by `ArchitectureTest`) is preserved:

```
AuditEventController  (API / HTTP)
        |
        v
AuditEventService     (application / domain rules)
        |
        v
AuditEventRepository  (infrastructure / JPA)
```

Per-layer responsibilities for this feature:

- **API layer (`AuditEventController`)**
  - Bind query parameters (`actor`, `resource`, `from`, `to`, `cursor`,
    `limit`). Parse failures at binding time — unparseable `from`/`to` and
    non-numeric `limit` — surface as `400` via a `ControllerAdvice`.
    `cursor` remains an opaque string at this layer.
  - Return a DTO `AuditEventPageResponse { items, nextCursor }`, mapped from
    domain results. No JPA types, no Specifications, no SQL leakage.
  - Map storage fields → response fields (`id` → `id`,
    `event_timestamp` → `occurredAt`, `context` → `payload`).

  Semantic failures (`from > to`, `limit > max` if the final requirement
  rejects instead of clamps, decoded-cursor inconsistent with current filters)
  are not the controller's job; they are caught in the service layer and mapped
  to `422` by the same advice. This split keeps the boundary clean: 400 = "I
  cannot read what you sent", 422 = "I read it but it does not make sense."

- **Application layer (`AuditEventService`)**
  - Define a `AuditEventQuery` value object: filters, decoded cursor keys,
    effective limit.
  - Decode / encode cursors here, not in the controller. Controller passes the
    raw cursor string; service rejects malformed or tampered cursors with a
    typed domain exception mapped to `400`, and rejects a valid cursor whose
    filter fingerprint does not match the current query with a typed exception
    mapped to `422`.
  - Call the repository, return a domain `Page<AuditEvent>` with a typed
    `Cursor` next-pointer (not a string). Encoding to the response string
    happens at the boundary.

- **Infrastructure layer (`AuditEventRepository`)**
  - Add a keyset query: either a typed query method, or a `Specification`
    composed with cursor predicates and the selected deterministic sort. Either
    way, the cursor predicate is built here, not in the service.
  - Repository never returns HTTP types or response DTOs.

The current `search(...)` method on `AuditEventService` returns `List<AuditEvent>`
without paging; it will be replaced (or kept narrow for internal callers and
supplemented by a paged variant).

## 7. AGENTS.md Alignment

| AGENTS.md invariant                                      | Where this design enforces it |
|----------------------------------------------------------|-------------------------------|
| Read-only query endpoint                                 | `GET /audit-events` only; no write paths added; no mutation in service or repository on the read path. |
| Append-only audit events                                 | No UPDATE / DELETE introduced; cursor strategy explicitly depends on rows never shifting. |
| No UPDATE / DELETE behavior                              | Repository changes are read-only (`Specification` queries + sort); no destructive operations. |
| Deterministic ordering with explicit tiebreaker (§3)     | `occurredAt DESC` + database `id` tiebreaker; indexes built to match the chosen direction; cursor encodes both keys. |

The requirements document remains the source of truth. Where this design
encountered decisions not fixed by requirements (maximum time-range span,
type-discriminator origin, public id representation, `limit > max` behavior,
`nextCursor` shape on last page, tiebreaker direction), they are flagged as
open and routed back to `requirements.md` before any implementation begins.
