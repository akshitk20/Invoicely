package com.invoicely.exception;

public class InvoiceLimitExceededException extends RuntimeException {
    public InvoiceLimitExceededException(String message) {
        super(message);
    }
}
