package com.microshop.product.config;

import com.microshop.common.api.PageResponse;
import com.microshop.product.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Proves what we write to Redis reads back identically (records, BigDecimal, Instant, generic page). */
class CacheSerializationTest {

    private final ProductResponse sample = new ProductResponse(
            UUID.randomUUID(), "Phone", "desc", "ELEC-1", new BigDecimal("199.99"), null,
            UUID.randomUUID(), "Electronics",
            Instant.parse("2026-01-02T03:04:05Z"), Instant.parse("2026-02-03T04:05:06Z"));

    @Test
    void productRoundTrips() {
        RedisSerializer<ProductResponse> serializer = CacheConfig.productSerializer();

        byte[] bytes = serializer.serialize(sample);

        assertThat(serializer.deserialize(bytes)).isEqualTo(sample);
        assertThat(new String(bytes)).doesNotContain("@class");   // no polymorphic type info in Redis
    }

    @Test
    void pageRoundTrips() {
        RedisSerializer<PageResponse<ProductResponse>> serializer = CacheConfig.pageSerializer();
        PageResponse<ProductResponse> page = new PageResponse<>(List.of(sample), 0, 12, 1, 1, true);

        PageResponse<ProductResponse> back = serializer.deserialize(serializer.serialize(page));

        assertThat(back).isEqualTo(page);
        assertThat(back.content().get(0)).isInstanceOf(ProductResponse.class);
    }
}
