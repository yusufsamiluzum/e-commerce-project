package com.ecommerce.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown for errors specific to the refund process.
 * Maps to HTTP 400 Bad Request or 500 Internal Server Error depending on the cause.
 * Using 400 here assuming the request itself was problematic (e.g., refunding non-SUCCESS payment).
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Refund operation failed")
public class RefundException extends PaymentException {

    public RefundException(String message) {
        super(message);
    }

    public RefundException(String message, Throwable cause) {
        super(message, cause);
    }
}
