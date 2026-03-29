package com.example.social.comment.service;

import java.time.Instant;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceTest {

    private CommentMapper commentMapper;
    private CommentStreamService commentStreamService;
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentMapper = mock(CommentMapper.class);
        commentStreamService = mock(CommentStreamService.class);
        commentService = new CommentService(commentMapper, commentStreamService);
    }

    @Test
    void listCommentsShouldSortByCreatedAtAndMapDeletedPlaceholder() {
        when(commentMapper.listCommentsByPost(1L))
            .thenReturn(
                List.of(
                    deletedComment(2L, 1L, 1L, "Brian", 1L, "deleted", "2026-03-29T09:02:00Z"),
                    comment(1L, 1L, 2L, "Alice", null, "first", "2026-03-29T09:01:00Z")
                )
            );

        List<CommentResponse> comments = commentService.listComments(1L);

        assertThat(comments).hasSize(2);
        assertThat(comments.get(0).parentCommentId()).isNull();
        assertThat(comments.get(0).content()).isEqualTo("first");
        assertThat(comments.get(1).parentCommentId()).isEqualTo(1L);
        assertThat(comments.get(1).content()).isEqualTo("Original reply is deleted.");
        assertThat(comments.get(1).deleted()).isTrue();
    }

    @Test
    void listCommentsPageShouldReturnPageAndNextOffset() {
        when(commentMapper.listCommentsPage(1L, null, true, 0, 3))
            .thenReturn(
                List.of(
                    comment(1L, 1L, 2L, "Alice", null, "first", "2026-03-29T09:01:00Z"),
                    comment(2L, 1L, 1L, "Brian", null, "second", "2026-03-29T09:02:00Z"),
                    comment(3L, 1L, 3L, "Carol", null, "third", "2026-03-29T09:03:00Z")
                )
            );

        CommentPageResponse page = commentService.listCommentsPage(1L, null, 0, 2);

        assertThat(page.comments()).hasSize(2);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextOffset()).isEqualTo(2);
        assertThat(page.comments().get(0).content()).isEqualTo("first");
    }

    @Test
    void listCommentsPageShouldValidateParentWhenProvided() {
        when(commentMapper.getCommentById(9L))
            .thenReturn(comment(9L, 1L, 1L, "Brian", null, "parent", "2026-03-29T09:00:00Z"));
        when(commentMapper.listCommentsPage(1L, 9L, false, 0, 2))
            .thenReturn(List.of(comment(10L, 1L, 2L, "Alice", 9L, "child", "2026-03-29T09:01:00Z")));

        CommentPageResponse page = commentService.listCommentsPage(1L, 9L, 0, 1);

        assertThat(page.comments()).hasSize(1);
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextOffset()).isNull();
    }

    @Test
    void listCommentsPageShouldRejectNegativeOffset() {
        ApiException exception = assertThrows(
            ApiException.class,
            () -> commentService.listCommentsPage(1L, null, -1, 5)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(commentMapper, never()).listCommentsPage(anyLong(), any(), anyBoolean(), anyInt(), anyInt());
    }

    @Test
    void listCommentsPageShouldRejectLimitOutOfRange() {
        ApiException exception = assertThrows(
            ApiException.class,
            () -> commentService.listCommentsPage(1L, null, 0, 99)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(commentMapper, never()).listCommentsPage(anyLong(), any(), anyBoolean(), anyInt(), anyInt());
    }

    @Test
    void createCommentShouldCreateReplyWhenParentExists() {
        when(commentMapper.getCommentById(1L))
            .thenReturn(comment(1L, 1L, 1L, "Brian", null, "parent", "2026-03-29T09:00:00Z"));
        when(commentMapper.createComment(1L, 2L, 1L, "Reply on seeded comment"))
            .thenReturn(3L);
        when(commentMapper.getCommentById(3L))
            .thenReturn(
                comment(3L, 1L, 2L, "Alice", 1L, "Reply on seeded comment", "2026-03-29T09:10:00Z")
            );

        CommentResponse created = commentService.createComment(
            1L,
            new CreateCommentRequest("Reply on seeded comment", 1L),
            2L
        );

        assertThat(created.parentCommentId()).isEqualTo(1L);
        assertThat(created.userId()).isEqualTo(2L);
        assertThat(created.userName()).isEqualTo("Alice");
        assertThat(created.content()).isEqualTo("Reply on seeded comment");
        assertThat(created.deleted()).isFalse();
        verify(commentStreamService).publish(eq(1L), eq(created));
    }

    @Test
    void createCommentShouldRejectUnknownParentCommentId() {
        when(commentMapper.getCommentById(999L)).thenReturn(null);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> commentService.createComment(1L, new CreateCommentRequest("Invalid reply", 999L), 1L)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        verify(commentStreamService, never()).publish(anyLong(), any(CommentResponse.class));
    }

    @Test
    void deleteCommentShouldSoftDeleteAndKeepThreadNode() {
        when(commentMapper.getCommentById(1L))
            .thenReturn(comment(1L, 1L, 1L, "Brian", null, "active", "2026-03-29T09:00:00Z"));
        when(commentMapper.softDeleteComment(1L))
            .thenReturn(deletedComment(1L, 1L, 1L, "Brian", null, "active", "2026-03-29T09:00:00Z"));

        CommentResponse deleted = commentService.deleteComment(1L, 1L, 1L);

        assertThat(deleted.deleted()).isTrue();
        assertThat(deleted.content()).isEqualTo("Original reply is deleted.");
    }

    @Test
    void deleteCommentShouldRejectNonOwner() {
        when(commentMapper.getCommentById(1L))
            .thenReturn(comment(1L, 1L, 1L, "Brian", null, "active", "2026-03-29T09:00:00Z"));

        ApiException exception = assertThrows(ApiException.class, () -> commentService.deleteComment(1L, 1L, 2L));

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_UNAUTHORIZED);
        verify(commentMapper, never()).softDeleteComment(anyLong());
    }

    @Test
    void deleteCommentShouldRejectCommentFromDifferentPost() {
        when(commentMapper.getCommentById(1L))
            .thenReturn(comment(1L, 2L, 1L, "Brian", null, "active", "2026-03-29T09:00:00Z"));

        ApiException exception = assertThrows(ApiException.class, () -> commentService.deleteComment(1L, 1L, 1L));

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        verify(commentMapper, never()).softDeleteComment(anyLong());
    }

    @Test
    void updateCommentShouldUpdateOwnedComment() {
        when(commentMapper.getCommentById(5L))
            .thenReturn(comment(5L, 1L, 2L, "Alice", null, "before", "2026-03-29T09:00:00Z"));
        when(commentMapper.updateComment(5L, "after"))
            .thenReturn(comment(5L, 1L, 2L, "Alice", null, "after", "2026-03-29T09:00:00Z"));

        CommentResponse updated = commentService.updateComment(1L, 5L, new UpdateCommentRequest("after"), 2L);

        assertThat(updated.content()).isEqualTo("after");
        assertThat(updated.deleted()).isFalse();
    }

    @Test
    void updateCommentShouldRejectNonOwner() {
        when(commentMapper.getCommentById(5L))
            .thenReturn(comment(5L, 1L, 2L, "Alice", null, "before", "2026-03-29T09:00:00Z"));

        ApiException exception = assertThrows(
            ApiException.class,
            () -> commentService.updateComment(1L, 5L, new UpdateCommentRequest("after"), 3L)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_UNAUTHORIZED);
        verify(commentMapper, never()).updateComment(anyLong(), any());
    }

    @Test
    void updateCommentShouldRejectDeletedComment() {
        when(commentMapper.getCommentById(5L))
            .thenReturn(deletedComment(5L, 1L, 2L, "Alice", null, "before", "2026-03-29T09:00:00Z"));

        ApiException exception = assertThrows(
            ApiException.class,
            () -> commentService.updateComment(1L, 5L, new UpdateCommentRequest("after"), 2L)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(commentMapper, never()).updateComment(anyLong(), any());
    }

    @Test
    void updateCommentShouldRejectCommentFromDifferentPost() {
        when(commentMapper.getCommentById(5L))
            .thenReturn(comment(5L, 2L, 2L, "Alice", null, "before", "2026-03-29T09:00:00Z"));

        ApiException exception = assertThrows(
            ApiException.class,
            () -> commentService.updateComment(1L, 5L, new UpdateCommentRequest("after"), 2L)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        verify(commentMapper, never()).updateComment(anyLong(), any());
    }

    private Comment comment(
        long id, long postId, long userId, String userName,
        Long parentCommentId, String content, String createdAt
    ) {
        return new Comment(
            id, postId, userId, userName, parentCommentId, content,
            Instant.parse(createdAt), null
        );
    }

    private Comment deletedComment(
        long id, long postId, long userId, String userName,
        Long parentCommentId, String content, String createdAt
    ) {
        return new Comment(
            id, postId, userId, userName, parentCommentId, content,
            Instant.parse(createdAt), Instant.parse(createdAt)
        );
    }
}
