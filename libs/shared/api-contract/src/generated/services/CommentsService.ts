/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiResponseCommentResponse } from '../models/ApiResponseCommentResponse';
import type { ApiResponseListCommentResponse } from '../models/ApiResponseListCommentResponse';
import type { CreateCommentRequest } from '../models/CreateCommentRequest';
import type { SseEmitter } from '../models/SseEmitter';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class CommentsService {
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
}
