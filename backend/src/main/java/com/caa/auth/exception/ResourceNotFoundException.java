package com.caa.auth.exception;

/**
 * Thrown when a requested resource cannot be found by its identifier.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
