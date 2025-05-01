package com.ecommerce.exceptions;

public class OrderCancellationException extends BusinessLogicException {
    public OrderCancellationException(String message) {
        super(message);
    }



    public OrderCancellationException(String resourceName, String fieldName, Object fieldValue) {
        super(resourceName, fieldName, fieldValue);
    }
}
