package com.example.social.comment.service;

import java.util.Comparator;
import java.util.List;

import com.example.social.comment.dto.CommentResponse;
import com.example.social.comment.dto.CreateCommentRequest;
import com.example.social.comment.mapper.CommentMapper;
import com.example.social.comment.model.Comment;
import com.example.social.common.exception.ApiException;
import com.example.social.common.exception.ErrorCode;
import com.example.social.sse.service.CommentStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

    private static final String DELETED_PLACEHOLDER = "Original reply is deleted.";
    private static final Logger LOGGER = LoggerFactory.getLogger(CommentService.class);

    private final CommentMapper commentMapper;
    private final CommentStreamService commentStreamService;

    public CommentService(CommentMapper commentMapper, CommentStreamService commentStreamService) {
        this.commentMapper = commentMapper;
        this.commentStreamService = commentStreamService;
    }

    public List<CommentResponse> listComments(long postId) {
        return commentMapper.listCommentsByPost(postId)
            .stream()
            .sorted(Comparator.comparing(Comment::createdAt))
            .map(this::toResponse)
            .toList();
    }

    public CommentResponse createComment(long postId, CreateCommentRequest request, long actorUserId) {
        Long parentCommentId = request.parentCommentId();
        if (parentCommentId != null) {
            ensureParentCommentExists(postId, parentCommentId);
        }

        Long commentId = commentMapper.createComment(postId, actorUserId, parentCommentId, request.content());
        if (commentId == null) {
            throw new IllegalStateException("Unable to create comment.");
        }

        Comment created = requireCommentInPost(postId, commentId);
        CommentResponse response = toResponse(created);
        try {
            commentStreamService.publish(postId, response);
        } catch (RuntimeException exception) {
            // Comment persistence is the source of truth; SSE push is best-effort.
            LOGGER.warn("Failed to publish comment stream event for post {}", postId, exception);
        }
        return response;
    }

    public CommentResponse deleteComment(long postId, long commentId, long actorUserId) {
        Comment existing = requireCommentInPost(postId, commentId);
        if (existing.userId() != actorUserId) {
            throw new ApiException(
                HttpStatus.FORBIDDEN,
                ErrorCode.AUTH_UNAUTHORIZED,
                "You can only delete your own comment."
            );
        }

        Comment deleted = commentMapper.softDeleteComment(commentId);
        if (deleted == null) {
            throw new IllegalStateException("Unable to delete comment.");
        }

        return toResponse(deleted);
    }

    public void deleteByPostId(long postId) {
        // No-op. Database-side post deletion procedure already soft-deletes linked comments.
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
            comment.id(),
            comment.postId(),
            comment.userId(),
            comment.userName(),
            comment.parentCommentId(),
            comment.deletedAt() == null ? comment.content() : DELETED_PLACEHOLDER,
            comment.createdAt(),
            comment.deletedAt() != null
        );
    }

    private void ensureParentCommentExists(long postId, long parentCommentId) {
        Comment parent = commentMapper.getCommentById(parentCommentId);
        if (parent == null || parent.postId() != postId) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.COMMENT_NOT_FOUND,
                "Parent comment not found for this post."
            );
        }
    }

    private Comment requireCommentInPost(long postId, long commentId) {
        Comment comment = commentMapper.getCommentById(commentId);
        if (comment == null || comment.postId() != postId) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.COMMENT_NOT_FOUND, "Comment not found.");
        }
        return comment;
    }
}
