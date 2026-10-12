package com.microshop.cart.controller;

import com.microshop.cart.dto.AddItemRequest;
import com.microshop.cart.dto.CartResponse;
import com.microshop.cart.dto.UpdateQuantityRequest;
import com.microshop.cart.service.CartService;
import com.microshop.common.api.ApiResponse;
import com.microshop.common.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** The cart owner is always the authenticated user; there is no way to address another user's cart. */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get my cart")
    public ApiResponse<CartResponse> get(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(cartService.getCart(user.userId()));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product (increments the quantity if it is already in the cart)")
    public ApiResponse<CartResponse> addItem(@AuthenticationPrincipal AuthenticatedUser user,
                                             @Valid @RequestBody AddItemRequest request) {
        return ApiResponse.ok("Item added", cartService.addItem(user.userId(), request));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Set the quantity of an item already in the cart")
    public ApiResponse<CartResponse> updateQuantity(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable UUID productId,
                                                    @Valid @RequestBody UpdateQuantityRequest request) {
        return ApiResponse.ok("Quantity updated", cartService.updateQuantity(user.userId(), productId, request.quantity()));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove an item")
    public ApiResponse<CartResponse> removeItem(@AuthenticationPrincipal AuthenticatedUser user,
                                                @PathVariable UUID productId) {
        return ApiResponse.ok("Item removed", cartService.removeItem(user.userId(), productId));
    }

    @DeleteMapping
    @Operation(summary = "Empty the cart")
    public ApiResponse<CartResponse> clear(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok("Cart cleared", cartService.clear(user.userId()));
    }
}
