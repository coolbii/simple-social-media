package com.example.social.comment.controller;

import java.util.List;

import com.example.social.auth.service.AuthService;
import com.example.social.comment.dto.CommentResponse;
import com.example.social.comment.dto.CreateCommentRequest;
import com.example.social.comment.service.CommentService;
import com.example.social.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final AuthService authService;
    private final String sessionCookieName;

    public CommentController(
        CommentService commentService,
        AuthService authService,
        @Value("${app.auth.session-cookie-name:sessionId}") String sessionCookieName
    ) {
        this.commentService = commentService;
        this.authService = authService;
        this.sessionCookieName = sessionCookieName;
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
        HttpServletRequest servletRequest,
        @Valid @RequestBody CreateCommentRequest request
    ) {
        AuthService.AuthenticatedUser currentUser = authService.requireAuthenticatedUser(
            extractSessionToken(servletRequest)
        );
        return ApiResponse.ok(commentService.createComment(postId, request, currentUser.id()));
    }

    @DeleteMapping("/{commentId}")
    @Operation(operationId = "deleteComment", summary = "Soft-delete a comment while preserving its child replies.")
    public ApiResponse<CommentResponse> deleteComment(
        @PathVariable long postId,
        @PathVariable long commentId,
        HttpServletRequest servletRequest
    ) {
        AuthService.AuthenticatedUser currentUser = authService.requireAuthenticatedUser(
            extractSessionToken(servletRequest)
        );
        return ApiResponse.ok(commentService.deleteComment(postId, commentId, currentUser.id()));
    }

    private String extractSessionToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return "";
        }

        for (Cookie cookie : cookies) {
            if (sessionCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return "";
    }
}
