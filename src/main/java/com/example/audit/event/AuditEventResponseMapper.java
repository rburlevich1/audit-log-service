package com.example.audit.event;

import org.springframework.stereotype.Component;

@Component
class AuditEventResponseMapper {
  AuditEventResponse toResponse(AuditEvent event) {
    return new AuditEventResponse(
        event.getId(),
        event.getTimestamp(),
        event.getActor(),
        event.getResource(),
        event.getAction(),
        event.getOutcome(),
        event.getContext());
  }
}
