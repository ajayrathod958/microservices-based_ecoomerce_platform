package com.microshop.cart.service;

import com.microshop.cart.client.ProductClient;
import com.microshop.cart.client.ProductInfo;
import com.microshop.cart.dto.AddItemRequest;
import com.microshop.cart.dto.CartItemResponse;
import com.microshop.cart.dto.CartResponse;
import com.microshop.common.exception.ConflictException;
import com.microshop.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartPersistence persistence;
    @Mock CartCache cache;
    @Mock ProductClient productClient;

    private CartService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final CartResponse sample = new CartResponse(
            List.of(new CartItemResponse(productId, "Phone", null, new BigDecimal("10.00"), 2, new BigDecimal("20.00"))),
            2, new BigDecimal("20.00"), Instant.now());

    @BeforeEach
    void setUp() {
        service = new CartService(persistence, cache, productClient);
    }

    @Test
    void getCartServesFromRedisWithoutTouchingTheDatabase() {
        when(cache.get(userId)).thenReturn(Optional.of(sample));

        assertThat(service.getCart(userId)).isSameAs(sample);
        verifyNoInteractions(persistence);
    }

    @Test
    void getCartFallsBackToDatabaseAndRepopulatesRedis() {
        when(cache.get(userId)).thenReturn(Optional.empty());
        when(persistence.find(userId)).thenReturn(Optional.of(sample));

        assertThat(service.getCart(userId)).isSameAs(sample);
        verify(cache).put(userId, sample);
    }

    @Test
    void getCartReturnsEmptyCartForNewUserWithoutCachingIt() {
        when(cache.get(userId)).thenReturn(Optional.empty());
        when(persistence.find(userId)).thenReturn(Optional.empty());

        CartResponse res = service.getCart(userId);

        assertThat(res.items()).isEmpty();
        assertThat(res.subtotal()).isEqualByComparingTo("0");
        verify(cache, never()).put(any(), any());
    }

    @Test
    void addItemLooksUpProductThenPersistsAndWritesThrough() {
        when(productClient.fetch(productId))
                .thenReturn(new ProductInfo(productId, "Phone", new BigDecimal("10.00"), null));
        when(persistence.mutate(eq(userId), any())).thenReturn(sample);

        CartResponse res = service.addItem(userId, new AddItemRequest(productId, 2));

        assertThat(res).isSameAs(sample);
        verify(cache).put(userId, sample);
    }

    @Test
    void addItemFailsFastWhenProductDoesNotExist() {
        when(productClient.fetch(productId)).thenThrow(new ResourceNotFoundException("Product not found"));

        assertThatThrownBy(() -> service.addItem(userId, new AddItemRequest(productId, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(persistence);
        verifyNoInteractions(cache);
    }

    @Test
    void concurrentModificationIsRetried() {
        when(persistence.mutate(eq(userId), any()))
                .thenThrow(new OptimisticLockingFailureException("stale"))
                .thenReturn(sample);

        CartResponse res = service.removeItem(userId, productId);

        assertThat(res).isSameAs(sample);
        verify(persistence, times(2)).mutate(eq(userId), any());
        verify(cache).put(userId, sample);
    }

    @Test
    void givesUpWithConflictAfterThreeAttempts() {
        when(persistence.mutate(eq(userId), any())).thenThrow(new OptimisticLockingFailureException("stale"));

        assertThatThrownBy(() -> service.clear(userId)).isInstanceOf(ConflictException.class);
        verify(persistence, times(3)).mutate(eq(userId), any());
        verify(cache, never()).put(any(), any());
    }
}
