package com.microshop.inventory.controller;

import com.microshop.common.api.ApiResponse;
import com.microshop.common.api.PageResponse;
import com.microshop.common.exception.BadRequestException;
import com.microshop.inventory.dto.AdjustStockRequest;
import com.microshop.inventory.dto.AvailabilityResponse;
import com.microshop.inventory.dto.InventoryResponse;
import com.microshop.inventory.dto.SetStockRequest;
import com.microshop.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory")
public class InventoryController {

    private static final int MAX_AVAILABILITY_IDS = 50;

    private final InventoryService inventoryService;

    @GetMapping("/availability")
    @Operation(summary = "Public: is this product in stock?",
            description = "Pass up to 50 comma-separated product ids: ?productIds=id1,id2")
    public ApiResponse<List<AvailabilityResponse>> availability(@RequestParam List<UUID> productIds) {
        if (productIds.size() > MAX_AVAILABILITY_IDS) {
            throw new BadRequestException("At most " + MAX_AVAILABILITY_IDS + " product ids per request");
        }
        return ApiResponse.ok(inventoryService.availability(productIds));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List stock levels (admin)")
    public ApiResponse<PageResponse<InventoryResponse>> list(
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(inventoryService.list(pageable));
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Stock detail for one product (admin)")
    public ApiResponse<InventoryResponse> get(@PathVariable UUID productId) {
        return ApiResponse.ok(inventoryService.get(productId));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Set quantity on hand; creates the record if missing (admin)")
    public ApiResponse<InventoryResponse> setStock(@PathVariable UUID productId,
                                                   @Valid @RequestBody SetStockRequest request) {
        return ApiResponse.ok("Stock updated", inventoryService.setStock(productId, request.quantityOnHand()));
    }

    @PatchMapping("/{productId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add or remove stock by a delta, e.g. +25 / -3 (admin)")
    public ApiResponse<InventoryResponse> adjust(@PathVariable UUID productId,
                                                 @Valid @RequestBody AdjustStockRequest request) {
        return ApiResponse.ok("Stock adjusted", inventoryService.adjust(productId, request.delta()));
    }
}
