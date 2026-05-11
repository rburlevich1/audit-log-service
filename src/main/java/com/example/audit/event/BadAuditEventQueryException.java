package com.example.audit.event;

public class BadAuditEventQueryException extends RuntimeException {
  public BadAuditEventQueryException(String message) {
    super(message);
  }
}
