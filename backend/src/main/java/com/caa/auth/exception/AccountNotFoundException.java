package com.caa.auth.exception;

/**
 * Thrown when no account matches the given tenant + studentNo combination.
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException() {
        super("Account not found");
    }
}
