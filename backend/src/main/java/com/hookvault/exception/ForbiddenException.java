package com.hookvault.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends HookVaultException {
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    public ForbiddenException() {
        this("Access denied");
    }
}