package com.example.social.auth.service;

import java.time.Instant;

public record VerificationStartResult(String requestId, String status, Instant expiresAt) {
}
