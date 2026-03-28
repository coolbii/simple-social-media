package com.example.social.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyCodeRequest(
	@NotBlank(message = "Phone number is required.") String phoneNumber,
	@NotBlank(message = "Verification code is required.") String code
) {
}
