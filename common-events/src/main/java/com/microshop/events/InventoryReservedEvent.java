package com.microshop.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** inventory-service -> payment-service (carries what payment needs so it never calls order-service). */
public record InventoryReservedEvent(UUID eventId, Instant occurredAt, UUID orderId, UUID userId, String userEmail,
                                BigDecimal totalAmount, String currency, String paymentToken) implements DomainEvent {
}
