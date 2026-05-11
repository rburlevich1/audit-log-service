package com.example.audit.event;

import com.example.audit.event.cursor.CursorCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AuditQueryConfig {
  @Bean
  CursorCodec cursorCodec(AuditQueryProperties properties) {
    return new CursorCodec(properties.getCursorSecret());
  }
}
