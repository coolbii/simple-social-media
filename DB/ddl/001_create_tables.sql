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
