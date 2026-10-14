package com.fincore.exception;

public class DuplicateIdempotencyException extends FinCoreException {
    public DuplicateIdempotencyException(String message) {
        super(message);
    }
}
