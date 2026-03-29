package com.example.social.post.controller;

import java.util.List;

import com.example.social.common.response.ApiResponse;
import com.example.social.post.dto.CreatePostRequest;
import com.example.social.post.dto.DeletePostResponse;
import com.example.social.post.dto.PostResponse;
import com.example.social.post.dto.UpdatePostRequest;
import com.example.social.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    @Operation(operationId = "listPosts", summary = "Return the public feed.")
    public ApiResponse<List<PostResponse>> listPosts() {
        return ApiResponse.ok(postService.listPosts());
    }

    @PostMapping
    @Operation(operationId = "createPost", summary = "Create a post for the scaffold user.")
    public ApiResponse<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.ok(postService.createPost(request));
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
        @Valid @RequestBody UpdatePostRequest request
    ) {
        return ApiResponse.ok(postService.updatePost(postId, request));
    }

    @DeleteMapping("/{postId}")
    @Operation(operationId = "deletePost", summary = "Delete a post and its comments.")
    public ApiResponse<DeletePostResponse> deletePost(@PathVariable long postId) {
        postService.deletePost(postId);
        return ApiResponse.ok(new DeletePostResponse(true));
    }
}
