package com.example.gsm.exceptions;

public class BadRequestException extends BaseException {
    public BadRequestException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }

    public BadRequestException(ErrorCode errorCode, String customMessage) {
        super(errorCode.getCode(), customMessage);
    }
}
