-- Flyway baseline migration generated from DB/ scripts
-- Keeps existing Docker-initialized databases compatible via baseline-on-migrate=true

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone_number VARCHAR(20) NOT NULL UNIQUE,
  user_name VARCHAR(100) NOT NULL,
  email VARCHAR(255) NULL,
  password_hash VARCHAR(255) NOT NULL,
  cover_image_key VARCHAR(512) NULL,
  cover_image_url VARCHAR(1024) NULL,
  biography TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS phone_verification_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone_number VARCHAR(20) NOT NULL,
  provider VARCHAR(50) NOT NULL,
  provider_request_id VARCHAR(128) NOT NULL,
  status VARCHAR(50) NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  approved_at DATETIME NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_phone_verification_provider_request UNIQUE (provider, provider_request_id)
);

CREATE TABLE IF NOT EXISTS registration_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone_number VARCHAR(20) NOT NULL,
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  verification_request_id BIGINT NULL,
  expires_at DATETIME NOT NULL,
  consumed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_registration_tokens_verification_request FOREIGN KEY (verification_request_id) REFERENCES phone_verification_requests(id)
);

CREATE TABLE IF NOT EXISTS posts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  image_key VARCHAR(512) NULL,
  image_url VARCHAR(1024) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS comments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id),
  CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(255) NOT NULL,
  family_id VARCHAR(64) NOT NULL,
  parent_token_id BIGINT NULL,
  expires_at DATETIME NOT NULL,
  revoked_at DATETIME NULL,
  revoke_reason VARCHAR(100) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_used_at DATETIME NULL,
  user_agent VARCHAR(512) NULL,
  ip_address VARCHAR(64) NULL,
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_refresh_tokens_parent FOREIGN KEY (parent_token_id) REFERENCES refresh_tokens(id)
);

CREATE INDEX idx_posts_user_created_at ON posts(user_id, created_at DESC);
CREATE INDEX idx_comments_post_created_at ON comments(post_id, created_at ASC);
CREATE INDEX idx_refresh_tokens_user_expires_at ON refresh_tokens(user_id, expires_at);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_phone_verification_phone_created_at ON phone_verification_requests(phone_number, created_at DESC);
CREATE INDEX idx_phone_verification_expires_at ON phone_verification_requests(expires_at);
CREATE INDEX idx_registration_tokens_phone_expires_at ON registration_tokens(phone_number, expires_at);
CREATE INDEX idx_registration_tokens_expires_at ON registration_tokens(expires_at);
CREATE INDEX idx_registration_tokens_verification_request ON registration_tokens(verification_request_id);

DROP PROCEDURE IF EXISTS sp_register_user;
DROP PROCEDURE IF EXISTS sp_find_user_by_phone;
DROP PROCEDURE IF EXISTS sp_find_user_by_id;

DELIMITER $$

CREATE PROCEDURE sp_register_user(
  IN p_phone_number VARCHAR(20),
  IN p_user_name VARCHAR(100),
  IN p_email VARCHAR(255),
  IN p_password_hash VARCHAR(255)
)
BEGIN
  INSERT INTO users (
    phone_number,
    user_name,
    email,
    password_hash
  ) VALUES (
    p_phone_number,
    p_user_name,
    p_email,
    p_password_hash
  );

  SELECT LAST_INSERT_ID() AS user_id;
END $$

CREATE PROCEDURE sp_find_user_by_phone(IN p_phone_number VARCHAR(20))
BEGIN
  SELECT *
  FROM users
  WHERE phone_number = p_phone_number
    AND deleted_at IS NULL
  LIMIT 1;
END $$

CREATE PROCEDURE sp_find_user_by_id(IN p_user_id BIGINT)
BEGIN
  SELECT *
  FROM users
  WHERE id = p_user_id
    AND deleted_at IS NULL
  LIMIT 1;
END $$

DELIMITER ;

DROP PROCEDURE IF EXISTS sp_create_post;
DROP PROCEDURE IF EXISTS sp_list_posts;
DROP PROCEDURE IF EXISTS sp_get_post_detail;
DROP PROCEDURE IF EXISTS sp_update_post;
DROP PROCEDURE IF EXISTS sp_delete_post;

DELIMITER $$

CREATE PROCEDURE sp_create_post(
  IN p_user_id BIGINT,
  IN p_content TEXT,
  IN p_image_key VARCHAR(512),
  IN p_image_url VARCHAR(1024)
)
BEGIN
  INSERT INTO posts (
    user_id,
    content,
    image_key,
    image_url
  ) VALUES (
    p_user_id,
    p_content,
    p_image_key,
    p_image_url
  );

  SELECT LAST_INSERT_ID() AS post_id;
END $$

CREATE PROCEDURE sp_list_posts()
BEGIN
  SELECT
    p.*,
    u.user_name
  FROM posts p
  INNER JOIN users u ON u.id = p.user_id
  WHERE p.deleted_at IS NULL
  ORDER BY p.created_at DESC;
END $$

CREATE PROCEDURE sp_get_post_detail(IN p_post_id BIGINT)
BEGIN
  SELECT
    p.*,
    u.user_name
  FROM posts p
  INNER JOIN users u ON u.id = p.user_id
  WHERE p.id = p_post_id
    AND p.deleted_at IS NULL
  LIMIT 1;
END $$

CREATE PROCEDURE sp_update_post(
  IN p_post_id BIGINT,
  IN p_content TEXT,
  IN p_image_key VARCHAR(512),
  IN p_image_url VARCHAR(1024)
)
BEGIN
  UPDATE posts
  SET
    content = p_content,
    image_key = p_image_key,
    image_url = p_image_url,
    updated_at = NOW()
  WHERE id = p_post_id
    AND deleted_at IS NULL;

  CALL sp_get_post_detail(p_post_id);
END $$

CREATE PROCEDURE sp_delete_post(IN p_post_id BIGINT)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  UPDATE comments
  SET
    deleted_at = NOW(),
    updated_at = NOW()
  WHERE post_id = p_post_id
    AND deleted_at IS NULL;

  UPDATE posts
  SET
    deleted_at = NOW(),
    updated_at = NOW()
  WHERE id = p_post_id
    AND deleted_at IS NULL;

  COMMIT;

  SELECT TRUE AS success;
END $$

DELIMITER ;

DROP PROCEDURE IF EXISTS sp_create_comment;
DROP PROCEDURE IF EXISTS sp_list_comments_by_post;

DELIMITER $$

CREATE PROCEDURE sp_create_comment(
  IN p_post_id BIGINT,
  IN p_user_id BIGINT,
  IN p_content TEXT
)
BEGIN
  INSERT INTO comments (
    post_id,
    user_id,
    content
  ) VALUES (
    p_post_id,
    p_user_id,
    p_content
  );

  SELECT LAST_INSERT_ID() AS comment_id;
END $$

CREATE PROCEDURE sp_list_comments_by_post(IN p_post_id BIGINT)
BEGIN
  SELECT
    c.*,
    u.user_name
  FROM comments c
  INNER JOIN users u ON u.id = c.user_id
  WHERE c.post_id = p_post_id
    AND c.deleted_at IS NULL
  ORDER BY c.created_at ASC;
END $$

DELIMITER ;

DROP PROCEDURE IF EXISTS sp_insert_refresh_token;
DROP PROCEDURE IF EXISTS sp_revoke_refresh_token;
DROP PROCEDURE IF EXISTS sp_find_refresh_token_by_hash;
DROP PROCEDURE IF EXISTS sp_revoke_token_family;

DELIMITER $$

CREATE PROCEDURE sp_insert_refresh_token(
  IN p_user_id BIGINT,
  IN p_token_hash VARCHAR(255),
  IN p_family_id VARCHAR(64),
  IN p_parent_token_id BIGINT,
  IN p_expires_at DATETIME,
  IN p_user_agent VARCHAR(512),
  IN p_ip_address VARCHAR(64)
)
BEGIN
  INSERT INTO refresh_tokens (
    user_id,
    token_hash,
    family_id,
    parent_token_id,
    expires_at,
    user_agent,
    ip_address
  ) VALUES (
    p_user_id,
    p_token_hash,
    p_family_id,
    p_parent_token_id,
    p_expires_at,
    p_user_agent,
    p_ip_address
  );

  SELECT LAST_INSERT_ID() AS refresh_token_id;
END $$

CREATE PROCEDURE sp_revoke_refresh_token(
  IN p_token_id BIGINT,
  IN p_revoke_reason VARCHAR(100)
)
BEGIN
  UPDATE refresh_tokens
  SET
    revoked_at = NOW(),
    revoke_reason = p_revoke_reason
  WHERE id = p_token_id
    AND revoked_at IS NULL;

  SELECT TRUE AS success;
END $$

CREATE PROCEDURE sp_find_refresh_token_by_hash(IN p_token_hash VARCHAR(255))
BEGIN
  SELECT *
  FROM refresh_tokens
  WHERE token_hash = p_token_hash
  LIMIT 1;
END $$

CREATE PROCEDURE sp_revoke_token_family(
  IN p_family_id VARCHAR(64),
  IN p_revoke_reason VARCHAR(100)
)
BEGIN
  UPDATE refresh_tokens
  SET
    revoked_at = NOW(),
    revoke_reason = p_revoke_reason
  WHERE family_id = p_family_id
    AND revoked_at IS NULL;

  SELECT TRUE AS success;
END $$

DELIMITER ;
