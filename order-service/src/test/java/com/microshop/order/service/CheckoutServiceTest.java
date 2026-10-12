package com.microshop.order.service;

import com.microshop.common.exception.BadRequestException;
import com.microshop.common.exception.ResourceNotFoundException;
import com.microshop.common.exception.ServiceUnavailableException;
import com.microshop.common.security.AuthenticatedUser;
import com.microshop.events.OrderLine;
import com.microshop.order.client.CartClient;
import com.microshop.order.client.CartItemSnapshot;
import com.microshop.order.client.CartSnapshot;
import com.microshop.order.client.ProductClient;
import com.microshop.order.client.ProductInfo;
import com.microshop.order.dto.CheckoutRequest;
import com.microshop.order.dto.OrderResponse;
import com.microshop.order.dto.ShippingAddressDto;
import com.microshop.order.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock CartClient cartClient;
    @Mock ProductClient productClient;
    @Mock OrderCreationService creation;

    private CheckoutService service;
    private final AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "a@b.com");
    private final UUID productId = UUID.randomUUID();
    private final CheckoutRequest request = new CheckoutRequest(
            new ShippingAddressDto("Jane", "1 Main St", null, "Pune", "411001", "IN"), null);
    private final OrderResponse created = new OrderResponse(UUID.randomUUID(), OrderStatus.PENDING,
            new BigDecimal("20.00"), "USD", List.of(), null, null, List.of(), Instant.now(), Instant.now());

    @BeforeEach
    void setUp() {
        service = new CheckoutService(cartClient, productClient, creation);
    }

    private void cartWithOneItem() {
        when(cartClient.fetchCart()).thenReturn(new CartSnapshot(List.of(new CartItemSnapshot(productId, 2))));
    }

    @Test
    void emptyCartIsRejected() {
        when(cartClient.fetchCart()).thenReturn(new CartSnapshot(List.of()));

        assertThatThrownBy(() -> service.checkout(user, request, null)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(creation);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pricesComeFromProductServiceAndDefaultTokenIsUsed() {
        cartWithOneItem();
        when(productClient.fetch(productId)).thenReturn(new ProductInfo(productId, "Phone", new BigDecimal("10.00")));
        when(creation.create(eq(user), eq(request), any(), eq("tok_success"), isNull())).thenReturn(created);

        OrderResponse res = service.checkout(user, request, null);

        assertThat(res).isSameAs(created);
        ArgumentCaptor<List<OrderLine>> lines = ArgumentCaptor.forClass(List.class);
        verify(creation).create(eq(user), eq(request), lines.capture(), eq("tok_success"), isNull());
        assertThat(lines.getValue()).hasSize(1);
        assertThat(lines.getValue().get(0).unitPrice()).isEqualByComparingTo("10.00");
        assertThat(lines.getValue().get(0).quantity()).isEqualTo(2);
        verify(cartClient).clear();
    }

    @Test
    void failureToClearTheCartDoesNotFailTheCheckout() {
        cartWithOneItem();
        when(productClient.fetch(productId)).thenReturn(new ProductInfo(productId, "Phone", BigDecimal.TEN));
        when(creation.create(any(), any(), any(), any(), any())).thenReturn(created);
        doThrow(new ServiceUnavailableException("cart down")).when(cartClient).clear();

        assertThat(service.checkout(user, request, null)).isSameAs(created);
    }

    @Test
    void productThatNoLongerExistsBlocksCheckoutWithAFriendlyMessage() {
        cartWithOneItem();
        when(productClient.fetch(productId)).thenThrow(new ResourceNotFoundException("Product not found"));

        assertThatThrownBy(() -> service.checkout(user, request, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no longer available");
        verifyNoInteractions(creation);
    }

    @Test
    void retriedRequestWithSameIdempotencyKeyReturnsTheExistingOrder() {
        when(creation.findByIdempotencyKey(user.userId(), "abc-123")).thenReturn(Optional.of(created));

        assertThat(service.checkout(user, request, "abc-123")).isSameAs(created);
        verifyNoInteractions(cartClient, productClient);
        verify(creation, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void malformedPaymentTokenOrIdempotencyKeyIsRejectedBeforeAnyWork() {
        CheckoutRequest badToken = new CheckoutRequest(request.shippingAddress(), "tok bad!");

        assertThatThrownBy(() -> service.checkout(user, badToken, null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.checkout(user, request, "bad key!")).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(cartClient, productClient, creation);
    }
}
