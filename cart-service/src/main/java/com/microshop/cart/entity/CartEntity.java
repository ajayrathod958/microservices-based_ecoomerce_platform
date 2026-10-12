package com.microshop.cart.entity;

import com.microshop.common.exception.BadRequestException;
import com.microshop.common.exception.ResourceNotFoundException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One cart per user (the user id is the primary key). Every mutation calls touch(), which dirties the row so
 * the @Version column increments: two concurrent modifications of the same cart cannot silently overwrite each other.
 */
@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor
public class CartEntity {

    public static final int MAX_QUANTITY = 99;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Version
    private Long version;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("addedAt ASC")
    private List<CartItemEntity> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static CartEntity newFor(UUID userId) {
        CartEntity cart = new CartEntity();
        cart.userId = userId;
        cart.updatedAt = Instant.now();
        return cart;
    }

    public void addOrIncrease(UUID productId, String name, String imageUrl, BigDecimal unitPrice, int quantity) {
        CartItemEntity existing = find(productId);
        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + quantity;
        requireValidQuantity(newQuantity);

        if (existing == null) {
            CartItemEntity item = new CartItemEntity();
            item.setCart(this);
            item.setProductId(productId);
            item.setProductName(name);
            item.setImageUrl(imageUrl);
            item.setUnitPrice(unitPrice);
            item.setQuantity(newQuantity);
            items.add(item);
        } else {
            existing.setQuantity(newQuantity);
            existing.setProductName(name);          // refresh the snapshot while we have fresh data
            existing.setImageUrl(imageUrl);
            existing.setUnitPrice(unitPrice);
        }
        touch();
    }

    public void setQuantity(UUID productId, int quantity) {
        requireValidQuantity(quantity);
        CartItemEntity item = find(productId);
        if (item == null) {
            throw new ResourceNotFoundException("Item is not in the cart");
        }
        item.setQuantity(quantity);
        touch();
    }

    /** Idempotent: removing an item that is not there is not an error. */
    public void removeItem(UUID productId) {
        if (items.removeIf(i -> i.getProductId().equals(productId))) {
            touch();
        }
    }

    public void clear() {
        items.clear();
        touch();
    }

    private CartItemEntity find(UUID productId) {
        return items.stream().filter(i -> i.getProductId().equals(productId)).findFirst().orElse(null);
    }

    private void requireValidQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new BadRequestException("Quantity per item must be between 1 and " + MAX_QUANTITY);
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
