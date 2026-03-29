package com.example.social.auth.model;

import java.time.Instant;

public record RegistrationTokenRecord(
    long id,
    String phoneNumber,
    String tokenHash,
    Long verificationRequestId,
    Instant expiresAt,
    Instant consumedAt,
    Instant createdAt
) {
}
