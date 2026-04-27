create table audit_events (
    id bigserial primary key,
    event_timestamp timestamptz not null,
    actor text not null,
    action text,
    resource text,
    outcome text check (outcome in ('success', 'denied', 'error')),
    context jsonb
);

create index idx_audit_events_actor on audit_events (actor);
create index idx_audit_events_resource on audit_events (resource);
create index idx_audit_events_timestamp on audit_events (event_timestamp);

