package com.microshop.inventory.service;

import com.microshop.common.api.PageResponse;
import com.microshop.common.exception.BadRequestException;
import com.microshop.common.exception.ResourceNotFoundException;
import com.microshop.inventory.dto.AvailabilityResponse;
import com.microshop.inventory.dto.InventoryResponse;
import com.microshop.inventory.dto.StockLine;
import com.microshop.inventory.entity.InventoryItem;
import com.microshop.inventory.entity.ReservationStatus;
import com.microshop.inventory.entity.StockReservation;
import com.microshop.inventory.exception.InsufficientStockException;
import com.microshop.inventory.exception.InsufficientStockException.Shortage;
import com.microshop.inventory.repository.InventoryItemRepository;
import com.microshop.inventory.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * reserve / release / commit are the building blocks of the order saga (wired to Kafka in Phase 5).
 * All three are idempotent per orderId, so redelivered events are harmless.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository items;
    private final StockReservationRepository reservations;

    // ------------------------------------------------------------------ saga operations

    public record ReservationOutcome(boolean reserved, List<Shortage> shortages) {
        static ReservationOutcome success() {
            return new ReservationOutcome(true, List.of());
        }

        static ReservationOutcome shortage(List<Shortage> shortages) {
            return new ReservationOutcome(false, List.copyOf(shortages));
        }
    }

    /** Throwing variant: all-or-nothing, InsufficientStockException when anything is short. */
    @Transactional
    public void reserve(UUID orderId, List<StockLine> lines) {
        ReservationOutcome outcome = tryReserve(orderId, lines);
        if (!outcome.reserved()) {
            throw new InsufficientStockException(orderId, outcome.shortages());
        }
    }

    /**
     * Non-throwing variant used by the saga. A shortage is a normal business outcome; throwing would mark the
     * consumer's transaction rollback-only and lose the "inventory-failed" event we need to publish.
     * All-or-nothing: either every line is reserved or nothing changes.
     */
    @Transactional
    public ReservationOutcome tryReserve(UUID orderId, List<StockLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("At least one line is required");
        }
        Map<UUID, Integer> wanted = mergeLines(lines);

        if (reservations.existsByOrderId(orderId)) {
            log.info("Reservation for order {} already exists; ignoring duplicate request", orderId);
            return ReservationOutcome.success();
        }

        Map<UUID, InventoryItem> stock = lock(wanted.keySet());

        List<Shortage> shortages = new ArrayList<>();
        wanted.forEach((productId, qty) -> {
            InventoryItem item = stock.get(productId);
            int available = item == null ? 0 : item.available();
            if (available < qty) {
                shortages.add(new Shortage(productId, qty, available));
            }
        });
        if (!shortages.isEmpty()) {
            log.info("Order {} cannot be reserved: {}", orderId, shortages);
            return ReservationOutcome.shortage(shortages);
        }

        wanted.forEach((productId, qty) -> {
            stock.get(productId).reserve(qty);
            reservations.save(StockReservation.builder()
                    .orderId(orderId).productId(productId).quantity(qty)
                    .status(ReservationStatus.RESERVED).build());
        });
        log.info("Reserved stock for order {} ({} line(s))", orderId, wanted.size());
        return ReservationOutcome.success();
    }

    /** Compensating action: give back whatever is still RESERVED for the order. */
    @Transactional
    public void release(UUID orderId) {
        transition(orderId, false);
    }

    /** Order confirmed: turn the reservation into a permanent deduction. */
    @Transactional
    public void commit(UUID orderId) {
        transition(orderId, true);
    }

    private void transition(UUID orderId, boolean commit) {
        List<StockReservation> active = reservations.findByOrderId(orderId).stream()
                .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                .toList();
        if (active.isEmpty()) {
            log.info("Nothing to {} for order {}", commit ? "commit" : "release", orderId);
            return;
        }
        Map<UUID, InventoryItem> stock = lock(active.stream().map(StockReservation::getProductId).toList());
        for (StockReservation r : active) {
            InventoryItem item = stock.get(r.getProductId());
            if (item == null) {
                throw new IllegalStateException("Inventory row missing for reserved product " + r.getProductId());
            }
            if (commit) {
                item.commit(r.getQuantity());
                r.setStatus(ReservationStatus.COMMITTED);
            } else {
                item.release(r.getQuantity());
                r.setStatus(ReservationStatus.RELEASED);
            }
        }
        log.info("{} stock for order {} ({} line(s))", commit ? "Committed" : "Released", orderId, active.size());
    }

    // ------------------------------------------------------------------ admin / query operations

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> list(Pageable pageable) {
        return PageResponse.from(items.findAll(pageable).map(InventoryResponse::from));
    }

    @Transactional(readOnly = true)
    public InventoryResponse get(UUID productId) {
        return items.findById(productId).map(InventoryResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No inventory record for this product"));
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> availability(Collection<UUID> productIds) {
        Collection<UUID> distinct = new LinkedHashSet<>(productIds);
        Map<UUID, InventoryItem> found = items.findAllById(distinct).stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));
        return distinct.stream().map(id -> {
            int available = found.containsKey(id) ? Math.max(0, found.get(id).available()) : 0;
            return new AvailabilityResponse(id, available, available > 0);
        }).toList();
    }

    /** Creates the record if the product has none yet. */
    @Transactional
    public InventoryResponse setStock(UUID productId, int quantityOnHand) {
        InventoryItem item = items.findWithLockByProductId(productId).orElse(null);
        if (item == null) {
            item = items.save(InventoryItem.builder().productId(productId).quantityOnHand(quantityOnHand).build());
        } else {
            requireNotBelowReserved(item, quantityOnHand);
            item.setQuantityOnHand(quantityOnHand);
        }
        return InventoryResponse.from(item);
    }

    @Transactional
    public InventoryResponse adjust(UUID productId, int delta) {
        InventoryItem item = items.findWithLockByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("No inventory record for this product"));
        int newOnHand = item.getQuantityOnHand() + delta;
        requireNotBelowReserved(item, newOnHand);
        item.setQuantityOnHand(newOnHand);
        return InventoryResponse.from(item);
    }

    // ------------------------------------------------------------------ helpers

    private static void requireNotBelowReserved(InventoryItem item, int newOnHand) {
        if (newOnHand < item.getQuantityReserved()) {
            throw new BadRequestException(
                    "Stock cannot drop below the reserved quantity (" + item.getQuantityReserved() + ")");
        }
    }

    private Map<UUID, InventoryItem> lock(Collection<UUID> productIds) {
        return items.findWithLockByProductIdInOrderByProductIdAsc(productIds).stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));
    }

    private static Map<UUID, Integer> mergeLines(List<StockLine> lines) {
        Map<UUID, Integer> merged = new LinkedHashMap<>();
        for (StockLine line : lines) {
            if (line.quantity() <= 0) {
                throw new BadRequestException("Quantity must be positive");
            }
            merged.merge(line.productId(), line.quantity(), Integer::sum);
        }
        return merged;
    }
}
