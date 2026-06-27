package com.entitykart.userservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Global exception handler for user-service.
 * Converts RuntimeExceptions to a proper JSON response with HTTP 400/500
 * so the Angular frontend can read err.data.message and show the correct error.
 */
@RestControllerAdvice(name = "userServiceGlobalExceptionHandler")
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle all business logic exceptions (user not found, invalid input, etc.)
     * Returns HTTP 400 with {"message": "..."} body so Angular shows the real error.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.warn("Business error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "An error occurred"));
    }

    /**
     * Catch-all for unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "An unexpected error occurred. Please try again."));
    }
}
