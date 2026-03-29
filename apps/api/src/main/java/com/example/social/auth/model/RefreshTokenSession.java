package com.example.social.auth.model;

import java.time.Instant;

public record RefreshTokenSession(
    long id,
    long userId,
    String tokenHash,
    String familyId,
    Long parentTokenId,
    Instant expiresAt,
    Instant revokedAt,
    String revokeReason
) {
}
