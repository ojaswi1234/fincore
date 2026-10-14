package com.fincore.exception;

public class FinCoreException extends RuntimeException {
    public FinCoreException(String message) {
        super(message);
    }

    public FinCoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
