package com.fincore.exception;

public class AccountNotFoundException extends FinCoreException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
