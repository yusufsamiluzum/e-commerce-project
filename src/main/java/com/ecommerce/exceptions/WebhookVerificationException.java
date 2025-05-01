package com.ecommerce.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when webhook signature verification fails.
 * Indicates a potential security issue or misconfiguration.
 * Maps to HTTP 400 Bad Request.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Webhook signature verification failed")
public class WebhookVerificationException extends PaymentException {

    public WebhookVerificationException(String message) {
        super(message);
    }

    public WebhookVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
