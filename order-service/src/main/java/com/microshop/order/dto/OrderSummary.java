package com.microshop.order.dto;

import com.microshop.order.entity.CustomerOrder;
import com.microshop.order.entity.OrderItem;
import com.microshop.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummary(UUID id, OrderStatus status, BigDecimal totalAmount, String currency,
                           int itemCount, Instant createdAt) {

    public static OrderSummary from(CustomerOrder o) {
        return new OrderSummary(o.getId(), o.getStatus(), o.getTotalAmount(), o.getCurrency(),
                o.getItems().stream().mapToInt(OrderItem::getQuantity).sum(), o.getCreatedAt());
    }
}
