package com.microshop.inventory.dto;

import com.microshop.inventory.entity.InventoryItem;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(UUID productId, int quantityOnHand, int quantityReserved, int available,
                                Instant updatedAt) {

    public static InventoryResponse from(InventoryItem i) {
        return new InventoryResponse(i.getProductId(), i.getQuantityOnHand(), i.getQuantityReserved(),
                i.available(), i.getUpdatedAt());
    }
}
