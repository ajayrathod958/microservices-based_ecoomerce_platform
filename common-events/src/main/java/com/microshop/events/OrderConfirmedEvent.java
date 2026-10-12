package com.microshop.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** order-service -> inventory-service (commit stock) and notification-service. */
public record OrderConfirmedEvent(UUID eventId, Instant occurredAt, UUID orderId, UUID userId, String userEmail,
                             BigDecimal totalAmount, String currency, List<OrderLine> items) implements DomainEvent {
}
