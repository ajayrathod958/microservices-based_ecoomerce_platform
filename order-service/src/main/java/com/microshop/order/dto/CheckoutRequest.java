package com.microshop.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The items come from the user's server-side cart, never from the client, and prices are re-read from
 * product-service, so a tampered request cannot change what is charged.
 * paymentToken drives the mock gateway: tok_success (default), tok_decline, tok_insufficient_funds, tok_gateway_error.
 */
public record CheckoutRequest(
        @NotNull @Valid ShippingAddressDto shippingAddress,
        @Size(max = 64) String paymentToken) {
}
