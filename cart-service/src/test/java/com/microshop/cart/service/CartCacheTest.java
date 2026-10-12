package com.microshop.cart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microshop.cart.config.CartProperties;
import com.microshop.cart.dto.CartItemResponse;
import com.microshop.cart.dto.CartResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartCacheTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> ops;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final UUID userId = UUID.randomUUID();
    private final String key = "cart-service:cart:" + userId;
    private final CartResponse sample = new CartResponse(
            List.of(new CartItemResponse(UUID.randomUUID(), "Phone", null, new BigDecimal("10.00"), 2, new BigDecimal("20.00"))),
            2, new BigDecimal("20.00"), Instant.parse("2026-01-02T03:04:05Z"));

    private CartCache cache;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(ops);
        cache = new CartCache(redis, mapper, new CartProperties(Duration.ofHours(1)));
    }

    @Test
    void putWritesJsonWithTtl() {
        cache.put(userId, sample);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(ops).set(eq(key), json.capture(), eq(Duration.ofHours(1)));
        assertThat(json.getValue()).contains("\"totalItems\":2");
    }

    @Test
    void getRoundTripsWhatWasWritten() throws Exception {
        when(ops.get(key)).thenReturn(mapper.writeValueAsString(sample));

        assertThat(cache.get(userId)).contains(sample);
    }

    @Test
    void getTreatsMissAsEmpty() {
        when(ops.get(key)).thenReturn(null);

        assertThat(cache.get(userId)).isEmpty();
    }

    @Test
    void corruptEntryIsEvictedAndTreatedAsMiss() {
        when(ops.get(key)).thenReturn("{not json");

        assertThat(cache.get(userId)).isEmpty();
        verify(redis).delete(key);
    }

    @Test
    void redisOutageDegradesToMissInsteadOfFailing() {
        when(ops.get(any())).thenThrow(new RuntimeException("connection refused"));

        assertThat(cache.get(userId)).isEmpty();
    }
}
