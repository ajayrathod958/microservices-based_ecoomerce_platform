package com.microshop.order.messaging;

import com.microshop.common.messaging.idempotency.IdempotencyGuard;
import com.microshop.common.messaging.outbox.EventPublisher;
import com.microshop.events.InventoryFailedEvent;
import com.microshop.events.InventoryReservedEvent;
import com.microshop.events.OrderCancelledEvent;
import com.microshop.events.OrderConfirmedEvent;
import com.microshop.events.OrderLine;
import com.microshop.events.PaymentCompletedEvent;
import com.microshop.events.PaymentFailedEvent;
import com.microshop.events.Topics;
import com.microshop.order.entity.CustomerOrder;
import com.microshop.order.entity.OrderStatus;
import com.microshop.order.entity.ShippingAddress;
import com.microshop.order.repository.CustomerOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventHandlerTest {

    @Mock CustomerOrderRepository orders;
    @Mock IdempotencyGuard guard;
    @Mock EventPublisher publisher;

    private OrderEventHandler handler;
    private CustomerOrder order;

    @BeforeEach
    void setUp() {
        handler = new OrderEventHandler(orders, guard, publisher);
        order = CustomerOrder.create(UUID.randomUUID(), "a@b.com",
                List.of(new OrderLine(UUID.randomUUID(), "Phone", 1, new BigDecimal("10.00"))),
                new ShippingAddress("Jane", "1 Main St", null, "Pune", "411001", "IN"), "USD", null);
    }

    private PaymentCompletedEvent paid() {
        return new PaymentCompletedEvent(UUID.randomUUID(), Instant.now(), order.getId(), order.getUserId(),
                "a@b.com", UUID.randomUUID(), new BigDecimal("10.00"), "USD", "mock_1");
    }

    @Test
    void paymentCompletedConfirmsOrderAndPublishesOrderConfirmed() {
        PaymentCompletedEvent event = paid();
        when(guard.firstTime(event.eventId(), "order.payment-completed")).thenReturn(true);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        handler.onPaymentCompleted(event);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        ArgumentCaptor<OrderConfirmedEvent> captor = ArgumentCaptor.forClass(OrderConfirmedEvent.class);
        verify(publisher).publish(eq(Topics.ORDER_CONFIRMED), eq(order.getId().toString()), captor.capture());
        assertThat(captor.getValue().items()).hasSize(1);
        assertThat(captor.getValue().userEmail()).isEqualTo("a@b.com");
    }

    @Test
    void duplicateEventIsIgnoredEntirely() {
        PaymentCompletedEvent event = paid();
        when(guard.firstTime(event.eventId(), "order.payment-completed")).thenReturn(false);

        handler.onPaymentCompleted(event);

        verifyNoInteractions(orders, publisher);
    }

    @Test
    void paymentForAnAlreadyCancelledOrderDoesNotConfirmIt() {
        order.cancel("Insufficient stock");
        PaymentCompletedEvent event = paid();
        when(guard.firstTime(event.eventId(), "order.payment-completed")).thenReturn(true);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        handler.onPaymentCompleted(event);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(publisher, never()).publish(any(), any(), any());
    }

    @Test
    void inventoryFailureCancelsOrderAndTriggersCompensation() {
        InventoryFailedEvent event = new InventoryFailedEvent(UUID.randomUUID(), Instant.now(), order.getId(),
                order.getUserId(), "a@b.com", List.of(), "Insufficient stock for 1 item(s)");
        when(guard.firstTime(event.eventId(), "order.inventory-failed")).thenReturn(true);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        handler.onInventoryFailed(event);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        ArgumentCaptor<OrderCancelledEvent> captor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(publisher).publish(eq(Topics.ORDER_CANCELLED), eq(order.getId().toString()), captor.capture());
        assertThat(captor.getValue().reason()).contains("Insufficient stock");
    }

    @Test
    void paymentFailureCancelsOrderWithReadableReason() {
        PaymentFailedEvent event = new PaymentFailedEvent(UUID.randomUUID(), Instant.now(), order.getId(),
                order.getUserId(), "a@b.com", new BigDecimal("10.00"), "USD", "Card was declined");
        when(guard.firstTime(event.eventId(), "order.payment-failed")).thenReturn(true);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        handler.onPaymentFailed(event);

        assertThat(order.getFailureReason()).isEqualTo("Payment failed: Card was declined");
        verify(publisher).publish(eq(Topics.ORDER_CANCELLED), eq(order.getId().toString()), any(OrderCancelledEvent.class));
    }

    @Test
    void inventoryReservedMovesPendingOrderForward() {
        InventoryReservedEvent event = new InventoryReservedEvent(UUID.randomUUID(), Instant.now(), order.getId(),
                order.getUserId(), "a@b.com", new BigDecimal("10.00"), "USD", "tok_success");
        when(guard.firstTime(event.eventId(), "order.inventory-reserved")).thenReturn(true);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        handler.onInventoryReserved(event);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
        verify(publisher, never()).publish(any(), any(), any());
    }

    @Test
    void unknownOrderIsAnErrorSoTheContainerRetriesThenDeadLetters() {
        PaymentFailedEvent event = new PaymentFailedEvent(UUID.randomUUID(), Instant.now(), UUID.randomUUID(),
                UUID.randomUUID(), "a@b.com", BigDecimal.ONE, "USD", "x");
        when(guard.firstTime(event.eventId(), "order.payment-failed")).thenReturn(true);
        when(orders.findById(event.orderId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.onPaymentFailed(event)).isInstanceOf(IllegalStateException.class);
    }
}
