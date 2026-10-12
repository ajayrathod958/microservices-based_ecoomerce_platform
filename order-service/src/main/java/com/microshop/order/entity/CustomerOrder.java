package com.microshop.order.entity;

import com.microshop.events.OrderLine;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The order aggregate and its state machine:
 *
 *   PENDING -> STOCK_RESERVED -> CONFIRMED
 *        \________________________\-> CANCELLED      (CONFIRMED / CANCELLED are final)
 *
 * Every transition returns whether it changed anything; events that arrive late, twice or out of order
 * (different topics have no ordering between them) are therefore harmless no-ops.
 * (Named CustomerOrder because "Order" is an SQL/HQL keyword.)
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class CustomerOrder {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Embedded
    private ShippingAddress shippingAddress;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @BatchSize(size = 50)
    private List<OrderStatusHistory> history = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static CustomerOrder create(UUID userId, String userEmail, List<OrderLine> lines,
                                       ShippingAddress address, String currency, String idempotencyKey) {
        CustomerOrder order = new CustomerOrder();
        order.id = UUID.randomUUID();
        order.userId = userId;
        order.userEmail = userEmail;
        order.status = OrderStatus.PENDING;
        order.currency = currency;
        order.shippingAddress = address;
        order.idempotencyKey = idempotencyKey;
        lines.forEach(l -> order.items.add(
                new OrderItem(order, l.productId(), l.productName(), l.unitPrice(), l.quantity())));
        order.totalAmount = order.items.stream().map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        order.createdAt = Instant.now();
        order.updatedAt = order.createdAt;
        order.history.add(new OrderStatusHistory(order, OrderStatus.PENDING, "Order placed"));
        return order;
    }

    public boolean markStockReserved() {
        if (status != OrderStatus.PENDING) {
            return false;
        }
        transition(OrderStatus.STOCK_RESERVED, "Stock reserved");
        return true;
    }

    public boolean confirm() {
        if (status.isTerminal()) {
            return false;
        }
        transition(OrderStatus.CONFIRMED, "Payment received - order confirmed");
        return true;
    }

    public boolean cancel(String reason) {
        if (status.isTerminal()) {
            return false;
        }
        this.failureReason = reason;
        transition(OrderStatus.CANCELLED, reason);
        return true;
    }

    public List<OrderLine> toLines() {
        return items.stream()
                .map(i -> new OrderLine(i.getProductId(), i.getProductName(), i.getQuantity(), i.getUnitPrice()))
                .toList();
    }

    private void transition(OrderStatus next, String note) {
        this.status = next;
        this.updatedAt = Instant.now();
        history.add(new OrderStatusHistory(this, next, note));
    }
}
