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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Saga step 3. One transaction: idempotency marker + payment row + outgoing event.
 * A declined payment is a normal business outcome (publish payment-failed), not an exception.
 * The UNIQUE(order_id) constraint guarantees a single payment per order even if the guard were bypassed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final IdempotencyGuard guard;
    private final EventPublisher publisher;

    @Transactional
    public void onInventoryReserved(InventoryReservedEvent event) {
        if (!guard.firstTime(event.eventId(), "payment.inventory-reserved")) {
            return;
        }
        if (payments.existsByOrderId(event.orderId())) {
            log.info("Order {} already has a payment; ignoring", event.orderId());
            return;
        }

        ChargeResult result = gateway.charge(event.paymentToken(), event.totalAmount(), event.currency());
        Payment payment = payments.save(Payment.builder()
                .id(UUID.randomUUID())
                .orderId(event.orderId())
                .userId(event.userId())
                .amount(event.totalAmount())
                .currency(event.currency())
                .status(result.success() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED)
                .transactionRef(result.transactionRef())
                .failureReason(result.failureReason())
                .createdAt(Instant.now())
                .build());

        String key = event.orderId().toString();
        if (result.success()) {
            publisher.publish(Topics.PAYMENT_COMPLETED, key, new PaymentCompletedEvent(
                    UUID.randomUUID(), Instant.now(), event.orderId(), event.userId(), event.userEmail(),
                    payment.getId(), payment.getAmount(), payment.getCurrency(), payment.getTransactionRef()));
        } else {
            publisher.publish(Topics.PAYMENT_FAILED, key, new PaymentFailedEvent(
                    UUID.randomUUID(), Instant.now(), event.orderId(), event.userId(), event.userEmail(),
                    payment.getAmount(), payment.getCurrency(), result.failureReason()));
        }
    }
}
