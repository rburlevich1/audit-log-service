package com.example.audit.event;

import java.time.Instant;

public record AuditEventQuery(
    String actor, String resource, Instant from, Instant to, String cursor, Integer limit) {}
