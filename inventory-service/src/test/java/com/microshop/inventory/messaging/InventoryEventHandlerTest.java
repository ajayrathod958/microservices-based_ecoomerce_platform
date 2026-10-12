package com.microshop.inventory.messaging;

import com.microshop.common.messaging.idempotency.IdempotencyGuard;
import com.microshop.common.messaging.outbox.EventPublisher;
import com.microshop.events.InventoryFailedEvent;
import com.microshop.events.InventoryReservedEvent;
import com.microshop.events.OrderCancelledEvent;
import com.microshop.events.OrderConfirmedEvent;
import com.microshop.events.OrderCreatedEvent;
import com.microshop.events.OrderLine;
import com.microshop.events.Topics;
import com.microshop.inventory.exception.InsufficientStockException.Shortage;
import com.microshop.inventory.service.InventoryService;
import com.microshop.inventory.service.InventoryService.ReservationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryEventHandlerTest {

    @Mock InventoryService inventory;
    @Mock IdempotencyGuard guard;
    @Mock EventPublisher publisher;

    private InventoryEventHandler handler;
    private final UUID orderId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new InventoryEventHandler(inventory, guard, publisher);
    }

    private OrderCreatedEvent orderCreated() {
        return new OrderCreatedEvent(UUID.randomUUID(), Instant.now(), orderId, userId, "a@b.com",
                List.of(new OrderLine(productId, "Phone", 2, new BigDecimal("10.00"))),
                new BigDecimal("20.00"), "USD", "tok_success");
    }

    /** ReservationOutcome's factories are package-private in the service package, so build it reflectively. */
    private static ReservationOutcome outcome(boolean reserved, List<Shortage> shortages) {
        try {
            Constructor<ReservationOutcome> c =
                    ReservationOutcome.class.getDeclaredConstructor(boolean.class, List.class);
            c.setAccessible(true);
            return c.newInstance(reserved, shortages);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void reservedStockPublishesInventoryReservedWithPaymentDetails() {
        OrderCreatedEvent event = orderCreated();
        when(guard.firstTime(event.eventId(), "inventory.order-created")).thenReturn(true);
        when(inventory.tryReserve(eq(orderId), anyList())).thenReturn(outcome(true, List.of()));

        handler.onOrderCreated(event);

        ArgumentCaptor<InventoryReservedEvent> captor = ArgumentCaptor.forClass(InventoryReservedEvent.class);
        verify(publisher).publish(eq(Topics.INVENTORY_RESERVED), eq(orderId.toString()), captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        assertThat(captor.getValue().totalAmount()).isEqualByComparingTo("20.00");
        assertThat(captor.getValue().paymentToken()).isEqualTo("tok_success");
        assertThat(captor.getValue().userEmail()).isEqualTo("a@b.com");
    }

    @Test
    void shortagePublishesInventoryFailedInsteadOfThrowing() {
        OrderCreatedEvent event = orderCreated();
        when(guard.firstTime(event.eventId(), "inventory.order-created")).thenReturn(true);
        when(inventory.tryReserve(eq(orderId), anyList()))
                .thenReturn(outcome(false, List.of(new Shortage(productId, 2, 1))));

        handler.onOrderCreated(event);

        ArgumentCaptor<InventoryFailedEvent> captor = ArgumentCaptor.forClass(InventoryFailedEvent.class);
        verify(publisher).publish(eq(Topics.INVENTORY_FAILED), eq(orderId.toString()), captor.capture());
        assertThat(captor.getValue().shortages()).hasSize(1);
        assertThat(captor.getValue().shortages().get(0).available()).isEqualTo(1);
        assertThat(captor.getValue().reason()).contains("Insufficient stock");
        verify(publisher, never()).publish(eq(Topics.INVENTORY_RESERVED), any(), any());
    }

    @Test
    void duplicateOrderCreatedChangesNothing() {
        OrderCreatedEvent event = orderCreated();
        when(guard.firstTime(event.eventId(), "inventory.order-created")).thenReturn(false);

        handler.onOrderCreated(event);

        verifyNoInteractions(inventory, publisher);
    }

    @Test
    void orderConfirmedCommitsStockOnce() {
        OrderConfirmedEvent event = new OrderConfirmedEvent(UUID.randomUUID(), Instant.now(), orderId, userId,
                "a@b.com", BigDecimal.TEN, "USD", List.of());
        when(guard.firstTime(event.eventId(), "inventory.order-confirmed")).thenReturn(true, false);

        handler.onOrderConfirmed(event);
        handler.onOrderConfirmed(event);   // redelivery

        verify(inventory).commit(orderId);
    }

    @Test
    void orderCancelledReleasesStock() {
        OrderCancelledEvent event = new OrderCancelledEvent(UUID.randomUUID(), Instant.now(), orderId, userId,
                "a@b.com", "Payment failed: Card was declined");
        when(guard.firstTime(event.eventId(), "inventory.order-cancelled")).thenReturn(true);

        handler.onOrderCancelled(event);

        verify(inventory).release(orderId);
    }
}
