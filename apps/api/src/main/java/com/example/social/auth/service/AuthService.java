package com.example.social.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import com.example.social.auth.dto.LoginRequest;
import com.example.social.auth.dto.LoginResponse;
import com.example.social.auth.dto.MeResponse;
import com.example.social.auth.dto.RegisterRequest;
import com.example.social.auth.dto.RegisterResponse;
import com.example.social.auth.dto.SendCodeRequest;
import com.example.social.auth.dto.SendCodeResponse;
import com.example.social.auth.dto.UserSummary;
import com.example.social.auth.dto.VerifyCodeRequest;
import com.example.social.auth.dto.VerifyCodeResponse;
import com.example.social.auth.model.RegisteredUser;
import com.example.social.common.exception.ApiException;
import com.example.social.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private final PasswordEncoder passwordEncoder;
    private final SmsVerificationProvider smsVerificationProvider;
    private final long refreshTokenTtlSeconds;
    private final long registrationTokenTtlSeconds;
    private final int otpMaxAttempts;
    private final AtomicLong userSequence = new AtomicLong(2);
    private final Map<Long, RegisteredUser> users = new ConcurrentHashMap<>();
    private final Map<String, RegisteredUser> usersByPhone = new ConcurrentHashMap<>();
    private final Map<String, VerificationRequestRecord> verificationRequestsByPhone =
        new ConcurrentHashMap<>();
    private final Map<String, RegistrationTokenRecord> registrationTokens = new ConcurrentHashMap<>();
    private final Map<String, RefreshTokenRecord> refreshTokensByHash = new ConcurrentHashMap<>();

    public AuthService(
        PasswordEncoder passwordEncoder,
        SmsVerificationProvider smsVerificationProvider,
        @Value("${app.auth.refresh-token-ttl-seconds:2592000}") long refreshTokenTtlSeconds,
        @Value("${app.auth.registration-token-ttl-seconds:600}") long registrationTokenTtlSeconds,
        @Value("${app.auth.otp.max-attempts:5}") int otpMaxAttempts
    ) {
        this.passwordEncoder = passwordEncoder;
        this.smsVerificationProvider = smsVerificationProvider;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
        this.registrationTokenTtlSeconds = registrationTokenTtlSeconds;
        this.otpMaxAttempts = otpMaxAttempts;
        seedUsers();
    }

    public SendCodeResponse sendCode(SendCodeRequest request) {
        evictExpiredRecords();

        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        if (findByPhone(phoneNumber).isPresent()) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.AUTH_PHONE_ALREADY_REGISTERED,
                "This phone number is already registered."
            );
        }

        VerificationStartResult verification = smsVerificationProvider.sendCode(phoneNumber);
        Instant expiresAt = verification.expiresAt() == null
            ? Instant.now().plus(5, ChronoUnit.MINUTES)
            : verification.expiresAt();

        verificationRequestsByPhone.put(
            phoneNumber,
            new VerificationRequestRecord(
                phoneNumber,
                verification.requestId(),
                expiresAt,
                "pending",
                0
            )
        );

        long expiresIn = Math.max(1, Instant.now().until(expiresAt, ChronoUnit.SECONDS));
        return new SendCodeResponse(verification.requestId(), expiresIn);
    }

    public VerifyCodeResponse verifyCode(VerifyCodeRequest request) {
        evictExpiredRecords();

        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        VerificationRequestRecord verification = verificationRequestsByPhone.get(phoneNumber);
        if (verification == null) {
            throw new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.AUTH_VERIFICATION_NOT_FOUND,
                "Verification request not found."
            );
        }

        if (Instant.now().isAfter(verification.expiresAt())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.AUTH_OTP_EXPIRED,
                "Verification code has expired."
            );
        }

        if (verification.attempts() >= otpMaxAttempts) {
            throw tooManyVerifyAttempts();
        }

        VerificationCheckResult result = smsVerificationProvider.verifyCode(phoneNumber, request.code());
        if (!result.approved()) {
            int nextAttempts = verification.attempts() + 1;
            verificationRequestsByPhone.put(
                phoneNumber,
                new VerificationRequestRecord(
                    verification.phoneNumber(),
                    verification.requestId(),
                    verification.expiresAt(),
                    nextAttempts >= otpMaxAttempts ? "locked" : result.status(),
                    nextAttempts
                )
            );

            if (nextAttempts >= otpMaxAttempts) {
                throw tooManyVerifyAttempts();
            }

            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.AUTH_OTP_INVALID,
                "Verification code is invalid."
            );
        }

        verificationRequestsByPhone.remove(phoneNumber);
        String registrationToken = "reg_" + UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(registrationTokenTtlSeconds);
        registrationTokens.put(
            registrationToken,
            new RegistrationTokenRecord(registrationToken, phoneNumber, expiresAt, null)
        );

        return new VerifyCodeResponse(registrationToken, registrationTokenTtlSeconds);
    }

    public RegisterResponse register(RegisterRequest request) {
        evictExpiredRecords();

        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        RegistrationTokenRecord token = registrationTokens.get(request.registrationToken());
        if (token == null || token.consumedAt() != null || Instant.now().isAfter(token.expiresAt())) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.AUTH_REGISTRATION_TOKEN_INVALID,
                "Registration token is invalid."
            );
        }

        if (!token.phoneNumber().equals(phoneNumber)) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.AUTH_REGISTRATION_TOKEN_INVALID,
                "Registration token does not match the phone number."
            );
        }

        if (findByPhone(phoneNumber).isPresent()) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.AUTH_PHONE_ALREADY_REGISTERED,
                "A user with this phone number already exists."
            );
        }

        long userId = userSequence.getAndIncrement();
        RegisteredUser user = new RegisteredUser(
            userId,
            phoneNumber,
            request.userName(),
            request.email(),
            passwordEncoder.encode(request.password()),
            "https://images.example.com/covers/default-cover.png",
            "Scaffold account created from the Nx monorepo starter."
        );
        if (usersByPhone.putIfAbsent(phoneNumber, user) != null) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.AUTH_PHONE_ALREADY_REGISTERED,
                "A user with this phone number already exists."
            );
        }
        users.put(userId, user);
        registrationTokens.put(
            token.token(),
            new RegistrationTokenRecord(token.token(), token.phoneNumber(), token.expiresAt(), Instant.now())
        );
        return new RegisterResponse(userId);
    }

    public LoginSession login(LoginRequest request) {
        evictExpiredRecords();

        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        RegisteredUser user = findByPhone(phoneNumber)
            .filter(candidate -> passwordEncoder.matches(request.password(), candidate.passwordHash()))
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        ErrorCode.AUTH_CREDENTIALS_INVALID,
                        "Phone number or password is incorrect."
                    )
            );

        String sessionToken = issueSessionToken(user.id());
        return new LoginSession(
            new LoginResponse(toSummary(user)),
            sessionToken
        );
    }

    public void logout(String rawSessionToken) {
        evictExpiredRecords();

        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            return;
        }
        RefreshTokenRecord token = refreshTokensByHash.get(sha256(rawSessionToken));
        if (token != null) {
            token.revokeIfActive("LOGOUT");
        }
    }

    public MeResponse me(String rawSessionToken) {
        evictExpiredRecords();

        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            throw unauthorized("Session is missing.");
        }

        RefreshTokenRecord session = refreshTokensByHash.get(sha256(rawSessionToken));
        if (session == null || Instant.now().isAfter(session.expiresAt()) || session.revokedAt() != null) {
            throw unauthorized("Session is invalid.");
        }

        long userId = session.userId();
        RegisteredUser user = users.get(userId);
        if (user == null) {
            throw unauthorized("Session is invalid.");
        }

        return new MeResponse(
            user.id(),
            user.userName(),
            user.phoneNumber(),
            user.email(),
            user.coverImageUrl(),
            user.biography()
        );
    }

    public long refreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    private void seedUsers() {
        RegisteredUser seededUser = new RegisteredUser(
            1L,
            normalizePhoneNumber("0912345678"),
            "Brian",
            "brian@example.com",
            passwordEncoder.encode("StrongPassword123"),
            "https://images.example.com/covers/brian-cover.png",
            "Seed user for the scaffold auth flow."
        );
        users.put(seededUser.id(), seededUser);
        usersByPhone.put(seededUser.phoneNumber(), seededUser);
    }

    private Optional<RegisteredUser> findByPhone(String phoneNumber) {
        return Optional.ofNullable(usersByPhone.get(phoneNumber));
    }

    private UserSummary toSummary(RegisteredUser user) {
        return new UserSummary(user.id(), user.userName(), user.phoneNumber());
    }

    private ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_UNAUTHORIZED, message);
    }

    private String issueSessionToken(long userId) {
        String token = "rt_" + UUID.randomUUID() + UUID.randomUUID();
        String tokenHash = sha256(token);
        RefreshTokenRecord record = new RefreshTokenRecord(
            tokenHash,
            userId,
            Instant.now().plusSeconds(refreshTokenTtlSeconds)
        );
        refreshTokensByHash.put(tokenHash, record);
        return token;
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.AUTH_PHONE_INVALID, "Phone number is required.");
        }
        String normalized = phoneNumber.replaceAll("[\\s\\-()]", "");
        if (normalized.startsWith("09") && normalized.length() == 10) {
            normalized = "+886" + normalized.substring(1);
        } else if (normalized.startsWith("886")) {
            normalized = "+" + normalized;
        }

        if (!E164_PATTERN.matcher(normalized).matches()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.AUTH_PHONE_INVALID,
                "Phone number must be in E.164 format."
            );
        }

        return normalized;
    }

    private void evictExpiredRecords() {
        Instant now = Instant.now();
        verificationRequestsByPhone.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
        registrationTokens.entrySet().removeIf(
            entry -> now.isAfter(entry.getValue().expiresAt()) || entry.getValue().consumedAt() != null
        );
        refreshTokensByHash.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    private ApiException tooManyVerifyAttempts() {
        return new ApiException(
            HttpStatus.TOO_MANY_REQUESTS,
            ErrorCode.AUTH_OTP_ATTEMPTS_EXCEEDED,
            "Verification attempts exceeded. Please request a new code."
        );
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record LoginSession(LoginResponse response, String sessionToken) {
    }

    private record VerificationRequestRecord(
        String phoneNumber,
        String requestId,
        Instant expiresAt,
        String status,
        int attempts
    ) {
    }

    private record RegistrationTokenRecord(
        String token,
        String phoneNumber,
        Instant expiresAt,
        Instant consumedAt
    ) {
    }

    private static final class RefreshTokenRecord {

        private final String tokenHash;
        private final long userId;
        private final Instant expiresAt;
        private volatile Instant revokedAt;
        private volatile String revokeReason;

        private RefreshTokenRecord(
            String tokenHash,
            long userId,
            Instant expiresAt
        ) {
            this.tokenHash = tokenHash;
            this.userId = userId;
            this.expiresAt = expiresAt;
        }

        private String tokenHash() {
            return tokenHash;
        }

        private long userId() {
            return userId;
        }

        private Instant expiresAt() {
            return expiresAt;
        }

        private Instant revokedAt() {
            return revokedAt;
        }

        private String revokeReason() {
            return revokeReason;
        }

        private synchronized boolean revokeIfActive(String reason) {
            if (revokedAt != null) {
                return false;
            }
            this.revokedAt = Instant.now();
            this.revokeReason = reason;
            return true;
        }
    }
}
