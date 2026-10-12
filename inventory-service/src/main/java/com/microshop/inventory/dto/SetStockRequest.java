package com.microshop.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetStockRequest(@NotNull @Min(0) @Max(1_000_000) Integer quantityOnHand) {
}
