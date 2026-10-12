package com.microshop.payment.messaging;

import com.microshop.common.messaging.EventParser;
import com.microshop.events.InventoryReservedEvent;
import com.microshop.events.Topics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final EventParser parser;
    private final PaymentEventHandler handler;

    @KafkaListener(topics = Topics.INVENTORY_RESERVED, groupId = "payment-service")
    public void onInventoryReserved(ConsumerRecord<String, String> record) {
        handler.onInventoryReserved(parser.parse(record.value(), InventoryReservedEvent.class));
    }
}
