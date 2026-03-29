-- Add comment update procedure for owner edit flow.

DROP PROCEDURE IF EXISTS sp_update_comment;

DELIMITER $$

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
