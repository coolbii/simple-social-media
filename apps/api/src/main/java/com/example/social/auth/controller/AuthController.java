package com.example.social.auth.controller;

import java.time.Duration;

import com.example.social.auth.dto.LoginRequest;
import com.example.social.auth.dto.LoginResponse;
import com.example.social.auth.dto.LogoutResponse;
import com.example.social.auth.dto.MeResponse;
import com.example.social.auth.dto.RefreshResponse;
import com.example.social.auth.dto.RegisterRequest;
import com.example.social.auth.dto.RegisterResponse;
import com.example.social.auth.dto.SendCodeRequest;
import com.example.social.auth.dto.SendCodeResponse;
import com.example.social.auth.dto.VerifyCodeRequest;
import com.example.social.auth.dto.VerifyCodeResponse;
import com.example.social.auth.service.AuthService;
import com.example.social.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

	private final AuthService authService;
	private final String refreshCookieName;
	private final boolean cookieSecure;
	private final String cookieSameSite;

	public AuthController(
		AuthService authService,
		@Value("${app.auth.refresh-cookie-name:refreshToken}") String refreshCookieName,
		@Value("${app.auth.cookie-secure:false}") boolean cookieSecure,
		@Value("${app.auth.cookie-same-site:Lax}") String cookieSameSite
	) {
		this.authService = authService;
		this.refreshCookieName = refreshCookieName;
		this.cookieSecure = cookieSecure;
		this.cookieSameSite = cookieSameSite;
	}

	@PostMapping("/phone/send-code")
	@Operation(operationId = "authSendCode", summary = "Send phone verification code.")
	public ApiResponse<SendCodeResponse> sendCode(@Valid @RequestBody SendCodeRequest request) {
		return ApiResponse.ok(authService.sendCode(request));
	}

	@PostMapping("/phone/verify-code")
	@Operation(operationId = "authVerifyCode", summary = "Verify OTP and issue registration token.")
	public ApiResponse<VerifyCodeResponse> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
		return ApiResponse.ok(authService.verifyCode(request));
	}

	@PostMapping("/register")
	@Operation(operationId = "authRegister", summary = "Register a new user by phone number.")
	public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ApiResponse.ok(authService.register(request));
	}

	@PostMapping("/login")
	@Operation(operationId = "authLogin", summary = "Log in with phone number and password.")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
		AuthService.LoginSession session = authService.login(request);
		return ResponseEntity.ok()
			.header(
				HttpHeaders.SET_COOKIE,
				refreshCookie(session.refreshToken()).toString()
			)
			.body(ApiResponse.ok(session.response()));
	}

	@PostMapping("/refresh")
	@Operation(operationId = "authRefresh", summary = "Rotate the refresh token and return a new access token.")
	public ResponseEntity<ApiResponse<RefreshResponse>> refresh(HttpServletRequest request) {
		String refreshToken = extractRefreshToken(request);
		AuthService.RefreshSession session = authService.refresh(refreshToken);
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
			.body(ApiResponse.ok(session.response()));
	}

	@PostMapping("/logout")
	@Operation(operationId = "authLogout", summary = "Clear the refresh cookie for the current session.")
	public ResponseEntity<ApiResponse<LogoutResponse>> logout(HttpServletRequest request) {
		String refreshToken = extractRefreshToken(request);
		authService.logout(refreshToken);
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
			.body(ApiResponse.ok(new LogoutResponse(true)));
	}

	@GetMapping("/me")
	@Operation(operationId = "authMe", summary = "Return the current scaffold user profile.")
	public ApiResponse<MeResponse> me(
		@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
	) {
		return ApiResponse.ok(authService.me(extractAccessToken(authorization)));
	}

	private ResponseCookie refreshCookie(String token) {
		return ResponseCookie.from(refreshCookieName, token)
			.httpOnly(true)
			.secure(cookieSecure)
			.sameSite(cookieSameSite)
			.path("/api/auth")
			.maxAge(Duration.ofSeconds(authService.refreshTokenTtlSeconds()))
			.build();
	}

	private ResponseCookie clearRefreshCookie() {
		return ResponseCookie.from(refreshCookieName, "")
			.httpOnly(true)
			.secure(cookieSecure)
			.sameSite(cookieSameSite)
			.path("/api/auth")
			.maxAge(Duration.ZERO)
			.build();
	}

	private String extractAccessToken(String authorization) {
		if (authorization == null || authorization.isBlank()) {
			return "";
		}
		if (authorization.startsWith("Bearer ")) {
			return authorization.substring(7).trim();
		}
		return authorization.trim();
	}

	private String extractRefreshToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null || cookies.length == 0) {
			return "";
		}

		for (Cookie cookie : cookies) {
			if (refreshCookieName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}

		return "";
	}
}
