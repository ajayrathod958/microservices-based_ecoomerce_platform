package com.microshop.notification.service;

import com.microshop.events.OrderCancelledEvent;
import com.microshop.events.OrderConfirmedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTemplatesTest {

    private final UUID orderId = UUID.fromString("0a1b2c3d-0000-0000-0000-000000000000");

    @Test
    void confirmedMessageShowsRoundedAmountAndOrderNumber() {
        var c = NotificationTemplates.orderConfirmed(new OrderConfirmedEvent(UUID.randomUUID(), Instant.now(),
                orderId, UUID.randomUUID(), "a@b.com", new BigDecimal("19.999"), "EUR", List.of()));

        assertThat(c.title()).isEqualTo("Order #0A1B2C3D confirmed");
        assertThat(c.message()).contains("20.00 EUR").contains("#0A1B2C3D");
    }

    @Test
    void cancelledMessageHandlesMissingReason() {
        var c = NotificationTemplates.orderCancelled(new OrderCancelledEvent(UUID.randomUUID(), Instant.now(),
                orderId, UUID.randomUUID(), "a@b.com", null));

        assertThat(c.message()).contains("an unexpected problem").contains("not been charged");
    }

    @Test
    void emailBodyWrapsTheMessage() {
        assertThat(NotificationTemplates.emailBody("Hi there")).startsWith("Hello,").contains("Hi there");
    }
}
