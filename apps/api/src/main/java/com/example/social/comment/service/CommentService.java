package com.example.social.comment.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.example.social.comment.dto.CommentResponse;
import com.example.social.comment.dto.CreateCommentRequest;
import com.example.social.comment.model.Comment;
import com.example.social.sse.service.CommentStreamService;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

    private final CommentStreamService commentStreamService;
    private final AtomicLong commentSequence = new AtomicLong(2);
    private final Map<Long, List<Comment>> commentsByPostId = new ConcurrentHashMap<>();

    public CommentService(CommentStreamService commentStreamService) {
        this.commentStreamService = commentStreamService;
        seedComments();
    }

    public List<CommentResponse> listComments(long postId) {
        return commentsByPostId.getOrDefault(postId, List.of())
            .stream()
            .sorted(Comparator.comparing(Comment::createdAt))
            .map(this::toResponse)
            .toList();
    }

    public CommentResponse createComment(long postId, CreateCommentRequest request) {
        Comment comment = new Comment(
            commentSequence.getAndIncrement(),
            postId,
            1L,
            "Brian",
            request.content(),
            Instant.now()
        );
        commentsByPostId.computeIfAbsent(postId, ignored -> new ArrayList<>()).add(comment);
        CommentResponse response = toResponse(comment);
        commentStreamService.publish(postId, response);
        return response;
    }

    public void deleteByPostId(long postId) {
        commentsByPostId.remove(postId);
    }

    private void seedComments() {
        commentsByPostId.put(
            1L,
            new ArrayList<>(
                List.of(
                    new Comment(
                        1L,
                        1L,
                        1L,
                        "Brian",
                        "This comment is pushed to the Vue detail page over SSE when new comments are added.",
                        Instant.parse("2026-03-28T11:30:00Z")
                    )
                )
            )
        );
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
            comment.id(),
            comment.postId(),
            comment.userId(),
            comment.userName(),
            comment.content(),
            comment.createdAt()
        );
    }
}
