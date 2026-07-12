package com.entitykart.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler — centralized error response normalizer.
 *
 * <p>All microservices that include shared-lib on their classpath will
 * automatically inherit this handler.  Every error response follows the same
 * envelope:
 * <pre>
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "<human-readable message>",
 *   "timestamp": "2025-01-01T00:00:00Z"
 * }
 * </pre>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Domain Exceptions ─────────────────────────────────────────────────────

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("EntityNotFound: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ValidationException ex) {
        log.warn("ValidationException: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleStock(InsufficientStockException ex) {
        log.warn("InsufficientStock: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, Object>> handlePayment(PaymentException ex) {
        log.error("PaymentException: {}", ex.getMessage());
        return buildError(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    // ── Validation (Bean Validation @Valid) ───────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            fieldErrors.put(fieldName, error.getDefaultMessage());
        });

        Map<String, Object> body = new HashMap<>();
        body.put("status", 400);
        body.put("error", "Validation Failed");
        body.put("message", "One or more fields failed validation");
        body.put("fieldErrors", fieldErrors);
        body.put("timestamp", Instant.now().toString());
        log.warn("Validation errors: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ── Type Mismatch ─────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String message = String.format("Parameter '%s' must be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return buildError(HttpStatus.BAD_REQUEST, message);
    }

    // ── Generic Fallback ──────────────────────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        String message = ex.getMessage();
        // Classify common message patterns into proper HTTP codes
        HttpStatus status;
        if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("not found"))          { status = HttpStatus.NOT_FOUND; }
            else if (lower.contains("unauthorized")
                  || lower.contains("access denied")) { status = HttpStatus.FORBIDDEN; }
            else if (lower.contains("already exists")
                  || lower.contains("duplicate"))     { status = HttpStatus.CONFLICT; }
            else if (lower.contains("expired"))       { status = HttpStatus.UNAUTHORIZED; }
            else                                      { status = HttpStatus.BAD_REQUEST; }
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            log.error("Unhandled runtime exception", ex);
        } else {
            log.warn("RuntimeException [{}]: {}", status.value(), message);
        }

        return buildError(status, message != null ? message : "An unexpected error occurred");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal server error occurred. Please try again later.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
