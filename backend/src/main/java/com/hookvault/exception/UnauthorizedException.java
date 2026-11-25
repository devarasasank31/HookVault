package com.hookvault.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends HookVaultException {
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException() {
        this("Authentication required");
    }
}