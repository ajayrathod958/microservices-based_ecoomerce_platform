package com.microshop.inventory.repository;

import com.microshop.inventory.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    /** SELECT ... FOR UPDATE in a deterministic order, so concurrent multi-item reservations cannot deadlock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<InventoryItem> findWithLockByProductIdInOrderByProductIdAsc(Collection<UUID> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItem> findWithLockByProductId(UUID productId);
}
