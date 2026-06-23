package com.caa.auth.exception;

/**
 * Thrown when the submitted captcha code is invalid or expired.
 */
public class CaptchaInvalidException extends RuntimeException {
    public CaptchaInvalidException() {
        super("Invalid or expired captcha code");
    }
}
