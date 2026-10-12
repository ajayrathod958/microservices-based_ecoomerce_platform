package com.microshop.events;

import java.util.ArrayList;
import java.util.List;

public final class Topics {
    public static final String ORDER_CREATED = "order-created";
    public static final String INVENTORY_RESERVED = "inventory-reserved";
    public static final String INVENTORY_FAILED = "inventory-failed";
    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String PAYMENT_FAILED = "payment-failed";
    public static final String ORDER_CONFIRMED = "order-confirmed";
    public static final String ORDER_CANCELLED = "order-cancelled";
    public static final String NOTIFICATION_EVENTS = "notification-events";

    public static final List<String> ALL = List.of(
            ORDER_CREATED, INVENTORY_RESERVED, INVENTORY_FAILED, PAYMENT_COMPLETED,
            PAYMENT_FAILED, ORDER_CONFIRMED, ORDER_CANCELLED, NOTIFICATION_EVENTS);

    /** Dead-letter topic for messages that exhausted their retries (or can never succeed). */
    public static String dlt(String topic) {
        return topic + ".DLT";
    }

    public static List<String> allWithDeadLetters() {
        List<String> all = new ArrayList<>(ALL);
        ALL.forEach(t -> all.add(dlt(t)));
        return all;
    }

    private Topics() {
    }
}
