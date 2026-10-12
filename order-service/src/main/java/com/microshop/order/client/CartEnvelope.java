package com.microshop.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartEnvelope(CartSnapshot data) {
}
