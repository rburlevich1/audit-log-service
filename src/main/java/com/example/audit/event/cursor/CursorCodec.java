package com.example.audit.event.cursor;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class CursorCodec {
  private static final String ALGORITHM = "HmacSHA256";
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final byte[] secret;

  public CursorCodec(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "Cursor secret must be configured (audit.query.cursor-secret)");
    }
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
  }

  public String encode(CursorPayload payload) {
    String body =
        payload.occurredAt().toEpochMilli()
            + ":"
            + payload.id()
            + ":"
            + payload.filterFingerprint();
    byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
    byte[] tag = hmac(bodyBytes);
    return ENCODER.encodeToString(bodyBytes) + "." + ENCODER.encodeToString(tag);
  }

  public CursorPayload decode(String token) {
    if (token == null) {
      throw new MalformedCursorException("Cursor is null");
    }
    int dot = token.indexOf('.');
    if (dot < 1 || dot >= token.length() - 1) {
      throw new MalformedCursorException("Cursor missing signature segment");
    }
    byte[] bodyBytes;
    byte[] tagBytes;
    try {
      bodyBytes = DECODER.decode(token.substring(0, dot));
      tagBytes = DECODER.decode(token.substring(dot + 1));
    } catch (IllegalArgumentException e) {
      throw new MalformedCursorException("Cursor not valid base64url");
    }
    byte[] expectedTag = hmac(bodyBytes);
    if (!MessageDigest.isEqual(expectedTag, tagBytes)) {
      throw new TamperedCursorException("Cursor signature verification failed");
    }
    String body = new String(bodyBytes, StandardCharsets.UTF_8);
    String[] parts = body.split(":", -1);
    if (parts.length != 3 || parts[2].isEmpty()) {
      throw new MalformedCursorException("Cursor payload shape invalid");
    }
    try {
      long epochMillis = Long.parseLong(parts[0]);
      long id = Long.parseLong(parts[1]);
      return new CursorPayload(Instant.ofEpochMilli(epochMillis), id, parts[2]);
    } catch (NumberFormatException e) {
      throw new MalformedCursorException("Cursor payload fields invalid");
    }
  }

  private byte[] hmac(byte[] data) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret, ALGORITHM));
      return mac.doFinal(data);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("HMAC unavailable", e);
    }
  }
}
