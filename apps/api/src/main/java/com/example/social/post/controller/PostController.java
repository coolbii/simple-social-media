package com.example.social.post.controller;

import java.util.List;

import com.example.social.auth.service.AuthService;
import com.example.social.common.response.ApiResponse;
import com.example.social.post.dto.CreatePostRequest;
import com.example.social.post.dto.DeletePostResponse;
import com.example.social.post.dto.PostResponse;
import com.example.social.post.dto.UpdatePostRequest;
import com.example.social.post.service.PostService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Posts")
public class PostController {

    private final PostService postService;
    private final AuthService authService;
    private final String sessionCookieName;

    public PostController(
        PostService postService,
        AuthService authService,
        @Value("${app.auth.session-cookie-name:sessionId}") String sessionCookieName
    ) {
        this.postService = postService;
        this.authService = authService;
        this.sessionCookieName = sessionCookieName;
    }

    @GetMapping
    @Operation(operationId = "listPosts", summary = "Return the public feed.")
    public ApiResponse<List<PostResponse>> listPosts() {
        return ApiResponse.ok(postService.listPosts());
    }

    @PostMapping
    @Operation(operationId = "createPost", summary = "Create a post for the authenticated user.")
    public ApiResponse<PostResponse> createPost(
        HttpServletRequest servletRequest,
        @Valid @RequestBody CreatePostRequest request
    ) {
        AuthService.AuthenticatedUser currentUser = authService.requireAuthenticatedUser(
            extractSessionToken(servletRequest)
        );
        return ApiResponse.ok(postService.createPost(request, currentUser.id()));
    }

    @GetMapping("/{postId}")
    @Operation(operationId = "getPostDetail", summary = "Return a single post detail.")
    public ApiResponse<PostResponse> getPost(@PathVariable long postId) {
        return ApiResponse.ok(postService.getPost(postId));
    }

    @PutMapping("/{postId}")
    @Operation(operationId = "updatePost", summary = "Update an existing post.")
    public ApiResponse<PostResponse> updatePost(
        @PathVariable long postId,
        HttpServletRequest servletRequest,
        @Valid @RequestBody UpdatePostRequest request
    ) {
        AuthService.AuthenticatedUser currentUser = authService.requireAuthenticatedUser(
            extractSessionToken(servletRequest)
        );
        return ApiResponse.ok(postService.updatePost(postId, request, currentUser.id()));
    }

    @DeleteMapping("/{postId}")
    @Operation(operationId = "deletePost", summary = "Delete a post and its comments.")
    public ApiResponse<DeletePostResponse> deletePost(
        @PathVariable long postId,
        HttpServletRequest servletRequest
    ) {
        AuthService.AuthenticatedUser currentUser = authService.requireAuthenticatedUser(
            extractSessionToken(servletRequest)
        );
        postService.deletePost(postId, currentUser.id());
        return ApiResponse.ok(new DeletePostResponse(true));
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
