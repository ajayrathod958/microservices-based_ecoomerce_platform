package com.microshop.notification.messaging;

import com.microshop.common.messaging.EventParser;
import com.microshop.events.NotificationEvent;
import com.microshop.events.OrderCancelledEvent;
import com.microshop.events.OrderConfirmedEvent;
import com.microshop.events.Topics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final String GROUP = "notification-service";

    private final EventParser parser;
    private final NotificationEventHandler handler;

    @KafkaListener(topics = Topics.ORDER_CONFIRMED, groupId = GROUP)
    public void onOrderConfirmed(ConsumerRecord<String, String> record) {
        handler.onOrderConfirmed(parser.parse(record.value(), OrderConfirmedEvent.class));
    }

    @KafkaListener(topics = Topics.ORDER_CANCELLED, groupId = GROUP)
    public void onOrderCancelled(ConsumerRecord<String, String> record) {
        handler.onOrderCancelled(parser.parse(record.value(), OrderCancelledEvent.class));
    }

    @KafkaListener(topics = Topics.NOTIFICATION_EVENTS, groupId = GROUP)
    public void onNotificationEvent(ConsumerRecord<String, String> record) {
        handler.onNotificationEvent(parser.parse(record.value(), NotificationEvent.class));
    }
}
