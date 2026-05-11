package com.example.audit.event;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

class AuditEventKeysetQueryImpl implements AuditEventKeysetQuery {
  private final EntityManager entityManager;

  AuditEventKeysetQueryImpl(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public List<AuditEvent> findKeysetPage(Specification<AuditEvent> spec, int limit) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<AuditEvent> cq = cb.createQuery(AuditEvent.class);
    Root<AuditEvent> root = cq.from(AuditEvent.class);
    if (spec != null) {
      Predicate predicate = spec.toPredicate(root, cq, cb);
      if (predicate != null) {
        cq.where(predicate);
      }
    }
    cq.orderBy(cb.desc(root.get("timestamp")), cb.desc(root.get("id")));
    return entityManager.createQuery(cq).setMaxResults(limit).getResultList();
  }
}
