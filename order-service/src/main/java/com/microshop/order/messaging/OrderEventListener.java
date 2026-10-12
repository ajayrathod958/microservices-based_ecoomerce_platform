package com.microshop.order.messaging;

import com.microshop.common.messaging.EventParser;
import com.microshop.events.InventoryFailedEvent;
import com.microshop.events.InventoryReservedEvent;
import com.microshop.events.PaymentCompletedEvent;
import com.microshop.events.PaymentFailedEvent;
import com.microshop.events.Topics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private static final String GROUP = "order-service";

    private final EventParser parser;
    private final OrderEventHandler handler;

    @KafkaListener(topics = Topics.INVENTORY_RESERVED, groupId = GROUP)
    public void onInventoryReserved(ConsumerRecord<String, String> record) {
        handler.onInventoryReserved(parser.parse(record.value(), InventoryReservedEvent.class));
    }

    @KafkaListener(topics = Topics.INVENTORY_FAILED, groupId = GROUP)
    public void onInventoryFailed(ConsumerRecord<String, String> record) {
        handler.onInventoryFailed(parser.parse(record.value(), InventoryFailedEvent.class));
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = GROUP)
    public void onPaymentCompleted(ConsumerRecord<String, String> record) {
        handler.onPaymentCompleted(parser.parse(record.value(), PaymentCompletedEvent.class));
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = GROUP)
    public void onPaymentFailed(ConsumerRecord<String, String> record) {
        handler.onPaymentFailed(parser.parse(record.value(), PaymentFailedEvent.class));
    }
}
