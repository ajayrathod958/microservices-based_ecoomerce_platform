package com.microshop.inventory.dto;

import java.util.UUID;

/** Public view: no on-hand / reserved breakdown, just what a shopper needs. */
public record AvailabilityResponse(UUID productId, int available, boolean inStock) {
}
