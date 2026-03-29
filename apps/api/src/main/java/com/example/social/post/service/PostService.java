package com.example.social.post.service;

import java.util.Comparator;
import java.util.List;

import com.example.social.common.exception.ApiException;
import com.example.social.common.exception.ErrorCode;
import com.example.social.post.dto.CreatePostRequest;
import com.example.social.post.dto.PostResponse;
import com.example.social.post.dto.UpdatePostRequest;
import com.example.social.post.mapper.PostMapper;
import com.example.social.post.model.Post;
import com.example.social.storage.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    private final PostMapper postMapper;
    private final StorageService storageService;

    public PostService(PostMapper postMapper, StorageService storageService) {
        this.postMapper = postMapper;
        this.storageService = storageService;
    }

    public List<PostResponse> listPosts() {
        return postMapper.listPosts()
            .stream()
            .sorted(Comparator.comparing(Post::createdAt).reversed())
            .map(this::toResponse)
            .toList();
    }

    public PostResponse createPost(CreatePostRequest request, long actorUserId) {
        String imageKey = normalizeStoredImageReference(request.imageUrl());
        Long postId = postMapper.createPost(actorUserId, request.content(), imageKey, null);
        if (postId == null) {
            throw new IllegalStateException("Unable to create post.");
        }
        return toResponse(requirePost(postId));
    }

    public PostResponse getPost(long postId) {
        return toResponse(requirePost(postId));
    }

    public PostResponse updatePost(long postId, UpdatePostRequest request, long actorUserId) {
        Post existing = requirePost(postId);
        ensureOwner(existing, actorUserId);

        String imageKey = request.imageUrl() == null
            ? resolveStoredImageReference(existing)
            : normalizeStoredImageReference(request.imageUrl());
        Post updated = postMapper.updatePost(postId, request.content(), imageKey, null);
        if (updated == null) {
            throw new IllegalStateException("Unable to update post.");
        }
        return toResponse(updated);
    }

    public void deletePost(long postId, long actorUserId) {
        Post existing = requirePost(postId);
        ensureOwner(existing, actorUserId);

        Boolean deleted = postMapper.deletePost(postId);
        if (Boolean.FALSE.equals(deleted)) {
            throw new IllegalStateException("Unable to delete post.");
        }
    }

    private Post requirePost(long postId) {
        Post post = postMapper.getPostDetail(postId);
        if (post == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.POST_NOT_FOUND, "Post not found.");
        }
        return post;
    }

    private void ensureOwner(Post post, long actorUserId) {
        if (post.userId() != actorUserId) {
            throw new ApiException(
                HttpStatus.FORBIDDEN,
                ErrorCode.AUTH_UNAUTHORIZED,
                "You can only edit your own post."
            );
        }
    }

    private PostResponse toResponse(Post post) {
        return new PostResponse(
            post.id(),
            post.userId(),
            post.userName(),
            post.content(),
            storageService.resolveImageUrl(resolveStoredImageReference(post)),
            post.createdAt(),
            post.updatedAt()
        );
    }

    private String resolveStoredImageReference(Post post) {
        if (post.imageKey() != null && !post.imageKey().isBlank()) {
            return post.imageKey();
        }
        return post.imageUrl();
    }

    private String normalizeStoredImageReference(String imageReference) {
        if (imageReference == null || imageReference.isBlank()) {
            return null;
        }
        return imageReference.trim();
    }
}
