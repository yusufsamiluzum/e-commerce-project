package com.ecommerce.exceptions;

public class AddressNotFoundException extends ResourceNotFoundException {
    public AddressNotFoundException(String message) {
        super(message);
    }



    public AddressNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(resourceName, fieldName, fieldValue);
    }
}
