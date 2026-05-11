package com.example.audit.event.cursor;

public class CursorFilterMismatchException extends RuntimeException {
  public CursorFilterMismatchException(String message) {
    super(message);
  }
}
