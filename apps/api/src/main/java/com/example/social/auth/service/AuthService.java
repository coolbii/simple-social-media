package com.example.social.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.example.social.auth.dto.LoginRequest;
import com.example.social.auth.dto.LoginResponse;
import com.example.social.auth.dto.MeResponse;
import com.example.social.auth.dto.RefreshResponse;
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
	private static final String HMAC_SHA256 = "HmacSHA256";

	private final PasswordEncoder passwordEncoder;
	private final SmsVerificationProvider smsVerificationProvider;
	private final long accessTokenTtlSeconds;
	private final long refreshTokenTtlSeconds;
	private final long registrationTokenTtlSeconds;
	private final int otpMaxAttempts;
	private final String accessTokenSecret;
	private final AtomicLong userSequence = new AtomicLong(2);
	private final Map<Long, RegisteredUser> users = new ConcurrentHashMap<>();
	private final Map<String, RegisteredUser> usersByPhone = new ConcurrentHashMap<>();
	private final Map<String, VerificationRequestRecord> verificationRequestsByPhone =
		new ConcurrentHashMap<>();
	private final Map<String, RegistrationTokenRecord> registrationTokens = new ConcurrentHashMap<>();
	private final Map<String, RefreshTokenRecord> refreshTokensByHash = new ConcurrentHashMap<>();
	private final Map<String, Object> refreshTokenLocks = new ConcurrentHashMap<>();

	public AuthService(
		PasswordEncoder passwordEncoder,
		SmsVerificationProvider smsVerificationProvider,
		@Value("${app.auth.access-token-ttl-seconds:900}") long accessTokenTtlSeconds,
		@Value("${app.auth.refresh-token-ttl-seconds:2592000}") long refreshTokenTtlSeconds,
		@Value("${app.auth.registration-token-ttl-seconds:600}") long registrationTokenTtlSeconds,
		@Value("${app.auth.otp.max-attempts:5}") int otpMaxAttempts,
		@Value("${app.auth.access-token-secret:change-me-in-env}") String accessTokenSecret
	) {
		this.passwordEncoder = passwordEncoder;
		this.smsVerificationProvider = smsVerificationProvider;
		this.accessTokenTtlSeconds = accessTokenTtlSeconds;
		this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
		this.registrationTokenTtlSeconds = registrationTokenTtlSeconds;
		this.otpMaxAttempts = otpMaxAttempts;
		this.accessTokenSecret = accessTokenSecret;
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

		String refreshToken = issueRefreshToken(user.id(), null, null);
		return new LoginSession(
			new LoginResponse(newAccessToken(user.id()), accessTokenTtlSeconds, toSummary(user)),
			refreshToken
		);
	}

	public RefreshSession refresh(String rawRefreshToken) {
		evictExpiredRecords();

		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new ApiException(
				HttpStatus.UNAUTHORIZED,
				ErrorCode.AUTH_REFRESH_TOKEN_MISSING,
				"Refresh token is missing."
			);
		}

		String tokenHash = sha256(rawRefreshToken);
		Object lock = refreshTokenLocks.computeIfAbsent(tokenHash, ignored -> new Object());
		try {
			synchronized (lock) {
				RefreshTokenRecord current = refreshTokensByHash.get(tokenHash);
				if (current == null) {
					throw new ApiException(
						HttpStatus.UNAUTHORIZED,
						ErrorCode.AUTH_REFRESH_TOKEN_INVALID,
						"Refresh token is invalid."
					);
				}

				if (Instant.now().isAfter(current.expiresAt())) {
					refreshTokensByHash.remove(tokenHash);
					throw new ApiException(
						HttpStatus.UNAUTHORIZED,
						ErrorCode.AUTH_REFRESH_TOKEN_INVALID,
						"Refresh token has expired."
					);
				}

				if (!current.revokeIfActive("ROTATED")) {
					revokeFamily(current.familyId(), "REUSE_DETECTED");
					throw new ApiException(
						HttpStatus.UNAUTHORIZED,
						ErrorCode.AUTH_REFRESH_TOKEN_REUSE_DETECTED,
						"Refresh token reuse detected."
					);
				}

				String nextRefreshToken = issueRefreshToken(
					current.userId(),
					current.familyId(),
					current.tokenHash()
				);
				return new RefreshSession(
					new RefreshResponse(newAccessToken(current.userId()), accessTokenTtlSeconds),
					nextRefreshToken
				);
			}
		} finally {
			refreshTokenLocks.remove(tokenHash, lock);
		}
	}

	public void logout(String rawRefreshToken) {
		evictExpiredRecords();

		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			return;
		}
		RefreshTokenRecord token = refreshTokensByHash.get(sha256(rawRefreshToken));
		if (token != null) {
			token.revokeIfActive("LOGOUT");
		}
	}

	public MeResponse me(String accessToken) {
		evictExpiredRecords();

		long userId = parseAndValidateAccessToken(accessToken);
		RegisteredUser user = users.get(userId);
		if (user == null) {
			throw new ApiException(
				HttpStatus.UNAUTHORIZED,
				ErrorCode.AUTH_UNAUTHORIZED,
				"Access token is invalid."
			);
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

	private String newAccessToken(long userId) {
		long expiresAtEpochSeconds = Instant.now().plusSeconds(accessTokenTtlSeconds).getEpochSecond();
		String payload = userId + ":" + expiresAtEpochSeconds;
		String encodedPayload = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
		String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(payload));
		return encodedPayload + "." + signature;
	}

	private long parseAndValidateAccessToken(String token) {
		if (token == null || token.isBlank()) {
			throw unauthorized("Access token is missing.");
		}

		String[] parts = token.split("\\.");
		if (parts.length != 2) {
			throw unauthorized("Access token format is invalid.");
		}

		String payload;
		try {
			payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException exception) {
			throw unauthorized("Access token payload is invalid.");
		}

		byte[] providedSignature;
		try {
			providedSignature = Base64.getUrlDecoder().decode(parts[1]);
		} catch (IllegalArgumentException exception) {
			throw unauthorized("Access token signature is invalid.");
		}

		byte[] expectedSignature = hmac(payload);
		if (!MessageDigest.isEqual(expectedSignature, providedSignature)) {
			throw unauthorized("Access token signature is invalid.");
		}

		String[] segments = payload.split(":");
		if (segments.length != 2) {
			throw unauthorized("Access token payload is malformed.");
		}

		try {
			long userId = Long.parseLong(segments[0]);
			long expiresAt = Long.parseLong(segments[1]);
			if (Instant.now().isAfter(Instant.ofEpochSecond(expiresAt))) {
				throw unauthorized("Access token has expired.");
			}
			return userId;
		} catch (NumberFormatException exception) {
			throw unauthorized("Access token payload is malformed.");
		}
	}

	private ApiException unauthorized(String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_UNAUTHORIZED, message);
	}

	private String issueRefreshToken(long userId, String familyId, String parentTokenHash) {
		String token = "rt_" + UUID.randomUUID() + UUID.randomUUID();
		String tokenHash = sha256(token);
		String resolvedFamilyId = familyId == null || familyId.isBlank() ? UUID.randomUUID().toString() : familyId;
		RefreshTokenRecord record = new RefreshTokenRecord(
			tokenHash,
			userId,
			resolvedFamilyId,
			parentTokenHash,
			Instant.now().plusSeconds(refreshTokenTtlSeconds)
		);
		refreshTokensByHash.put(tokenHash, record);
		return token;
	}

	private void revokeFamily(String familyId, String reason) {
		for (RefreshTokenRecord token : refreshTokensByHash.values()) {
			if (token.familyId().equals(familyId)) {
				token.revokeIfActive(reason);
			}
		}
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

	private byte[] hmac(String payload) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA256);
			mac.init(new SecretKeySpec(accessTokenSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
			return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("HmacSHA256 is not available", exception);
		}
	}

	private void evictExpiredRecords() {
		Instant now = Instant.now();
		verificationRequestsByPhone.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
		registrationTokens.entrySet().removeIf(
			entry -> now.isAfter(entry.getValue().expiresAt()) || entry.getValue().consumedAt() != null
		);
		refreshTokensByHash.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
		refreshTokenLocks.keySet().removeIf(hash -> !refreshTokensByHash.containsKey(hash));
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

	public record LoginSession(LoginResponse response, String refreshToken) {
	}

	public record RefreshSession(RefreshResponse response, String refreshToken) {
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
		private final String familyId;
		private final String parentTokenHash;
		private final Instant expiresAt;
		private volatile Instant revokedAt;
		private volatile String revokeReason;

		private RefreshTokenRecord(
			String tokenHash,
			long userId,
			String familyId,
			String parentTokenHash,
			Instant expiresAt
		) {
			this.tokenHash = tokenHash;
			this.userId = userId;
			this.familyId = familyId;
			this.parentTokenHash = parentTokenHash;
			this.expiresAt = expiresAt;
		}

		private String tokenHash() {
			return tokenHash;
		}

		private long userId() {
			return userId;
		}

		private String familyId() {
			return familyId;
		}

		private String parentTokenHash() {
			return parentTokenHash;
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
