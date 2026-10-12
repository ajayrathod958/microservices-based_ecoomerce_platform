package com.microshop.product.dto;

import com.microshop.product.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Also the value stored in Redis, so it must stay a plain, Jackson-friendly record. */
public record ProductResponse(UUID id, String name, String description, String sku, BigDecimal price,
                              String imageUrl, UUID categoryId, String categoryName,
                              Instant createdAt, Instant updatedAt) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getSku(), p.getPrice(),
                p.getImageUrl(), p.getCategory().getId(), p.getCategory().getName(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
