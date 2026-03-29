package com.example.social.post.dto;

import java.time.Instant;

public record PostResponse(
    long id,
    long userId,
    String userName,
    String content,
    String imageUrl,
    Instant createdAt,
    Instant updatedAt
) {
}
