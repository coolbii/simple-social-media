package com.example.social.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SendCodeRequest(
	@NotBlank(message = "Phone number is required.") String phoneNumber
) {
}
