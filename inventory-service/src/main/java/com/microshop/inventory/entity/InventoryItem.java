package com.microshop.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** available = on hand - reserved. The domain methods keep that invariant (the DB also CHECKs it). */
@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public int available() {
        return quantityOnHand - quantityReserved;
    }

    public void reserve(int quantity) {
        if (quantity > available()) {
            throw new IllegalStateException("Cannot reserve " + quantity + "; only " + available() + " available");
        }
        quantityReserved += quantity;
    }

    public void release(int quantity) {
        if (quantity > quantityReserved) {
            throw new IllegalStateException("Cannot release " + quantity + "; only " + quantityReserved + " reserved");
        }
        quantityReserved -= quantity;
    }

    public void commit(int quantity) {
        if (quantity > quantityReserved) {
            throw new IllegalStateException("Cannot commit " + quantity + "; only " + quantityReserved + " reserved");
        }
        quantityReserved -= quantity;
        quantityOnHand -= quantity;
    }
}
