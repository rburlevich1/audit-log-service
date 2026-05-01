package com.example.audit.retention;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RetentionJob {
  private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

  private final Clock clock;
  private final int retentionDays;

  public RetentionJob(@Value("${audit.retention.days}") int retentionDays) {
    this.clock = Clock.systemUTC();
    this.retentionDays = retentionDays;
  }

  @Scheduled(cron = "${audit.retention.cron}")
  public void run() {
    Instant cutoff = Instant.now(clock).minus(retentionDays, ChronoUnit.DAYS);
    log.info("Audit events older than {} are ready for archival", cutoff);
  }
}
