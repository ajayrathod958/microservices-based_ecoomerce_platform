package com.microshop.order.repository;

import com.microshop.order.entity.CustomerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    Page<CustomerOrder> findByUserId(UUID userId, Pageable pageable);

    Optional<CustomerOrder> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
