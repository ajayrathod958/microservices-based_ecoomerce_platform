package com.microshop.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** payment-service -> order-service. */
public record PaymentFailedEvent(UUID eventId, Instant occurredAt, UUID orderId, UUID userId, String userEmail,
                            BigDecimal amount, String currency, String reason) implements DomainEvent {
}
