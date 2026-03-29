package com.example.social.sse.dto;

import com.example.social.comment.dto.CommentResponse;

public record CommentCreatedEvent(String type, long postId, CommentResponse comment) {
}
