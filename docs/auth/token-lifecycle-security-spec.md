# Auth Token 生命週期與安全策略（#002）

## 文件狀態

- 卡片：`#002 定案 Auth 安全策略與 Token 生命週期`
- 狀態：`Accepted`
- 生效日：`2026-03-28`
- 關聯文件：`docs/auth/phone-auth-flow-spec.md`

本文件定義 Access Token 與 Refresh Token 的儲存策略、TTL、rotation 與 revoke 規則，作為前後端共同實作基準。

## Token 儲存策略

1. Access Token
   - 儲存位置：前端記憶體（Pinia/Runtime state）
   - 禁止：`localStorage`、`sessionStorage`
   - 傳遞方式：`Authorization: Bearer <accessToken>`
2. Refresh Token
   - 儲存位置：瀏覽器 `HttpOnly` cookie
   - 前端 JS 不可讀取 refresh token 原文
   - 由瀏覽器自動隨請求帶至 `/api/auth/refresh` 與 `/api/auth/logout`

## TTL 與有效期規則（V1）

1. Access Token TTL：`15 分鐘`（`900 秒`）
2. Refresh Token TTL：`30 天`
3. Registration Token（註冊階段短效憑證）：`10 分鐘`
4. 以上值可由環境變數調整，但預設值視為規格基準，不得任意更改。

## Refresh Token Rotation 規則

每次 `POST /api/auth/refresh` 成功時必須同時完成：

1. 驗證目前 refresh token（存在、未過期、未撤銷）。
2. 將舊 token 標記撤銷（`revoke_reason=ROTATED`）。
3. 簽發新 refresh token（同一 `family_id`，並記錄 `parent_token_id`）。
4. 重新設定 refresh cookie（覆蓋舊值）。
5. 回傳新的 access token。

上述 2~4 必須在同一個交易邏輯中完成，避免半成功狀態。

## Revoke 規則

以下情境需撤銷 refresh token：

1. 使用者登出：撤銷目前 refresh token（`LOGOUT`）。
2. 正常 refresh：舊 token 被 rotation 撤銷（`ROTATED`）。
3. 偵測 token reuse：撤銷整個 token family（`REUSE_DETECTED`）。
4. 保留規劃（後續任務）：密碼變更、帳號停用時撤銷 token family。

## Cookie 屬性策略

Refresh cookie 預設策略：

1. `HttpOnly=true`
2. `Secure=true`（本機開發可由設定暫時關閉）
3. `SameSite=Lax`（僅跨站需求時才改 `None` 並搭配 `Secure=true`）
4. `Path=/api/auth`
5. `Max-Age` 與 refresh token TTL 一致（30 天）
6. `Domain` 預設不設，使用 host-only cookie

## Session Restore 與責任分工

前端責任：

1. App 啟動進入 `checking` 狀態。
2. 呼叫 `/api/auth/refresh` 嘗試換發 access token。
3. refresh 成功後呼叫 `/api/auth/me` 取得目前使用者。
4. 失敗則轉為 `anonymous`，不做無限重試。

後端責任：

1. refresh 成功才回新 access token，並更新 refresh cookie。
2. refresh 失敗回明確錯誤碼，不回敏感細節。
3. 所有撤銷/rotation 行為要落庫可追蹤。

## 錯誤處理策略（Token 相關）

| 情境 | 錯誤碼 | HTTP | 前端行為 |
|---|---|---:|---|
| 缺少 refresh cookie | `AUTH_REFRESH_TOKEN_MISSING` | 401 | 視為未登入，轉 `anonymous` |
| refresh token 無效/過期/已撤銷 | `AUTH_REFRESH_TOKEN_INVALID` | 401 | 清空 auth state，導向登入 |
| access token 過期 | `AUTH_UNAUTHORIZED` | 401 | 觸發一次 refresh 後重試原請求 |
| refresh token reuse 偵測 | `AUTH_REFRESH_TOKEN_REUSE_DETECTED` | 401 | 強制登出並提示重新登入 |
| 權限不足 | `AUTH_FORBIDDEN` | 403 | 顯示無權限頁或訊息 |

## 安全落地要求

1. DB 僅存 refresh token hash，不存原文。
2. refresh token 必須具備唯一識別（例如 `jti`）與 family 關聯。
3. 日誌可記錄 token 事件（login/refresh/logout/revoke），但不得記錄 token 原文。
4. 前端不得將 access token 寫入可持久化儲存。
