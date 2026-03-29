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
