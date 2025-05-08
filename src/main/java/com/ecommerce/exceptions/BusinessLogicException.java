package com.ecommerce.exceptions;

public class BusinessLogicException extends RuntimeException {
    public BusinessLogicException(String message) {
       super(message);
   }



    public BusinessLogicException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
    
    public BusinessLogicException(String message, Throwable cause) {
        super(message, cause); // RuntimeException'ın ilgili constructor'ını çağırır
    }
    
}