package com.example.social.storage.controller;

import com.example.social.common.response.ApiResponse;
import com.example.social.storage.dto.PresignRequest;
import com.example.social.storage.dto.PresignResponse;
import com.example.social.storage.dto.UploadResponse;
import com.example.social.storage.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@Tag(name = "Uploads")
public class UploadController {

    private final StorageService storageService;

    public UploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/cover-image")
    @Operation(operationId = "uploadCoverImage", summary = "Upload a cover image.")
    public ApiResponse<UploadResponse> uploadCoverImage(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(storageService.uploadCoverImage(file));
    }

    @PostMapping("/post-image")
    @Operation(operationId = "uploadPostImage", summary = "Upload a post image.")
    public ApiResponse<UploadResponse> uploadPostImage(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(storageService.uploadPostImage(file));
    }

    @PostMapping("/presign")
    @Operation(operationId = "presignUpload", summary = "Return a presigned upload URL.")
    public ApiResponse<PresignResponse> presignUpload(@Valid @RequestBody PresignRequest request) {
        return ApiResponse.ok(storageService.presignUpload(request));
    }
}
