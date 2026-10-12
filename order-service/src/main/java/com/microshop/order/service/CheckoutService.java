package com.microshop.order.service;

import com.microshop.common.exception.BadRequestException;
import com.microshop.common.exception.ResourceNotFoundException;
import com.microshop.common.security.AuthenticatedUser;
import com.microshop.events.OrderLine;
import com.microshop.order.client.CartClient;
import com.microshop.order.client.CartSnapshot;
import com.microshop.order.client.ProductClient;
import com.microshop.order.client.ProductInfo;
import com.microshop.order.dto.CheckoutRequest;
import com.microshop.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Orchestrates checkout. All remote calls (cart, product) happen BEFORE the database transaction in
 * OrderCreationService, so a slow dependency never holds a DB connection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final Pattern TOKEN = Pattern.compile("^[A-Za-z0-9_]{1,64}$");
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final String DEFAULT_TOKEN = "tok_success";

    private final CartClient cartClient;
    private final ProductClient productClient;
    private final OrderCreationService creation;

    public OrderResponse checkout(AuthenticatedUser user, CheckoutRequest request, String idempotencyKey) {
        String paymentToken = normalizeToken(request.paymentToken());
        String key = normalizeKey(idempotencyKey);

        if (key != null) {
            Optional<OrderResponse> existing = creation.findByIdempotencyKey(user.userId(), key);
            if (existing.isPresent()) {
                return existing.get();          // a retried request: same order, no second charge
            }
        }

        CartSnapshot cart = cartClient.fetchCart();
        if (cart.items().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }
        List<OrderLine> lines = cart.items().stream().map(item -> {
            try {
                ProductInfo product = productClient.fetch(item.productId());    // authoritative price, not the cart's
                return new OrderLine(product.id(), product.name(), item.quantity(), product.price());
            } catch (ResourceNotFoundException e) {
                throw new BadRequestException("A product in your cart is no longer available. Please review your cart.");
            }
        }).toList();

        OrderResponse order;
        try {
            order = creation.create(user, request, lines, paymentToken, key);
        } catch (DataIntegrityViolationException e) {
            // two concurrent requests with the same Idempotency-Key: the loser returns the winner's order
            if (key != null) {
                return creation.findByIdempotencyKey(user.userId(), key).orElseThrow(() -> e);
            }
            throw e;
        }

        try {
            cartClient.clear();
        } catch (RuntimeException e) {
            // The order exists and is already on its way; a leftover cart is an inconvenience, not a failure.
            log.warn("Order {} created but the cart could not be cleared: {}", order.id(), e.getMessage());
        }
        return order;
    }

    private static String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            return DEFAULT_TOKEN;
        }
        if (!TOKEN.matcher(token).matches()) {
            throw new BadRequestException("Invalid payment token");
        }
        return token;
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (!IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new BadRequestException("Idempotency-Key must be 1-64 characters: letters, digits, '-' or '_'");
        }
        return key;
    }
}
