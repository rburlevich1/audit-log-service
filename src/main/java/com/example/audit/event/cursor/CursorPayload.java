package com.example.audit.event.cursor;

import java.time.Instant;

public record CursorPayload(Instant occurredAt, long id, String filterFingerprint) {}
