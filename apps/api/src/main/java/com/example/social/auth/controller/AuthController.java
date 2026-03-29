package com.example.social.auth.controller;

import java.time.Duration;

import com.example.social.auth.dto.LoginRequest;
import com.example.social.auth.dto.LoginResponse;
import com.example.social.auth.dto.LogoutResponse;
import com.example.social.auth.dto.MeResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final String sessionCookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(
        AuthService authService,
        @Value("${app.auth.session-cookie-name:sessionId}") String sessionCookieName,
        @Value("${app.auth.cookie-secure:false}") boolean cookieSecure,
        @Value("${app.auth.cookie-same-site:Lax}") String cookieSameSite
    ) {
        this.authService = authService;
        this.sessionCookieName = sessionCookieName;
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
                sessionCookie(session.sessionToken()).toString()
            )
            .body(ApiResponse.ok(session.response()));
    }

    @PostMapping("/logout")
    @Operation(operationId = "authLogout", summary = "Clear the session cookie for the current session.")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(HttpServletRequest request) {
        String sessionToken = extractSessionToken(request);
        authService.logout(sessionToken);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, clearSessionCookie().toString())
            .body(ApiResponse.ok(new LogoutResponse(true)));
    }

    @GetMapping("/me")
    @Operation(operationId = "authMe", summary = "Return the current user profile from session.")
    @SuppressWarnings("PMD.ShortMethodName")
    public ApiResponse<MeResponse> me(HttpServletRequest request) {
        return ApiResponse.ok(authService.me(extractSessionToken(request)));
    }

    private ResponseCookie sessionCookie(String token) {
        return ResponseCookie.from(sessionCookieName, token)
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path("/")
            .maxAge(Duration.ofSeconds(authService.refreshTokenTtlSeconds()))
            .build();
    }

    private ResponseCookie clearSessionCookie() {
        return ResponseCookie.from(sessionCookieName, "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path("/")
            .maxAge(Duration.ZERO)
            .build();
    }

    private String extractSessionToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return "";
        }

        for (Cookie cookie : cookies) {
            if (sessionCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return "";
    }
}
