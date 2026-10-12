package com.microshop.inventory.exception;

import com.microshop.common.exception.ApiException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

/** Carries the per-product shortages so the saga (Phase 5) can publish a meaningful inventory-failed event. */
@Getter
public class InsufficientStockException extends ApiException {

    public record Shortage(UUID productId, int requested, int available) {
    }

    private final UUID orderId;
    private final List<Shortage> shortages;

    public InsufficientStockException(UUID orderId, List<Shortage> shortages) {
        super(HttpStatus.CONFLICT, "Insufficient stock for " + shortages.size() + " product(s)");
        this.orderId = orderId;
        this.shortages = List.copyOf(shortages);
    }
}
