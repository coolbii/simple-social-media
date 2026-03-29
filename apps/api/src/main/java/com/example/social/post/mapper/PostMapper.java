package com.example.social.post.mapper;

import java.util.List;

import com.example.social.post.model.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper {

    Post createPost(long userId, String content, String imageUrl);

    List<Post> listPosts();

    Post getPostDetail(long postId);

    Post updatePost(long postId, String content, String imageUrl);

    void deletePost(long postId);
}
