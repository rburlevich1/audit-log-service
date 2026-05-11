package com.example.audit.event;

import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventService {
  private final AuditEventRepository repository;
  private final int maxPageSize;

  public AuditEventService(
      AuditEventRepository repository, @Value("${audit.query.max-page-size:200}") int maxPageSize) {
    this.repository = repository;
    this.maxPageSize = maxPageSize;
  }

  @Transactional
  public AuditEvent create(AuditEventRequest request) {
    AuditEvent event =
        new AuditEvent(
            Instant.now(),
            request.getActor(),
            request.getAction(),
            request.getResource(),
            request.getOutcome(),
            request.getContext());
    return repository.save(event);
  }

  @Transactional(readOnly = true)
  public List<AuditEvent> search(AuditEventQuery query) {
    validate(query);

    Specification<AuditEvent> spec = Specification.where(null);

    if (query.actor() != null && !query.actor().isBlank()) {
      spec = spec.and((root, criteriaQuery, cb) -> cb.equal(root.get("actor"), query.actor()));
    }
    if (query.resource() != null && !query.resource().isBlank()) {
      spec =
          spec.and((root, criteriaQuery, cb) -> cb.equal(root.get("resource"), query.resource()));
    }
    if (query.from() != null) {
      spec =
          spec.and(
              (root, criteriaQuery, cb) ->
                  cb.greaterThanOrEqualTo(root.get("timestamp"), query.from()));
    }
    if (query.to() != null) {
      spec =
          spec.and(
              (root, criteriaQuery, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), query.to()));
    }

    return repository.findAll(spec);
  }

  private void validate(AuditEventQuery query) {
    if (query.cursor() != null) {
      throw new BadAuditEventQueryException("Cursor is not supported yet");
    }
    if (query.limit() != null && (query.limit() < 1 || query.limit() > maxPageSize)) {
      throw new BadAuditEventQueryException("Limit must be between 1 and " + maxPageSize);
    }
    if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
      throw new UnprocessableAuditEventQueryException("from must be before or equal to to");
    }
  }
}
