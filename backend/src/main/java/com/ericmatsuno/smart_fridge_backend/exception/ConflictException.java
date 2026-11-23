package com.ericmatsuno.smart_fridge_backend.exception;

/**
 * The request is valid but clashes with the current state (duplicate name, using more
 * than you have). Turned into HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
