package com.microshop.cart.service;

import com.microshop.cart.dto.CartResponse;
import com.microshop.cart.entity.CartEntity;
import com.microshop.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/** The PostgreSQL side: short, self-contained transactions. Optimistic-lock failures surface on commit. */
@Component
@RequiredArgsConstructor
public class CartPersistence {

    private final CartRepository carts;

    @Transactional(readOnly = true)
    public Optional<CartResponse> find(UUID userId) {
        return carts.findById(userId).map(CartResponse::from);
    }

    /** Loads (or creates) the cart, applies the change, saves, and returns the resulting view. */
    @Transactional
    public CartResponse mutate(UUID userId, Consumer<CartEntity> change) {
        CartEntity cart = carts.findById(userId).orElseGet(() -> CartEntity.newFor(userId));
        change.accept(cart);
        return CartResponse.from(carts.save(cart));
    }
}
