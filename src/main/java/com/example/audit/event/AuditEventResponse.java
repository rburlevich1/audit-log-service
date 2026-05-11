package com.example.audit.event;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record AuditEventResponse(
    Long id,
    Instant occurredAt,
    String actor,
    String resource,
    String action,
    String outcome,
    JsonNode payload) {}
