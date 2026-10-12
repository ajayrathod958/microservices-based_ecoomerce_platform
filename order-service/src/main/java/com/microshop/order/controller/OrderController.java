package com.microshop.order.controller;

import com.microshop.common.api.ApiResponse;
import com.microshop.common.api.PageResponse;
import com.microshop.common.security.AuthenticatedUser;
import com.microshop.order.dto.CheckoutRequest;
import com.microshop.order.dto.OrderResponse;
import com.microshop.order.dto.OrderSummary;
import com.microshop.order.service.CheckoutService;
import com.microshop.order.service.OrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders")
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderQueryService queryService;

    @PostMapping
    @Operation(summary = "Checkout: turn my cart into an order",
            description = "Returns 201 with status PENDING; the order is then confirmed or cancelled asynchronously "
                    + "(poll GET /api/orders/{id}). Send an Idempotency-Key header so retries never create a second order.")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed", checkoutService.checkout(user, request, idempotencyKey)));
    }

    @GetMapping
    @Operation(summary = "My order history (newest first)")
    public ApiResponse<PageResponse<OrderSummary>> history(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(hidden = true)
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(queryService.listMine(user.userId(), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Order details with the status timeline (owner or admin)")
    public ApiResponse<OrderResponse> get(@AuthenticationPrincipal AuthenticatedUser user,
                                          Authentication authentication,
                                          @PathVariable UUID id) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ApiResponse.ok(queryService.get(id, user.userId(), admin));
    }
}
