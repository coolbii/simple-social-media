package com.example.social.auth.dto;

public record SendCodeResponse(String requestId, long expiresInSeconds) {
}
