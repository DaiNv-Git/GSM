package com.example.gsm.exceptions;

public class UnauthorizedException extends BaseException {
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }
}
