package com.microshop.inventory.messaging;

import com.microshop.common.messaging.EventParser;
import com.microshop.events.OrderCancelledEvent;
import com.microshop.events.OrderConfirmedEvent;
import com.microshop.events.OrderCreatedEvent;
import com.microshop.events.Topics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Thin adapter: parse (poison messages go to the DLT) and delegate to the transactional handler. */
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private static final String GROUP = "inventory-service";

    private final EventParser parser;
    private final InventoryEventHandler handler;

    @KafkaListener(topics = Topics.ORDER_CREATED, groupId = GROUP)
    public void onOrderCreated(ConsumerRecord<String, String> record) {
        handler.onOrderCreated(parser.parse(record.value(), OrderCreatedEvent.class));
    }

    @KafkaListener(topics = Topics.ORDER_CONFIRMED, groupId = GROUP)
    public void onOrderConfirmed(ConsumerRecord<String, String> record) {
        handler.onOrderConfirmed(parser.parse(record.value(), OrderConfirmedEvent.class));
    }

    @KafkaListener(topics = Topics.ORDER_CANCELLED, groupId = GROUP)
    public void onOrderCancelled(ConsumerRecord<String, String> record) {
        handler.onOrderCancelled(parser.parse(record.value(), OrderCancelledEvent.class));
    }
}
