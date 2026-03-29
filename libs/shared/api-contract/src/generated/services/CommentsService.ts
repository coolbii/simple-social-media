/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiResponseCommentPageResponse } from '../models/ApiResponseCommentPageResponse';
import type { ApiResponseCommentResponse } from '../models/ApiResponseCommentResponse';
import type { ApiResponseListCommentResponse } from '../models/ApiResponseListCommentResponse';
import type { CreateCommentRequest } from '../models/CreateCommentRequest';
import type { SseEmitter } from '../models/SseEmitter';
import type { UpdateCommentRequest } from '../models/UpdateCommentRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class CommentsService {
    /**
     * Update a comment content.
     * @returns ApiResponseCommentResponse OK
     * @throws ApiError
     */
    public static updateComment({
        postId,
        commentId,
        requestBody,
    }: {
        postId: number,
        commentId: number,
        requestBody: UpdateCommentRequest,
    }): CancelablePromise<ApiResponseCommentResponse> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/posts/{postId}/comments/{commentId}',
            path: {
                'postId': postId,
                'commentId': commentId,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * Soft-delete a comment while preserving its child replies.
     * @returns ApiResponseCommentResponse OK
     * @throws ApiError
     */
    public static deleteComment({
        postId,
        commentId,
    }: {
        postId: number,
        commentId: number,
    }): CancelablePromise<ApiResponseCommentResponse> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/posts/{postId}/comments/{commentId}',
            path: {
                'postId': postId,
                'commentId': commentId,
            },
        });
    }
    /**
     * Return comments for a post.
     * @returns ApiResponseListCommentResponse OK
     * @throws ApiError
     */
    public static listCommentsByPost({
        postId,
    }: {
        postId: number,
    }): CancelablePromise<ApiResponseListCommentResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/posts/{postId}/comments',
            path: {
                'postId': postId,
            },
        });
    }
    /**
     * Create a comment for a post.
     * @returns ApiResponseCommentResponse OK
     * @throws ApiError
     */
    public static createComment({
        postId,
        requestBody,
    }: {
        postId: number,
        requestBody: CreateCommentRequest,
    }): CancelablePromise<ApiResponseCommentResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/posts/{postId}/comments',
            path: {
                'postId': postId,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * Subscribe to live comment events.
     * @returns SseEmitter OK
     * @throws ApiError
     */
    public static streamComments({
        postId,
    }: {
        postId: number,
    }): CancelablePromise<SseEmitter> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/posts/{postId}/comments/stream',
            path: {
                'postId': postId,
            },
        });
    }
    /**
     * Return paged comments for a post thread node.
     * @returns ApiResponseCommentPageResponse OK
     * @throws ApiError
     */
    public static listCommentsPage({
        postId,
        parentCommentId,
        offset,
        limit = 5,
    }: {
        postId: number,
        parentCommentId?: number,
        offset?: number,
        limit?: number,
    }): CancelablePromise<ApiResponseCommentPageResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/posts/{postId}/comments/page',
            path: {
                'postId': postId,
            },
            query: {
                'parentCommentId': parentCommentId,
                'offset': offset,
                'limit': limit,
            },
        });
    }
}
