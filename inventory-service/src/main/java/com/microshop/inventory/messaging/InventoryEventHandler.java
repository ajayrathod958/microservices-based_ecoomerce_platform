package com.microshop.inventory.messaging;

import com.microshop.common.messaging.idempotency.IdempotencyGuard;
import com.microshop.common.messaging.outbox.EventPublisher;
import com.microshop.events.InventoryFailedEvent;
import com.microshop.events.InventoryReservedEvent;
import com.microshop.events.OrderCancelledEvent;
import com.microshop.events.OrderConfirmedEvent;
import com.microshop.events.OrderCreatedEvent;
import com.microshop.events.StockShortage;
import com.microshop.events.Topics;
import com.microshop.inventory.dto.StockLine;
import com.microshop.inventory.service.InventoryService;
import com.microshop.inventory.service.InventoryService.ReservationOutcome;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Each method is ONE transaction: idempotency marker + stock change + outgoing event (outbox row).
 * Either all of it happens or none of it, and a redelivered event changes nothing.
 */
@Component
@RequiredArgsConstructor
public class InventoryEventHandler {

    private final InventoryService inventory;
    private final IdempotencyGuard guard;
    private final EventPublisher publisher;

    /** Saga step 2: reserve stock, then announce the outcome. */
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        if (!guard.firstTime(event.eventId(), "inventory.order-created")) {
            return;
        }
        List<StockLine> lines = event.items().stream()
                .map(i -> new StockLine(i.productId(), i.quantity()))
                .toList();
        ReservationOutcome outcome = inventory.tryReserve(event.orderId(), lines);
        String key = event.orderId().toString();

        if (outcome.reserved()) {
            publisher.publish(Topics.INVENTORY_RESERVED, key, new InventoryReservedEvent(
                    UUID.randomUUID(), Instant.now(), event.orderId(), event.userId(), event.userEmail(),
                    event.totalAmount(), event.currency(), event.paymentToken()));
        } else {
            List<StockShortage> shortages = outcome.shortages().stream()
                    .map(s -> new StockShortage(s.productId(), s.requested(), s.available()))
                    .toList();
            publisher.publish(Topics.INVENTORY_FAILED, key, new InventoryFailedEvent(
                    UUID.randomUUID(), Instant.now(), event.orderId(), event.userId(), event.userEmail(),
                    shortages, "Insufficient stock for " + shortages.size() + " item(s)"));
        }
    }

    /** Order confirmed: the held stock becomes a permanent deduction. */
    @Transactional
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        if (guard.firstTime(event.eventId(), "inventory.order-confirmed")) {
            inventory.commit(event.orderId());
        }
    }

    /** Compensating action (saga step 5): payment failed or order cancelled, so give the stock back. */
    @Transactional
    public void onOrderCancelled(OrderCancelledEvent event) {
        if (guard.firstTime(event.eventId(), "inventory.order-cancelled")) {
            inventory.release(event.orderId());
        }
    }
}
