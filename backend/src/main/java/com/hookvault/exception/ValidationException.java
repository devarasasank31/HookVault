package com.hookvault.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ValidationException extends HookVaultException {
    private final Map<String, String> fieldErrors;

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String message) {
        this(message, Map.of());
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}