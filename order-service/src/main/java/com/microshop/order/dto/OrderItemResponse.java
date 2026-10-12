package com.microshop.order.dto;

import com.microshop.order.entity.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(UUID productId, String productName, BigDecimal unitPrice, int quantity,
                                BigDecimal lineTotal) {

    public static OrderItemResponse from(OrderItem i) {
        return new OrderItemResponse(i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity(),
                i.lineTotal());
    }
}
