package com.microshop.cart.service;

import com.microshop.cart.client.ProductClient;
import com.microshop.cart.client.ProductInfo;
import com.microshop.cart.dto.AddItemRequest;
import com.microshop.cart.dto.CartResponse;
import com.microshop.cart.entity.CartEntity;
import com.microshop.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Reads: Redis first, then PostgreSQL (and re-populate Redis).
 * Writes: always against PostgreSQL (authoritative), then write-through to Redis.
 * Not @Transactional itself: each attempt is its own short transaction inside CartPersistence, so the
 * optimistic-lock retry loop sits outside it and every retry starts from freshly loaded data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private static final int MAX_ATTEMPTS = 3;

    private final CartPersistence persistence;
    private final CartCache cache;
    private final ProductClient productClient;

    public CartResponse getCart(UUID userId) {
        return cache.get(userId).orElseGet(() -> persistence.find(userId)
                .map(cart -> {
                    cache.put(userId, cart);
                    return cart;
                })
                .orElseGet(CartResponse::empty));
    }

    public CartResponse addItem(UUID userId, AddItemRequest request) {
        // Remote call happens BEFORE any DB transaction so a slow product-service never holds a connection.
        ProductInfo product = productClient.fetch(request.productId());
        return mutate(userId, cart -> cart.addOrIncrease(
                product.id(), product.name(), product.imageUrl(), product.price(), request.quantity()));
    }

    public CartResponse updateQuantity(UUID userId, UUID productId, int quantity) {
        return mutate(userId, cart -> cart.setQuantity(productId, quantity));
    }

    public CartResponse removeItem(UUID userId, UUID productId) {
        return mutate(userId, cart -> cart.removeItem(productId));
    }

    public CartResponse clear(UUID userId) {
        return mutate(userId, CartEntity::clear);
    }

    private CartResponse mutate(UUID userId, Consumer<CartEntity> change) {
        for (int attempt = 1; ; attempt++) {
            try {
                CartResponse updated = persistence.mutate(userId, change);
                cache.put(userId, updated);
                return updated;
            } catch (OptimisticLockingFailureException | DataIntegrityViolationException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new ConflictException("Your cart was changed by another request. Please try again.");
                }
                log.debug("Concurrent cart update for {} (attempt {}), retrying", userId, attempt);
            }
        }
    }
}
