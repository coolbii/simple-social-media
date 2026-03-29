package com.example.social.storage.dto;

public record PresignResponse(String objectKey, String uploadUrl, String publicUrl, String method) {
}
