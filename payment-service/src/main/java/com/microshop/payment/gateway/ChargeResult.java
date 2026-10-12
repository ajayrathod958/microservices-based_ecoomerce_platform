package com.microshop.payment.gateway;

public record ChargeResult(boolean success, String transactionRef, String failureReason) {

    public static ChargeResult approved(String transactionRef) {
        return new ChargeResult(true, transactionRef, null);
    }

    public static ChargeResult declined(String reason) {
        return new ChargeResult(false, null, reason);
    }
}
