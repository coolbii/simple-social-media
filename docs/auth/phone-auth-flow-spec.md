# 手機註冊與登入流程規格（#001）

## 文件狀態

- 卡片：`#001 定案手機註冊與登入流程規格`
- 狀態：`Accepted`
- 生效日：`2026-03-28`
- 範圍：註冊驗證、登入、refresh、logout、me、Twilio Verify

本文件作為 Auth 主流程基準版，後續若要調整主流程，需先更新此文件並在 PR 說明變更理由。

## 核心決策

1. 帳號識別使用手機號碼（統一正規化為 E.164）。
2. 註冊採「先手機驗證，再建立帳號」：
   - `send-code` -> `verify-code` -> `register`
3. 登入採「手機號碼 + 密碼」。
4. Access Token 與 Refresh Token 分流：
   - Access Token：短效，用於 API Authorization header
   - Refresh Token：用於換發，透過 cookie 傳遞

## API 邊界定義

1. `POST /api/auth/phone/send-code`
   - 用途：發送手機驗證碼（註冊前流程）
   - 輸入：`phoneNumber`
   - 輸出：送碼成功資訊（不回傳 OTP）
2. `POST /api/auth/phone/verify-code`
   - 用途：驗證 OTP，成功後發短效 `registrationToken`
   - 輸入：`phoneNumber`, `otpCode`
   - 輸出：`registrationToken`, 到期資訊
3. `POST /api/auth/register`
   - 用途：消耗 `registrationToken` 建立新帳號
   - 輸入：`registrationToken`, `userName`, `password`, `email?`
   - 輸出：註冊成功資訊
4. `POST /api/auth/login`
   - 用途：手機號碼 + 密碼登入
   - 輸入：`phoneNumber`, `password`
   - 輸出：`accessToken` + `user` 摘要（並設定 refresh cookie）
5. `POST /api/auth/refresh`
   - 用途：用 refresh cookie 換發 access token（並旋轉 refresh token）
   - 輸入：cookie
   - 輸出：新 `accessToken`
6. `POST /api/auth/logout`
   - 用途：撤銷目前 refresh token 並清除 cookie
   - 輸出：登出成功資訊
7. `GET /api/auth/me`
   - 用途：取得目前登入使用者
   - 輸入：Authorization: Bearer access token
   - 輸出：`user` 摘要

## Twilio Verify 流程定義

1. `send-code` 呼叫 Twilio Verify Start API 發送 OTP 簡訊。
2. `verify-code` 呼叫 Twilio Verify Check API 檢查 OTP 狀態。
3. 後端只信任 Twilio 回傳狀態，不在後端保存 OTP 明碼。
4. 需保存 provider 請求識別資訊（例如 request SID）供追蹤與稽核。

## Auth 錯誤碼（V1）

| 錯誤碼 | HTTP | 說明 |
|---|---:|---|
| `AUTH_PHONE_INVALID` | 400 | 手機格式不合法 |
| `AUTH_PHONE_ALREADY_REGISTERED` | 409 | 手機已註冊 |
| `AUTH_VERIFICATION_NOT_FOUND` | 404 | 找不到驗證請求 |
| `AUTH_OTP_INVALID` | 400 | 驗證碼錯誤 |
| `AUTH_OTP_EXPIRED` | 400 | 驗證碼過期 |
| `AUTH_REGISTRATION_TOKEN_INVALID` | 401 | 註冊憑證無效 |
| `AUTH_CREDENTIALS_INVALID` | 401 | 登入帳密錯誤 |
| `AUTH_REFRESH_TOKEN_MISSING` | 401 | 缺少 refresh cookie |
| `AUTH_REFRESH_TOKEN_INVALID` | 401 | refresh token 無效/已撤銷 |
| `AUTH_UNAUTHORIZED` | 401 | 未登入或 access token 無效 |
| `AUTH_FORBIDDEN` | 403 | 已登入但無權限 |
| `AUTH_TWILIO_UNAVAILABLE` | 503 | Twilio 服務不可用 |

## 非本階段範圍

1. 忘記密碼/重設密碼
2. 第三方社群登入（Google/Apple/Facebook）
3. 多因子驗證（MFA）
4. 多租戶或 RBAC 權限模型
