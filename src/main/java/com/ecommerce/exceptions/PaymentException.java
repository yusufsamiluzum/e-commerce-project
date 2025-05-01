package com.ecommerce.exceptions;

/**
 * Base exception class for all payment-related errors.
 * Using a RuntimeException means these are unchecked exceptions,
 * which is common in Spring applications for service layer errors.
 */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
