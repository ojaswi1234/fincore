package com.fincore.exception;

public class UnauthorizedAccountAccessException extends FinCoreException {
    public UnauthorizedAccountAccessException(String message) {
        super(message);
    }
}
