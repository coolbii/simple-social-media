package com.example.social.auth.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
	name = "app.auth.sms.provider",
	havingValue = "mock",
	matchIfMissing = true
)
public class MockSmsVerificationProvider implements SmsVerificationProvider {

	@Override
	public VerificationStartResult sendCode(String phoneNumber) {
		return new VerificationStartResult(
			"mock-" + UUID.randomUUID(),
			"pending",
			Instant.now().plusSeconds(300)
		);
	}

	@Override
	public VerificationCheckResult verifyCode(String phoneNumber, String code) {
		boolean approved = "123456".equals(code);
		return new VerificationCheckResult(
			"mock-check-" + UUID.randomUUID(),
			approved ? "approved" : "pending",
			approved
		);
	}
}
