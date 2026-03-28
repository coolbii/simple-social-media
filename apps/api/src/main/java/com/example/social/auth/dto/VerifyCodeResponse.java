package com.example.social.auth.dto;

public record VerifyCodeResponse(String registrationToken, long expiresInSeconds) {
}
