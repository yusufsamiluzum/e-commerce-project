package com.ecommerce.exceptions;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(String message) {
        super(message);
    }

    public ShipmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
