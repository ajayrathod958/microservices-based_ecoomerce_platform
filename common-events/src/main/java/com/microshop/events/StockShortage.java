package com.microshop.events;

import java.util.UUID;

public record StockShortage(UUID productId, int requested, int available) {
}
