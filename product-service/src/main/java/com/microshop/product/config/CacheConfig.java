package com.microshop.product.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microshop.common.api.PageResponse;
import com.microshop.product.dto.ProductResponse;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis-backed caches with TTLs and typed JSON values (no polymorphic typing, so no class names are
 * written into Redis and no deserialisation gadgets are possible).
 *
 * order = LOWEST_PRECEDENCE - 1 puts the cache interceptor OUTSIDE the @Transactional one, so
 * evictions happen after the transaction commits (no window where a reader re-caches stale data)
 * and cache hits never open a DB transaction.
 */
@Configuration
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
public class CacheConfig {

    public static final String PRODUCT = "product";
    public static final String PRODUCT_PAGE = "productPage";

    static ObjectMapper redisObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    static RedisSerializer<ProductResponse> productSerializer() {
        ObjectMapper mapper = redisObjectMapper();
        JavaType type = mapper.constructType(ProductResponse.class);
        return new Jackson2JsonRedisSerializer<ProductResponse>(mapper, type);
    }

    static RedisSerializer<PageResponse<ProductResponse>> pageSerializer() {
        ObjectMapper mapper = redisObjectMapper();
        JavaType type = mapper.getTypeFactory().constructParametricType(PageResponse.class, ProductResponse.class);
        return new Jackson2JsonRedisSerializer<PageResponse<ProductResponse>>(mapper, type);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, AppCacheProperties props) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .prefixCacheNameWith("product-service:")
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(props.pageTtl()))
                .withCacheConfiguration(PRODUCT, base
                        .entryTtl(props.productTtl())
                        .serializeValuesWith(SerializationPair.fromSerializer(productSerializer())))
                .withCacheConfiguration(PRODUCT_PAGE, base
                        .entryTtl(props.pageTtl())
                        .serializeValuesWith(SerializationPair.fromSerializer(pageSerializer())))
                .build();
    }
}
