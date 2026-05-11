create index idx_audit_events_ts_id
    on audit_events (event_timestamp desc, id desc);

create index idx_audit_events_actor_ts_id
    on audit_events (actor, event_timestamp desc, id desc);

create index idx_audit_events_resource_ts_id
    on audit_events (resource, event_timestamp desc, id desc);

create index idx_audit_events_actor_resource_ts_id
    on audit_events (actor, resource, event_timestamp desc, id desc);
