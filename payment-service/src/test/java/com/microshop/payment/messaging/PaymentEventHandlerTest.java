package com.microshop.payment.messaging;

import com.microshop.common.messaging.idempotency.IdempotencyGuard;
import com.microshop.common.messaging.outbox.EventPublisher;
import com.microshop.events.InventoryReservedEvent;
import com.microshop.events.PaymentCompletedEvent;
import com.microshop.events.PaymentFailedEvent;
import com.microshop.events.Topics;
import com.microshop.payment.entity.Payment;
import com.microshop.payment.entity.PaymentStatus;
import com.microshop.payment.gateway.ChargeResult;
import com.microshop.payment.gateway.PaymentGateway;
import com.microshop.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventHandlerTest {

    @Mock PaymentRepository payments;
    @Mock PaymentGateway gateway;
    @Mock IdempotencyGuard guard;
    @Mock EventPublisher publisher;

    private PaymentEventHandler handler;
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new PaymentEventHandler(payments, gateway, guard, publisher);
    }

    private InventoryReservedEvent reserved(String token) {
        return new InventoryReservedEvent(UUID.randomUUID(), Instant.now(), orderId, UUID.randomUUID(),
                "a@b.com", new BigDecimal("20.00"), "USD", token);
    }

    @Test
    void approvedChargeSavesPaymentAndPublishesPaymentCompleted() {
        InventoryReservedEvent event = reserved("tok_success");
        when(guard.firstTime(event.eventId(), "payment.inventory-reserved")).thenReturn(true);
        when(payments.existsByOrderId(orderId)).thenReturn(false);
        when(gateway.charge("tok_success", new BigDecimal("20.00"), "USD")).thenReturn(ChargeResult.approved("mock_abc"));
        when(payments.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        handler.onInventoryReserved(event);

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        ArgumentCaptor<PaymentCompletedEvent> published = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(publisher).publish(eq(Topics.PAYMENT_COMPLETED), eq(orderId.toString()), published.capture());
        assertThat(published.getValue().transactionRef()).isEqualTo("mock_abc");
        assertThat(published.getValue().paymentId()).isEqualTo(saved.getValue().getId());
    }

    @Test
    void declinedChargeIsRecordedAndPublishedAsPaymentFailedWithoutThrowing() {
        InventoryReservedEvent event = reserved("tok_decline");
        when(guard.firstTime(event.eventId(), "payment.inventory-reserved")).thenReturn(true);
        when(payments.existsByOrderId(orderId)).thenReturn(false);
        when(gateway.charge(any(), any(), any())).thenReturn(ChargeResult.declined("Card was declined"));
        when(payments.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        handler.onInventoryReserved(event);

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        ArgumentCaptor<PaymentFailedEvent> published = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(publisher).publish(eq(Topics.PAYMENT_FAILED), eq(orderId.toString()), published.capture());
        assertThat(published.getValue().reason()).isEqualTo("Card was declined");
        verify(publisher, never()).publish(eq(Topics.PAYMENT_COMPLETED), any(), any());
    }

    @Test
    void duplicateEventNeverChargesAgain() {
        InventoryReservedEvent event = reserved("tok_success");
        when(guard.firstTime(event.eventId(), "payment.inventory-reserved")).thenReturn(false);

        handler.onInventoryReserved(event);

        verifyNoInteractions(gateway, publisher);
        verify(payments, never()).save(any());
    }

    @Test
    void orderThatAlreadyHasAPaymentIsNotChargedAgain() {
        InventoryReservedEvent event = reserved("tok_success");   // new eventId, same order
        when(guard.firstTime(event.eventId(), "payment.inventory-reserved")).thenReturn(true);
        when(payments.existsByOrderId(orderId)).thenReturn(true);

        handler.onInventoryReserved(event);

        verifyNoInteractions(gateway, publisher);
    }
}
