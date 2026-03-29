package com.example.social.auth.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.social.common.exception.ApiException;
import com.example.social.common.exception.ErrorCode;

@Service
@ConditionalOnProperty(name = "app.auth.sms.provider", havingValue = "twilio")
public class TwilioSmsVerificationProvider implements SmsVerificationProvider {

    private final RestClient restClient;
    private final String serviceSid;
    private final String locale;

    public TwilioSmsVerificationProvider(
            @Value("${app.auth.sms.twilio.account-sid:}") String accountSid,
            @Value("${app.auth.sms.twilio.auth-token:}") String authToken,
            @Value("${app.auth.sms.twilio.verify-service-sid:}") String serviceSid,
            @Value("${app.auth.sms.twilio.locale:zh-HK}") String locale
    ) {
        if (isBlank(accountSid) || isBlank(authToken) || isBlank(serviceSid)) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.AUTH_TWILIO_UNAVAILABLE,
                    "Twilio credentials are not configured."
            );
        }

        this.restClient = RestClient.builder()
                .baseUrl("https://verify.twilio.com/v2")
                .defaultHeaders(headers -> headers.setBasicAuth(accountSid, authToken))
                .build();
        this.serviceSid = serviceSid;
        this.locale = locale;
    }

    @Override
    public VerificationStartResult sendCode(String phoneNumber) {
        String form = form("To", phoneNumber)
                + "&"
                + form("Channel", "sms")
                + "&"
                + form("Locale", locale);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) restClient
                    .post()
                    .uri("/Services/{serviceSid}/Verifications", serviceSid)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            return new VerificationStartResult(
                    required(response, "sid"),
                    required(response, "status"),
                    Instant.now().plusSeconds(300)
            );
        } catch (RestClientException exception) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.AUTH_TWILIO_UNAVAILABLE,
                    "Failed to start Twilio verification.",
                    exception
            );
        }
    }

    @Override
    public VerificationCheckResult verifyCode(String phoneNumber, String code) {
        String form = form("To", phoneNumber) + "&" + form("Code", code);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) restClient
                    .post()
                    .uri("/Services/{serviceSid}/VerificationCheck", serviceSid)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            String status = required(response, "status");
            return new VerificationCheckResult(
                    optional(response, "sid"),
                    status,
                    "approved".equalsIgnoreCase(status)
            );
        } catch (RestClientException exception) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.AUTH_TWILIO_UNAVAILABLE,
                    "Failed to verify Twilio code.",
                    exception
            );
        }
    }

    private static String form(String key, String value) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8)
                + "="
                + URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String required(Map<String, Object> node, String field) {
        if (node == null || !node.containsKey(field) || node.get(field) == null) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.AUTH_TWILIO_UNAVAILABLE,
                    "Twilio response missing field: " + field
            );
        }
        return String.valueOf(node.get(field));
    }

    private static String optional(Map<String, Object> node, String field) {
        if (node == null || !node.containsKey(field) || node.get(field) == null) {
            return "";
        }
        return String.valueOf(node.get(field));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
