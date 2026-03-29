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
