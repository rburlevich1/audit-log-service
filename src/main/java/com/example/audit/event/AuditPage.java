package com.example.audit.event;

import java.util.List;

public record AuditPage(List<AuditEvent> items, String nextCursor) {}
