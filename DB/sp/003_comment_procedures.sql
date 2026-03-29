DROP PROCEDURE IF EXISTS sp_create_comment;
DROP PROCEDURE IF EXISTS sp_list_comments_by_post;
DROP PROCEDURE IF EXISTS sp_get_comment_by_id;
DROP PROCEDURE IF EXISTS sp_soft_delete_comment;
DROP PROCEDURE IF EXISTS sp_update_comment;

DELIMITER $$

CREATE PROCEDURE sp_create_comment(
  IN p_post_id BIGINT,
  IN p_user_id BIGINT,
  IN p_parent_comment_id BIGINT,
  IN p_content TEXT
)
BEGIN
  INSERT INTO comments (
    post_id,
    user_id,
    parent_comment_id,
    content
  ) VALUES (
    p_post_id,
    p_user_id,
    p_parent_comment_id,
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
  ORDER BY c.created_at ASC;
END $$

CREATE PROCEDURE sp_get_comment_by_id(IN p_comment_id BIGINT)
BEGIN
  SELECT
    c.*,
    u.user_name
  FROM comments c
  INNER JOIN users u ON u.id = c.user_id
  WHERE c.id = p_comment_id
  LIMIT 1;
END $$

CREATE PROCEDURE sp_soft_delete_comment(IN p_comment_id BIGINT)
BEGIN
  UPDATE comments
  SET
    deleted_at = NOW(),
    updated_at = NOW()
  WHERE id = p_comment_id
    AND deleted_at IS NULL;

  CALL sp_get_comment_by_id(p_comment_id);
END $$

CREATE PROCEDURE sp_update_comment(
  IN p_comment_id BIGINT,
  IN p_content TEXT
)
BEGIN
  UPDATE comments
  SET
    content = p_content,
    updated_at = NOW()
  WHERE id = p_comment_id
    AND deleted_at IS NULL;

  CALL sp_get_comment_by_id(p_comment_id);
END $$

DELIMITER ;
