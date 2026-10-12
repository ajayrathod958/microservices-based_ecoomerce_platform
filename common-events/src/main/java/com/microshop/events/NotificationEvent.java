package com.microshop.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Generic user-facing notification (consumed by notification-service). */
public record NotificationEvent(UUID eventId, Instant occurredAt, UUID userId, String email, String type,
                           String title, String message, Map<String, String> data) implements DomainEvent {
}
