package com.example.social.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
    @NotBlank(message = "Comment content is required.")
    @Size(max = 280, message = "Comment content must be 280 characters or fewer.") String content,
    @Positive(message = "Parent comment id must be positive.") Long parentCommentId
) {
}
