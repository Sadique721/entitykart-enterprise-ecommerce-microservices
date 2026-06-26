package com.entitykart.commonservices.common.exception;

/**
 * Domain-level business exception (originally from common-service).
 * Throw this when a business rule is violated (e.g. insufficient stock,
 * duplicate email, invalid coupon code).
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
