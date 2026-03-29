package com.example.social.comment.dto;

import java.time.Instant;

public record CommentResponse(
    long id,
    long postId,
    long userId,
    String userName,
    String content,
    Instant createdAt
) {
}
