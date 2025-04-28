package com.ecommerce.exception;

import lombok.Getter;

@Getter
public enum MessageType {
    NO_RECORD_EXISTS("1001", "No record exists with the given ID."),
    GENERAL_EXCEPTION("9999", "An unexpected error occurred."),;

    MessageType(String code, String message) {
        this.code = code;
        this.message = message;
    }

    private String code;

    private String message;

}
