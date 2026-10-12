package com.microshop.order.messaging;

import com.microshop.common.messaging.idempotency.IdempotencyGuard;
import com.microshop.common.messaging.outbox.EventPublisher;
import com.microshop.events.InventoryFailedEvent;
import com.microshop.events.InventoryReservedEvent;
import com.microshop.events.OrderCancelledEvent;
import com.microshop.events.OrderConfirmedEvent;
import com.microshop.events.PaymentCompletedEvent;
import com.microshop.events.PaymentFailedEvent;
import com.microshop.events.Topics;
import com.microshop.order.entity.CustomerOrder;
import com.microshop.order.repository.CustomerOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Saga step 4: the order reacts to what inventory and payment decided. Each method is one transaction
 * (idempotency marker + state change + outgoing event). The aggregate's guarded transitions make late or
 * out-of-order events harmless.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final CustomerOrderRepository orders;
    private final IdempotencyGuard guard;
    private final EventPublisher publisher;

    @Transactional
    public void onInventoryReserved(InventoryReservedEvent event) {
        if (!guard.firstTime(event.eventId(), "order.inventory-reserved")) {
            return;
        }
        if (!load(event.orderId()).markStockReserved()) {
            log.info("Order {} already moved past PENDING; ignoring inventory-reserved", event.orderId());
        }
    }

    @Transactional
    public void onInventoryFailed(InventoryFailedEvent event) {
        if (!guard.firstTime(event.eventId(), "order.inventory-failed")) {
            return;
        }
        CustomerOrder order = load(event.orderId());
        if (order.cancel(event.reason())) {
            publishCancelled(order, event.reason());
        }
    }

    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        if (!guard.firstTime(event.eventId(), "order.payment-completed")) {
            return;
        }
        CustomerOrder order = load(event.orderId());
        if (order.confirm()) {
            publisher.publish(Topics.ORDER_CONFIRMED, order.getId().toString(), new OrderConfirmedEvent(
                    UUID.randomUUID(), Instant.now(), order.getId(), order.getUserId(), order.getUserEmail(),
                    order.getTotalAmount(), order.getCurrency(), order.toLines()));
        } else {
            // Money was taken for an order that is already final: needs a refund process (out of scope here).
            log.error("Payment {} completed for order {} which is already {}; manual refund required",
                    event.paymentId(), order.getId(), order.getStatus());
        }
    }

    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
        if (!guard.firstTime(event.eventId(), "order.payment-failed")) {
            return;
        }
        CustomerOrder order = load(event.orderId());
        String reason = "Payment failed: " + event.reason();
        if (order.cancel(reason)) {
            publishCancelled(order, reason);
        }
    }

    private void publishCancelled(CustomerOrder order, String reason) {
        publisher.publish(Topics.ORDER_CANCELLED, order.getId().toString(), new OrderCancelledEvent(
                UUID.randomUUID(), Instant.now(), order.getId(), order.getUserId(), order.getUserEmail(), reason));
    }

    private CustomerOrder load(UUID orderId) {
        // Thanks to the outbox the order is always committed before any event about it exists, so a miss is a real
        // error: let the container retry, then dead-letter it.
        return orders.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Event for unknown order " + orderId));
    }
}
