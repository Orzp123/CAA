package com.caa.auth.exception;

/**
 * Thrown when the account status is DISABLED.
 */
public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() {
        super("Account is disabled");
    }
}
