-- Add parent-child relation for comment reply threads.

ALTER TABLE comments
  ADD COLUMN parent_comment_id BIGINT NULL AFTER user_id;

ALTER TABLE comments
  ADD CONSTRAINT fk_comments_parent
  FOREIGN KEY (parent_comment_id) REFERENCES comments(id);

CREATE INDEX idx_comments_post_parent_created_at
  ON comments(post_id, parent_comment_id, created_at ASC);

DROP PROCEDURE IF EXISTS sp_create_comment;
DROP PROCEDURE IF EXISTS sp_list_comments_by_post;

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
    AND c.deleted_at IS NULL
  ORDER BY c.created_at ASC;
END $$

DELIMITER ;
