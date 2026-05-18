# Query API Requirements

## Problem

Audit users need a read-only HTTP endpoint to retrieve immutable `audit_events`
by actor, resource, and time range, with cursor pagination over large result
sets.

```http
GET /audit-events?actor=u_42&resource=order/9f3b...&from=2026-04-01T00:00:00Z&to=2026-05-01T00:00:00Z&cursor=...&limit=50
```

Response item:

```json
{
  "id": 12345,
  "occurredAt": "2026-04-17T11:02:14Z",
  "actor": "u_42",
  "resource": "order/9f3b",
  "action": "order.refunded",
  "outcome": "success",
  "payload": {}
}
```

Page response includes `items` and page-level `nextCursor`. On the last page,
`nextCursor` is omitted.

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
  descending, with event `id` descending as the deterministic tiebreaker.
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
- When `limit` is between `1` and the configured maximum inclusive, the system
  shall return at most `limit` items.
- When `limit` is omitted, the system shall apply the default page size.
- When `limit` is greater than the configured maximum, the system shall return
  `400 Bad Request`.
- When `limit` is less than `1`, the system shall return `400 Bad Request`.
- When `from` or `to` is supplied, the value shall be an ISO-8601 UTC instant.
- When `from` or `to` cannot be parsed as an ISO-8601 instant, the system shall
  return `400 Bad Request`.
- When both `from` and `to` are supplied and `from` is after `to`, the system
  shall return `422 Unprocessable Entity`.

## Out of Scope

- Creating, updating, or deleting audit events through this API.
- Archival retrieval.
- Caller-selectable sort order.
- Filtering by `outcome`, `action`, or `payload` contents.
- Authentication and authorization changes.

## Open Questions

- None.

## Fixed Decisions

- Default page size is configurable via property; default value is `50`.
- Maximum page size is configurable via property; default value is `200`.
- There is no maximum `from`/`to` time-range span; pagination limits response
  size.
- Response `id` is the numeric database id.
- `actor` and `resource` are scalar response fields.
- Page response contains only `items` and `nextCursor`.
