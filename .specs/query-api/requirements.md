# Query API Requirements

## Problem

Audit users need a read-only HTTP endpoint to retrieve immutable `audit_events`
by actor, resource, and time range, with cursor pagination over large result
sets.

```http
GET /audit-events?actor=u_42&resource=order/9f3b...&from=2026-04-01T00:00:00Z&to=2026-05-01T00:00:00Z&cursor=...&limit=50
```

Illustrative response item, pending open questions on public `id` and
actor/resource shape:

```json
{
  "id": "<event-id>",
  "occurredAt": "2026-04-17T11:02:14Z",
  "actor": "<actor>",
  "resource": "<resource>",
  "action": "order.refunded",
  "outcome": "success",
  "payload": {}
}
```

Page response includes `items` and page-level `nextCursor`.

## User Stories With AC

### Compliance Officer

As a compliance officer, I want to query audit events by actor, resource, or
time range, so that I can confirm or refute an action.

- When `actor` is supplied, the system shall return only events for that actor.
- When `resource` is supplied, the system shall return only events for that
  resource.
- When `from` or `to` is supplied, the system shall apply the boundary
  inclusively on both ends.
- When no filters are supplied, the system shall return events using the
  default page size.

### SRE / Security Analyst

As an SRE or security analyst, I want to query actions on a resource, so that I
can reconstruct an incident timeline.

- When events are returned, the system shall order them by `occurredAt`
  descending, with event `id` as the deterministic tiebreaker.
- When the endpoint handles a read request, the system shall not mutate stored
  audit events.

### Analyst

As an analyst, I want cursor pagination, so that I can page through large
result sets without loss or duplication.

- When more results exist, the system shall return `nextCursor` at the page
  level.
- When `cursor` is supplied, the system shall continue after that cursor using
  the same deterministic ordering.
- When `cursor` is malformed or tampered, the system shall return
  `400 Bad Request`.
- When `cursor` is valid but belongs to different filters than the current
  request, the system shall return `422 Unprocessable Entity`.
- When `limit` is supplied, the system shall compare it with the configured
  maximum.
- When `limit` is omitted, the system shall apply the default page size.

## Out of Scope

- Creating, updating, or deleting audit events through this API.
- Archival retrieval.
- Caller-selectable sort order.

## Open Questions

- Default page size?
- Maximum page size?
- Should the event `id` tiebreaker sort ascending or descending?
- Should `limit` above the configured maximum return `400 Bad Request` or be
  clamped to the maximum?
- Should the page response include extra metadata such as `limit` or `hasMore`?
- Should filtering by `outcome`, `action`, or `payload` contents be supported?
- Required format for `from`/`to` (ISO-8601 UTC only?) and response on malformed
  timestamps?
- Should there be a maximum allowed time range for `from`/`to`?
- Representation of `nextCursor` on the last page — omitted, `null`, or empty
  string?
- Should response `id` be the numeric database id, or an opaque public id such
  as a ULID?
- Should `actor` and `resource` remain scalar response fields, or become
  structured objects with `id` and `type`?
- Authentication and authorization model for the endpoint?
