package com.microshop.notification.messaging;

import com.microshop.common.messaging.idempotency.IdempotencyGuard;
import com.microshop.events.NotificationEvent;
import com.microshop.events.OrderCancelledEvent;
import com.microshop.events.OrderConfirmedEvent;
import com.microshop.notification.service.NotificationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {

    @Mock IdempotencyGuard guard;
    @Mock NotificationFactory factory;

    private NotificationEventHandler handler;
    private final UUID orderId = UUID.fromString("abcdef12-0000-0000-0000-000000000000");
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new NotificationEventHandler(guard, factory);
    }

    @Test
    void orderConfirmedCreatesNotificationWithAmountAndShortOrderNumber() {
        OrderConfirmedEvent event = new OrderConfirmedEvent(UUID.randomUUID(), Instant.now(), orderId, userId,
                "a@b.com", new BigDecimal("27.5"), "USD", List.of());
        when(guard.firstTime(event.eventId(), "notification.order-confirmed")).thenReturn(true);

        handler.onOrderConfirmed(event);

        verify(factory).create(eq(userId), eq("a@b.com"), eq("ORDER_CONFIRMED"),
                eq("Order #ABCDEF12 confirmed"), contains("27.50 USD"), eq(orderId));
    }

    @Test
    void orderCancelledExplainsTheReason() {
        OrderCancelledEvent event = new OrderCancelledEvent(UUID.randomUUID(), Instant.now(), orderId, userId,
                "a@b.com", "Payment failed: Card was declined");
        when(guard.firstTime(event.eventId(), "notification.order-cancelled")).thenReturn(true);

        handler.onOrderCancelled(event);

        verify(factory).create(eq(userId), eq("a@b.com"), eq("ORDER_CANCELLED"),
                eq("Order #ABCDEF12 cancelled"), contains("Card was declined"), eq(orderId));
    }

    @Test
    void duplicateEventCreatesNothing() {
        OrderConfirmedEvent event = new OrderConfirmedEvent(UUID.randomUUID(), Instant.now(), orderId, userId,
                "a@b.com", BigDecimal.ONE, "USD", List.of());
        when(guard.firstTime(event.eventId(), "notification.order-confirmed")).thenReturn(false);

        handler.onOrderConfirmed(event);

        verifyNoInteractions(factory);
    }

    @Test
    void genericEventUsesOrderIdFromDataWhenItIsAValidUuid() {
        NotificationEvent event = new NotificationEvent(UUID.randomUUID(), Instant.now(), userId, "a@b.com",
                "PROMO", "Sale", "50% off", Map.of("orderId", orderId.toString()));
        when(guard.firstTime(event.eventId(), "notification.notification-event")).thenReturn(true);

        handler.onNotificationEvent(event);

        verify(factory).create(userId, "a@b.com", "PROMO", "Sale", "50% off", orderId);
    }

    @Test
    void genericEventIgnoresGarbageOrderId() {
        NotificationEvent event = new NotificationEvent(UUID.randomUUID(), Instant.now(), userId, "a@b.com",
                null, "Hello", "World", Map.of("orderId", "not-a-uuid"));
        when(guard.firstTime(event.eventId(), "notification.notification-event")).thenReturn(true);

        handler.onNotificationEvent(event);

        verify(factory).create(eq(userId), eq("a@b.com"), eq("GENERAL"), eq("Hello"), eq("World"), isNull());
    }
}
