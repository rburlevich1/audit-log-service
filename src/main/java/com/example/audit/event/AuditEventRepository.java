package com.example.audit.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEventRepository
    extends JpaRepository<AuditEvent, Long>,
        JpaSpecificationExecutor<AuditEvent>,
        AuditEventKeysetQuery {}
