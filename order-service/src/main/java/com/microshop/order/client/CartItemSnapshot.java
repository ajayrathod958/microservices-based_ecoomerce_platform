package com.microshop.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartItemSnapshot(UUID productId, int quantity) {
}
