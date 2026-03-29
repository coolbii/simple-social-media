package com.example.social.comment.mapper;

import java.util.List;

import com.example.social.comment.model.Comment;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
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

    @ConstructorArgs(
        {
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "post_id", javaType = long.class),
            @Arg(column = "user_id", javaType = long.class),
            @Arg(column = "user_name", javaType = String.class),
            @Arg(column = "parent_comment_id", javaType = Long.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class),
            @Arg(column = "deleted_at", javaType = java.time.Instant.class)
        }
    )
    @Select("CALL sp_get_comment_by_id(#{commentId})")
    Comment getCommentById(@Param("commentId") long commentId);

    @ConstructorArgs(
        {
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "post_id", javaType = long.class),
            @Arg(column = "user_id", javaType = long.class),
            @Arg(column = "user_name", javaType = String.class),
            @Arg(column = "parent_comment_id", javaType = Long.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class),
            @Arg(column = "deleted_at", javaType = java.time.Instant.class)
        }
    )
    @Select("CALL sp_soft_delete_comment(#{commentId})")
    Comment softDeleteComment(@Param("commentId") long commentId);

    @ConstructorArgs(
        {
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "post_id", javaType = long.class),
            @Arg(column = "user_id", javaType = long.class),
            @Arg(column = "user_name", javaType = String.class),
            @Arg(column = "parent_comment_id", javaType = Long.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class),
            @Arg(column = "deleted_at", javaType = java.time.Instant.class)
        }
    )
    @Select("CALL sp_update_comment(#{commentId}, #{content})")
    Comment updateComment(@Param("commentId") long commentId, @Param("content") String content);

    @ConstructorArgs(
        {
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "post_id", javaType = long.class),
            @Arg(column = "user_id", javaType = long.class),
            @Arg(column = "user_name", javaType = String.class),
            @Arg(column = "parent_comment_id", javaType = Long.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class),
            @Arg(column = "deleted_at", javaType = java.time.Instant.class)
        }
    )
    @Select("CALL sp_list_comments_by_post(#{postId})")
    List<Comment> listCommentsByPost(@Param("postId") long postId);

    @ConstructorArgs(
        {
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "post_id", javaType = long.class),
            @Arg(column = "user_id", javaType = long.class),
            @Arg(column = "user_name", javaType = String.class),
            @Arg(column = "parent_comment_id", javaType = Long.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class),
            @Arg(column = "deleted_at", javaType = java.time.Instant.class)
        }
    )
    @Select(
        """
        CALL sp_list_comments_page(
            #{postId},
            #{parentCommentId},
            #{isRoot},
            #{offset},
            #{limit}
        )
        """
    )
    List<Comment> listCommentsPage(
        @Param("postId") long postId,
        @Param("parentCommentId") Long parentCommentId,
        @Param("isRoot") boolean isRoot,
        @Param("offset") int offset,
        @Param("limit") int limit
    );
}
