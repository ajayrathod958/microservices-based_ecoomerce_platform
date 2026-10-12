package com.microshop.cart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.cart")
public record CartProperties(@DefaultValue("1h") Duration cacheTtl) {
}
