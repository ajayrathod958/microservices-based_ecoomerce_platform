package com.microshop.events;

import java.time.Instant;
import java.util.UUID;

/** eventId is the idempotency key: consumers remember it and ignore redeliveries. */
public interface DomainEvent {
    UUID eventId();

    Instant occurredAt();
}
