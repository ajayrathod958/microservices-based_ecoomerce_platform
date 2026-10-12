package com.microshop.notification.entity;

public enum EmailStatus {
    PENDING,   // waiting for the dispatcher
    SENT,
    FAILED,    // gave up after the maximum number of attempts
    SKIPPED    // no recipient address
}
