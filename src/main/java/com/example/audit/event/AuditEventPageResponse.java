package com.example.audit.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record AuditEventPageResponse(
    List<AuditEventResponse> items, @JsonInclude(JsonInclude.Include.NON_NULL) String nextCursor) {}
