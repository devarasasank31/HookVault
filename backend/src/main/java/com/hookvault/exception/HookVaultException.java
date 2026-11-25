package com.hookvault.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class HookVaultException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    public HookVaultException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public HookVaultException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}