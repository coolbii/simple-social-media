package com.example.social.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
    @NotBlank(message = "Post content is required.")
    @Size(max = 500, message = "Post content must be 500 characters or fewer.") String content,
    String imageUrl
) {
}
