package com.example.social.comment.dto;

import java.util.List;

public record CommentPageResponse(
    List<CommentResponse> comments,
    boolean hasMore,
    Integer nextOffset
) {
}
