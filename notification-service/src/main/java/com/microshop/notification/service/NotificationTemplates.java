package com.microshop.notification.service;

import com.microshop.events.OrderCancelledEvent;
import com.microshop.events.OrderConfirmedEvent;

import java.math.BigDecimal;
import java.util.UUID;

/** Wording of every notification in one place. Plain text only: nothing user-supplied is ever rendered as HTML. */
public final class NotificationTemplates {

    public static final String ORDER_CONFIRMED = "ORDER_CONFIRMED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";

    public record Content(String type, String title, String message) {
    }

    private NotificationTemplates() {
    }

    public static Content orderConfirmed(OrderConfirmedEvent e) {
        return new Content(ORDER_CONFIRMED,
                "Order " + shortId(e.orderId()) + " confirmed",
                "Thank you! Your payment of " + money(e.totalAmount(), e.currency())
                        + " was received and order " + shortId(e.orderId()) + " is confirmed.");
    }

    public static Content orderCancelled(OrderCancelledEvent e) {
        String reason = e.reason() == null || e.reason().isBlank() ? "an unexpected problem" : e.reason();
        return new Content(ORDER_CANCELLED,
                "Order " + shortId(e.orderId()) + " cancelled",
                "Unfortunately order " + shortId(e.orderId()) + " could not be completed: " + reason
                        + ". You have not been charged for it.");
    }

    public static String emailBody(String message) {
        return "Hello,\n\n" + message + "\n\nThe MicroShop team";
    }

    static String shortId(UUID id) {
        return "#" + id.toString().substring(0, 8).toUpperCase();
    }

    static String money(BigDecimal amount, String currency) {
        return (amount == null ? "0.00" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                + " " + (currency == null ? "" : currency);
    }
}
