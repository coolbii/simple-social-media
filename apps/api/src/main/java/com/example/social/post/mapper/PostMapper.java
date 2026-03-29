package com.example.social.post.mapper;

import java.util.List;

import com.example.social.post.model.Post;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostMapper {

    @Select(
        """
        CALL sp_create_post(
            #{userId},
            #{content},
            #{imageKey},
            #{imageUrl}
        )
        """
    )
    Long createPost(
        @Param("userId") long userId,
        @Param("content") String content,
        @Param("imageKey") String imageKey,
        @Param("imageUrl") String imageUrl
    );

    @ConstructorArgs(
        {
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "user_id", javaType = long.class),
            @Arg(column = "user_name", javaType = String.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "image_key", javaType = String.class),
            @Arg(column = "image_url", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class)
        }
    )
    @Select("CALL sp_list_posts()")
    List<Post> listPosts();

    @ConstructorArgs(
        {
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "user_id", javaType = long.class),
            @Arg(column = "user_name", javaType = String.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "image_key", javaType = String.class),
            @Arg(column = "image_url", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class)
        }
    )
    @Select("CALL sp_get_post_detail(#{postId})")
    Post getPostDetail(@Param("postId") long postId);

    @ConstructorArgs(
        {
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "user_id", javaType = long.class),
            @Arg(column = "user_name", javaType = String.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "image_key", javaType = String.class),
            @Arg(column = "image_url", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class)
        }
    )
    @Select(
        """
        CALL sp_update_post(
            #{postId},
            #{content},
            #{imageKey},
            #{imageUrl}
        )
        """
    )
    Post updatePost(
        @Param("postId") long postId,
        @Param("content") String content,
        @Param("imageKey") String imageKey,
        @Param("imageUrl") String imageUrl
    );

    @Select("CALL sp_delete_post(#{postId})")
    Boolean deletePost(@Param("postId") long postId);
}
