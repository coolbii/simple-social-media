/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiResponseLoginResponse } from '../models/ApiResponseLoginResponse';
import type { ApiResponseLogoutResponse } from '../models/ApiResponseLogoutResponse';
import type { ApiResponseMeResponse } from '../models/ApiResponseMeResponse';
import type { ApiResponseRefreshResponse } from '../models/ApiResponseRefreshResponse';
import type { ApiResponseRegisterResponse } from '../models/ApiResponseRegisterResponse';
import type { ApiResponseSendCodeResponse } from '../models/ApiResponseSendCodeResponse';
import type { ApiResponseVerifyCodeResponse } from '../models/ApiResponseVerifyCodeResponse';
import type { LoginRequest } from '../models/LoginRequest';
import type { RegisterRequest } from '../models/RegisterRequest';
import type { SendCodeRequest } from '../models/SendCodeRequest';
import type { VerifyCodeRequest } from '../models/VerifyCodeRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class AuthService {
    /**
     * Register a new user by phone number.
     * @returns ApiResponseRegisterResponse OK
     * @throws ApiError
     */
    public static authRegister({
        requestBody,
    }: {
        requestBody: RegisterRequest,
    }): CancelablePromise<ApiResponseRegisterResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/auth/register',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * Rotate the refresh token and return a new access token.
     * @returns ApiResponseRefreshResponse OK
     * @throws ApiError
     */
    public static authRefresh(): CancelablePromise<ApiResponseRefreshResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/auth/refresh',
        });
    }
    /**
     * Verify OTP and issue registration token.
     * @returns ApiResponseVerifyCodeResponse OK
     * @throws ApiError
     */
    public static authVerifyCode({
        requestBody,
    }: {
        requestBody: VerifyCodeRequest,
    }): CancelablePromise<ApiResponseVerifyCodeResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/auth/phone/verify-code',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * Send phone verification code.
     * @returns ApiResponseSendCodeResponse OK
     * @throws ApiError
     */
    public static authSendCode({
        requestBody,
    }: {
        requestBody: SendCodeRequest,
    }): CancelablePromise<ApiResponseSendCodeResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/auth/phone/send-code',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * Clear the refresh cookie for the current session.
     * @returns ApiResponseLogoutResponse OK
     * @throws ApiError
     */
    public static authLogout(): CancelablePromise<ApiResponseLogoutResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/auth/logout',
        });
    }
    /**
     * Log in with phone number and password.
     * @returns ApiResponseLoginResponse OK
     * @throws ApiError
     */
    public static authLogin({
        requestBody,
    }: {
        requestBody: LoginRequest,
    }): CancelablePromise<ApiResponseLoginResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/auth/login',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * Return the current scaffold user profile.
     * @returns ApiResponseMeResponse OK
     * @throws ApiError
     */
    public static authMe({
        authorization,
    }: {
        authorization?: string,
    }): CancelablePromise<ApiResponseMeResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/auth/me',
            headers: {
                'Authorization': authorization,
            },
        });
    }
}
