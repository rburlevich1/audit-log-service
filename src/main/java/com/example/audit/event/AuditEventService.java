package com.example.audit.event;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventService {
    private final AuditEventRepository repository;

    public AuditEventService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AuditEvent create(AuditEventRequest request) {
        AuditEvent event = new AuditEvent(
                Instant.now(),
                request.getActor(),
                request.getAction(),
                request.getResource(),
                request.getOutcome(),
                request.getContext()
        );
        return repository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> search(String actor, String resource, Instant from, Instant to) {
        Specification<AuditEvent> spec = Specification.where(null);

        if (actor != null && !actor.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actor"), actor));
        }
        if (resource != null && !resource.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("resource"), resource));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), to));
        }

        return repository.findAll(spec);
    }
}

