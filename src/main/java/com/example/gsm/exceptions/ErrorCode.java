package com.example.gsm.exceptions;

public enum ErrorCode {
    USER_NOT_FOUND("USER_001", "User not found"),
    INVALID_INPUT("COMMON_001", "Invalid input"),
    PERMISSION_DENIED("AUTH_001", "Permission denied"),
    BAD_EXCEPTION("BAD_EXCEPTION", " BAD_EXCEPTION"),
    USER_EXISTED("USER_EXISTED", " USER EXISTED");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
