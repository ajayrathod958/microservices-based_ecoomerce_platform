package com.microshop.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.cache")
public record AppCacheProperties(@DefaultValue("10m") Duration productTtl,
                                 @DefaultValue("2m") Duration pageTtl) {
}
