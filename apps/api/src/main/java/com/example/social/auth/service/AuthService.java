package com.example.social.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
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
import com.example.social.auth.mapper.AuthVerificationMapper;
import com.example.social.auth.mapper.RefreshTokenMapper;
import com.example.social.auth.mapper.UserMapper;
import com.example.social.auth.model.PhoneVerificationRequestRecord;
import com.example.social.auth.model.RefreshTokenSession;
import com.example.social.auth.model.RegisteredUser;
import com.example.social.auth.model.RegistrationTokenRecord;
import com.example.social.common.exception.ApiException;
import com.example.social.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final AuthVerificationMapper authVerificationMapper;
    private final SmsVerificationProvider smsVerificationProvider;
    @Value("${app.auth.sms.provider:mock}")
    private String smsProviderName;
    @Value("${app.auth.refresh-token-ttl-seconds:2592000}")
    private long refreshTokenTtlSeconds;
    @Value("${app.auth.registration-token-ttl-seconds:600}")
    private long registrationTokenTtlSeconds;
    @Value("${app.auth.otp.max-attempts:5}")
    private int otpMaxAttempts;

    public AuthService(
        PasswordEncoder passwordEncoder,
        UserMapper userMapper,
        RefreshTokenMapper refreshTokenMapper,
        AuthVerificationMapper authVerificationMapper,
        SmsVerificationProvider smsVerificationProvider
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.authVerificationMapper = authVerificationMapper;
        this.smsVerificationProvider = smsVerificationProvider;
    }

    public SendCodeResponse sendCode(SendCodeRequest request) {
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

        Long verificationRequestId = authVerificationMapper.createPhoneVerificationRequest(
            phoneNumber,
            smsProviderName,
            verification.requestId(),
            verification.status(),
            expiresAt
        );
        if (verificationRequestId == null) {
            throw new IllegalStateException("Unable to persist phone verification request.");
        }

        long expiresIn = Math.max(1, Instant.now().until(expiresAt, ChronoUnit.SECONDS));
        return new SendCodeResponse(verification.requestId(), expiresIn);
    }

    public VerifyCodeResponse verifyCode(VerifyCodeRequest request) {
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        PhoneVerificationRequestRecord verification = authVerificationMapper.findLatestVerificationByPhone(phoneNumber);
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

        if (verification.attemptCount() >= otpMaxAttempts) {
            throw tooManyVerifyAttempts();
        }

        VerificationCheckResult result = smsVerificationProvider.verifyCode(phoneNumber, request.code());
        if (!result.approved()) {
            int nextAttempts = verification.attemptCount() + 1;
            String nextStatus = nextAttempts >= otpMaxAttempts ? "locked" : result.status();
            authVerificationMapper.updateVerificationStatus(verification.id(), nextStatus, nextAttempts);

            if (nextAttempts >= otpMaxAttempts) {
                throw tooManyVerifyAttempts();
            }

            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.AUTH_OTP_INVALID,
                "Verification code is invalid."
            );
        }

        authVerificationMapper.markVerificationApproved(verification.id());

        String registrationToken = "reg_" + UUID.randomUUID();
        Instant registrationTokenExpiresAt = Instant.now().plusSeconds(registrationTokenTtlSeconds);
        Long registrationTokenId = authVerificationMapper.insertRegistrationToken(
            phoneNumber,
            sha256(registrationToken),
            verification.id(),
            registrationTokenExpiresAt
        );
        if (registrationTokenId == null) {
            throw new IllegalStateException("Unable to persist registration token.");
        }

        return new VerifyCodeResponse(registrationToken, registrationTokenTtlSeconds);
    }

    public RegisterResponse register(RegisterRequest request) {
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        RegistrationTokenRecord token = authVerificationMapper.findRegistrationTokenByHash(
            sha256(request.registrationToken())
        );
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

        long userId;
        try {
            Long registeredUserId = userMapper.registerUser(
                phoneNumber,
                request.userName(),
                request.email(),
                passwordEncoder.encode(request.password())
            );
            if (registeredUserId == null) {
                throw new IllegalStateException("Unable to register user.");
            }
            userId = registeredUserId;
        } catch (DuplicateKeyException exception) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.AUTH_PHONE_ALREADY_REGISTERED,
                "A user with this phone number already exists."
            );
        }

        Boolean consumed = authVerificationMapper.consumeRegistrationToken(token.id());
        if (consumed == null || !consumed) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.AUTH_REGISTRATION_TOKEN_INVALID,
                "Registration token is invalid."
            );
        }

        return new RegisterResponse(userId);
    }

    public LoginSession login(LoginRequest request) {
        RegisteredUser user = findByLoginIdentifier(request.phoneNumber())
            .filter(candidate -> passwordEncoder.matches(request.password(), candidate.passwordHash()))
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        ErrorCode.AUTH_CREDENTIALS_INVALID,
                        "Phone number/email or password is incorrect."
                    )
            );

        String sessionToken = issueSessionToken(user.id());
        return new LoginSession(
            new LoginResponse(toSummary(user)),
            sessionToken
        );
    }

    public void logout(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            return;
        }
        RefreshTokenSession token = refreshTokenMapper.findByHash(sha256(rawSessionToken));
        if (token != null && token.revokedAt() == null) {
            refreshTokenMapper.revokeRefreshToken(token.id(), "LOGOUT");
        }
    }

    public MeResponse me(String rawSessionToken) {
        AuthenticatedUser currentUser = requireAuthenticatedUser(rawSessionToken);

        return new MeResponse(
            currentUser.id(),
            currentUser.userName(),
            currentUser.phoneNumber(),
            currentUser.email(),
            currentUser.coverImageUrl(),
            currentUser.biography()
        );
    }

    public AuthenticatedUser requireAuthenticatedUser(String rawSessionToken) {
        RefreshTokenSession session = requireValidSession(rawSessionToken);
        RegisteredUser user = requireExistingUser(session.userId());
        return new AuthenticatedUser(
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

    private Optional<RegisteredUser> findByPhone(String phoneNumber) {
        return Optional.ofNullable(userMapper.findByPhone(phoneNumber));
    }

    private Optional<RegisteredUser> findByLoginIdentifier(String loginIdentifier) {
        if (looksLikeEmail(loginIdentifier)) {
            return Optional.ofNullable(userMapper.findByEmail(loginIdentifier.trim()));
        }
        return findByPhone(normalizePhoneNumber(loginIdentifier));
    }

    private UserSummary toSummary(RegisteredUser user) {
        return new UserSummary(user.id(), user.userName(), user.phoneNumber());
    }

    private RefreshTokenSession requireValidSession(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            throw unauthorized("Session is missing.");
        }

        RefreshTokenSession session = refreshTokenMapper.findByHash(sha256(rawSessionToken));
        if (session == null || Instant.now().isAfter(session.expiresAt()) || session.revokedAt() != null) {
            throw unauthorized("Session is invalid.");
        }
        return session;
    }

    private RegisteredUser requireExistingUser(long userId) {
        RegisteredUser user = userMapper.findById(userId);
        if (user == null) {
            throw unauthorized("Session is invalid.");
        }
        return user;
    }

    private ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_UNAUTHORIZED, message);
    }

    private String issueSessionToken(long userId) {
        String token = "rt_" + UUID.randomUUID() + UUID.randomUUID();
        String tokenHash = sha256(token);
        Long insertedId = refreshTokenMapper.insertRefreshToken(
            userId,
            tokenHash,
            "fam_" + UUID.randomUUID(),
            null,
            Instant.now().plusSeconds(refreshTokenTtlSeconds),
            null,
            null
        );
        if (insertedId == null) {
            throw new IllegalStateException("Unable to create session.");
        }
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

    private static boolean looksLikeEmail(String value) {
        return value != null && value.contains("@");
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

    public record AuthenticatedUser(
        long id,
        String userName,
        String phoneNumber,
        String email,
        String coverImageUrl,
        String biography
    ) {
    }
}
