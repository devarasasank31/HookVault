package com.hookvault.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends HookVaultException {
    public ConflictException(String message) {
        super(message, "CONFLICT", HttpStatus.CONFLICT);
    }

    public ConflictException(String resource, String identifier) {
        this(String.format("%s already exists with identifier: %s", resource, identifier));
    }
}