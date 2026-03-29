package com.example.social.common.exception;

import java.util.HashMap;
import java.util.Map;

import com.example.social.common.response.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, ApiError>> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
            .body(Map.of("error", new ApiError(exception.getErrorCode().name(), exception.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, ApiError>> handleValidationException(
        MethodArgumentNotValidException exception
    ) {
        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String message = validationErrors.isEmpty()
            ? "Request validation failed."
            : "Request validation failed.";

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                Map.of(
                    "error",
                    new ApiError(ErrorCode.VALIDATION_ERROR.name(), message, Map.copyOf(validationErrors))
                )
            );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, ApiError>> handleMaxUploadSizeExceededException() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(
                Map.of(
                    "error",
                    new ApiError(ErrorCode.UPLOAD_TOO_LARGE.name(), "Uploaded file exceeds the 1 MB limit.")
                )
            );
    }
}
