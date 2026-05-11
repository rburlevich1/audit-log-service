package com.example.audit.event.cursor;

public class TamperedCursorException extends RuntimeException {
  public TamperedCursorException(String message) {
    super(message);
  }
}
