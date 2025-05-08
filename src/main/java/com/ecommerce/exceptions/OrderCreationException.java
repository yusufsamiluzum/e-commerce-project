package com.ecommerce.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an order cannot be created due to business logic
 * violations or missing critical information (e.g., missing seller).
 * Maps to HTTP 400 Bad Request or 409 Conflict depending on the cause.
 * Using 400 Bad Request here as it often relates to the request data leading to the failure.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Order creation failed due to invalid data or state")
public class OrderCreationException extends BusinessLogicException { // Veya direkt RuntimeException

    public OrderCreationException(String message) {
        super(message);
    }

    public OrderCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}