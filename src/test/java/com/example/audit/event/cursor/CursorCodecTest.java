package com.example.audit.event.cursor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CursorCodecTest {
  private static final String SECRET = "unit-test-secret";

  @Test
  void roundTripsPayload() {
    CursorCodec codec = new CursorCodec(SECRET);
    CursorPayload original = new CursorPayload(Instant.parse("2026-05-01T10:00:00Z"), 42L, "fp1");

    CursorPayload decoded = codec.decode(codec.encode(original));

    assertThat(decoded.occurredAt()).isEqualTo(original.occurredAt());
    assertThat(decoded.id()).isEqualTo(original.id());
    assertThat(decoded.filterFingerprint()).isEqualTo(original.filterFingerprint());
  }

  @Test
  void rejectsTamperedTag() {
    CursorCodec codec = new CursorCodec(SECRET);
    String token = codec.encode(new CursorPayload(Instant.now(), 1L, "fp"));

    int dot = token.indexOf('.');
    char tagChar = token.charAt(dot + 1);
    String tampered =
        token.substring(0, dot + 1) + (tagChar == 'A' ? 'B' : 'A') + token.substring(dot + 2);

    assertThatThrownBy(() -> codec.decode(tampered)).isInstanceOf(TamperedCursorException.class);
  }

  @Test
  void rejectsTokenSignedWithDifferentSecret() {
    CursorCodec writer = new CursorCodec("secret-a");
    CursorCodec reader = new CursorCodec("secret-b");
    String token = writer.encode(new CursorPayload(Instant.now(), 1L, "fp"));

    assertThatThrownBy(() -> reader.decode(token)).isInstanceOf(TamperedCursorException.class);
  }

  @Test
  void rejectsTokenWithoutSignatureSeparator() {
    CursorCodec codec = new CursorCodec(SECRET);

    assertThatThrownBy(() -> codec.decode("nosignaturepart"))
        .isInstanceOf(MalformedCursorException.class);
  }

  @Test
  void rejectsTokenWithEmptySignatureSegment() {
    CursorCodec codec = new CursorCodec(SECRET);

    assertThatThrownBy(() -> codec.decode("body.")).isInstanceOf(MalformedCursorException.class);
  }

  @Test
  void rejectsNonBase64Token() {
    CursorCodec codec = new CursorCodec(SECRET);

    assertThatThrownBy(() -> codec.decode("not!base64!.also!bad"))
        .isInstanceOf(MalformedCursorException.class);
  }

  @Test
  void rejectsBlankSecret() {
    assertThatThrownBy(() -> new CursorCodec("")).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new CursorCodec(null)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new CursorCodec("   ")).isInstanceOf(IllegalStateException.class);
  }
}
