package com.microshop.cart.dto;

import com.microshop.cart.entity.CartEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** API response and the exact value stored in Redis. */
public record CartResponse(List<CartItemResponse> items, int totalItems, BigDecimal subtotal, Instant updatedAt) {

    public static CartResponse empty() {
        return new CartResponse(List.of(), 0, BigDecimal.ZERO.setScale(2), null);
    }

    public static CartResponse from(CartEntity cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(i -> new CartItemResponse(i.getProductId(), i.getProductName(), i.getImageUrl(),
                        i.getUnitPrice(), i.getQuantity(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        int totalItems = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal subtotal = items.stream().map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        return new CartResponse(items, totalItems, subtotal, cart.getUpdatedAt());
    }
}
