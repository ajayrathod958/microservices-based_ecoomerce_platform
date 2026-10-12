package com.microshop.order.dto;

import com.microshop.order.entity.OrderStatus;
import com.microshop.order.entity.OrderStatusHistory;

import java.time.Instant;

public record StatusHistoryResponse(OrderStatus status, String note, Instant at) {

    public static StatusHistoryResponse from(OrderStatusHistory h) {
        return new StatusHistoryResponse(h.getStatus(), h.getNote(), h.getCreatedAt());
    }
}
