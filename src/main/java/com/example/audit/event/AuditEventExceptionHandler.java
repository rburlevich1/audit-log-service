package com.example.audit.event;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class AuditEventExceptionHandler {
  @ExceptionHandler(BadAuditEventQueryException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  Map<String, String> badQuery(BadAuditEventQueryException exception) {
    return Map.of("error", exception.getMessage());
  }

  @ExceptionHandler(UnprocessableAuditEventQueryException.class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  Map<String, String> unprocessableQuery(UnprocessableAuditEventQueryException exception) {
    return Map.of("error", exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  Map<String, String> typeMismatch(MethodArgumentTypeMismatchException exception) {
    return Map.of("error", "Invalid query parameter");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  Map<String, String> invalidRequestBody(MethodArgumentNotValidException exception) {
    return Map.of("error", "Invalid request body");
  }
}
