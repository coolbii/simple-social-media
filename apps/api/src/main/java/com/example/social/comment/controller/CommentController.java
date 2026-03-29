package com.example.social.comment.controller;

import java.util.List;

import com.example.social.comment.dto.CommentResponse;
import com.example.social.comment.dto.CreateCommentRequest;
import com.example.social.comment.service.CommentService;
import com.example.social.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@Tag(name = "Comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(operationId = "listCommentsByPost", summary = "Return comments for a post.")
    public ApiResponse<List<CommentResponse>> listComments(@PathVariable long postId) {
        return ApiResponse.ok(commentService.listComments(postId));
    }

    @PostMapping
    @Operation(operationId = "createComment", summary = "Create a comment for a post.")
    public ApiResponse<CommentResponse> createComment(
        @PathVariable long postId,
        @Valid @RequestBody CreateCommentRequest request
    ) {
        return ApiResponse.ok(commentService.createComment(postId, request));
    }
}
