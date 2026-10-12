package com.microshop.cart.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/** The few fields of product-service's ProductResponse the cart needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductInfo(UUID id, String name, BigDecimal price, String imageUrl) {
}
