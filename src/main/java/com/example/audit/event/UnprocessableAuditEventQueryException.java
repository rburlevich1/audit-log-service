package com.example.audit.event;

public class UnprocessableAuditEventQueryException extends RuntimeException {
  public UnprocessableAuditEventQueryException(String message) {
    super(message);
  }
}
