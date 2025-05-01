package com.ecommerce.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an operation is attempted on a Payment
 * that is not in an appropriate state for that operation.
 * (e.g., trying to initiate payment on an already SUCCESSFUL payment).
 * Maps to HTTP 409 Conflict or 400 Bad Request. Using 409 here.
 */
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Invalid state for payment operation")
public class InvalidPaymentStateException extends PaymentException {

    public InvalidPaymentStateException(String message) {
        super(message);
    }

    public InvalidPaymentStateException(String message, Throwable cause) {
        super(message, cause);
    }
}

