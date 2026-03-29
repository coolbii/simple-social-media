package com.example.social.comment.mapper;

import java.util.List;

import com.example.social.comment.model.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper {

    Comment createComment(long postId, long userId, String content);

    List<Comment> listCommentsByPost(long postId);
}
