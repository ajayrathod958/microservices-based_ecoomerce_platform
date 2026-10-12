package com.microshop.common.messaging;

public class UnparseableEventException extends RuntimeException {
    public UnparseableEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
