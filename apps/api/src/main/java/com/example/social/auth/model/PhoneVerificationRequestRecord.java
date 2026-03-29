package com.example.social.auth.model;

import java.time.Instant;

public record PhoneVerificationRequestRecord(
    long id,
    String phoneNumber,
    String provider,
    String providerRequestId,
    String status,
    int attemptCount,
    Instant approvedAt,
    Instant expiresAt,
    Instant createdAt,
    Instant updatedAt
) {
}
