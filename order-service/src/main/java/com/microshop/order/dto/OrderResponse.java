package com.microshop.order.dto;

import com.microshop.order.entity.CustomerOrder;
import com.microshop.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, OrderStatus status, BigDecimal totalAmount, String currency,
                            List<OrderItemResponse> items, ShippingAddressDto shippingAddress,
                            String failureReason, List<StatusHistoryResponse> history,
                            Instant createdAt, Instant updatedAt) {

    public static OrderResponse from(CustomerOrder o) {
        return new OrderResponse(o.getId(), o.getStatus(), o.getTotalAmount(), o.getCurrency(),
                o.getItems().stream().map(OrderItemResponse::from).toList(),
                ShippingAddressDto.from(o.getShippingAddress()),
                o.getFailureReason(),
                o.getHistory().stream().map(StatusHistoryResponse::from).toList(),
                o.getCreatedAt(), o.getUpdatedAt());
    }
}
