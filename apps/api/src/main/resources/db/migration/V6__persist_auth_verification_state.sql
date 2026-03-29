-- Persist OTP verification and registration token lifecycle in MySQL.

DROP PROCEDURE IF EXISTS sp_create_phone_verification_request;
DROP PROCEDURE IF EXISTS sp_find_latest_phone_verification_request_by_phone;
DROP PROCEDURE IF EXISTS sp_update_phone_verification_status;
DROP PROCEDURE IF EXISTS sp_mark_phone_verification_approved;
DROP PROCEDURE IF EXISTS sp_insert_registration_token;
DROP PROCEDURE IF EXISTS sp_find_registration_token_by_hash;
DROP PROCEDURE IF EXISTS sp_consume_registration_token;

DELIMITER $$

CREATE PROCEDURE sp_create_phone_verification_request(
  IN p_phone_number VARCHAR(20),
  IN p_provider VARCHAR(50),
  IN p_provider_request_id VARCHAR(128),
  IN p_status VARCHAR(50),
  IN p_expires_at DATETIME
)
BEGIN
  INSERT INTO phone_verification_requests (
    phone_number,
    provider,
    provider_request_id,
    status,
    attempt_count,
    expires_at
  ) VALUES (
    p_phone_number,
    p_provider,
    p_provider_request_id,
    p_status,
    0,
    p_expires_at
  );

  SELECT LAST_INSERT_ID() AS verification_request_id;
END $$

CREATE PROCEDURE sp_find_latest_phone_verification_request_by_phone(IN p_phone_number VARCHAR(20))
BEGIN
  SELECT *
  FROM phone_verification_requests
  WHERE phone_number = p_phone_number
    AND approved_at IS NULL
  ORDER BY created_at DESC, id DESC
  LIMIT 1;
END $$

CREATE PROCEDURE sp_update_phone_verification_status(
  IN p_verification_request_id BIGINT,
  IN p_status VARCHAR(50),
  IN p_attempt_count INT
)
BEGIN
  UPDATE phone_verification_requests
  SET
    status = p_status,
    attempt_count = p_attempt_count,
    updated_at = NOW()
  WHERE id = p_verification_request_id;

  SELECT ROW_COUNT() > 0 AS success;
END $$

CREATE PROCEDURE sp_mark_phone_verification_approved(IN p_verification_request_id BIGINT)
BEGIN
  UPDATE phone_verification_requests
  SET
    status = 'approved',
    approved_at = COALESCE(approved_at, NOW()),
    updated_at = NOW()
  WHERE id = p_verification_request_id;

  SELECT ROW_COUNT() > 0 AS success;
END $$

CREATE PROCEDURE sp_insert_registration_token(
  IN p_phone_number VARCHAR(20),
  IN p_token_hash VARCHAR(255),
  IN p_verification_request_id BIGINT,
  IN p_expires_at DATETIME
)
BEGIN
  INSERT INTO registration_tokens (
    phone_number,
    token_hash,
    verification_request_id,
    expires_at
  ) VALUES (
    p_phone_number,
    p_token_hash,
    p_verification_request_id,
    p_expires_at
  );

  SELECT LAST_INSERT_ID() AS registration_token_id;
END $$

CREATE PROCEDURE sp_find_registration_token_by_hash(IN p_token_hash VARCHAR(255))
BEGIN
  SELECT *
  FROM registration_tokens
  WHERE token_hash = p_token_hash
  LIMIT 1;
END $$

CREATE PROCEDURE sp_consume_registration_token(IN p_registration_token_id BIGINT)
BEGIN
  UPDATE registration_tokens
  SET consumed_at = NOW()
  WHERE id = p_registration_token_id
    AND consumed_at IS NULL;

  SELECT ROW_COUNT() > 0 AS success;
END $$

DELIMITER ;
