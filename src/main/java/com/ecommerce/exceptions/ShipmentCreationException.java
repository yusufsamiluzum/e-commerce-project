package com.ecommerce.exceptions;

public class ShipmentCreationException extends RuntimeException{

    public ShipmentCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
