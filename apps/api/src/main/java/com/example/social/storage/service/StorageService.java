package com.example.social.storage.service;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import com.example.social.common.exception.ApiException;
import com.example.social.common.exception.ErrorCode;
import com.example.social.storage.dto.PresignRequest;
import com.example.social.storage.dto.PresignResponse;
import com.example.social.storage.dto.UploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
public class StorageService {

    private final String bucket;
    private final String region;
    private final long presignExpireSeconds;
    private final String publicBaseUrl;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public StorageService(
        @Value("${app.storage.s3.bucket:}") String bucket,
        @Value("${app.storage.s3.region:}") String region,
        @Value("${app.storage.s3.presign-expire-seconds:900}") long presignExpireSeconds,
        @Value("${app.storage.s3.public-base-url:}") String publicBaseUrl
    ) {
        if (bucket == null || bucket.isBlank() || region == null || region.isBlank()) {
            throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                ErrorCode.UPLOAD_STORAGE_UNAVAILABLE,
                "S3 storage is not configured."
            );
        }

        this.bucket = bucket.trim();
        this.region = region.trim();
        this.presignExpireSeconds = presignExpireSeconds;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();

        Region awsRegion = Region.of(this.region);
        this.s3Client = S3Client.builder()
            .region(awsRegion)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
        this.s3Presigner = S3Presigner.builder()
            .region(awsRegion)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }

    public UploadResponse uploadCoverImage(MultipartFile file) {
        return upload("cover-images", file);
    }

    public UploadResponse uploadPostImage(MultipartFile file) {
        return upload("post-images", file);
    }

    public PresignResponse presignUpload(PresignRequest request) {
        String objectKey = "uploads/" + UUID.randomUUID() + "-" + sanitizeFileName(request.fileName());
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(request.contentType())
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(presignExpireSeconds))
            .putObjectRequest(putObjectRequest)
            .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return new PresignResponse(
            objectKey,
            presignedRequest.url().toString(),
            buildPublicUrl(objectKey),
            "PUT"
        );
    }

    private UploadResponse upload(String folder, MultipartFile file) {
        validateFile(file);
        String objectKey = folder + "/" + UUID.randomUUID() + "-" + sanitizeFileName(file.getOriginalFilename());

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(file.getContentType())
                .build();
            s3Client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            return new UploadResponse(objectKey, buildPublicUrl(objectKey));
        } catch (IOException exception) {
            throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                ErrorCode.UPLOAD_STORAGE_UNAVAILABLE,
                "Failed to read file for upload.",
                exception
            );
        } catch (Exception exception) {
            throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                ErrorCode.UPLOAD_STORAGE_UNAVAILABLE,
                "Failed to upload file to S3.",
                exception
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.UPLOAD_INVALID_FILE,
                "Uploaded file must not be empty."
            );
        }
        if (file.getSize() > 5_000_000) {
            throw new ApiException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ErrorCode.UPLOAD_TOO_LARGE,
                "Uploaded file exceeds the 5 MB scaffold limit."
            );
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "upload.bin";
        }
        return fileName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }

    private String buildPublicUrl(String objectKey) {
        if (!publicBaseUrl.isBlank()) {
            String normalized = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
            return normalized + "/" + objectKey;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + objectKey;
    }
}
