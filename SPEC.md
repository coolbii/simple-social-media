Below is a consolidated English specification document that reflects all decisions made so far.

---

# Social Media System Specification

**Tech Stack:** Vue 3 + Spring Boot + MySQL 8 + MyBatis + Nx Monorepo
**Deployment Target:** EC2 + S3
**Realtime Comments:** Server-Sent Events (SSE)

## 1. Document Purpose

This document defines the implementation specification for a simplified social media platform built for the ESUN Bank Java backend coding assignment. It combines the original assignment requirements with the final technical decisions made during architecture planning.

The assignment requires a simple social media platform with registration, authentication, post CRUD, commenting, a three-tier architecture, Vue.js on the frontend, Spring Boot on the backend, RESTful APIs, stored procedures, transaction handling for multi-table updates, DDL/DML delivery under a `DB` folder, and protection against SQL injection and XSS. It also provides base data tables for `User`, `Post`, and `Comment`, while explicitly allowing additional tables beyond those examples.  

---

## 2. Final Scope Decision

### 2.1 In Scope

* User registration by phone number
* User login/logout
* Access token + refresh token authentication flow
* Authenticated users can create posts
* Public users can read posts
* Post owner can edit and delete their own posts
* Authenticated users can create comments on posts
* Public users can read comments
* Realtime comment updates via SSE
* Optional image upload to S3 for:

  * user cover image
  * post image
* MySQL relational database
* Database access through stored procedures
* Transaction handling for multi-table operations
* Nx monorepo for project management
* GitHub as source control

### 2.2 Out of Scope

* Full RBAC system
* Admin console
* Moderation workflow
* Nested/threaded comments
* Reactions/likes
* Follow system
* Notifications
* Direct messaging
* WebSocket
* Rich text editor
* Search engine
* Full audit system
* Multi-image posts
* Comment edit/delete

---

## 3. Product Summary

The system is a lightweight social media platform with the following product behavior:

* A user can register using a phone number and password.
* A user can log in and obtain authenticated access.
* A logged-in user can create a post.
* All users can view posts and comments.
* The owner of a post can edit and delete that post.
* A logged-in user can add a comment to a post.
* Other users viewing the same post should see new comments appear in near real time via SSE.

This aligns with the assignment’s mandatory functional requirements for registration, login validation, post creation/listing/editing/deletion, and commenting.

---

## 4. Architecture Overview

## 4.1 Architecture Style

The system shall use a three-tier architecture:

* **Presentation Layer**

  * Vue 3 frontend
  * Spring Boot REST controllers

* **Business Layer**

  * Spring Boot service layer
  * authentication logic
  * post/comment business rules
  * token lifecycle
  * ownership checks
  * SSE event broadcasting

* **Data Layer**

  * MyBatis mappers
  * MySQL stored procedures
  * S3 integration for file storage

This matches the assignment requirement for a Web Server + Application Server + relational database three-tier architecture, with explicit backend layering into presentation, business, data, and shared/common layers.

## 4.2 Deployment Topology

Planned deployment:

* **Frontend**

  * Vue 3 application
  * static assets served from EC2 or a static hosting/CDN setup if later desired

* **Backend**

  * Spring Boot application running on EC2

* **Database**

  * MySQL 8

* **Object Storage**

  * Amazon S3 for uploaded images

---

## 5. Monorepo Structure

The project will be managed using **Nx**.

### 5.1 Repository Layout

```text
/
├─ apps/
│  ├─ web/                  # Vue 3 frontend
│  └─ api/                  # Spring Boot backend
├─ libs/
│  ├─ shared/
│  │  ├─ api-contract/      # shared DTO/docs/schemas if needed
│  │  └─ utils/
│  ├─ web/
│  │  ├─ ui/
│  │  ├─ feature-auth/
│  │  ├─ feature-post/
│  │  └─ feature-comment/
│  └─ api/
│     ├─ common/
│     ├─ auth/
│     ├─ post/
│     ├─ comment/
│     ├─ sse/
│     └─ storage/
├─ DB/
│  ├─ ddl/
│  ├─ dml/
│  └─ sp/
├─ docs/
│  └─ architecture/
└─ README.md
```

### 5.2 Build Tool

* Frontend: Nx-managed Vue app
* Backend: Spring Boot with **Gradle**
* Nx is used for orchestration, task execution, and monorepo management

The assignment allows Maven or Gradle. This specification chooses Gradle.

---

## 6. Technology Decisions

## 6.1 Frontend

* Vue 3
* TypeScript
* Pinia
* Vue Router
* Axios
* EventSource API for SSE

The assignment explicitly requires Vue.js as the frontend technology.

## 6.2 Backend

* Spring Boot
* Spring Security
* MyBatis
* JWT for access token
* Cookie-based refresh token
* SseEmitter for SSE

The assignment explicitly requires Spring Boot and RESTful APIs.

## 6.3 Database

* MySQL 8
* InnoDB
* utf8mb4

## 6.4 Object Storage

* Amazon S3

---

## 7. Functional Requirements

## 7.1 Registration

The system shall provide user registration.

### Rules

* Registration identifier: **phone number**
* Password is required
* Username is required
* Email is optional
* Cover image is optional
* Biography is optional

This follows the assignment requirement that users register and log in with a phone number.

### Validation

* `phoneNumber`

  * required
  * normalized format
  * unique
* `password`

  * required
  * minimum length configurable
* `userName`

  * required
  * max length configurable

### Output

* created user ID
* optionally authenticated session bootstrap response if desired, though initial implementation may return only success metadata

---

## 7.2 Login

The system shall provide login by phone number and password.

### Rules

* User logs in using:

  * `phoneNumber`
  * `password`
* On successful login:

  * issue short-lived access token
  * issue long-lived refresh token
  * return current user summary

### Failure Conditions

* invalid phone number
* invalid password
* disabled/deleted account if such logic is added later

---

## 7.3 Logout

The system shall support logout.

### Rules

* Logout revokes the current refresh token
* Logout clears the refresh cookie
* Frontend clears in-memory access token and user state

---

## 7.4 Post Management

The system shall support:

* create post
* list posts
* get post detail
* update post
* delete post

This follows the assignment requirement for adding, listing, editing, and deleting posts.

### Business Rules

* Only authenticated users may create posts
* Only the owner of a post may edit that post
* Only the owner of a post may delete that post
* Public users may read post lists and post details

### Post Fields

* content
* optional image

---

## 7.5 Comment Management

The system shall support:

* create comment for a post
* list comments for a post

This follows the assignment requirement that users can add comments to posts.

### Business Rules

* Only authenticated users may create comments
* Public users may read comments
* Nested comments are out of scope
* Comment edit/delete is out of scope

---

## 7.6 Realtime Comment Updates

The system shall provide near-real-time comment updates using **Server-Sent Events (SSE)**.

### Rules

* When a new comment is successfully committed to the database:

  * an SSE event is emitted to subscribers of that post
* The frontend shall:

  * fetch the initial comment list via REST
  * subscribe to SSE for incremental updates
* Public read access is allowed for comment streams

### Reasoning

This project only needs one-way server-to-client updates for new comments. SSE is therefore sufficient and preferred over WebSocket for this scope.

---

## 7.7 Image Upload

The system shall support optional image uploads to S3 for:

* user cover image
* post image

### Rules

* Binary file content is stored in S3
* Database stores image metadata only
* Uploaded file validation shall include:

  * allowed MIME types
  * file size limit
  * filename normalization or replacement
* Image upload may be implemented either:

  * through backend proxy upload
  * or via pre-signed URL flow
    Initial implementation may use backend-managed upload for simplicity

---

## 8. Authorization Model

## 8.1 RBAC Decision

A full RBAC system is **not required** for this project.

The assignment only requires authentication so that only logged-in users can post or comment. It does not require multiple roles, role hierarchies, admin users, or permission matrices.

## 8.2 Access Model

The project uses:

* **authentication**
* **ownership-based authorization**

### Access Rules

* Public:

  * can view posts
  * can view comments
  * can subscribe to comment stream
* Authenticated user:

  * can create posts
  * can create comments
* Post owner:

  * can update own post
  * can delete own post

---

## 9. Authentication and Session Model

## 9.1 Token Strategy

The system shall use:

* **Access Token**

  * JWT
  * short-lived
  * stored in frontend memory only
* **Refresh Token**

  * opaque or signed token
  * longer-lived
  * stored in `HttpOnly` cookie
  * hashed in database

## 9.2 Token Storage

### Access Token

* stored only in memory
* not stored in localStorage
* not stored in sessionStorage unless future design changes require it

### Refresh Token

* stored in cookie with:

  * `HttpOnly`
  * `Secure`
  * `SameSite=Lax` by default
* `SameSite=None` may be used only if deployment requires cross-site cookie flow

## 9.3 Refresh Token Rotation

Every successful refresh operation shall:

* invalidate the currently used refresh token
* issue a new refresh token
* update the refresh token family chain

## 9.4 Refresh Token Revocation

Refresh tokens shall be revoked when:

* user logs out
* refresh token is rotated
* suspicious reuse is detected
* password is changed, if implemented later
* account is disabled, if implemented later

## 9.5 Frontend Session Restoration

On app bootstrap:

1. frontend starts in `checking` state
2. frontend calls refresh endpoint
3. if refresh succeeds:

   * store new access token in memory
   * call `/auth/me`
   * transition to `authenticated`
4. if refresh fails:

   * transition to `anonymous`

## 9.6 Route Guard Strategy

Protected actions and screens should rely on auth state:

* `checking`
* `authenticated`
* `anonymous`

---

## 10. Data Model

The assignment provides base `User`, `Post`, and `Comment` tables and allows additional tables. This specification adds `refresh_tokens` because the selected authentication model requires server-side refresh token lifecycle management.

## 10.1 `users`

### Purpose

Stores user identity and profile information.

### Fields

* `id` BIGINT PK
* `phone_number` VARCHAR(20) NOT NULL UNIQUE
* `user_name` VARCHAR(100) NOT NULL
* `email` VARCHAR(255) NULL
* `password_hash` VARCHAR(255) NOT NULL
* `cover_image_key` VARCHAR(512) NULL
* `cover_image_url` VARCHAR(1024) NULL
* `biography` TEXT NULL
* `created_at` DATETIME NOT NULL
* `updated_at` DATETIME NOT NULL
* `deleted_at` DATETIME NULL

### Notes

* `password_salt` is **not** stored as a separate column
* password salt is handled inside the password hashing format
* this still satisfies the assignment’s requirement that passwords be salted and hashed before storage.

---

## 10.2 `posts`

### Purpose

Stores social posts.

### Fields

* `id` BIGINT PK
* `user_id` BIGINT NOT NULL FK -> users.id
* `content` TEXT NOT NULL
* `image_key` VARCHAR(512) NULL
* `image_url` VARCHAR(1024) NULL
* `created_at` DATETIME NOT NULL
* `updated_at` DATETIME NOT NULL
* `deleted_at` DATETIME NULL

---

## 10.3 `comments`

### Purpose

Stores comments on posts.

### Fields

* `id` BIGINT PK
* `post_id` BIGINT NOT NULL FK -> posts.id
* `user_id` BIGINT NOT NULL FK -> users.id
* `content` TEXT NOT NULL
* `created_at` DATETIME NOT NULL
* `updated_at` DATETIME NOT NULL
* `deleted_at` DATETIME NULL

### Notes

* No `parent_comment_id`
* No nested replies in current scope

---

## 10.4 `refresh_tokens`

### Purpose

Stores hashed refresh token records for rotation and revocation.

### Fields

* `id` BIGINT PK
* `user_id` BIGINT NOT NULL FK -> users.id
* `token_hash` VARCHAR(255) NOT NULL
* `family_id` VARCHAR(64) NOT NULL
* `parent_token_id` BIGINT NULL
* `expires_at` DATETIME NOT NULL
* `revoked_at` DATETIME NULL
* `revoke_reason` VARCHAR(100) NULL
* `created_at` DATETIME NOT NULL
* `last_used_at` DATETIME NULL
* `user_agent` VARCHAR(512) NULL
* `ip_address` VARCHAR(64) NULL

---

## 10.5 Entity Relationships

```text
users 1 --- n posts
users 1 --- n comments
posts 1 --- n comments
users 1 --- n refresh_tokens
```

---

## 11. Database Access Strategy

## 11.1 ORM / Data Access Decision

The backend shall use **MyBatis** rather than JPA/Hibernate as the primary persistence mechanism.

### Reason

The assignment explicitly requires database access through **stored procedures**, making SQL-first/data-mapper style access more suitable than JPA entity-first modeling.

## 11.2 Database Choice

The selected relational database is **MySQL 8**.

### Reason

* stable and common for CRUD workloads
* straightforward stored procedure support
* simple deployment story for this project
* fully satisfies the assignment requirement of using a relational database.

---

## 12. Stored Procedures

The assignment explicitly requires database access through stored procedures.

## 12.1 Required Stored Procedures

### User

* `sp_register_user`
* `sp_find_user_by_phone`
* `sp_find_user_by_id`

### Post

* `sp_create_post`
* `sp_list_posts`
* `sp_get_post_detail`
* `sp_update_post`
* `sp_delete_post`

### Comment

* `sp_create_comment`
* `sp_list_comments_by_post`

### Refresh Token

* `sp_insert_refresh_token`
* `sp_revoke_refresh_token`
* `sp_find_refresh_token_by_hash`
* `sp_revoke_token_family`

## 12.2 Stored Procedure Responsibilities

* all CRUD-oriented database access should be routed through stored procedures
* parameters must be bound safely
* business rules remain in service layer, not buried entirely in SQL logic

---

## 13. Transaction Design

The assignment requires transaction handling when multiple tables are modified together.

## 13.1 Mandatory Transaction Use Cases

### Delete Post Flow

When deleting a post:

1. delete related comments
2. delete or soft-delete the post
3. rollback if any step fails

### Refresh Token Rotation

When refreshing:

1. validate current refresh token
2. revoke old token
3. insert new token
4. rollback if any step fails

## 13.2 Optional Transaction Use Cases

* account deletion if later added
* media record cleanup if later normalized into separate table

---

## 14. API Specification

## 14.1 Authentication APIs

### `POST /api/auth/register`

Registers a new user.

**Request**

```json
{
  "phoneNumber": "0912345678",
  "userName": "Brian",
  "password": "StrongPassword123",
  "email": "brian@example.com"
}
```

**Response**

```json
{
  "data": {
    "userId": 1
  }
}
```

---

### `POST /api/auth/login`

Logs in user by phone and password.

**Request**

```json
{
  "phoneNumber": "0912345678",
  "password": "StrongPassword123"
}
```

**Response**

```json
{
  "data": {
    "accessToken": "jwt-token",
    "expiresIn": 900,
    "user": {
      "id": 1,
      "userName": "Brian",
      "phoneNumber": "0912345678"
    }
  }
}
```

**Cookie**

* refresh token written by server as HttpOnly cookie

---

### `POST /api/auth/refresh`

Refreshes access token using refresh cookie.

**Request**

```json
{}
```

**Response**

```json
{
  "data": {
    "accessToken": "new-jwt-token",
    "expiresIn": 900
  }
}
```

---

### `POST /api/auth/logout`

Logs out current session.

**Response**

```json
{
  "data": {
    "success": true
  }
}
```

---

### `GET /api/auth/me`

Returns current authenticated user.

**Response**

```json
{
  "data": {
    "id": 1,
    "userName": "Brian",
    "phoneNumber": "0912345678",
    "email": "brian@example.com",
    "coverImageUrl": "https://...",
    "biography": "..."
  }
}
```

---

## 14.2 Post APIs

### `GET /api/posts`

Returns all posts.

### `POST /api/posts`

Creates a post. Requires authentication.

**Request**

```json
{
  "content": "Hello world",
  "imageUrl": "https://..."
}
```

### `GET /api/posts/{postId}`

Returns post detail.

### `PUT /api/posts/{postId}`

Updates a post. Requires ownership.

### `DELETE /api/posts/{postId}`

Deletes a post. Requires ownership.

---

## 14.3 Comment APIs

### `GET /api/posts/{postId}/comments`

Returns comments for a post.

### `POST /api/posts/{postId}/comments`

Creates a comment for a post. Requires authentication.

**Request**

```json
{
  "content": "Nice post"
}
```

---

## 14.4 SSE API

### `GET /api/posts/{postId}/comments/stream`

Subscribes to comment-created events for a post.

**Response Type**

* `text/event-stream`

**Example Event**

```text
event: comment.created
data: {"type":"comment.created","postId":123,"comment":{"id":456,"userId":8,"userName":"Brian","content":"hello","createdAt":"2026-03-28T11:30:00Z"}}
```

---

## 14.5 Upload APIs

### Option A: Backend-managed upload

#### `POST /api/uploads/cover-image`

Uploads user cover image.

#### `POST /api/uploads/post-image`

Uploads post image.

### Option B: Pre-signed URL flow

#### `POST /api/uploads/presign`

Returns a pre-signed upload URL.

For the first implementation, backend-managed upload is acceptable and simpler.

---

## 15. Frontend Behavior Specification

## 15.1 App Initialization

On app start:

1. set auth state to `checking`
2. call `/auth/refresh`
3. if success:

   * store access token in memory
   * call `/auth/me`
   * state becomes `authenticated`
4. if fail:

   * state becomes `anonymous`

## 15.2 Auth State Model

```ts
type AuthStatus = 'checking' | 'authenticated' | 'anonymous'
```

Store fields:

* `status`
* `accessToken`
* `currentUser`

## 15.3 HTTP Behavior

### Request Interceptor

* attach `Authorization: Bearer <accessToken>` when available

### Response Interceptor

* on access-token-expired response:

  * call `/auth/refresh`
  * retry original request once
* on refresh failure:

  * clear auth state
  * redirect to login if current route is protected

## 15.4 Post Page Behavior

* public list available
* authenticated users can create posts
* post owner sees edit/delete controls
* non-owner does not see edit/delete controls

## 15.5 Comment UI Behavior

* initial comments fetched via REST
* SSE connection established when entering post detail page
* when `comment.created` arrives:

  * append comment if not already present
* on page leave:

  * close `EventSource`

## 15.6 SSE Failure Handling

* allow browser auto-reconnect
* if reconnection is suspected to have missed events, re-fetch full comment list

---

## 16. Backend Layering and Package Structure

## 16.1 Suggested Package Layout

```text
com.example.social
├─ common
│  ├─ config
│  ├─ exception
│  ├─ response
│  └─ security
├─ auth
│  ├─ controller
│  ├─ service
│  ├─ mapper
│  ├─ dto
│  └─ model
├─ post
│  ├─ controller
│  ├─ service
│  ├─ mapper
│  ├─ dto
│  └─ model
├─ comment
│  ├─ controller
│  ├─ service
│  ├─ mapper
│  ├─ dto
│  └─ model
├─ sse
│  ├─ controller
│  ├─ service
│  └─ registry
└─ storage
   ├─ s3
   ├─ service
   └─ dto
```

## 16.2 Responsibilities

* **Controller**

  * request parsing
  * response formatting
  * no business logic
* **Service**

  * use case orchestration
  * transaction boundaries
  * auth checks
  * ownership checks
  * SSE publish call after commit
* **Mapper**

  * MyBatis + stored procedure invocation
* **Common**

  * shared config
  * exception handling
  * auth utilities
  * API response wrapper

---

## 17. Security Requirements

The assignment explicitly requires protection against SQL injection and XSS.

## 17.1 SQL Injection Protection

* all database access must use parameterized calls
* stored procedures must not concatenate raw user input into dynamic SQL
* MyBatis parameter binding shall be used correctly
* avoid unsafe string interpolation in SQL

## 17.2 XSS Protection

* do not render user content using unsafe raw HTML
* frontend should avoid `v-html` for user-generated content
* output user-generated content as plain text
* validate file metadata and input length
* optionally sanitize dangerous text if future rich text is added

## 17.3 Authentication Security

* passwords stored as salted+hashed `password_hash`
* access token short-lived
* refresh token in HttpOnly cookie
* refresh token hash stored in DB
* revoke/rotation supported

## 17.4 Upload Security

* validate content type
* validate file extension
* enforce file size limit
* normalize S3 object keys
* never trust original filename as final storage key

---

## 18. Error Handling

## 18.1 API Error Shape

```json
{
  "error": {
    "code": "ACCESS_TOKEN_EXPIRED",
    "message": "Access token expired"
  }
}
```

## 18.2 Suggested Error Codes

* `VALIDATION_ERROR`
* `UNAUTHENTICATED`
* `FORBIDDEN`
* `POST_NOT_FOUND`
* `COMMENT_NOT_FOUND`
* `USER_ALREADY_EXISTS`
* `INVALID_CREDENTIALS`
* `ACCESS_TOKEN_EXPIRED`
* `ACCESS_TOKEN_INVALID`
* `REFRESH_TOKEN_EXPIRED`
* `REFRESH_TOKEN_REVOKED`
* `UPLOAD_INVALID_FILE`
* `UPLOAD_TOO_LARGE`

---

## 19. DDL / DML / DB Folder Requirements

The assignment explicitly requires DDL and DML files under a `DB` folder.

## 19.1 Required Folder Layout

```text
DB/
├─ ddl/
│  ├─ 001_create_tables.sql
│  └─ 002_create_indexes.sql
├─ dml/
│  └─ 001_seed_data.sql
└─ sp/
   ├─ 001_user_procedures.sql
   ├─ 002_post_procedures.sql
   ├─ 003_comment_procedures.sql
   └─ 004_refresh_token_procedures.sql
```

## 19.2 Seed Data

Seed data should include:

* sample users
* sample posts
* sample comments

---

## 20. Testing Requirements

## 20.1 Backend Tests

* auth service tests
* post service tests
* comment service tests
* integration tests for stored procedures
* transaction rollback tests
* refresh token rotation tests

## 20.2 Frontend Tests

* auth form tests
* route guard tests
* post create/edit/delete tests
* comment creation tests
* SSE subscription behavior tests where practical

## 20.3 End-to-End Tests

Minimum E2E scenarios:

1. register user
2. login
3. create post
4. add comment
5. verify another client receives comment via SSE
6. owner edits post
7. owner deletes post

---

## 21. Logging and Observability

## 21.1 Backend Logging

Log:

* request ID / trace ID
* auth events
* refresh events
* SSE connection open/close
* upload success/failure
* handled exceptions

Sensitive data must never be logged:

* raw password
* raw refresh token
* access token

## 21.2 Frontend Logging

* keep console noise minimal
* do not log secrets/tokens
* only log non-sensitive debug information in development mode

---

## 22. Non-Functional Requirements

## 22.1 Correctness

The system must compile and run successfully before submission, consistent with the assignment’s requirement to ensure the submission is complete and buildable.

## 22.2 Readability

Code should prioritize:

* clear naming
* consistent layering
* maintainable structure
* explicit business rules

This aligns with the assignment’s evaluation criteria of correctness, logic, and readability.

## 22.3 Simplicity

Scope shall remain intentionally constrained to avoid unnecessary overengineering.

---

## 23. Explicit Design Decisions

This project intentionally makes the following design choices:

1. **No full RBAC**

   * only authenticated write access and owner-based edit/delete checks

2. **No nested comments**

   * comments are flat under posts

3. **No WebSocket**

   * SSE is used for realtime comment updates

4. **MySQL + MyBatis**

   * chosen because the assignment requires stored procedures

5. **Access token in memory**

   * avoids persistent browser storage

6. **Refresh token in HttpOnly cookie**

   * improves security posture for browser-based auth

7. **Refresh token database table included**

   * required for revocation and rotation

8. **S3 for images**

   * DB stores metadata only

9. **Posts/comments publicly readable**

   * only posting/commenting requires login

10. **SSE stream is publicly readable**

* keeps the realtime implementation simple and aligned with public read access

---

## 24. Acceptance Criteria

The implementation is considered complete when all of the following are true:

* user can register with phone number and password
* user can log in successfully
* only authenticated users can create posts
* only authenticated users can comment
* all users can list posts
* all users can read comments
* post owner can edit own post
* post owner can delete own post
* new comment is persisted through stored procedure
* new comment is broadcast via SSE after commit
* comments appear in near real time on another client subscribed to the same post
* database access is performed through stored procedures
* multi-table update flows use transactions
* DDL and DML files exist under `DB/`
* system prevents obvious SQL injection patterns
* frontend does not render user-generated HTML unsafely
* project can build and run successfully
* GitHub repository is submission-ready

---

## 25. Submission Notes

The assignment requires GitHub source control and a buildable submission, and states the evaluation focuses on correctness, logic, and readability.

Therefore, before final submission, the repository should include:

* complete README
* startup instructions
* environment variable guide
* DB setup guide
* API usage examples
* notes on SSE behavior
* sample test accounts if seed data is provided

---

If you want, I can turn this next into a stricter **PRD-style version** or into a **technical design spec with API tables, database tables, and sequence diagrams**.
