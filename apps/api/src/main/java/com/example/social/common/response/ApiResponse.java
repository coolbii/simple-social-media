package com.example.social.common.response;

public record ApiResponse<T>(T data) {

    @SuppressWarnings("PMD.ShortMethodName")
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data);
    }
}
