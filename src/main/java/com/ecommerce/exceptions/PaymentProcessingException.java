package com.ecommerce.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown for general errors during payment processing,
 * such as communication issues with the payment gateway or unexpected errors.
 * Maps to HTTP 500 Internal Server Error or 400 Bad Request depending on context.
 * Using 500 here as a general server-side processing issue.
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Payment processing failed")
public class PaymentProcessingException extends PaymentException {

    public PaymentProcessingException(String message) {
        super(message);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
