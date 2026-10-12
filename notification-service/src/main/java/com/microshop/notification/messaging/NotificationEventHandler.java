package com.microshop.notification.messaging;

import com.microshop.common.messaging.idempotency.IdempotencyGuard;
import com.microshop.events.NotificationEvent;
import com.microshop.events.OrderCancelledEvent;
import com.microshop.events.OrderConfirmedEvent;
import com.microshop.notification.service.NotificationFactory;
import com.microshop.notification.service.NotificationTemplates;
import com.microshop.notification.service.NotificationTemplates.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Saga step 6. Each method = one transaction: idempotency marker + notification row (+ queued email). */
@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final IdempotencyGuard guard;
    private final NotificationFactory factory;

    @Transactional
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        if (!guard.firstTime(event.eventId(), "notification.order-confirmed")) {
            return;
        }
        Content c = NotificationTemplates.orderConfirmed(event);
        factory.create(event.userId(), event.userEmail(), c.type(), c.title(), c.message(), event.orderId());
    }

    @Transactional
    public void onOrderCancelled(OrderCancelledEvent event) {
        if (!guard.firstTime(event.eventId(), "notification.order-cancelled")) {
            return;
        }
        Content c = NotificationTemplates.orderCancelled(event);
        factory.create(event.userId(), event.userEmail(), c.type(), c.title(), c.message(), event.orderId());
    }

    /** Generic notifications from any service (topic notification-events). */
    @Transactional
    public void onNotificationEvent(NotificationEvent event) {
        if (!guard.firstTime(event.eventId(), "notification.notification-event")) {
            return;
        }
        UUID orderId = null;
        if (event.data() != null && event.data().get("orderId") != null) {
            try {
                orderId = UUID.fromString(event.data().get("orderId"));
            } catch (IllegalArgumentException ignored) {
                // not a UUID: simply no order link
            }
        }
        factory.create(event.userId(), event.email(),
                event.type() == null ? "GENERAL" : event.type(), event.title(), event.message(), orderId);
    }
}
