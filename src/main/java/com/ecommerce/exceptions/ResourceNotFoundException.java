package com.ecommerce.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    /**
     * Constructor with a custom message.
     * @param message The detail message.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor to create a standardized "not found" message.
     *
     * @param resourceName Name of the resource type (e.g., "Product", "Customer").
     * @param fieldName Name of the field used for lookup (e.g., "id", "username").
     * @param fieldValue The value of the field used for lookup.
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        // Creates a message like: "Product not found with id : '123'"
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
}
