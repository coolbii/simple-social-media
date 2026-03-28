package com.example.social.auth.service;

public interface SmsVerificationProvider {

    VerificationStartResult sendCode(String phoneNumber);

    VerificationCheckResult verifyCode(String phoneNumber, String code);
}
