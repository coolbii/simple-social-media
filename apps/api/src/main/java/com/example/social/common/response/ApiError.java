package com.example.social.common.response;

import java.util.Map;

public record ApiError(String code, String message, Map<String, String> details) {

	public ApiError(String code, String message) {
		this(code, message, Map.of());
	}
}
