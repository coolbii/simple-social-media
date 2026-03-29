/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiResponseDeletePostResponse } from '../models/ApiResponseDeletePostResponse';
import type { ApiResponseListPostResponse } from '../models/ApiResponseListPostResponse';
import type { ApiResponsePostResponse } from '../models/ApiResponsePostResponse';
import type { CreatePostRequest } from '../models/CreatePostRequest';
import type { UpdatePostRequest } from '../models/UpdatePostRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class PostsService {
    /**
     * Return a single post detail.
     * @returns ApiResponsePostResponse OK
     * @throws ApiError
     */
    public static getPostDetail({
        postId,
    }: {
        postId: number,
    }): CancelablePromise<ApiResponsePostResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/posts/{postId}',
            path: {
                'postId': postId,
            },
        });
    }
    /**
     * Update an existing post.
     * @returns ApiResponsePostResponse OK
     * @throws ApiError
     */
    public static updatePost({
        postId,
        requestBody,
    }: {
        postId: number,
        requestBody: UpdatePostRequest,
    }): CancelablePromise<ApiResponsePostResponse> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/posts/{postId}',
            path: {
                'postId': postId,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * Delete a post and its comments.
     * @returns ApiResponseDeletePostResponse OK
     * @throws ApiError
     */
    public static deletePost({
        postId,
    }: {
        postId: number,
    }): CancelablePromise<ApiResponseDeletePostResponse> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/posts/{postId}',
            path: {
                'postId': postId,
            },
        });
    }
    /**
     * Return the public feed.
     * @returns ApiResponseListPostResponse OK
     * @throws ApiError
     */
    public static listPosts(): CancelablePromise<ApiResponseListPostResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/posts',
        });
    }
    /**
     * Create a post for the authenticated user.
     * @returns ApiResponsePostResponse OK
     * @throws ApiError
     */
    public static createPost({
        requestBody,
    }: {
        requestBody: CreatePostRequest,
    }): CancelablePromise<ApiResponsePostResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/posts',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
