package com.example.social.comment.mapper;

import java.util.List;

import com.example.social.comment.model.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentMapper {

    @Select(
        """
        CALL sp_create_comment(
            #{postId},
            #{userId},
            #{parentCommentId},
            #{content}
        )
        """
    )
    Long createComment(
        @Param("postId") long postId,
        @Param("userId") long userId,
        @Param("parentCommentId") Long parentCommentId,
        @Param("content") String content
    );

    @Select("CALL sp_get_comment_by_id(#{commentId})")
    Comment getCommentById(@Param("commentId") long commentId);

    @Select("CALL sp_soft_delete_comment(#{commentId})")
    Comment softDeleteComment(@Param("commentId") long commentId);

    @Select("CALL sp_list_comments_by_post(#{postId})")
    List<Comment> listCommentsByPost(@Param("postId") long postId);
}
