package com.example.social.storage.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignRequest(
    @NotBlank(message = "File name is required.") String fileName,
    @NotBlank(message = "Content type is required.") String contentType
) {
}
