package com.example.audit.event;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "audit.query")
public class AuditQueryProperties {
  @NotBlank private String cursorSecret;

  @Min(1)
  private int defaultPageSize = 50;

  @Min(1)
  private int maxPageSize = 200;

  public String getCursorSecret() {
    return cursorSecret;
  }

  public void setCursorSecret(String cursorSecret) {
    this.cursorSecret = cursorSecret;
  }

  public int getDefaultPageSize() {
    return defaultPageSize;
  }

  public void setDefaultPageSize(int defaultPageSize) {
    this.defaultPageSize = defaultPageSize;
  }

  public int getMaxPageSize() {
    return maxPageSize;
  }

  public void setMaxPageSize(int maxPageSize) {
    this.maxPageSize = maxPageSize;
  }
}
