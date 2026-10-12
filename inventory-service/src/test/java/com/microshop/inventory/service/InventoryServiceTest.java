package com.microshop.inventory.service;

import com.microshop.common.exception.BadRequestException;
import com.microshop.common.exception.ResourceNotFoundException;
import com.microshop.inventory.dto.InventoryResponse;
import com.microshop.inventory.dto.StockLine;
import com.microshop.inventory.entity.InventoryItem;
import com.microshop.inventory.entity.ReservationStatus;
import com.microshop.inventory.entity.StockReservation;
import com.microshop.inventory.exception.InsufficientStockException;
import com.microshop.inventory.repository.InventoryItemRepository;
import com.microshop.inventory.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock InventoryItemRepository items;
    @Mock StockReservationRepository reservations;

    private InventoryService service;
    private final UUID orderId = UUID.randomUUID();
    private final UUID p1 = UUID.randomUUID();
    private final UUID p2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new InventoryService(items, reservations);
    }

    private InventoryItem item(UUID id, int onHand, int reserved) {
        return InventoryItem.builder().productId(id).quantityOnHand(onHand).quantityReserved(reserved).build();
    }

    @Test
    void reserveHoldsStockAndRecordsReservation() {
        InventoryItem stock = item(p1, 10, 0);
        when(reservations.existsByOrderId(orderId)).thenReturn(false);
        when(items.findWithLockByProductIdInOrderByProductIdAsc(anyCollection())).thenReturn(List.of(stock));

        service.reserve(orderId, List.of(new StockLine(p1, 3)));

        assertThat(stock.getQuantityReserved()).isEqualTo(3);
        assertThat(stock.available()).isEqualTo(7);
        ArgumentCaptor<StockReservation> captor = ArgumentCaptor.forClass(StockReservation.class);
        verify(reservations).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(captor.getValue().getQuantity()).isEqualTo(3);
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void reserveMergesDuplicateLines() {
        InventoryItem stock = item(p1, 10, 0);
        when(reservations.existsByOrderId(orderId)).thenReturn(false);
        when(items.findWithLockByProductIdInOrderByProductIdAsc(anyCollection())).thenReturn(List.of(stock));

        service.reserve(orderId, List.of(new StockLine(p1, 2), new StockLine(p1, 3)));

        assertThat(stock.getQuantityReserved()).isEqualTo(5);
        verify(reservations, times(1)).save(any(StockReservation.class));
    }

    @Test
    void reserveIsAllOrNothingAndReportsEveryShortage() {
        InventoryItem a = item(p1, 10, 0);
        InventoryItem b = item(p2, 1, 0);
        when(reservations.existsByOrderId(orderId)).thenReturn(false);
        when(items.findWithLockByProductIdInOrderByProductIdAsc(anyCollection())).thenReturn(List.of(a, b));

        assertThatThrownBy(() -> service.reserve(orderId, List.of(new StockLine(p1, 2), new StockLine(p2, 5))))
                .isInstanceOfSatisfying(InsufficientStockException.class, e -> {
                    assertThat(e.getShortages()).hasSize(1);
                    assertThat(e.getShortages().get(0).productId()).isEqualTo(p2);
                    assertThat(e.getShortages().get(0).available()).isEqualTo(1);
                });

        assertThat(a.getQuantityReserved()).isZero();   // the line that WOULD have fit was not touched
        verify(reservations, never()).save(any());
    }

    @Test
    void reserveTreatsUnknownProductAsOutOfStock() {
        when(reservations.existsByOrderId(orderId)).thenReturn(false);
        when(items.findWithLockByProductIdInOrderByProductIdAsc(anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> service.reserve(orderId, List.of(new StockLine(p1, 1))))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void reserveIsIdempotentPerOrder() {
        when(reservations.existsByOrderId(orderId)).thenReturn(true);

        service.reserve(orderId, List.of(new StockLine(p1, 3)));

        verifyNoInteractions(items);
        verify(reservations, never()).save(any());
    }

    @Test
    void reserveRejectsEmptyOrNonPositiveLines() {
        assertThatThrownBy(() -> service.reserve(orderId, List.of())).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.reserve(orderId, List.of(new StockLine(p1, 0))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void releaseReturnsStockExactlyOnce() {
        InventoryItem stock = item(p1, 10, 3);
        StockReservation r = StockReservation.builder().orderId(orderId).productId(p1).quantity(3)
                .status(ReservationStatus.RESERVED).build();
        when(reservations.findByOrderId(orderId)).thenReturn(List.of(r));
        when(items.findWithLockByProductIdInOrderByProductIdAsc(anyCollection())).thenReturn(List.of(stock));

        service.release(orderId);
        service.release(orderId);   // redelivered event

        assertThat(stock.getQuantityReserved()).isZero();
        assertThat(stock.getQuantityOnHand()).isEqualTo(10);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        verify(items, times(1)).findWithLockByProductIdInOrderByProductIdAsc(anyCollection());
    }

    @Test
    void commitDeductsOnHandAndClearsReservation() {
        InventoryItem stock = item(p1, 10, 3);
        StockReservation r = StockReservation.builder().orderId(orderId).productId(p1).quantity(3)
                .status(ReservationStatus.RESERVED).build();
        when(reservations.findByOrderId(orderId)).thenReturn(List.of(r));
        when(items.findWithLockByProductIdInOrderByProductIdAsc(anyCollection())).thenReturn(List.of(stock));

        service.commit(orderId);

        assertThat(stock.getQuantityOnHand()).isEqualTo(7);
        assertThat(stock.getQuantityReserved()).isZero();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.COMMITTED);
    }

    @Test
    void releaseAfterCommitDoesNothing() {
        StockReservation r = StockReservation.builder().orderId(orderId).productId(p1).quantity(3)
                .status(ReservationStatus.COMMITTED).build();
        when(reservations.findByOrderId(orderId)).thenReturn(List.of(r));

        service.release(orderId);

        verifyNoInteractions(items);
    }

    @Test
    void setStockCreatesMissingRecord() {
        when(items.findWithLockByProductId(p1)).thenReturn(Optional.empty());
        when(items.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));

        InventoryResponse res = service.setStock(p1, 25);

        assertThat(res.quantityOnHand()).isEqualTo(25);
        assertThat(res.available()).isEqualTo(25);
    }

    @Test
    void setStockCannotGoBelowReserved() {
        when(items.findWithLockByProductId(p1)).thenReturn(Optional.of(item(p1, 10, 4)));

        assertThatThrownBy(() -> service.setStock(p1, 3)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void adjustAppliesDeltaAndGuardsReserved() {
        InventoryItem stock = item(p1, 10, 4);
        when(items.findWithLockByProductId(p1)).thenReturn(Optional.of(stock));

        assertThat(service.adjust(p1, 5).quantityOnHand()).isEqualTo(15);
        assertThatThrownBy(() -> service.adjust(p1, -12)).isInstanceOf(BadRequestException.class);
        assertThat(stock.getQuantityOnHand()).isEqualTo(15);
    }

    @Test
    void adjustUnknownProductIsNotFound() {
        when(items.findWithLockByProductId(p1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adjust(p1, 1)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void availabilityReportsZeroForUnknownAndDeduplicates() {
        when(items.findAllById(anyCollection())).thenReturn(List.of(item(p1, 10, 4)));

        var result = service.availability(List.of(p1, p2, p1));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).available()).isEqualTo(6);
        assertThat(result.get(0).inStock()).isTrue();
        assertThat(result.get(1).available()).isZero();
        assertThat(result.get(1).inStock()).isFalse();
    }
}
