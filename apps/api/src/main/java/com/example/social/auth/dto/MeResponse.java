package com.example.social.auth.dto;

public record MeResponse(
	long id,
	String userName,
	String phoneNumber,
	String email,
	String coverImageUrl,
	String biography
) {
}
