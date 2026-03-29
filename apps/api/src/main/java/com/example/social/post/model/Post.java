package com.example.social.post.model;

import java.time.Instant;

public record Post(
    long id,
    long userId,
    String userName,
    String content,
    String imageKey,
    String imageUrl,
    Instant createdAt,
    Instant updatedAt
) {
}
