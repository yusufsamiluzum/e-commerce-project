package com.ecommerce.dto.error;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;

import lombok.Data;

@Data
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status; // HTTP status code
    private String error; // HTTP status phrase (e.g., "Not Found", "Bad Request")
    private String message; // Custom error message from the exception
    private String path; // The requested URI path

    // Optional: For validation errors
    private Map<String, String> validationErrors;

    public ErrorResponse(HttpStatus httpStatus, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = httpStatus.value();
        this.error = httpStatus.getReasonPhrase();
        this.message = message;
        this.path = path;
    }
}
