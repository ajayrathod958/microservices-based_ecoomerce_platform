package com.microshop.inventory.repository;

import com.microshop.inventory.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    boolean existsByOrderId(UUID orderId);

    List<StockReservation> findByOrderId(UUID orderId);
}
