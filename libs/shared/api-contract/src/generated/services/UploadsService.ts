/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiResponsePresignResponse } from '../models/ApiResponsePresignResponse';
import type { ApiResponseUploadResponse } from '../models/ApiResponseUploadResponse';
import type { PresignRequest } from '../models/PresignRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class UploadsService {
    /**
     * Return a presigned upload URL.
     * @returns ApiResponsePresignResponse OK
     * @throws ApiError
     */
    public static presignUpload({
        requestBody,
    }: {
        requestBody: PresignRequest,
    }): CancelablePromise<ApiResponsePresignResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/uploads/presign',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * Upload a post image.
     * @returns ApiResponseUploadResponse OK
     * @throws ApiError
     */
    public static uploadPostImage({
        requestBody,
    }: {
        requestBody?: {
            file: Blob;
        },
    }): CancelablePromise<ApiResponseUploadResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/uploads/post-image',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * Upload a cover image.
     * @returns ApiResponseUploadResponse OK
     * @throws ApiError
     */
    public static uploadCoverImage({
        requestBody,
    }: {
        requestBody?: {
            file: Blob;
        },
    }): CancelablePromise<ApiResponseUploadResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/uploads/cover-image',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
