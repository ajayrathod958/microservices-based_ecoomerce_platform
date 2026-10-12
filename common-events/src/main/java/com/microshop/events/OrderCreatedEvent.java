package com.microshop.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** order-service -> inventory-service. paymentToken is a mock-gateway token, never a card number. */
public record OrderCreatedEvent(UUID eventId, Instant occurredAt, UUID orderId, UUID userId, String userEmail,
                           List<OrderLine> items, BigDecimal totalAmount, String currency, String paymentToken) implements DomainEvent {
}
