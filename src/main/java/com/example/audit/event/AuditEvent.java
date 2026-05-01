package com.example.audit.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  private Long id;

  @Column(name = "event_timestamp", nullable = false, updatable = false)
  private Instant timestamp;

  @Column(nullable = false, updatable = false)
  private String actor;

  @Column(updatable = false)
  private String action;

  @Column(updatable = false)
  private String resource;

  @Column(updatable = false)
  private String outcome;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", updatable = false)
  private JsonNode context;

  protected AuditEvent() {}

  public AuditEvent(
      Instant timestamp,
      String actor,
      String action,
      String resource,
      String outcome,
      JsonNode context) {
    this.timestamp = timestamp;
    this.actor = actor;
    this.action = action;
    this.resource = resource;
    this.outcome = outcome;
    this.context = context;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getActor() {
    return actor;
  }

  public String getAction() {
    return action;
  }

  public String getResource() {
    return resource;
  }

  public String getOutcome() {
    return outcome;
  }

  public JsonNode getContext() {
    return context;
  }
}
