-- Add paged comment listing procedure for root/child thread loading.

DROP PROCEDURE IF EXISTS sp_list_comments_page;

DELIMITER $$

CREATE PROCEDURE sp_list_comments_page(
  IN p_post_id BIGINT,
  IN p_parent_comment_id BIGINT,
  IN p_is_root BOOLEAN,
  IN p_offset INT,
  IN p_limit INT
)
BEGIN
  SELECT
    c.*,
    u.user_name
  FROM comments c
  INNER JOIN users u ON u.id = c.user_id
  WHERE c.post_id = p_post_id
    AND (
      (p_is_root = TRUE AND c.parent_comment_id IS NULL)
      OR (p_is_root = FALSE AND c.parent_comment_id = p_parent_comment_id)
    )
  ORDER BY c.created_at ASC, c.id ASC
  LIMIT p_offset, p_limit;
END $$

DELIMITER ;
