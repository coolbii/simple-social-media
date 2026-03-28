package com.example.social.auth.dto;

public record LoginResponse(String accessToken, long expiresIn, UserSummary user) {
}
