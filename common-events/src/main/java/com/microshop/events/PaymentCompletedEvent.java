package com.microshop.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** payment-service -> order-service. */
public record PaymentCompletedEvent(UUID eventId, Instant occurredAt, UUID orderId, UUID userId, String userEmail,
                               UUID paymentId, BigDecimal amount, String currency, String transactionRef) implements DomainEvent {
}
