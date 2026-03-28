package com.example.social.auth.dto;

public record RefreshResponse(String accessToken, long expiresIn) {
}
