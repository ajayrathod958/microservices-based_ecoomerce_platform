package com.microshop.cart.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Unwraps product-service's ApiResponse envelope. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductEnvelope(ProductInfo data) {
}
