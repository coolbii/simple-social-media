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
