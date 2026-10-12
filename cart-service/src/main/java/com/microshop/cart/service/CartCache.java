package com.microshop.cart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microshop.cart.config.CartProperties;
import com.microshop.cart.dto.CartResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Redis is the fast read path. Every failure degrades to "cache miss"; PostgreSQL stays the source of truth. */
@Slf4j
@Component
public class CartCache {

    private static final String PREFIX = "cart-service:cart:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration ttl;

    public CartCache(StringRedisTemplate redis, ObjectMapper mapper, CartProperties props) {
        this.redis = redis;
        this.mapper = mapper;
        this.ttl = props.cacheTtl();
    }

    public Optional<CartResponse> get(UUID userId) {
        try {
            String json = redis.opsForValue().get(key(userId));
            return json == null ? Optional.empty() : Optional.of(mapper.readValue(json, CartResponse.class));
        } catch (JsonProcessingException e) {
            log.warn("Discarding unreadable cached cart for {}: {}", userId, e.getMessage());
            evict(userId);
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("Redis read failed for cart {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(UUID userId, CartResponse cart) {
        try {
            redis.opsForValue().set(key(userId), mapper.writeValueAsString(cart), ttl);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Redis write failed for cart {}: {}", userId, e.getMessage());
        }
    }

    public void evict(UUID userId) {
        try {
            redis.delete(key(userId));
        } catch (RuntimeException e) {
            log.warn("Redis delete failed for cart {}: {}", userId, e.getMessage());
        }
    }

    private static String key(UUID userId) {
        return PREFIX + userId;
    }
}
