package com.microshop.common.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microshop.events.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * The only way services publish events. It does NOT talk to Kafka: it inserts a row into outbox_events inside the
 * caller's transaction (MANDATORY: calling it without one is a bug). OutboxRelay delivers it afterwards, so a crash can
 * never leave "order saved but event lost" or "event sent but order rolled back".
 */
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String topic, String key, DomainEvent event) {
        String json;
        try {
            json = mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise " + event.getClass().getSimpleName(), e);
        }
        outbox.save(OutboxEvent.builder()
                .id(event.eventId())
                .topic(topic)
                .messageKey(key)
                .eventType(event.getClass().getSimpleName())
                .payload(json)
                .createdAt(Instant.now())
                .build());
    }
}
