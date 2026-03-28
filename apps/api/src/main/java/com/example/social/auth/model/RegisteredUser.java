package com.example.social.auth.model;

public record RegisteredUser(
	long id,
	String phoneNumber,
	String userName,
	String email,
	String passwordHash,
	String coverImageUrl,
	String biography
) {
}
