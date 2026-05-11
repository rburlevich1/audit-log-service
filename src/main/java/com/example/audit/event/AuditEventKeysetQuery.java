package com.example.audit.event;

import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public interface AuditEventKeysetQuery {
  List<AuditEvent> findKeysetPage(Specification<AuditEvent> spec, int limit);
}
