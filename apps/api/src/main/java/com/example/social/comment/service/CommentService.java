package com.example.social.comment.service;

import java.util.Comparator;
import java.util.List;

import com.example.social.comment.dto.CommentPageResponse;
import com.example.social.comment.dto.CommentResponse;
import com.example.social.comment.dto.CreateCommentRequest;
import com.example.social.comment.dto.UpdateCommentRequest;
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

    private static final int MAX_PAGE_SIZE = 50;
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

    public CommentPageResponse listCommentsPage(long postId, Long parentCommentId, int offset, int limit) {
        if (offset < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Offset must be >= 0.");
        }
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                "Limit must be between 1 and " + MAX_PAGE_SIZE + "."
            );
        }

        if (parentCommentId != null) {
            ensureParentCommentExists(postId, parentCommentId);
        }

        int queryLimit = Math.min(limit + 1, MAX_PAGE_SIZE + 1);
        List<Comment> fetched = commentMapper.listCommentsPage(
            postId,
            parentCommentId,
            parentCommentId == null,
            offset,
            queryLimit
        );
        boolean hasMore = fetched.size() > limit;
        List<CommentResponse> page = fetched.stream()
            .limit(limit)
            .map(this::toResponse)
            .toList();
        Integer nextOffset = hasMore ? offset + page.size() : null;
        return new CommentPageResponse(page, hasMore, nextOffset);
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

    public CommentResponse updateComment(long postId, long commentId, UpdateCommentRequest request, long actorUserId) {
        Comment existing = requireCommentInPost(postId, commentId);
        if (existing.userId() != actorUserId) {
            throw new ApiException(
                HttpStatus.FORBIDDEN,
                ErrorCode.AUTH_UNAUTHORIZED,
                "You can only edit your own comment."
            );
        }
        if (existing.deletedAt() != null) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                "Deleted comment cannot be edited."
            );
        }

        Comment updated = commentMapper.updateComment(commentId, request.content());
        if (updated == null) {
            throw new IllegalStateException("Unable to update comment.");
        }
        return toResponse(updated);
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
