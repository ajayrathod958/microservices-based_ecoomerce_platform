package com.microshop.common.messaging.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Call first inside the handler's transaction: the "processed" marker and the business effect commit (or roll back)
 * together. Two concurrent deliveries of the same event collide on the primary key; the loser rolls back and, on its
 * redelivery, finds the marker and skips.
 */
@Component
@RequiredArgsConstructor
public class IdempotencyGuard {

    private final ProcessedEventRepository processed;

    /** @return true if this handler has not seen the event before (and records it); false for a duplicate. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean firstTime(UUID eventId, String handler) {
        String id = handler + ":" + eventId;
        if (processed.existsById(id)) {
            return false;
        }
        processed.saveAndFlush(new ProcessedEvent(id, Instant.now()));
        return true;
    }
}
