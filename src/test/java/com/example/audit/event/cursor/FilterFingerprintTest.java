package com.example.audit.event.cursor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class FilterFingerprintTest {
  @Test
  void identicalInputsProduceIdenticalFingerprints() {
    String a =
        FilterFingerprint.compute(
            "u_42",
            "order/9f3b",
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-05-01T00:00:00Z"));
    String b =
        FilterFingerprint.compute(
            "u_42",
            "order/9f3b",
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-05-01T00:00:00Z"));

    assertThat(a).isEqualTo(b);
  }

  @Test
  void differentActorProducesDifferentFingerprint() {
    String a = FilterFingerprint.compute("u_42", "r", null, null);
    String b = FilterFingerprint.compute("u_43", "r", null, null);

    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void allNullTupleHashesDeterministically() {
    String a = FilterFingerprint.compute(null, null, null, null);
    String b = FilterFingerprint.compute(null, null, null, null);

    assertThat(a).isEqualTo(b);
    assertThat(a).isNotBlank();
  }

  @Test
  void unitSeparatorPreventsDelimiterCollision() {
    // actor "a|b" + resource "c" vs actor "a" + resource "b|c"
    // With a pipe delimiter both would collapse to "a|b|c"; with the unit
    // separator () they hash differently because the delimiter cannot
    // appear inside the user-provided values used here.
    String a = FilterFingerprint.compute("a|b", "c", null, null);
    String b = FilterFingerprint.compute("a", "b|c", null, null);

    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void outputIsBase64UrlAnd22Chars() {
    // 16 bytes base64url without padding = ceil(16 * 4 / 3) = 22 characters
    String fingerprint = FilterFingerprint.compute("actor", "resource", null, null);

    assertThat(fingerprint).hasSize(22);
    assertThat(fingerprint).matches("[A-Za-z0-9_-]+");
  }
}
