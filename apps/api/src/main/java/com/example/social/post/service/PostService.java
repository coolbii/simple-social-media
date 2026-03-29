package com.example.social.post.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.example.social.comment.service.CommentService;
import com.example.social.common.exception.ApiException;
import com.example.social.common.exception.ErrorCode;
import com.example.social.post.dto.CreatePostRequest;
import com.example.social.post.dto.PostResponse;
import com.example.social.post.dto.UpdatePostRequest;
import com.example.social.post.model.Post;
import com.example.social.storage.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    private final CommentService commentService;
    private final StorageService storageService;
    private final AtomicLong postSequence = new AtomicLong(2);
    private final Map<Long, Post> posts = new ConcurrentHashMap<>();

    public PostService(CommentService commentService, StorageService storageService) {
        this.commentService = commentService;
        this.storageService = storageService;
        seedPosts();
    }

    public List<PostResponse> listPosts() {
        return posts.values()
            .stream()
            .sorted(Comparator.comparing(Post::createdAt).reversed())
            .map(this::toResponse)
            .toList();
    }

    public PostResponse createPost(CreatePostRequest request) {
        long postId = postSequence.getAndIncrement();
        Instant now = Instant.now();
        Post post = new Post(
            postId,
            1L,
            "Brian",
            request.content(),
            normalizeStoredImageReference(request.imageUrl()),
            now,
            now
        );
        posts.put(postId, post);
        return toResponse(post);
    }

    public PostResponse getPost(long postId) {
        return toResponse(requirePost(postId));
    }

    public PostResponse updatePost(long postId, UpdatePostRequest request) {
        Post existing = requirePost(postId);
        Post updated = new Post(
            existing.id(),
            existing.userId(),
            existing.userName(),
            request.content(),
            normalizeStoredImageReference(request.imageUrl()),
            existing.createdAt(),
            Instant.now()
        );
        posts.put(postId, updated);
        return toResponse(updated);
    }

    public void deletePost(long postId) {
        requirePost(postId);
        commentService.deleteByPostId(postId);
        posts.remove(postId);
    }

    private Post requirePost(long postId) {
        Post post = posts.get(postId);
        if (post == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.POST_NOT_FOUND, "Post not found.");
        }
        return post;
    }

    private void seedPosts() {
        Instant createdAt = Instant.parse("2026-03-28T11:00:00Z");
        posts.put(
            1L,
            new Post(
                1L,
                1L,
                "Brian",
                "Hello from the Spring Boot scaffold. "
                    + "This post is seeded in memory so the frontend has immediate data.",
                "https://images.example.com/posts/sample-post.png",
                createdAt,
                createdAt
            )
        );
    }

    private PostResponse toResponse(Post post) {
        return new PostResponse(
            post.id(),
            post.userId(),
            post.userName(),
            post.content(),
            storageService.resolveImageUrl(post.imageUrl()),
            post.createdAt(),
            post.updatedAt()
        );
    }

    private String normalizeStoredImageReference(String imageReference) {
        if (imageReference == null || imageReference.isBlank()) {
            return null;
        }
        return imageReference.trim();
    }
}
