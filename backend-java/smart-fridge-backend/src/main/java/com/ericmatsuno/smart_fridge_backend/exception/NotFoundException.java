package com.ericmatsuno.smart_fridge_backend.exception;

/** Thrown when a requested row doesn't exist. Turned into HTTP 404 by {@link GlobalExceptionHandler}. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
