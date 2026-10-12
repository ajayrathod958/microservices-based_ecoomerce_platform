package com.microshop.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** inventory-service -> order-service. */
public record InventoryFailedEvent(UUID eventId, Instant occurredAt, UUID orderId, UUID userId, String userEmail,
                              List<StockShortage> shortages, String reason) implements DomainEvent {
}
