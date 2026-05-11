package com.example.audit.event;

import com.example.audit.event.cursor.CursorCodec;
import com.example.audit.event.cursor.CursorFilterMismatchException;
import com.example.audit.event.cursor.CursorPayload;
import com.example.audit.event.cursor.FilterFingerprint;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventService {
  private final AuditEventRepository repository;
  private final AuditQueryProperties properties;
  private final CursorCodec cursorCodec;

  public AuditEventService(
      AuditEventRepository repository, AuditQueryProperties properties, CursorCodec cursorCodec) {
    this.repository = repository;
    this.properties = properties;
    this.cursorCodec = cursorCodec;
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
  public AuditPage search(AuditEventQuery query) {
    validate(query);

    int limit = query.limit() == null ? properties.getDefaultPageSize() : query.limit();
    String fingerprint =
        FilterFingerprint.compute(query.actor(), query.resource(), query.from(), query.to());

    CursorPayload cursor = null;
    if (query.cursor() != null) {
      cursor = cursorCodec.decode(query.cursor());
      if (!cursor.filterFingerprint().equals(fingerprint)) {
        throw new CursorFilterMismatchException(
            "Cursor was issued for a different filter combination");
      }
    }

    Specification<AuditEvent> spec = buildSpecification(query, cursor);
    List<AuditEvent> rows = repository.findKeysetPage(spec, limit + 1);

    if (rows.size() <= limit) {
      return new AuditPage(rows, null);
    }
    List<AuditEvent> page = rows.subList(0, limit);
    AuditEvent last = page.get(page.size() - 1);
    String nextCursor =
        cursorCodec.encode(new CursorPayload(last.getTimestamp(), last.getId(), fingerprint));
    return new AuditPage(page, nextCursor);
  }

  private Specification<AuditEvent> buildSpecification(
      AuditEventQuery query, CursorPayload cursor) {
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
    if (cursor != null) {
      Instant ts = cursor.occurredAt();
      long id = cursor.id();
      spec =
          spec.and(
              (root, criteriaQuery, cb) ->
                  cb.or(
                      cb.lessThan(root.get("timestamp"), ts),
                      cb.and(
                          cb.equal(root.get("timestamp"), ts), cb.lessThan(root.get("id"), id))));
    }
    return spec;
  }

  private void validate(AuditEventQuery query) {
    if (query.limit() != null
        && (query.limit() < 1 || query.limit() > properties.getMaxPageSize())) {
      throw new BadAuditEventQueryException(
          "Limit must be between 1 and " + properties.getMaxPageSize());
    }
    if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
      throw new UnprocessableAuditEventQueryException("from must be before or equal to to");
    }
  }
}
