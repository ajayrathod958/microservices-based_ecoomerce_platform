package com.microshop.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartSnapshot(List<CartItemSnapshot> items) {
}
