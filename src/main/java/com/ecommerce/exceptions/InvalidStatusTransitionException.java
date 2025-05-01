package com.ecommerce.exceptions;

public class InvalidStatusTransitionException extends BusinessLogicException {
    public InvalidStatusTransitionException(String message) {
       super(message);
   }



    public InvalidStatusTransitionException(String resourceName, String fieldName, Object fieldValue) {
        super(resourceName, fieldName, fieldValue);
    }
}
