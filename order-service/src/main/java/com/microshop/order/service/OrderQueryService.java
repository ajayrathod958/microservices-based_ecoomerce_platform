package com.microshop.order.service;

import com.microshop.common.api.PageResponse;
import com.microshop.common.exception.BadRequestException;
import com.microshop.common.exception.ResourceNotFoundException;
import com.microshop.order.dto.OrderResponse;
import com.microshop.order.dto.OrderSummary;
import com.microshop.order.repository.CustomerOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private static final Set<String> SORTABLE = Set.of("createdAt", "totalAmount", "status");

    private final CustomerOrderRepository orders;

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> listMine(UUID userId, Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty()
                        + "'. Allowed: " + String.join(", ", SORTABLE));
            }
        }
        return PageResponse.from(orders.findByUserId(userId, pageable).map(OrderSummary::from));
    }

    /** Owners see their own orders, admins see all. Someone else's order is a 404, not a 403 (no existence leak). */
    @Transactional(readOnly = true)
    public OrderResponse get(UUID orderId, UUID requesterId, boolean admin) {
        return orders.findById(orderId)
                .filter(o -> admin || o.getUserId().equals(requesterId))
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }
}
