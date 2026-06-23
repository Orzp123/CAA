package com.caa.auth.exception;

import java.time.LocalDateTime;

/**
 * Thrown when the account is locked and lockedUntil is still in the future.
 */
public class AccountLockedException extends RuntimeException {

    private final LocalDateTime lockedUntil;

    public AccountLockedException(LocalDateTime lockedUntil) {
        super("Account is locked until " + lockedUntil);
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
}
