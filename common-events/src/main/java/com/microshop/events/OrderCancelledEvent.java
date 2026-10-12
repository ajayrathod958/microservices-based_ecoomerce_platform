package com.microshop.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** order-service -> inventory-service (release stock: the compensating action) and notification-service. */
public record OrderCancelledEvent(UUID eventId, Instant occurredAt, UUID orderId, UUID userId, String userEmail, String reason) implements DomainEvent {
}
