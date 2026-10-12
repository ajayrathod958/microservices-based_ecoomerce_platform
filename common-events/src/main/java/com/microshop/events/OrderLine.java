package com.microshop.events;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLine(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
}
