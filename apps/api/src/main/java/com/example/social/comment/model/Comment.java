package com.example.social.comment.model;

import java.time.Instant;

public record Comment(
    long id,
    long postId,
    long userId,
    String userName,
    Long parentCommentId,
    String content,
    Instant createdAt,
    Instant deletedAt
) {
}
