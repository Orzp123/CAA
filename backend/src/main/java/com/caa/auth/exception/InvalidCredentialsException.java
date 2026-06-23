package com.caa.auth.exception;

/**
 * Thrown when the supplied raw password does not match the stored hash.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
