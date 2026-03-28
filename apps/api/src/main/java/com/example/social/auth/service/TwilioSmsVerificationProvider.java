package com.example.social.auth.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.example.social.common.exception.ApiException;
import com.example.social.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
        String form = form("To", phoneNumber) +
        "&" +
        form("Channel", "sms") +
        "&" +
        form("Locale", locale);

        try {
            JsonNode response = restClient
                .post()
                .uri("/Services/{serviceSid}/Verifications", serviceSid)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

            return new VerificationStartResult(
                required(response, "sid"),
                required(response, "status"),
                Instant.now().plusSeconds(300)
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
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
            JsonNode response = restClient
                .post()
                .uri("/Services/{serviceSid}/VerificationCheck", serviceSid)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

            String status = required(response, "status");
            return new VerificationCheckResult(
                optional(response, "sid"),
                status,
                "approved".equalsIgnoreCase(status)
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                ErrorCode.AUTH_TWILIO_UNAVAILABLE,
                "Failed to verify Twilio code.",
                exception
            );
        }
    }

    private static String form(String key, String value) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8) +
        "=" +
        URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String required(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                ErrorCode.AUTH_TWILIO_UNAVAILABLE,
                "Twilio response missing field: " + field
            );
        }
        return node.path(field).asText();
    }

    private static String optional(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return "";
        }
        return node.path(field).asText("");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
