package com.microshop.inventory.dto;

import java.util.UUID;

public record StockLine(UUID productId, int quantity) {
}
