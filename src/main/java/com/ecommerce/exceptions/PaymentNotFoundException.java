package com.ecommerce.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a specific Payment record cannot be found.
 * Maps to HTTP 404 Not Found.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Payment record not found")
public class PaymentNotFoundException extends PaymentException {

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public PaymentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
