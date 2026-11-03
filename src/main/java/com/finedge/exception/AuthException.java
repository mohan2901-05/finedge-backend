package com.finedge.exception;

/**
 * Thrown when authentication fails. Handled by {@link
 * com.finedge.exception.GlobalExceptionHandler} as a generic 401 that does not
 * reveal whether an email address exists.
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }
}
