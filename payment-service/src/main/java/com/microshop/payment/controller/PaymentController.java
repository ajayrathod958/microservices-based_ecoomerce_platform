package com.microshop.payment.controller;

import com.microshop.common.api.ApiResponse;
import com.microshop.common.exception.ResourceNotFoundException;
import com.microshop.common.security.AuthenticatedUser;
import com.microshop.payment.dto.PaymentResponse;
import com.microshop.payment.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Read-only: payments are created exclusively by the saga, never by a client call. */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentRepository payments;

    @GetMapping("/order/{orderId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Payment record for one of my orders (owner or admin)")
    public ApiResponse<PaymentResponse> byOrder(@AuthenticationPrincipal AuthenticatedUser user,
                                                Authentication authentication,
                                                @PathVariable UUID orderId) {
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ApiResponse.ok(payments.findByOrderId(orderId)
                .filter(p -> admin || p.getUserId().equals(user.userId()))
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found")));
    }
}
