package com.microshop.cart.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(UUID productId, String name, String imageUrl, BigDecimal unitPrice,
                               int quantity, BigDecimal lineTotal) {
}
