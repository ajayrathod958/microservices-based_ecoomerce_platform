package com.microshop.order.service;

import com.microshop.common.messaging.outbox.EventPublisher;
import com.microshop.common.security.AuthenticatedUser;
import com.microshop.events.OrderCreatedEvent;
import com.microshop.events.OrderLine;
import com.microshop.events.Topics;
import com.microshop.order.dto.CheckoutRequest;
import com.microshop.order.dto.OrderResponse;
import com.microshop.order.entity.CustomerOrder;
import com.microshop.order.repository.CustomerOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Saga step 1: ONE local transaction saves the PENDING order and queues order-created in the outbox. */
@Service
public class OrderCreationService {

    private final CustomerOrderRepository orders;
    private final EventPublisher publisher;
    private final String currency;

    public OrderCreationService(CustomerOrderRepository orders, EventPublisher publisher,
                                @Value("${app.currency:USD}") String currency) {
        this.orders = orders;
        this.publisher = publisher;
        this.currency = currency;
    }

    @Transactional
    public OrderResponse create(AuthenticatedUser user, CheckoutRequest request, List<OrderLine> lines,
                                String paymentToken, String idempotencyKey) {
        CustomerOrder order = orders.save(CustomerOrder.create(
                user.userId(), user.email(), lines, request.shippingAddress().toEntity(), currency, idempotencyKey));

        publisher.publish(Topics.ORDER_CREATED, order.getId().toString(), new OrderCreatedEvent(
                UUID.randomUUID(), Instant.now(), order.getId(), user.userId(), user.email(),
                lines, order.getTotalAmount(), order.getCurrency(), paymentToken));

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> findByIdempotencyKey(UUID userId, String idempotencyKey) {
        return orders.findByUserIdAndIdempotencyKey(userId, idempotencyKey).map(OrderResponse::from);
    }
}
