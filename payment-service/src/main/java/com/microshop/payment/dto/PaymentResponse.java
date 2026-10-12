package com.microshop.payment.dto;

import com.microshop.payment.entity.Payment;
import com.microshop.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID orderId, BigDecimal amount, String currency, PaymentStatus status,
                              String transactionRef, String failureReason, Instant createdAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getCurrency(), p.getStatus(),
                p.getTransactionRef(), p.getFailureReason(), p.getCreatedAt());
    }
}
