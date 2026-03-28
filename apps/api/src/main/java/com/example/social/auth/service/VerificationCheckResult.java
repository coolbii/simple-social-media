package com.example.social.auth.service;

public record VerificationCheckResult(String requestId, String status, boolean approved) {
}
