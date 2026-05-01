package com.example.audit.event;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-events")
public class AuditEventController {
  private final AuditEventService service;

  public AuditEventController(AuditEventService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AuditEvent create(@Valid @RequestBody AuditEventRequest request) {
    return service.create(request);
  }

  @GetMapping
  public List<AuditEvent> search(
      @RequestParam(required = false) String actor,
      @RequestParam(required = false) String resource,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to) {
    return service.search(actor, resource, from, to);
  }
}
