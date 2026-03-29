package com.example.social.auth.service;

import java.time.Instant;

import com.example.social.auth.dto.RegisterRequest;
import com.example.social.auth.dto.SendCodeRequest;
import com.example.social.auth.dto.SendCodeResponse;
import com.example.social.auth.dto.VerifyCodeRequest;
import com.example.social.auth.dto.VerifyCodeResponse;
import com.example.social.auth.mapper.AuthVerificationMapper;
import com.example.social.auth.mapper.RefreshTokenMapper;
import com.example.social.auth.mapper.UserMapper;
import com.example.social.auth.model.PhoneVerificationRequestRecord;
import com.example.social.auth.model.RegistrationTokenRecord;
import com.example.social.common.exception.ApiException;
import com.example.social.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private PasswordEncoder passwordEncoder;
    private UserMapper userMapper;
    private AuthVerificationMapper authVerificationMapper;
    private SmsVerificationProvider smsVerificationProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = mock(PasswordEncoder.class);
        userMapper = mock(UserMapper.class);
        RefreshTokenMapper refreshTokenMapper = mock(RefreshTokenMapper.class);
        authVerificationMapper = mock(AuthVerificationMapper.class);
        smsVerificationProvider = mock(SmsVerificationProvider.class);

        authService = new AuthService(
            passwordEncoder,
            userMapper,
            refreshTokenMapper,
            authVerificationMapper,
            smsVerificationProvider
        );
        ReflectionTestUtils.setField(authService, "smsProviderName", "twilio");
        ReflectionTestUtils.setField(authService, "refreshTokenTtlSeconds", 2_592_000L);
        ReflectionTestUtils.setField(authService, "registrationTokenTtlSeconds", 600L);
        ReflectionTestUtils.setField(authService, "otpMaxAttempts", 5);
    }

    @Test
    void sendCodeShouldPersistVerificationRequestInDatabase() {
        Instant expiresAt = Instant.now().plusSeconds(300);
        when(smsVerificationProvider.sendCode("+886905767109"))
            .thenReturn(new VerificationStartResult("request-1", "pending", expiresAt));
        when(authVerificationMapper.createPhoneVerificationRequest(
            anyString(), anyString(), anyString(), anyString(), any()))
            .thenReturn(10L);

        SendCodeResponse response = authService.sendCode(new SendCodeRequest("0905767109"));

        assertThat(response.requestId()).isEqualTo("request-1");
        assertThat(response.expiresInSeconds()).isPositive();
        verify(authVerificationMapper).createPhoneVerificationRequest(
            eq("+886905767109"),
            eq("twilio"),
            eq("request-1"),
            eq("pending"),
            any()
        );
    }

    @Test
    void verifyCodeShouldUpdateAttemptCountWhenCodeInvalid() {
        when(authVerificationMapper.findLatestVerificationByPhone("+886905767109"))
            .thenReturn(
                new PhoneVerificationRequestRecord(
                    1L,
                    "+886905767109",
                    "twilio",
                    "request-1",
                    "pending",
                    0,
                    null,
                    Instant.now().plusSeconds(300),
                    Instant.now(),
                    Instant.now()
                )
            );
        when(smsVerificationProvider.verifyCode("+886905767109", "000000"))
            .thenReturn(new VerificationCheckResult("check-1", "pending", false));

        ApiException exception = assertThrows(
            ApiException.class,
            () -> authService.verifyCode(new VerifyCodeRequest("0905767109", "000000"))
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_OTP_INVALID);
        verify(authVerificationMapper).updateVerificationStatus(1L, "pending", 1);
    }

    @Test
    void verifyCodeShouldCreateRegistrationTokenWhenApproved() {
        when(authVerificationMapper.findLatestVerificationByPhone("+886905767109"))
            .thenReturn(
                new PhoneVerificationRequestRecord(
                    1L,
                    "+886905767109",
                    "twilio",
                    "request-1",
                    "pending",
                    0,
                    null,
                    Instant.now().plusSeconds(300),
                    Instant.now(),
                    Instant.now()
                )
            );
        when(smsVerificationProvider.verifyCode("+886905767109", "123456"))
            .thenReturn(new VerificationCheckResult("check-1", "approved", true));
        when(authVerificationMapper.insertRegistrationToken(anyString(), anyString(), anyLong(), any()))
            .thenReturn(55L);

        VerifyCodeResponse response = authService.verifyCode(new VerifyCodeRequest("0905767109", "123456"));

        assertThat(response.registrationToken()).startsWith("reg_");
        assertThat(response.expiresInSeconds()).isEqualTo(600L);
        verify(authVerificationMapper).markVerificationApproved(1L);
        verify(authVerificationMapper).insertRegistrationToken(eq("+886905767109"), anyString(), eq(1L), any());
    }

    @Test
    void registerShouldRejectWhenTokenAlreadyConsumed() {
        when(authVerificationMapper.findRegistrationTokenByHash(anyString()))
            .thenReturn(
                new RegistrationTokenRecord(
                    8L,
                    "+886905767109",
                    "hashed",
                    1L,
                    Instant.now().plusSeconds(300),
                    Instant.now(),
                    Instant.now().minusSeconds(60)
                )
            );

        ApiException exception = assertThrows(
            ApiException.class,
            () -> authService.register(
                new RegisterRequest("reg-token", "0905767109", "Brian", "password123", "b@example.com"))
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_REGISTRATION_TOKEN_INVALID);
    }

    @Test
    void registerShouldConsumeTokenAfterSuccessfulRegistration() {
        when(authVerificationMapper.findRegistrationTokenByHash(anyString()))
            .thenReturn(
                new RegistrationTokenRecord(
                    8L,
                    "+886905767109",
                    "hashed",
                    1L,
                    Instant.now().plusSeconds(300),
                    null,
                    Instant.now().minusSeconds(60)
                )
            );
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userMapper.registerUser("+886905767109", "Brian", "b@example.com", "encoded"))
            .thenReturn(99L);
        when(authVerificationMapper.consumeRegistrationToken(8L)).thenReturn(true);

        long userId = authService
            .register(new RegisterRequest("reg-token", "0905767109", "Brian", "password123", "b@example.com"))
            .userId();

        assertThat(userId).isEqualTo(99L);
        verify(authVerificationMapper).consumeRegistrationToken(8L);
    }
}
