package com.example.social.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
	@NotBlank(message = "Registration token is required.") String registrationToken,
	@NotBlank(message = "Phone number is required.") String phoneNumber,
	@NotBlank(message = "User name is required.") String userName,
	@NotBlank(message = "Password is required.")
	@Size(min = 8, message = "Password must be at least 8 characters.") String password,
	@Email(message = "Email must be valid.") String email
) {
}
